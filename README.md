# X Game Assistant

A local-first Android game assistant prototype. No cloud API or account is required.

## Current prototype
- Dark native Android interface
- Offline practice history stored on-device
- Simple adaptive suggestions based on locally recorded results
- Android AccessibilityService foundation with user-authorized screen gestures
- GitHub Actions workflow configured to build a debug APK artifact

## Automation roadmap
1. Add local screen capture and recognition with clear Android permission prompts.
2. Build and test a game-specific decision module for the Android version of 9 Kings.
3. Add reusable game profiles and local strategy memory for other compatible Android games.
4. Test the APK on a real device and fix build/runtime issues before calling it ready.

## Important limitations
The current app is a foundation, not a finished game-playing bot. It does not yet recognize game screens or autonomously play 9 Kings or other games. Different games require different visual recognition and strategy logic; universal compatibility cannot be guaranteed. Automation must respect Android permissions, each game's rules, and anti-cheat/access protections.

## Build
The GitHub Actions workflow is configured to build and upload a debug APK. Build success must be verified in the Actions run before the APK can be described as tested.
