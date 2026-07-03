class AuthError(Exception):
    pass


class InvalidCredentialsError(AuthError):
    pass


class UserAlreadyExistsError(AuthError):
    pass


class InvalidGoogleTokenError(AuthError):
    pass


class InvalidRefreshTokenError(AuthError):
    pass


class StorageError(Exception):
    pass


class StorageFileNotFoundError(StorageError):
    pass


class StorageAccessError(StorageError):
    pass
