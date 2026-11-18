#!/usr/bin/env bash
# release APK를 설치하고 실행해 시작 직후 죽지 않는지 본다.
# R8 설정 문제(v0.2.0 IllegalAccessError)는 빌드는 통과하고 실행해야 드러나서 필요하다.
# 사용: smoke-test.sh <서명된 apk>
set -euo pipefail

apk="$1"
package="com.mg.pillreminder"

adb install -r "$apk"
adb logcat -b crash -c
adb shell am start -W -n "$package/.MainActivity"

# 크래시가 첫 화면 이후 코루틴이나 Room 초기화에서 날 수도 있어 잠시 기다린다.
sleep 5

crash_log="$(adb logcat -b crash -d)"
if ! adb shell pidof "$package" > /dev/null || grep -q "FATAL EXCEPTION" <<< "$crash_log"; then
  echo "::error::앱이 시작 후 죽었다"
  echo "$crash_log"
  exit 1
fi

echo "스모크 테스트 통과"
