# X Game Assistant

A local-first Android game assistant prototype. Screen text recognition and strategy heuristics run on-device; no cloud API is used.

## Implemented in source
- Dark native Android interface
- Offline practice history in SharedPreferences
- Android screen-capture consent flow and foreground capture service
- On-device OCR with the bundled ML Kit text-recognition model
- Starter 9 Kings heuristic strategy module
- Generic strategy interface and registry for adding other game profiles
- Android AccessibilityService gesture foundation
- GitHub Actions workflow configured to build and upload a debug APK

## Current limits
This is an early prototype, not a completed autonomous game-playing bot. It can read visible text and give basic heuristic advice. It does not yet understand the full board, compare card effects reliably, or automatically play through a 9 Kings run. Other games need their own screen interpretation and strategy modules. A generic bot cannot be guaranteed to play every game correctly.

Automation must respect Android permissions and game rules. No anti-cheat or access-protection bypass is included.

## Build and verification
The GitHub Actions workflow is configured to run `gradle assembleDebug` on pushes to `main` and upload `app/build/outputs/apk/debug/app-debug.apk`. A successful workflow run and on-device test are required before calling the APK verified.
