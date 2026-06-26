# LG_CARRY
## Local configuration

This repository does not commit Firebase app configuration or local API endpoints.

1. Copy `local.properties.example` to `local.properties`.
2. Set `VOICE_INTENT_API_URLS` and `MISSION_API_BASE_URLS` with comma-separated local or deployed endpoints.
3. Copy `app/google-services.example.json` to `app/google-services.json` and fill it from the Firebase console.

`local.properties` and `app/google-services.json` are intentionally ignored by Git.