import datetime
import hashlib
import hmac
import os
import secrets

_ITERATIONS = 600_000
_HASH = "sha256"
_SALT_BYTES = 16


def hash_password(password: str) -> str:
    salt = os.urandom(_SALT_BYTES)
    key = hashlib.pbkdf2_hmac(_HASH, password.encode(), salt, _ITERATIONS)
    return f"{_ITERATIONS}:{salt.hex()}:{key.hex()}"


def verify_password(password: str, hashed: str) -> bool:
    try:
        iterations_str, salt_hex, key_hex = hashed.split(":")
        salt = bytes.fromhex(salt_hex)
        expected = bytes.fromhex(key_hex)
        actual = hashlib.pbkdf2_hmac(_HASH, password.encode(), salt, int(iterations_str))
        return hmac.compare_digest(actual, expected)
    except Exception:
        return False


def generate_refresh_token_value(expire_days: int) -> tuple[str, datetime.datetime]:
    token = secrets.token_urlsafe(32)
    expires_at = datetime.datetime.now(datetime.UTC) + datetime.timedelta(days=expire_days)
    return token, expires_at
