# Galaxy Tab S8 Ultra Landscape Design

## Objective

Adapt the `VOICE_MERGE` Android app for the Galaxy Tab S8 Ultra in landscape orientation without changing the source branch or its phone layouts.

## Chosen approach

Use Android large-screen resource qualifiers and a small amount of screen-width-aware Java code. The tablet branch keeps every existing route, view ID, voice binding, Firebase call, and robot-control action intact. Tablet resources provide a permanent landscape shell, a left navigation rail, larger spacing, and a bounded content surface. High-focus screens such as voice control use a dedicated landscape composition; secondary forms remain centered instead of stretching across 14.6 inches.

Alternatives rejected:

- Uniformly scaling the 390 x 844 phone canvas wastes the tablet width and preserves phone interaction density.
- Rebuilding all screens and navigation in Compose would expand scope and risk breaking the current Java/XML bindings.

## Layout rules

- Lock the activity to landscape.
- Target large screens through `sw600dp-land` resources.
- Replace the bottom navigation with a 112dp left navigation rail while preserving its four existing IDs.
- Keep primary content within a readable maximum width and center it in the remaining area.
- Use 32-48dp outer spacing, 16-24sp hierarchy, and at least 48dp interactive targets.
- Use a two-zone voice screen: voice state and microphone on the left, recognized command and actions on the right.
- Preserve the existing teal, white, gray, typography, icons, and drawable assets.

## Compatibility and safety

- Branch from `origin/VOICE_MERGE` only.
- Do not merge, rebase, force-push, or commit to `VOICE_MERGE`, `front`, or `main`.
- Use the valid Git branch name `GALAXY-TAB` because Git ref names cannot contain spaces.
- Keep phone resources unchanged except where the tablet-only branch must declare landscape orientation or select tablet sizing at runtime.

## Verification

- Confirm the branch merge-base is the current `VOICE_MERGE` head.
- Compile the debug APK with Gradle.
- Run Android lint and unit tests.
- Inspect the manifest and resource qualifiers for landscape and `sw600dp-land` coverage.
- Verify all IDs referenced by `MainActivity` still exist in the selected tablet resources.

