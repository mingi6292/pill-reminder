# CLAUDE.md

## 프로젝트
음성 명령으로 조작하는 복약 알림 Android 앱. 개인 프로젝트이며 AI 코딩 에이전트로 만드는 연습이다.
설계는 [docs/design.md](docs/design.md)를 먼저 읽는다. PRD는 Manyfast `PillReminder 복약 알림 앱` 프로젝트에 있다.

## 사용자
- 10년 차 Native Android 개발자 (Kotlin, Compose, Hilt, Coroutines, Flow)
- Windows + Android Studio 사용. Claude Code는 WSL에서 실행
- 답변은 한국어로 짧고 직접적으로

## 작업 방식
1. 한 번에 한 단계씩 진행하고 단계마다 무엇을 바꿨는지 짧게 알린다.
2. 코드는 사람이 읽기 쉽게 쓴다. 함수는 짧게, 이름은 의도가 드러나게, 주석은 "왜"만.
3. `gradle/libs.versions.toml`에 없는 새 라이브러리는 넣기 전에 먼저 묻는다.
4. 에러 로그를 받으면 원인 한 줄 + 고친 코드로 답한다.
5. 시간 계산 같은 테스트 가능한 로직은 `domain/`에 순수 Kotlin으로 두고 `Clock`을 주입받는다.
6. 화면, 알림 버튼, 음성, AppFunctions 같은 입구에는 비즈니스 로직을 두지 않고 UseCase를 호출한다.
7. 단계마다 `feat/<작업>` 브랜치 worktree에서 작업하고 PR로 main에 merge한다. main에 직접 커밋하지 않는다. merge는 사용자가 GitHub UI에서 한다.
8. 커밋 메시지와 PR 본문에 `Co-Authored-By:` 줄이나 "Generated with Claude Code" 같은 attribution 문구를 넣지 않는다.

## 일정
- 1차 마감 11/8(일): 약 등록 화면, 매일 정해진 시각 알림, 단위 테스트, GitHub Actions CI, README
- 음성 대화와 AppFunctions는 2차 이후

## 진행 상황
- [x] `android-starter` 템플릿 복사, 패키지 `com.mg.pillreminder`로 변경, minSdk 26
- [x] 설계 문서 작성
- [x] GitHub push 후 CI 통과 확인
- [x] `ReminderTimeCalculator` + 단위 테스트
- [x] Hilt + Room
- [x] 약 등록 화면
- [x] AlarmManager 예약, 알림, BootReceiver
- [ ] 권한 처리
- [ ] README 정리

## 명령어
- 빌드와 테스트: `./gradlew assembleDebug testDebugUnitTest`
- CI: `.github/workflows/ci.yml` (assembleDebug, testDebugUnitTest, lintDebug)
- CD: `v*` 태그 push → `.github/workflows/release.yml`이 서명된 APK를 GitHub Release에 올린다. 예: `git tag v0.1.0 && git push origin v0.1.0`
  - Secrets: `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
