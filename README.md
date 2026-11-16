# PillReminder

[![CI](https://github.com/mingi6292/pill-reminder/actions/workflows/ci.yml/badge.svg)](https://github.com/mingi6292/pill-reminder/actions/workflows/ci.yml)

음성 명령으로 조작하는 것을 목표로 하는 복약 알림 Android 앱입니다. 개인 프로젝트이며 AI 코딩 에이전트와 함께 만들고 있습니다.

## 기능 (1차)

- **약 등록·목록·삭제**: 약 이름과 복용 시각을 등록하면 목록에 다음 알림 시각이 보입니다. 삭제하면 예약된 알림도 취소합니다.
- **매일 정해진 시각 알림**: `AlarmManager`로 예약하고, 알림이 울리면 다음 날 같은 시각으로 다시 예약합니다.
- **재예약**: 재부팅, 시각·타임존 변경, 정확한 알람 권한을 다시 받았을 때 DB 기준으로 모든 알림을 다시 예약합니다.
- **권한 안내**
  - 알림 권한(Android 13+)은 첫 약을 등록할 때 묻습니다. 거부하면 목록 상단 배너로 안내합니다.
  - 정확한 알람 권한(Android 12+)이 없으면 배너로 안내하고 설정 화면으로 보냅니다.

## 설치

[Releases](https://github.com/mingi6292/pill-reminder/releases)에서 최신 `pill-reminder-v*.apk`를 받아 설치합니다. minSdk는 26(Android 8.0)입니다.

## 개발

Android Studio와 JDK 25가 필요합니다. JDK는 Gradle 툴체인이 받아 옵니다.

```bash
# 빌드와 단위 테스트
./gradlew assembleDebug testDebugUnitTest

# CI와 같은 검사 (lint 포함)
./gradlew assembleDebug testDebugUnitTest lintDebug
```

- 시간 계산 같은 핵심 로직은 `domain/`에 순수 Kotlin으로 두고 `Clock`을 주입받아 단위 테스트합니다.
- 작업은 `feat/<작업>` 브랜치에서 하고 PR로 `main`에 merge합니다.

## CI/CD

| 워크플로 | 실행 시점 | 하는 일 |
|---|---|---|
| [`ci.yml`](.github/workflows/ci.yml) | PR, `main` push | `assembleDebug`, `testDebugUnitTest`, `lintDebug` |
| [`release.yml`](.github/workflows/release.yml) | `v*` 태그 push | 테스트·lint 후 서명된 release APK를 GitHub Release에 올림 |

배포는 `main`의 커밋에 태그를 달아 push합니다.

```bash
git tag -a v0.2.0 -m "v0.2.0"
git push origin v0.2.0
```

서명 키는 저장소에 두지 않습니다. `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` Secrets에서 복원합니다. `versionName`은 태그에서, `versionCode`는 워크플로 실행 번호에서 가져옵니다.

## 구조

단일 `app` 모듈, MVVM입니다. 자세한 설계는 [docs/design.md](docs/design.md)에 있습니다.

```
com.mg.pillreminder
├─ domain/        순수 Kotlin: 모델, UseCase, ReminderTimeCalculator, 인터페이스(AlarmScheduler, PermissionChecker)
├─ data/          Room, Repository 구현
├─ alarm/         AlarmManager 예약, AlarmReceiver, BootReceiver, ExactAlarmPermissionReceiver
├─ notification/  알림 채널과 표시
├─ permission/    권한 상태 읽기, 설정 화면 Intent
├─ ui/            Compose 화면과 ViewModel
└─ di/            Hilt 모듈
```

## 기술 스택

Kotlin · Jetpack Compose · Material 3 · Hilt · Room · Coroutines/Flow · AlarmManager · JUnit4 · Turbine · GitHub Actions

## 진행 상황

- [x] 약 등록 화면
- [x] 매일 정해진 시각 알림, 재부팅 후 재예약
- [x] 알림·정확한 알람 권한 처리
- [x] 단위 테스트
- [x] GitHub Actions CI, 태그 기반 릴리스 배포
- [ ] 복용 기록, "N시간 뒤 알려줘" (2차)
- [ ] 앱 자체 음성 대화: SpeechRecognizer + LLM + TTS (2차)
- [ ] AppFunctions로 핵심 기능 노출 (3차)
