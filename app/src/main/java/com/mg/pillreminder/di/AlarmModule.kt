package com.mg.pillreminder.di

import com.mg.pillreminder.alarm.AndroidAlarmScheduler
import com.mg.pillreminder.domain.alarm.AlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AlarmModule {

    @Binds
    fun bindAlarmScheduler(impl: AndroidAlarmScheduler): AlarmScheduler
}
