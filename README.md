# Devils Monitor

Android monitoring system with Telegram bot control.

## Components

- **Android App** — the client APK
- **Telegram Bot** — control panel
- **Firebase** — data relay

## Setup

### Firebase
Create project at https://console.firebase.google.com
- Enable Realtime Database
- Download `google-services.json` → put in `app/`
- Rules: `.read: true, .write: true` (test mode)

### GitHub Actions
The APK builds automatically on every push.

### Bot
Environment variables:
- `BOT_TOKEN` — from BotFather
- `FIREBASE_URL` — your database URL
- `ADMIN_ID` — your Telegram ID
