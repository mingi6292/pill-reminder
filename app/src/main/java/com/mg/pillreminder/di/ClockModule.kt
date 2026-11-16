package com.mg.pillreminder.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {

    // Clock은 Instant용으로만 쓴다. systemDefaultZone()은 생성 시점 타임존을 고정해서,
    // 사용자가 타임존을 바꾸면 틀어진다. 타임존은 쓰는 시점에 ZoneId.systemDefault()로 구한다.
    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}
