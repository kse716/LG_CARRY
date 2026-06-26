# ThinQ Carry Control UI - Figma frame specs

Source Figma file: `dDVbRNyEfohqyZUox2jCzf`

Extracted from Figma Dev context on 2026-06-06 for Android implementation.

## Design tokens

| Token | Value | Android use |
| --- | --- | --- |
| Screen size | 390 x 844 | Phone-first reference canvas |
| Background | `#F7F9F9` | Activity background |
| Card | `#FFFFFF` | Cards, panels, nav |
| Primary | `#008E84` | Active nav, main CTA, toggles |
| Primary dark | `#00756D` | Listening voice button |
| Text | `#14191B` | Primary copy |
| Muted text | `#798385`, `#7D8586` | Labels and inactive nav |
| Stroke | `#E2E7E7`, `#E9ECEC` | Dividers and outlined controls |
| Soft fill | `#E8F5F4`, `#EEF4F3` | Icon discs and chips |
| Type | Noto Sans KR, 11-24sp equivalent | Android default sans fallback |
| Radius | 12, 14, 18, 28, 999 | Cards, chips, screen corners, pills |

## Figma screen frames

| Figma ID | Frame | Android route |
| --- | --- | --- |
| `89:76` | 로그인 | `login` |
| `115:105` | 회원가입 | `signup` |
| `43:2` | 홈 · 대기 | `home` |
| `44:2` | 음성 호출 | `voice` |
| `44:20` | 음성 호출 · 인식중 | `voiceListening` |
| `44:50` | 음성 호출 · 결과 | `voiceResult` |
| `47:2` | 모듈 | `modules` |
| `47:106` | 모듈 상세 | `moduleDetail` |
| `48:2` | 모듈 추가 | `moduleAdd` |
| `48:26` | 모듈 이름 수정 | `moduleRename` |
| `48:42` | 물품 검색 | `itemSearch` |
| `91:146` | 물건 추가 | `itemAdd` |
| `23:110` | 루틴 | `routine` |
| `23:259` | 루틴 편집 | `routineEdit` |
| `90:489` | 모듈 선택 | `moduleSelect` |
| `90:525` | 루틴 시간 설정 | `routineTime` |
| `90:447` | 예약 시간 설정 | `reserveTime` |
| `90:468` | 이동 위치 설정 | `location` |
| `23:297` | 메뉴 | `menu` |
| `91:207` | 알림 설정 | `notification` |
| `68:127` | 알람 음 선택 | `alarm` |
| `91:264` | 로그 기록 | `logs` |
| `115:130` | 지도/거점 설정 | `map` |

## Navigation model

Bottom navigation follows the Figma main sections:

- `홈`
- `모듈`
- `루틴`
- `메뉴`

Voice call is launched from the Home voice card, matching the Figma voice flow rather than a separate bottom tab.

## Android implementation notes

- `MainActivity.java` renders the Figma routes programmatically to keep the 23-frame flow in one inspectable Android source.
- The voice flow keeps `SpeechRecognizer`, `RECORD_AUDIO`, Firebase logging, and the `VOICE_INTENT_API_URL` handoff from the VOICE_MERGE branch.
- Android Gradle path validation is disabled with `android.overridePathCheck=true` because this Windows workspace contains Korean path segments.
