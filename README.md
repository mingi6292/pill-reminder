# PillReminder

음성 명령으로 조작하는 복약 알림 Android 앱 (개인 프로젝트, AI 코딩 에이전트로 개발)

## 진행 상황
- [x] 프로젝트 생성 (`android-starter` 템플릿)
- [ ] 약 이름과 복용 시간 입력 화면
- [ ] 지정 시간 알림
- [ ] 단위 테스트
- [x] GitHub Actions CI (build / unit test / lint)

## 설계
[docs/design.md](docs/design.md)

## 기술 스택
Kotlin / Jetpack Compose / Hilt / Room / AlarmManager / JUnit4 / GitHub Actions

## 다음 목표
- 앱 자체 음성 명령 (SpeechRecognizer + LLM + TTS)
- AppFunctions로 핵심 기능 노출 (Gemini 연동 대비)
