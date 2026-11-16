package com.mg.pillreminder.di

import android.content.Context
import androidx.room.Room
import com.mg.pillreminder.data.local.MedicineDao
import com.mg.pillreminder.data.local.PillReminderDatabase
import com.mg.pillreminder.data.local.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PillReminderDatabase =
        Room.databaseBuilder(context, PillReminderDatabase::class.java, "pill_reminder.db").build()

    @Provides
    fun provideMedicineDao(database: PillReminderDatabase): MedicineDao = database.medicineDao()

    @Provides
    fun provideReminderDao(database: PillReminderDatabase): ReminderDao = database.reminderDao()
}
