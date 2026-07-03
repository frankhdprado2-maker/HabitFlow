import aioboto3

from app.projects.c21200065.infra.settings import settings


class StorageClient:

    def __init__(self) -> None:
        self._session = aioboto3.Session()
        self._bucket = settings.OBJECT_STORAGE_BUCKET
        self._client_kwargs = {
            "endpoint_url": settings.OBJECT_STORAGE_ENDPOINT,
            "aws_access_key_id": settings.OBJECT_STORAGE_ACCESS_KEY,
            "aws_secret_access_key": settings.OBJECT_STORAGE_SECRET_KEY,
            "region_name": settings.OBJECT_STORAGE_REGION,
        }

    async def upload(self, key: str, data: bytes, content_type: str = "application/octet-stream") -> str:
        async with self._session.client("s3", **self._client_kwargs) as s3:
            await s3.put_object(Bucket=self._bucket, Key=key, Body=data, ContentType=content_type)
        return key

    async def download(self, key: str) -> bytes:
        async with self._session.client("s3", **self._client_kwargs) as s3:
            response = await s3.get_object(Bucket=self._bucket, Key=key)
            return await response["Body"].read()

    async def delete(self, key: str) -> None:
        async with self._session.client("s3", **self._client_kwargs) as s3:
            await s3.delete_object(Bucket=self._bucket, Key=key)

    async def get_upload_url(self, key: str, content_type: str, expires_in: int = 3600) -> str:
        async with self._session.client("s3", **self._client_kwargs) as s3:
            return await s3.generate_presigned_url(
                "put_object",
                Params={"Bucket": self._bucket, "Key": key, "ContentType": content_type},
                ExpiresIn=expires_in,
            )

    async def get_url(self, key: str, expires_in: int = 3600) -> str:
        async with self._session.client("s3", **self._client_kwargs) as s3:
            return await s3.generate_presigned_url(
                "get_object",
                Params={"Bucket": self._bucket, "Key": key},
                ExpiresIn=expires_in,
            )


storage_client = StorageClient()
