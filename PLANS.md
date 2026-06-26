# KeepAliver — Implementation Plan

Build a new Android app `moe.lyniko.keepaliver` that auto-starts on system triggers (boot, QS tile visibility, account sync) and fires user-defined Intents to keep other apps alive. Supports Normal, Shizuku, and Root execution modes.

Reference project: [Anywhere-](https://github.com/zhaobozhen/Anywhere-) for Intent handling and Shizuku patterns.

**Key decisions:**
- QS Tile: fires on **visibility** (ACTIVE_TILE mode), not just tap
- Services: support **foreground service start** (`am start-foreground-service`)
- UI: **Jetpack Compose** with Material 3
- Sync interval: **user-configurable** (15min / 30min / 1hr / 6hr / 12hr / 24hr)

---

## Phase 1: Project Scaffolding

### Files:
- `build.gradle.kts` — top-level, AGP + Kotlin plugins
- `settings.gradle.kts` — plugin management, `include(":app")`
- `gradle.properties` — AndroidX, non-transitive R classes
- `gradle/libs.versions.toml` — version catalog: AGP 9.0.1, Kotlin 2.3.10, Compose BOM 2026.01.01, Room 2.7.1 (KSP 2.3.8), DataStore, Navigation Compose 2.9.0, Shizuku 13.1.5
- `app/build.gradle.kts` — `moe.lyniko.keepaliver`, minSdk 26, targetSdk 34, compose enabled, KSP for Room
- `app/proguard-rules.pro`
- `app/src/main/res/values/strings.xml` — all UI strings
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` — adaptive icon
- `app/src/main/res/drawable/ic_qs_keepalive.xml` — QS tile icon vector
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — launcher foreground
- `gradlew` + wrapper (generated via `gradle wrapper`)

---

## Phase 2: Data Layer

### Models (`data/model/`)
- `IntentType.kt` — enum: `ACTIVITY`, `BROADCAST`, `SERVICE`
- `ExecutionMode.kt` — enum: `NORMAL`, `SHIZUKU`, `ROOT`
- `ExtraItem.kt` — `data class ExtraItem(key, value, type: ExtraType)` + `enum ExtraType { STRING, INT, LONG, FLOAT, DOUBLE, BOOLEAN, URI }`

### Room (`data/db/`)
- `IntentEntry.kt` — `@Entity(tableName = "intent_entries")`:
  - `id: Long` (PK, auto-generate)
  - `name: String`, `enabled: Boolean`, `intentType: IntentType`
  - `targetPackage: String`, `targetClass: String?`
  - `action: String?`, `dataUri: String?`, `category: String?`, `flags: Int?`
  - `extrasJson: String?` (JSON list of ExtraItem)
  - `useForegroundService: Boolean` (for service type)
- `Converters.kt` — TypeConverters for IntentType ↔ String, List<ExtraItem> ↔ JSON
- `IntentEntryDao.kt` — `getAllEntries(): Flow<List>`, `getEnabledEntries(): suspend List`, CRUD by id
- `AppDatabase.kt` — Room database singleton, version 1

### Repository & Settings
- `data/repository/IntentRepository.kt` — wraps DAO, singleton via Application
- `data/SettingsStore.kt` — DataStore Preferences:
  - `bootTriggerEnabled: Boolean` (default true)
  - `tileTriggerEnabled: Boolean` (default true)
  - `syncTriggerEnabled: Boolean` (default true)
  - `executionMode: String` (NORMAL/SHIZUKU/ROOT)
  - `syncIntervalMinutes: Int` (default 60)

---

## Phase 3: Shizuku Integration

### `shizuku/ShizukuHelper.kt`
- `isShizukuReady()` — `Shizuku.pingBinder()`
- `checkPermission()` → PermissionState enum (GRANTED / REQUESTED / DENIED / NOT_READY)
- Permission request via `Shizuku.checkSelfPermission()` / `Shizuku.requestPermission()`

### `shizuku/ShizukuProcess.kt`
- `suspend fun execute(command: String): Result<String>` — uses `Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)`, reads stdout

### Manifest additions:
```xml
<uses-permission android:name="moe.shizuku.manager.permission.API_V23" />
<uses-permission android:name="moe.shizuku.manager.permission.EXEC_COMMAND" />
<provider android:name="rikka.shizuku.ShizukuProvider"
    android:authorities="${applicationId}.shizuku"
    android:exported="true" android:multiprocess="false"
    android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
```

---

## Phase 4: Intent Execution Engine

### `executor/ShellCommandBuilder.kt`
- Builds shell commands for each intent type:
  - Activity: `am start -n pkg/cls [-a action] [-d uri] [-c category] [-f flags] [--es k v] ...`
  - Broadcast: `am broadcast -n pkg/cls ...`
  - Service: `am startservice -n pkg/cls ...`
  - Foreground Service: `am start-foreground-service -n pkg/cls ...`
- Proper shell escaping for values with spaces/quotes/special chars

### `executor/IntentExecutor.kt`
- `suspend fun executeAll(context, entries: List<IntentEntry>, mode: ExecutionMode)`
- **Normal mode**: Build `Intent` object → `startActivity` / `sendBroadcast` / `startService` / `startForegroundService`
- **Shizuku mode**: Build shell command → `ShizukuProcess.execute(cmd)`
- **Root mode**: Build shell command → `Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))`

---

## Phase 5: Trigger Components

### 5.1 — BootReceiver (`receiver/BootReceiver.kt`)
- Handles `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED`
- `goAsync()` → coroutine → check bootTriggerEnabled → load enabled entries → execute all → `finish()`

### 5.2 — KeepAliveTileService (`service/KeepAliveTileService.kt`)
- ACTIVE_TILE mode: `onStartListening()` fires all enabled intents when tile becomes visible
- `onClick()` also fires all enabled intents (explicit tap)
- Tile state: `STATE_ACTIVE` when trigger enabled, `STATE_INACTIVE` when disabled

### 5.3 — SyncAdapter Stack
- `sync/StubAuthenticator.kt` — extends `AbstractAccountAuthenticator`, all methods return empty Bundle
- `sync/StubAuthenticatorService.kt` — `Service` returning `authenticator.iBinder`
- `provider/StubContentProvider.kt` — minimal no-op ContentProvider, authority = `${applicationId}.provider`
- `sync/SyncAdapter.kt` — `AbstractThreadedSyncAdapter`, `onPerformSync()` fires enabled entries
- `service/SyncService.kt` — `Service` returning `syncAdapter.syncAdapterBinder`
- `sync/SyncAccountHelper.kt` — manages Account creation, sync interval, sync toggles
- `res/xml/authenticator.xml` — accountType = `moe.lyniko.keepaliver`
- `res/xml/sync_adapter.xml` — contentAuthority = `moe.lyniko.keepaliver.provider`

---

## Phase 6: Application Class (`KeepAliverApp.kt`)
- Initialize Room database singleton
- Create IntentRepository singleton
- Call `SyncAccountHelper.ensureAccount(this)` if sync trigger enabled

---

## Phase 7: AndroidManifest.xml
**Permissions:**
```
RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC,
Shizuku API_V23 + EXEC_COMMAND,
AUTHENTICATE_ACCOUNTS, GET_ACCOUNTS, READ_SYNC_SETTINGS, WRITE_SYNC_SETTINGS
```
**Components:** MainActivity, BootReceiver (BOOT_COMPLETED + MY_PACKAGE_REPLACED), KeepAliveTileService (ACTIVE_TILE), SyncService, StubAuthenticatorService, StubContentProvider, ShizukuProvider.

---

## Phase 8: UI (Jetpack Compose + Material 3)

### Navigation (`ui/navigation/NavGraph.kt`)
Three routes: `main`, `editor/{entryId}`, `settings`. Bottom nav bar with "Intents" + "Settings" tabs.

### Main Screen (`ui/main/`)
- `LazyColumn` of intent entry cards with name, type badge, package, enabled switch
- Swipe-to-delete (`SwipeToDismissBox`)
- FAB → navigate to new entry (`entryId=-1`)
- Tap card → navigate to editor

### Editor Screen (`ui/editor/`)
- Form: Name, Intent Type (segmented), Target Package, Target Class, Action, Data URI, Category, Flags, Foreground Service toggle
- Dynamic extras list: type dropdown + key + value + delete per row
- Save with validation (targetPackage required)

### Settings Screen (`ui/settings/`)
- **Triggers**: Boot toggle, QS Tile toggle, Account Sync toggle
- **Sync Interval**: picker (15/30/60/360/720/1440 min) — shown when sync enabled
- **Execution Mode**: segmented buttons (Normal / Shizuku / Root)
- **Shizuku Status**: status chip + "Request Permission" button

### Theme (`ui/theme/`)
Standard Material 3 dynamic color theme: `Color.kt`, `Type.kt`, `Theme.kt`.

---

## File Tree

```
app/src/main/java/moe/lyniko/keepaliver/
├── KeepAliverApp.kt                      # Application class
├── data/
│   ├── model/          IntentType.kt, ExecutionMode.kt, ExtraItem.kt
│   ├── db/             IntentEntry.kt, IntentEntryDao.kt, AppDatabase.kt, Converters.kt
│   ├── repository/     IntentRepository.kt
│   └── SettingsStore.kt
├── executor/           IntentExecutor.kt, ShellCommandBuilder.kt
├── shizuku/            ShizukuHelper.kt, ShizukuProcess.kt
├── receiver/           BootReceiver.kt
├── service/            KeepAliveTileService.kt, SyncService.kt
├── sync/               SyncAdapter.kt, StubAuthenticator.kt, StubAuthenticatorService.kt, SyncAccountHelper.kt
├── provider/           StubContentProvider.kt
└── ui/
    ├── MainActivity.kt
    ├── navigation/     NavGraph.kt
    ├── main/           MainScreen.kt, MainViewModel.kt
    ├── editor/         EditorScreen.kt, EditorViewModel.kt
    ├── settings/       SettingsScreen.kt, SettingsViewModel.kt
    └── theme/          Color.kt, Type.kt, Theme.kt

app/src/main/res/
├── drawable/           ic_qs_keepalive.xml, ic_launcher_foreground.xml
├── mipmap-anydpi-v26/  ic_launcher.xml, ic_launcher_round.xml
├── values/             strings.xml, colors.xml
└── xml/                authenticator.xml, sync_adapter.xml, backup_rules.xml, data_extraction_rules.xml
```

---

## Verification Plan

1. **Build**: `./gradlew assembleDebug` succeeds
2. **Database**: Add/edit/delete entries via UI → Room persists across restart
3. **Normal mode**: Create Activity intent for Settings app → target opens
4. **Shizuku mode**: With Shizuku running, fire same intent via shell command
5. **Boot trigger**: `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p moe.lyniko.keepaliver`
6. **Tile trigger**: Add tile to QS panel → pull down → intents fire on visibility
7. **Sync trigger**: Enable sync → verify periodic sync fires intents
8. **Settings**: Toggle each trigger on/off, change mode → behavior matches
9. **Edge cases**: Empty list (empty state), disabled entries skipped, invalid package handled gracefully
