package com.unmsm.habitflow.di

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.unmsm.habitflow.BuildConfig
import com.unmsm.habitflow.data.local.HabitFlowDatabase
import com.unmsm.habitflow.data.remote.AuthInterceptor
import com.unmsm.habitflow.data.remote.TokenAuthenticator
import com.unmsm.habitflow.data.remote.api.AuthApi
import com.unmsm.habitflow.data.remote.api.HabitEventApi
import com.unmsm.habitflow.data.remote.api.StorageApi
import com.unmsm.habitflow.data.remote.api.VoiceApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Named("baseUrl")
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .authenticator(tokenAuthenticator)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        @Named("baseUrl") baseUrl: String,
        moshi: Moshi,
        okHttpClient: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    fun provideHabitEventApi(retrofit: Retrofit): HabitEventApi = retrofit.create(HabitEventApi::class.java)

    @Provides
    fun provideStorageApi(retrofit: Retrofit): StorageApi = retrofit.create(StorageApi::class.java)

    @Provides
    fun provideVoiceApi(retrofit: Retrofit): VoiceApi = retrofit.create(VoiceApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HabitFlowDatabase =
        Room.databaseBuilder(context, HabitFlowDatabase::class.java, "habitflow.db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideHabitDao(database: HabitFlowDatabase) = database.habitDao()

    @Provides
    fun provideHabitEventDao(database: HabitFlowDatabase) = database.habitEventDao()

    @Provides
    fun provideAchievementDao(database: HabitFlowDatabase) = database.achievementDao()

    @Provides
    fun provideNotificationDao(database: HabitFlowDatabase) = database.notificationDao()
}
