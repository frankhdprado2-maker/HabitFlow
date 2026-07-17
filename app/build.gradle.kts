import java.util.Properties
import java.security.MessageDigest
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProps = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use(::load)
    }
}

data class WhisperModelSpec(val fileName: String, val sha256: String, val sizeBytes: Long)

val whisperModels = mapOf(
    "base-q5_1" to WhisperModelSpec(
        "ggml-base-q5_1.bin",
        "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
        59_707_625L
    ),
    "tiny-q5_1" to WhisperModelSpec(
        "ggml-tiny-q5_1.bin",
        "818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7",
        32_152_673L
    )
)
val whisperModelId = providers.gradleProperty("WHISPER_MODEL").getOrElse("base-q5_1")
val whisperModel = whisperModels[whisperModelId]
    ?: error("WHISPER_MODEL must be one of: ${whisperModels.keys.joinToString()}")
val whisperModelRevision = "5359861c739e955e79d9a303bcbc70fb988958b1"
val generatedWhisperAssets = layout.buildDirectory.dir("generated/whisperAssets/main")

val prepareWhisperModel by tasks.registering {
    group = "whisper"
    description = "Downloads and verifies the selected local Whisper model."
    val outputFile = generatedWhisperAssets.map { it.file("whisper/${whisperModel.fileName}") }
    outputs.file(outputFile)
    doLast {
        val destination = outputFile.get().asFile
        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
        if (destination.isFile && destination.length() == whisperModel.sizeBytes &&
            sha256(destination).equals(whisperModel.sha256, ignoreCase = true)
        ) {
            logger.lifecycle("Whisper model already verified: ${destination.name}")
            return@doLast
        }
        destination.delete()
        destination.parentFile.mkdirs()
        val partial = File(destination.parentFile, "${destination.name}.part")
        partial.delete()
        val url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/$whisperModelRevision/${whisperModel.fileName}"
        try {
            logger.lifecycle("Downloading ${whisperModel.fileName} from the official whisper.cpp model repository")
            URI(url).toURL().openConnection().apply {
                connectTimeout = 30_000
                readTimeout = 120_000
            }.getInputStream().buffered().use { input ->
                partial.outputStream().buffered().use(input::copyTo)
            }
            val actualHash = sha256(partial)
            check(partial.length() == whisperModel.sizeBytes && actualHash.equals(whisperModel.sha256, true)) {
                "Downloaded Whisper model failed verification (size=${partial.length()}, sha256=$actualHash)."
            }
            check(partial.renameTo(destination)) { "Could not move verified Whisper model into generated assets." }
        } catch (error: Exception) {
            partial.delete()
            throw GradleException("Failed to prepare the local Whisper model: ${error.message}", error)
        }
    }
}

android {
    namespace = "com.unmsm.habitflow"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.unmsm.habitflow"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        buildConfigField("String", "WHISPER_MODEL_FILE", "\"${whisperModel.fileName}\"")
        buildConfigField("String", "WHISPER_MODEL_SHA256", "\"${whisperModel.sha256}\"")
        buildConfigField("long", "WHISPER_MODEL_SIZE", "${whisperModel.sizeBytes}L")
        buildConfigField(
            "String",
            "BASE_URL",
            "\"${localProps.getProperty("BASE_URL", "http://10.0.2.2:8000/c21200065/")}\""
        )
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\""
        )
    }
    ndkVersion = "28.2.13676358"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    sourceSets.getByName("main").assets.directories.add(generatedWhisperAssets.get().asFile.absolutePath)
}

tasks.named("preBuild").configure { dependsOn(prepareWhisperModel) }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.hilt.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.error.prone.annotations)
    ksp(libs.androidx.room.compiler)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.codegen)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
