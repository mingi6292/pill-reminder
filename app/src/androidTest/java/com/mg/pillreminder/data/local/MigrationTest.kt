package com.mg.pillreminder.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PillReminderDatabase::class.java)

    @Test
    fun migrate1To2_keepsMedicinesAndReminders() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            db.execSQL("INSERT INTO medicines (id, name, createdAt) VALUES (1, '비타민', 0)")
            db.execSQL(
                "INSERT INTO reminders (id, medicineId, type, timeOfDayMinutes, intervalMinutes, nextTriggerAt) " +
                    "VALUES (1, 1, 'DAILY', 480, NULL, 0)",
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT name FROM medicines").use { cursor ->
                cursor.moveToFirst()
                assertEquals("비타민", cursor.getString(0))
            }
            db.query("SELECT COUNT(*) FROM reminders").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migratedDatabase_opensWithCurrentSchema() {
        helper.createDatabase(DB_NAME, 1).close()

        // Room이 연 DB의 스키마가 엔티티와 다르면 여기서 예외가 난다.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Room.databaseBuilder(context, PillReminderDatabase::class.java, DB_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
            .apply { openHelper.writableDatabase }
            .close()
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
