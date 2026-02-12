![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)
![Platform](https://img.shields.io/badge/Platform-Android%208.0+-34A853?logo=android&logoColor=white)
![STT](https://img.shields.io/badge/STT-Deepgram-13EF93?logo=deepgram&logoColor=white)
![AI](https://img.shields.io/badge/AI-Claude-d97706?logo=anthropic&logoColor=white)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)

# stupidisco-android

<p align="center">
  <em>Android-Port des Desktop-Interview-Assistenten <a href="https://github.com/pepperonas/stupidisco">stupidisco</a></em>
</p>

---

Echtzeit-Interview-Assistent als Floating Overlay für Android. Erfasst gesprochene Fragen aus Videocalls (Google Meet, Teams, Zoom) per Mikrofon, transkribiert sie live und generiert kompakte deutsche Antworten — angezeigt in einem verschiebbaren Always-on-Top-Overlay.

**Dein unfairer Vorteil im Vorstellungsgespräch — jetzt auch mobil.** Jede Frage wird live transkribiert und von KI in Echtzeit beantwortet — unsichtbar für dein Gegenüber.

### Features

| Feature | Beschreibung |
|---------|-------------|
| **Live-Transkription** | Deepgram Streaming-STT (nova-3 Modell, Deutsch) via WebSocket |
| **KI-Antworten** | Claude generiert prägnante Antworten in 2–3 Sätzen, Token für Token gestreamt |
| **Floating Overlay** | Dunkles Overlay-Fenster über allen Apps, frei verschiebbar |
| **Pulsierender Mic-Button** | 64dp Button mit roter Puls-Animation (600ms) bei Aufnahme |
| **Kopieren & Regenerieren** | Antwort in die Zwischenablage kopieren oder neu generieren |
| **API-Key-Dialog** | Keys werden beim ersten Start abgefragt, verschlüsselt gespeichert |
| **Session-Logging** | Frage-Antwort-Paare in `files/sessions/` |
| **Verschlüsselte Speicherung** | API-Keys via EncryptedSharedPreferences (AES-256) |

### Download

| Plattform | Mindestversion | Format |
|-----------|---------------|--------|
| Android | 8.0 (API 26) | `.apk` |

Zur [**Releases-Seite**](https://github.com/pepperonas/stupidisco-android/releases)

### Voraussetzungen

- Android 8.0+ (API 26)
- [Deepgram API-Key](https://console.deepgram.com/) — kostenlose Stufe mit $200 Guthaben
- [Anthropic API-Key](https://console.anthropic.com/)

### Installation

**APK (empfohlen):**

APK von der [Releases-Seite](https://github.com/pepperonas/stupidisco-android/releases) herunterladen und auf dem Gerät installieren. "Installation aus unbekannten Quellen" muss aktiviert sein.

**Aus Source bauen:**

```bash
git clone https://github.com/pepperonas/stupidisco-android.git
cd stupidisco-android
export ANDROID_HOME=~/Library/Android/sdk  # Pfad anpassen
./gradlew assembleDebug
```

APK liegt dann unter `app/build/outputs/apk/debug/app-debug.apk`.

### Nutzung

1. App starten — API-Key Dialog erscheint beim ersten Start
2. Deepgram- und Anthropic-Key eingeben
3. Mikrofon- und Overlay-Berechtigung erteilen
4. Overlay erscheint über allen Apps
5. **Mic-Button** drücken — Aufnahme startet (Button pulsiert rot)
6. Frage sprechen — Transkript erscheint live
7. **Mic-Button** erneut drücken — *"Thinking..."* → Antwort streamt ein
8. **Kopieren** oder **Regenerieren**

> **Tipp:** Headset-Mikrofon verwenden, um Echo zu reduzieren.

### Berechtigungen

| Berechtigung | Grund |
|-------------|-------|
| `RECORD_AUDIO` | Mikrofon-Aufnahme für Speech-to-Text |
| `SYSTEM_ALERT_WINDOW` | Floating Overlay über anderen Apps |
| `FOREGROUND_SERVICE` | Service läuft im Hintergrund weiter |
| `INTERNET` | Deepgram STT + Claude API |

### Funktionsweise

```
          ┌──────────────┐
          │   Mikrofon    │
          │  (16kHz PCM)  │
          └──────┬───────┘
                 │ 100ms Chunks (Flow<ByteArray>)
                 v
          ┌──────────────┐
          │   Deepgram    │
          │  nova-3 STT   │
          │  (WebSocket)  │
          └──────┬───────┘
                 │ Transkript (partial/final)
                 v
          ┌──────────────┐
          │  Claude AI    │
          │  Sonnet 4.5   │
          │  (SSE Stream) │
          └──────┬───────┘
                 │ Antwort (Token für Token)
                 v
     ┌─────────────────────────┐
     │  Jetpack Compose Overlay │
     │  Transkript + Antwort    │
     └─────────────────────────┘
```

### Architektur

<table>
<tr><td>

**Komponenten**

```
MainActivity
  ├── Permission-Checks
  │   ├── RECORD_AUDIO (Runtime)
  │   └── SYSTEM_ALERT_WINDOW (Settings)
  ├── API-Key Check → Dialog
  └── Startet OverlayService → finish()

OverlayService (Foreground)
  ├── ComposeView im Overlay-Window
  ├── Drag-Logik (Touch-Events)
  ├── AudioRecorder (Flow<ByteArray>)
  ├── DeepgramClient (WebSocket)
  ├── ClaudeClient (SSE)
  └── MutableStateFlow<AppState>
```

</td><td>

**Tech Stack**

| Komponente | Technologie |
|-----------|-----------|
| UI | Jetpack Compose + Material3 |
| STT | Deepgram nova-3 (OkHttp WebSocket) |
| KI | Claude Sonnet 4.5 (OkHttp SSE) |
| Audio | AudioRecord (16kHz, Mono, PCM16) |
| State | Kotlin StateFlow |
| Async | Kotlin Coroutines + Flow |
| Storage | EncryptedSharedPreferences |
| Build | Gradle 8.5 + AGP 8.2 |

</td></tr>
</table>

### Konfiguration

API-Keys werden beim ersten Start per Dialog abgefragt und verschlüsselt auf dem Gerät gespeichert.

| Konstante | Standard | Beschreibung |
|-----------|---------|-------------|
| `MODEL` | `claude-sonnet-4-5-20250929` | Claude-Modell für Antworten |
| `MAX_TOKENS` | `600` | Maximale Antwortlänge (Token) |
| `SAMPLE_RATE` | `16000` | Audio-Abtastrate in Hz |
| `CHUNK_SIZE` | `3200` | Audio-Chunk-Größe in Bytes (100ms) |

### Unterschiede zur Desktop-Version

| | [Desktop (Python/Qt)](https://github.com/pepperonas/stupidisco) | Android (Kotlin/Compose) |
|---|---|---|
| **Plattform** | macOS, Windows, Linux | Android 8.0+ |
| **GUI** | PyQt6 | Jetpack Compose |
| **Overlay** | Qt `WindowStaysOnTopHint` | `TYPE_APPLICATION_OVERLAY` |
| **Audio** | sounddevice / PortAudio | `AudioRecord` |
| **STT** | Deepgram SDK v5 | OkHttp WebSocket |
| **AI** | Anthropic SDK | OkHttp SSE |
| **Hotkey** | `Cmd/Ctrl+Shift+R` | Mic-Button im Overlay |
| **Key Storage** | `.env` Datei | EncryptedSharedPreferences |

### Warum "stupidisco"?

> **stupidisco** — aus dem Italienischen *stupire* (erstaunen, verblüffen). *„Stupidisco"* ist die erste Person Singular: **„ich überrasche"**, **„ich erstaune"**, **„ich verblüffe"**.

Inspiriert von [Stupidisco](https://www.youtube.com/watch?v=GJfydUI2Hzs&list=RDGJfydUI2Hzs&start_radio=1) von Junior Jack.

---

## Entwickler

**Martin Pfeffer** — [celox.io](https://celox.io)

## Lizenz

MIT

<p align="center">
  <a href="https://www.youtube.com/watch?v=GJfydUI2Hzs&list=RDGJfydUI2Hzs&start_radio=1">
    <img src="https://img.youtube.com/vi/GJfydUI2Hzs/0.jpg" alt="Stupidisco von Junior Jack" width="320">
  </a>
</p>
