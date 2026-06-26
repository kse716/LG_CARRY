# Galaxy Tab S8 Ultra Landscape Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a verified Galaxy Tab S8 Ultra landscape UX on a branch created directly from `VOICE_MERGE`.

**Architecture:** Android `sw600dp-land` resources replace the phone shell with a navigation rail and tablet content surface. Existing Java/XML route bindings remain authoritative, with narrowly scoped width-aware behavior for large screens and a dedicated landscape voice layout.

**Tech Stack:** Android Java, XML resources, Material/AppCompat, Gradle

---

### Task 1: Isolated branch and tablet shell

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/layout-sw600dp-land/activity_main.xml`
- Create: `app/src/main/res/layout-sw600dp-land/view_bottom_nav.xml`
- Create: `app/src/main/res/values-sw600dp-land/dimens.xml`

- [ ] Create `GALAXY-TAB` from the exact `origin/VOICE_MERGE` commit.
- [ ] Lock `MainActivity` to landscape.
- [ ] Add a tablet activity shell with a 112dp navigation rail and centered content surface.
- [ ] Add a vertical navigation resource that retains `bottomNavHome`, `bottomNavModules`, `bottomNavRoutine`, and `bottomNavMenu`.

### Task 2: Tablet content behavior

**Files:**
- Modify: `app/src/main/java/com/example/dx_carry/MainActivity.java`
- Create: `app/src/main/res/values/bools.xml`
- Create: `app/src/main/res/values-sw600dp-land/bools.xml`

- [ ] Detect the tablet resource configuration without hardcoding pixel resolution.
- [ ] Center and bound detail/form screens while allowing dashboard routes to use the wider surface.
- [ ] Keep existing navigation, speech recognition, Firebase, API, and robot-control listeners unchanged.

### Task 3: Landscape voice UX

**Files:**
- Create: `app/src/main/res/layout-sw600dp-land/screen_voice.xml`

- [ ] Preserve every voice view ID used by `MainActivity`.
- [ ] Arrange idle/listening content as a large left voice stage with contextual guidance.
- [ ] Arrange recognition results and execution controls in a right-side action panel.
- [ ] Retain retry, execute, emergency stop, and mission test controls.

### Task 4: Verification and delivery

**Files:**
- Update only if required by build failures directly caused by Tasks 1-3.

- [ ] Run `gradlew.bat testDebugUnitTest` and confirm zero failures.
- [ ] Run `gradlew.bat lintDebug` and review all findings.
- [ ] Run `gradlew.bat assembleDebug` and confirm the APK is produced.
- [ ] Compare `GALAXY-TAB` with `origin/VOICE_MERGE` and confirm no unrelated files changed.
- [ ] Commit and push only `GALAXY-TAB` to `origin`.

