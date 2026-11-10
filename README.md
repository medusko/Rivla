# Rivla (`com.medusko.rivla`)

**BYOK EV & Home Energy Telemetry Widget Hub for Android**  
*Designed & Engineered by [Medusko](https://medusko.com)*

Rivla aggregates State of Charge (SoC), estimated range, and solar/backup power across **Rivian vehicles**, **Tesla vehicles**, and **Tesla Powerwalls** into a unified **4x2 Home Screen & Lock Screen Widget** rendered in a **2x2 grid (up to 4 products)**.

---

## Key Highlights

- **100% Client-Only BYOK (Bring Your Own Key) Vault**: All credentials, OAuth tokens, and session cookies are stored locally on your device inside Android Keystore (`EncryptedSharedPreferences`). No external backend server ever touches your secrets.
- **4 Supported Telemetry Integrations**:
  1. **Rivian Direct GraphQL (`$0 Cost`)**: Authenticates directly with `rivian.com/api/gql/gateway/graphql` (`CreateCSRFToken` -> `Login` -> `LoginWithOTP` -> `GetVehicleState`) or accepts existing session tokens.
  2. **Tesla Fleet API BYOK & Tessie / Teslemetry Proxy Keys**: Use your own Tesla Developer `client_id` & Bearer token (covered by Tesla's recurring `$10/month` personal credit with `$0` default billing cap) or paste a Tessie / Teslemetry personal token.
  3. **Tesla Powerwall Local LAN Gateway (`$0 Cost`)**: Direct Wi-Fi LAN connection to `https://<gateway-ip>/api/login/Basic` and `/api/system_status/soe` using your physical gateway customer password.
  4. **Home Assistant Long-Lived Access Token**: Query up to 4 `sensor.*` entities from your self-hosted Home Assistant instance.
- **Phantom-Drain Safe Polling**: Checks vehicle sleep state (`/api/1/vehicles`) before requesting telemetry so sleeping EVs stay in deep sleep without 1–3%/day phantom battery drain.
- **4x2 Lock Screen & Home Screen Widget**: Displays up to 4 products in a 2x2 grid with live progress bars, range (`mi`/`km`), kW flow, and one-tap `↻ Sync`.
- **Three-Tab Medusko Navigation**:
  - **Status**: Live 4x2 widget 2x2 grid preview, vault summary, and interactive product cards.
  - **Setup**: Local credential & BYOK session configuration + 4-slot customizer.
  - **Settings**: Vehicle sleep/wake protection, polling intervals, units, dark/light theme, and the complete BYOK API caveats guide.

---

## Build & Install

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
./gradlew testDebugUnitTest assembleDebug assembleRelease
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
