# Auth OpenSpec

## Token Model

- Access tokens are JWT Bearer tokens.
- Access token TTL defaults to 15 minutes.
- Refresh token TTL defaults to 30 days.
- Refresh tokens are stored server-side as hashes.
- Refresh token rotation invalidates the previous token.

## Login Methods

- Email + password.
- Phone + password.
- WeChat mini-program `code` login.

## Client Headers

```http
X-Client-Type: web | mini-program | ios | android
X-Client-Version: 1.0.0
X-Device-Id: client-generated-device-id
Authorization: Bearer <access-token>
```
