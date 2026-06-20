# Google Play Billing – Einrichtung (Premium-Paywall)

Die App blockt ab **Level 2** (`ScoreRules.level(raw) > 1`, also Roh-Score ≥ 1000) komplett,
bis die **Premium-Freischaltung** per einmaligem In-App-Kauf erworben wurde.

Code-Bestandteile:
- `billing/BillingManager.kt` – BillingClient (Verbindung, Preis, Kauf, Entitlement, Acknowledge).
- `ui/paywall/PaywallScreen.kt` – `PaywallGate` (Blockier-Logik) + `PaywallScreen` (UI).
- Eingehängt in `MainActivity` zwischen `PermissionGate` und `WellbeingNav`.
- Start in `WellbeingApplication.onCreate()` via `container.billingManager.start()`.

## Damit der Kauf real funktioniert

Google Play Billing lässt sich **nicht im Emulator/Debug ohne Play** testen – es braucht die
Play Console und einen über Play installierten Build.

1. **Play Console** (einmalig 25 $ Entwicklerkonto): App `com.project.wellbeingapp` anlegen.
2. **In-App-Produkt** anlegen → *Monetarisierung → Produkte → In-App-Produkte*:
   - Produkt-ID **exakt** `premium_unlock` (= `BillingManager.PREMIUM_PRODUCT_ID`).
   - Typ: einmaliges Produkt (kein Abo), Preis setzen, **aktivieren**.
3. **Signierten Build** (AAB) in ein Test-Track hochladen (interner Test reicht).
4. **Lizenztester** unter *Einstellungen → Lizenztests* hinterlegen (deine Google-Mail),
   damit Käufe kostenlos/erstattbar sind.
5. App über den **Play-Test-Link** auf einem echten Gerät installieren (nicht das Debug-APK).

## Schnell zum Testen der Paywall (ohne 1000 Punkte zu sammeln)

Im versteckten Dev-Menü (Settings) Bonuspunkte addieren, bis Level 2 erreicht ist – dann
greift das Gate. Siehe `SettingsScreen(onAddPoints = …)` / `DevSettings`.

## Hinweise

- **Entitlement-Quelle** ist `queryPurchasesAsync` (Google Play), nicht ein lokaler Flag –
  der Kauf gilt geräteübergreifend und übersteht App-Daten-Löschen.
- Käufe werden **acknowledged** (sonst storniert Google nach 3 Tagen automatisch).
- Da das Gate hart blockt, ist im gesperrten Zustand auch das Dev-Menü nicht erreichbar;
  zum erneuten Testen Kauf in Play erstatten/stornieren oder App-Daten löschen.
