package com.mg.pillreminder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MedicineEntity::class, ReminderEntity::class],
    version = 1,
)
abstract class PillReminderDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun reminderDao(): ReminderDao
}
