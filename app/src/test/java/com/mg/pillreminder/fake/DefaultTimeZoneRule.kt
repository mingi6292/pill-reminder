package com.mg.pillreminder.fake

import org.junit.rules.ExternalResource
import java.time.ZoneId
import java.util.TimeZone

/** UseCase가 사용 시점의 ZoneId.systemDefault()를 쓰므로 테스트 동안 기본 타임존을 고정한다. */
class DefaultTimeZoneRule(private val zone: ZoneId) : ExternalResource() {

    private lateinit var original: TimeZone

    override fun before() {
        original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
    }

    override fun after() = TimeZone.setDefault(original)
}
