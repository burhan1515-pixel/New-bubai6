# NEW DUBAI — Atelier Signature 1.5.1

A Kotlin + Jetpack Compose offline-first tailoring and fabric shop management app.

## 1.5.1 refinements
- New atelier visual identity using oxblood, warm ivory and brushed copper, with animated shears and moving stitch details.
- Language selection is located inside Settings; the login screen is kept focused on secure owner access.
- Measurement values are stored canonically in centimetres. Each saved measurement card now has its own cm/in display switch, and forms retain both unit options without changing the stored value.
- Afghan mobile validation is strict: exactly ten digits beginning with `07`. Persian/Arabic numerals are normalized to western digits and duplicate numbers are checked after normalization.
- Clothing catalogue now has working country/region filters for Afghan, Indian, Pakistani, Chinese, Arab, Iranian, Korean, Japanese, Turkish, Western/European and general styles, including children's garments.
- Added unit and catalogue regression tests.
- Strengthened validation for measurements, dates, and finite monetary/stock values.
- Restore validates backup references before replacing data, saves a pre-restore backup, and restores in dependency-safe order.

## Existing modules
- Owner PIN/password login and optional biometric unlock.
- Customer profiles, tailoring orders, measurement history.
- Clothing catalogue, fabric inventory, purchases, sales and low-stock alerts.
- Cash book, expenses, debts, partial payments and reports.
- Local backup/restore and optional Google Drive backup.
- Pashto, Dari and English.
- Offline-first Room database.

## 1.5.1 improvements

- Premium burgundy, brushed-gold and warm-ivory visual system.
- Custom garment names can be typed instead of being limited to the preset list.
- Corrected order validation for customer-provided fabric (`own`) and custom garment names.
- Android Back button and visible in-app back navigation, with exit confirmation on the dashboard.
- Scrollable navigation drawer and keyboard-aware form sheets with focused-field scrolling.
- Shareable PDF order receipts with customer, dates, totals, payments and balance.
- Encrypted backup sharing, alongside local export and restore.
- Delivery-day reminders in addition to overdue and upcoming-order alerts.
- Customer profile now summarizes order totals, payments and outstanding balance.

## Build
1. Upload the contents of this ZIP to the GitHub repository root (not inside an extra folder).
2. Confirm `.github/workflows/build.yml` is present. The workflow installs Gradle 8.7 directly, so a checked-in Gradle wrapper is not required.
3. Open Actions → `Build NEW DUBAI APK` → Run workflow.
4. Download `NEW-DUBAI-APK-v1.5.1` after the workflow succeeds.

The source changes have been made and archive integrity can be checked. This environment does not include Gradle or the Android SDK, so a local Android compilation could not be run. Treat the APK as verified only after GitHub Actions completes successfully.

## Data safety
Back up the app before installing a new APK or restoring a backup. The database schema is unchanged in this release, so existing Room data should remain in place.
