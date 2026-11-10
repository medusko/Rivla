# Changelog

All notable changes to **Rivla** for Android are documented in this file.

## [v1.0.0-Beta] - 2026-11-09

### Added
- **BYOK On-Device Encrypted Vault**: Local storage of Rivian GraphQL sessions, Tesla Fleet BYOK / Tessie / Teslemetry tokens, Powerwall Local LAN Gateway credentials, and Home Assistant Long-Lived Access Tokens using `EncryptedSharedPreferences`.
- **Three-Tab Navigation (`Status`, `Setup`, `Settings`)**:
  - **Status Tab**: Interactive 4x2 widget preview (2x2 grid for up to 4 products), quick vault status, and per-slot telemetry cards.
  - **Setup Tab**: Full authentication & BYOK key collection forms with MFA/OTP support for Rivian and 4-slot widget editor.
  - **Settings Tab**: Phantom-drain vehicle sleep protection toggle, polling interval controls, Imperial/Metric range units, Midnight Navy Dark Mode & Warm Linen Light Mode, and BYOK API caveats guide.
- **4x2 Home Screen & Lock Screen Widget (`RivlaEnergyWidgetProvider`)**: Supports both `home_screen` and `keyguard` widget categories with a 2x2 product grid and one-tap `↻ Sync`.
- **Medusko Identity & About Dialog**: Custom Blue & Orange/Gold Medusko icon (`BETA` badge) and `AboutRivlaDialog` in the top bar.
