# PillReminder 설계

> 제품 배경과 시나리오(PRD)는 Manyfast `PillReminder 복약 알림 앱` 프로젝트에 있다.
> 이 문서는 개발용 설계다. AI 코딩 에이전트는 작업 전에 이 문서를 먼저 읽는다.

## 1. 범위

| 단계 | 기간 | 범위 |
|---|---|---|
| 1차 | ~11/8 | 약 등록 화면 1개, 매일 정해진 시각 알림, 단위 테스트, CI |
| 2차 | 면접 이후 | 복용 기록, "N시간 뒤" 알림, 주기 제안, 앱 자체 음성 대화 |
| 3차 | AppFunctions 공개 후 | 핵심 UseCase를 AppFunctions로 노출해 Gemini에서 호출 |

빅스비와 구글 어시스턴트는 서드파티 앱 데이터 연동을 지원하지 않는다. 그래서 빅스비는 **앱을 여는 입구**로만 쓴다.

## 2. 요구사항

### R1. 약 관리 (1차)
- F1-1 약 등록: 약 이름과 복용 시각(시, 분)을 입력해 저장한다.
- F1-2 약 목록: 등록한 약과 다음 알림 시각을 보여준다.
- F1-3 약 삭제: 삭제하면 예약된 알림도 취소한다.

### R2. 복약 알림 (1차)
- F2-1 매일 정해진 시각 알림 예약 (`setExactAndAllowWhileIdle`)
- F2-2 알림 표시 (알림 채널 `medication_reminder`, 탭하면 앱 열기)
- F2-3 알림이 울리면 다음 날 같은 시각으로 다시 예약
- F2-4 재부팅 후 모든 알림 재등록 (`BOOT_COMPLETED`)
- F2-5 권한 안내: Android 13 이상 `POST_NOTIFICATIONS`, Android 12 이상 정확한 알람(`canScheduleExactAlarms()`가 false면 설정 화면으로 이동)

### R3. 복용 기록 (2차)
- F3-1 복용 기록: 약 이름과 현재 시각 저장. 알림의 "먹었어" 버튼으로도 기록
- F3-2 마지막 복용 조회: 약 이름으로 직전 복용 시각 조회
- F3-3 "N시간 뒤 알려줘": 1회성 알림 예약
- F3-4 주기 제안: 복용 간격이 반복되면 주기 알림을 제안 (규칙은 5장)

### R4. 음성 대화 (2차)
- F4-1 입구: 빅스비 빠른 명령어로 앱 열기 → 바로 듣기 시작 / 앱 안 마이크 버튼 / 기본 디지털 어시스턴트 지정
- F4-2 음성 인식: `SpeechRecognizer` (한국어)
- F4-3 의도 해석: LLM API로 문장을 `Intent`(아래)로 변환
- F4-4 음성 응답: `TextToSpeech`
- F4-5 이어지는 대화: 기록 후 "다음에 언제 알려드릴까요?" 질문

### R5. 외부 AI 에이전트 연동 (3차)
- F5-1 `recordDose`, `getLastDose`, `scheduleReminderAfter`를 `@AppFunctionDeclaration`으로 노출
- F5-2 ADB와 테스트 에이전트(github.com/android/appfunctions)로 검증

## 3. 구조

단일 `app` 모듈, MVVM. 핵심 로직은 Android에 의존하지 않는 순수 Kotlin으로 둔다.

```
com.mg.pillreminder
├─ domain/          순수 Kotlin. 단위 테스트 대상
│  ├─ model/        Medicine, DoseRecord, Reminder
│  ├─ usecase/      AddMedicine, RecordDose, GetLastDose, ScheduleReminderAfter ...
│  └─ ReminderTimeCalculator, RecurringIntervalDetector
├─ data/            Room (Entity, Dao, Database), Repository 구현
├─ alarm/           AlarmScheduler(AlarmManager 래퍼), AlarmReceiver, BootReceiver
├─ notification/    채널 생성, 알림 표시
├─ voice/           (2차) SpeechRecognizer, LLM 클라이언트, TTS
├─ appfunctions/    (3차) AppFunction 선언
├─ ui/              Compose 화면, ViewModel
└─ di/              Hilt 모듈
```

**입구와 기능 분리 원칙**: 화면, 알림 버튼, 음성, AppFunctions 모두 같은 UseCase를 호출한다. 입구에 비즈니스 로직을 두지 않는다.

```
화면 버튼 ─────────┐
알림 "먹었어" 버튼 ─┤
음성 대화 ─────────┼─→ UseCase ─→ Repository / AlarmScheduler
AppFunctions ─────┘
```

**시간 처리**: `java.time`(`Instant`, `LocalTime`, `ZoneId`)을 쓰고 현재 시각은 `Clock`을 주입받는다. 테스트에서 시각을 고정하기 위해서다.

## 4. 데이터 모델

```kotlin
data class Medicine(
    val id: Long,
    val name: String,          // 예: 생리통약
    val createdAt: Instant,
)

data class DoseRecord(          // 2차
    val id: Long,
    val medicineId: Long,
    val takenAt: Instant,
)

data class Reminder(
    val id: Long,
    val medicineId: Long,
    val schedule: ReminderSchedule,
    val nextTriggerAt: Instant,
)

sealed interface ReminderSchedule {
    data class Daily(val time: LocalTime) : ReminderSchedule           // 1차
    data object Once : ReminderSchedule                               // 2차: "4시간 뒤"
    data class Interval(val interval: Duration) : ReminderSchedule    // 2차: 주기 제안 수락
}
```

Room에는 `schedule`을 `type`(DAILY / ONCE / INTERVAL) + `timeOfDayMinutes` + `intervalMinutes` 컬럼으로 펼쳐 저장한다.

## 5. 핵심 규칙 (단위 테스트 대상)

### ReminderTimeCalculator
- `nextDaily(now, time, zone)`: 오늘 그 시각이 아직 안 지났으면 오늘, 지났으면 내일. 같은 분이면 내일로 본다.
- `after(now, duration)`: `now + duration`
- `nextInterval(lastTakenAt, interval, now)`: `lastTakenAt + interval`. 이미 지났으면 `now`에서 가장 가까운 다음 주기

테스트 케이스 예시
- 08:00 등록, 지금 07:59 → 오늘 08:00
- 08:00 등록, 지금 08:00 → 내일 08:00
- 23:30 등록, 지금 23:45 → 내일 23:30
- 서머타임 없는 `Asia/Seoul` 기준. 다른 타임존 테스트 1개

### RecurringIntervalDetector (2차)
- 같은 약의 최근 복용 기록 4개(간격 3개)를 본다.
- 간격 3개가 모두 중앙값 기준 ±30분 안이고 중앙값이 1시간 이상이면 주기로 판단한다.
- 결과는 30분 단위로 반올림한다. 예: 3시간 50분, 4시간 5분, 4시간 10분 → 4시간
- 이미 Interval 알림이 있거나 사용자가 이 약의 제안을 거절했으면 제안하지 않는다.

## 6. 음성 의도 (2차)

LLM은 사용자 문장을 아래 중 하나의 JSON으로만 바꾼다. 앱은 이 JSON을 UseCase 호출로 연결한다.

```kotlin
sealed interface VoiceIntent {
    data class RecordDose(val medicineName: String) : VoiceIntent
    data class ScheduleAfter(val medicineName: String?, val minutes: Int) : VoiceIntent
    data class AskLastDose(val medicineName: String) : VoiceIntent
    data class AcceptRecurring(val accept: Boolean) : VoiceIntent
    data object Unknown : VoiceIntent
}
```

대화 예시
1. "생리통약 먹었어" → `RecordDose("생리통약")` → "복약 기록을 체크했어요. 다음에 언제 알려드릴까요?"
2. "4시간 뒤에" → `ScheduleAfter(null, 240)` → 직전 대화의 약으로 예약 → "4시간 뒤 오후 3시 20분에 알려드릴게요."
3. "생리통약 언제 먹었지?" → `AskLastDose("생리통약")` → "오늘 오전 11시 20분에 드셨어요. 4시간 지났어요."

등록되지 않은 약 이름이면 새로 등록할지 묻는다.

## 7. 권한과 안정성

| 항목 | 처리 |
|---|---|
| `POST_NOTIFICATIONS` (13+) | 첫 알림 등록 시 요청. 거부하면 목록 상단에 안내 배너 |
| 정확한 알람 (12+) | `canScheduleExactAlarms()` false면 `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` 화면으로 이동 |
| 재부팅 | `BootReceiver`에서 DB의 모든 Reminder 재예약 |
| Doze / 배터리 최적화 | 알림이 늦으면 배터리 최적화 예외 설정 안내 (1차는 문구만) |
| `RECORD_AUDIO` (2차) | 마이크 버튼이나 음성 입구 진입 시 요청 |

## 8. 결정 사항

- minSdk를 24에서 **26**으로 올린다. `java.time`을 desugaring 없이 쓰기 위해서다.
- 1차는 Room 스키마 버전 1로 시작하고 2차에서 DoseRecord 추가 시 마이그레이션을 작성한다.

## 9. 미정 항목

- [ ] LLM API 선택 (Claude / Gemini / 기타)과 키 보관 방식 → 2차 시작 전 결정
- [ ] 빅스비 빠른 명령어로 앱을 열 때 잠금 화면에서도 음성 입력이 되는지 → 실기기 검증
- [ ] 알림의 "먹었어" 버튼을 1차에 넣을지 → 1차 진행 상황 보고 결정
- [ ] 같은 약을 짧은 간격으로 두 번 기록하면 경고할지 (예: 1시간 이내)
