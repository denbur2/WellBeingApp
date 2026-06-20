# TODO

## Daten-Retention (Aufräumen alter Einträge)

Aktuell wächst die Datenbank unbegrenzt – es gibt keine automatische Löschung.

- [ ] Regelmäßiges Löschen alter Einträge einbauen (z. B. einmal täglich im `TrackingCoordinator`).
  - `LocationDao.deleteOlderThan(timestamp)` existiert bereits, wird aber nirgends aufgerufen → aufrufen.
  - Für `app_usage_entries` analoge `deleteOlderThan`-Query im `AppUsageDao` ergänzen und ebenfalls aufrufen.
  - Sinnvolle Aufbewahrungsdauer festlegen (z. B. 30/90 Tage) und dokumentieren.
- [ ] **WICHTIG: `score_history` NICHT löschen** – diese Tabelle soll dauerhaft erhalten bleiben. Keine Retention/Löschlogik für `ScoreHistoryEntry`.

## Verwandte offene Punkte

- [x] `TrackingCoordinator` implementieren – erledigt: `DefaultTrackingCoordinator` + `TrackingService`.
- [ ] Migrations-Risiko: DB ist `version = 1` ohne `fallbackToDestructiveMigration`. Bei jeder Schemaänderung eine Room-Migration schreiben, sonst crasht die App beim Start.

---

# Feature-Planung: Score + Bonsai & Per-App-Heatmap

> **Status: alle 18 Aufgaben implementiert.** APK baut, Unit-Tests grün
> (Score-Regeln, Heatmap-Aggregator, Heat-Farbskala). Noch nicht auf einem
> echten Gerät verifiziert (kein Gerät angeschlossen).

## Getroffene Entscheidungen

### Design (app-weit)
- **Terminal-/ASCII-Stil**: weiße Schrift auf schwarzem Hintergrund, Monospace-Schrift.
- Gilt für **alle** Screens. `WellbeingAppTheme` umbauen: Material-You/Dynamic-Color aus,
  festes schwarzes Schema mit weißem Text, Monospace-Typografie. Passt zu ASCII-Bonsai
  und ASCII-Error-Bild.
- Heatmap-Karte bleibt dezent (siehe Heatmap-Skala); UI drumherum im Terminal-Stil.

### Score
- Aggregation pro **10-Minuten-Fenster** (Summe `duration` aller Apps).
- Regel: ≤ 1 min → **+1**, ≥ 5 min → **−1**, dazwischen (1 < t < 5) → **neutral**.
  Grenzfälle: genau 1 min zählt als ≤ 1 min, genau 5 min als ≥ 5 min.
- Score **darf negativ** werden (kein Floor bei 0).
- Bei **1000 Anzeige-Punkten** → **Level +1**, Anzeige-Score zurück auf 0.

### Baum (Easteregg im Score-Screen)
- **Diskrete Wachstumsstufen**, mit `bonsai.sh` vorgerendert (Bash läuft nicht auf Android).
- Genau **21 Stufen (Index 0–20)** über 0–1000 Punkte → eine Stufe alle **50 Punkte**
  (`Stufe = floor(score / 50)`, geclampt 0–20). Stufe 0 = Setzling, Stufe 20 = ausgewachsen.
- **Negativer Score → ASCII-Error-Bild** statt Baum (eigenes Asset).
- ASCII farbig: grün = Blätter, braun = Stamm (via `AnnotatedString`, wie im alten Projekt).

### Heatmap
- App-Auswahl: **alle getrackten Apps + „Gesamt"** (alle kombiniert).
- Beim App-Wechsel ändert sich **nur der Heat** (cells/intensity), **nicht** Kameraposition/Zoom.
- Feste Farbskala (unabhängig von App/Datenmenge): pro Rasterzelle = Nutzungsminuten;
  **~1 min = hell/weiß → ~60 min = dunkelblau** (geclampt bei 60 min).
- Karten-Hintergrund **dezent**, damit der Heat gut erkennbar bleibt.

## Aufgaben (sinnvolle Reihenfolge)

- [x] **1. Score-Berechnung (10-Min-Fenster-Logik).** ✅
  `ScoreRules` (rein/testbar) + `DefaultScoreCalculator`. 10-Min-Buckets, Regel ≤1/Mitte/≥5,
  Score darf negativ werden. `observeCurrentScore()` (heute, live) + `snapshotDay()`.
  Unit-Tests (drei Zonen, Grenzfälle 1/5 min, Bucketing, treeStage) grün.

- [x] **2. Level-/Reset-Mechanik bei 1000.** ✅
  `ScoreRules.level()` (Level-Up je 1000) + `displayScore()` (Reset auf 0, negativ wird
  durchgereicht → Error-Bild im Screen #4). `level` in `ScoreData`/`ScoreHistoryEntry`.

- [x] **3. Bonsai-Wachstumsstufen aus `bonsai.sh` vorrendern.** ✅
  21 Stufen (`life = 3 + Stufe·2`, Seed 7) via `bonsai.sh` gerendert, ANSI gestrippt,
  als `assets/trees/stage_00..20.txt` abgelegt. Plus `assets/trees/error.txt` für negativen Score.

- [x] **4. Score-Screen mit Baum-Easteregg (Compose).** ✅
  `ScoreScreen` + `Bonsai` (Asset-Loader + `&`=grün/Stamm=braun-Colorizer).
  `Stufe = ScoreRules.treeStage(score)`; bei negativem Score das Error-Bild (rot).

- [x] **5. Heatmap: Per-App-Filterung (Builder/ViewModel).** ✅
  `DefaultHeatmapBuilder` + reiner `HeatmapAggregator` (30-m-Raster, Minuten/Zelle).
  `observeHeatmap(start,end,packageName?)` + `observeAvailableApps`. VM mit `selectApp()`.

- [x] **6. Heatmap: feste Farbskala (1 min → 60 min) + dezente Karte.** ✅
  Reiner `HeatColor` (1 min hell/transparent → 60 min dunkelblau, geclampt) + `HeatmapOverlay`
  (weiche RadialGradient-Kreise). Tile-Dimming (entsättigt + abgedunkelt) im Screen.

- [x] **7. Heatmap-Screen mit Karte + App-Auswahl (Compose).** ✅
  `HeatmapScreen`: OSMDroid-MapView (über `remember` stabil → Kamera bleibt beim App-Wechsel),
  Chips „GESAMT" + alle Apps, Zeitraum letzte 7 Tage.

- [x] **8. Settings-Screen mit verstecktem Entwickler-Menü (Compose).** ✅
  `SettingsScreen`: 7× Tippen auf den Titel → Dev-Menü mit ±10/±100-Buttons. Wirkt über
  `DevSettings.bonusPoints` additiv auf den Score (nicht im Archiv).

- [x] **9. App-weites Theme: Terminal-/ASCII-Stil (weiß auf schwarz).** ✅
  `Theme.kt` festes schwarzes Schema (kein Dynamic Color), grüner Akzent; `Type.kt` Monospace;
  XML-Window-Hintergrund schwarz (kein Start-Flash).

## Infrastruktur / Grundgerüst (für eine lauffähige App)

- [x] **10. Navigation (navigation-compose).** ✅
  `WellbeingNav`: NavHost + Terminal-Bottom-Bar zwischen Score/Heatmap/Settings (`Dest`-Enum).

- [x] **11. Dependencies im Version-Catalog ergänzen.** ✅
  OSMDroid 6.1.20, `play-services-location` 21.3.0, `navigation-compose` 2.9.8,
  `work-runtime-ktx` 2.11.2, `kotlinx-coroutines-play-services` 1.11.0,
  `lifecycle-runtime-compose`. Nebenbei gefixt: KSP `2.2.10-1.0.31` → `2.2.10-2.0.2`,
  `android.disallowKotlinSourceSets=false` für AGP-9/KSP.

- [x] **12. Provider-Implementierungen.** ✅
  `AndroidAppUsageProvider` (UsageStatsManager) + `AndroidLocationProvider`
  (FusedLocationProviderClient, mit Permission-Check). Einheit Nutzung = Millisekunden.

- [x] **13. TrackingCoordinator-Impl als Foreground-Service.** ✅
  `DefaultTrackingCoordinator` (Minuten-Tick, LocationEntry zuerst = FK-Ziel, dann
  AppUsageEntries) in `TrackingService` (START_STICKY, Notification, type=location).

- [x] **14. AndroidManifest: Permissions, Service, App-Name/Icon.** ✅
  FINE/COARSE_LOCATION, FOREGROUND_SERVICE(+_LOCATION), POST_NOTIFICATIONS,
  PACKAGE_USAGE_STATS. Service deklariert. Background-Location bewusst weggelassen.

- [x] **15. Runtime-Permission-Flow.** ✅
  `Permissions` (Usage via AppOps, Location, Notifications) + `PermissionGate`
  (Terminal-Setup-Screen, Recheck bei ON_RESUME, Settings-Intent für Usage Access).

- [x] **16. DI / Wiring: Application + ViewModelFactory.** ✅
  `AppContainer` (Service-Locator) + `WellbeingApplication` + `AppViewModelFactory`.
  Im Manifest als `android:name` registriert.

- [x] **17. MainActivity: NavHost + Permission-Gate statt Stub.** ✅
  Stub ersetzt: Theme + `PermissionGate { TrackingService.start(); WellbeingNav() }`.

- [x] **18. Täglichen Score-Snapshot automatisch auslösen.** ✅
  `ScoreSnapshotWorker` (WorkManager, täglich, `ExistingPeriodicWorkPolicy.KEEP`),
  archiviert den Vortag in `score_history`. In `WellbeingApplication` eingeplant.

## Noch offen / nächste Schritte
- Auf echtem Gerät testen (Tracking, Heatmap-Tiles, Permission-Flow, Bonsai-Wachstum via Dev-Menü).
- Daten-Retention (oben) noch nicht umgesetzt.
- Room-Migrationsstrategie festlegen.
- Optional: App-Icon an den Terminal-Look anpassen.
