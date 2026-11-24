# AppFunctions 수동 검증 (adb)

API 36 이상 기기나 에뮬레이터가 필요하다. 약이 하나 이상 등록돼 있어야 한다(아래는 `Vitamin`).
앱을 한 번 실행한 뒤 인덱싱까지 몇 초 걸린다. 첫 조회가 `{}`면 잠시 뒤 다시 한다.

```sh
# 인덱싱: 함수 3개가 보여야 한다
adb shell cmd app_function list-app-functions --package com.mg.pillreminder

F=com.mg.pillreminder.appfunctions.PillAppFunctionService
run() { adb shell "cmd app_function execute-app-function --package com.mg.pillreminder --function '$F#$1' --parameters '$2'"; }

run recordDose '{"medicineName":["Vitamin"]}'
run getLastDose '{"medicineName":["vitamin"]}'
run scheduleReminderAfter '{"medicineName":["Vitamin"],"minutes":[120]}'
```

Git Bash에서는 `MSYS_NO_PATHCONV=1`을 앞에 붙인다. 파라미터 값은 배열로 감싼다.

## 기대 결과
- `recordDose`: `takenAt`이 지금 시각. 앱 목록의 "마지막 복용"이 바뀐다.
- `getLastDose`: 방금 기록한 시각. 기록이 없으면 `takenAt`이 없다.
- `scheduleReminderAfter`: `triggerAt`이 지금 + 120분. 같은 약의 이전 1회 알림은 대체된다.
- 등록되지 않은 약: `code 1500` (element not found)
- `minutes`가 0 이하: `code 1001` (invalid argument)

2026-11-24 API 37 에뮬레이터에서 위 항목을 모두 확인했다.
Gemini에서 부르는 검증은 Connected Apps Platform 프리뷰 신청이 필요해 아직 하지 않았다.
