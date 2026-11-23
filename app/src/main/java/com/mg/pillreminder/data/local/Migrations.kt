package com.mg.pillreminder.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v2: 복용 기록 테이블 추가. SQL은 schemas/2.json의 createSql과 같아야 한다. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    // SQLiteDriver를 쓰지 않는 빌더라 Room이 SupportSQLiteDatabase 버전을 호출한다.
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `dose_records` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`medicineId` INTEGER NOT NULL, " +
                "`takenAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`medicineId`) REFERENCES `medicines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_dose_records_medicineId` ON `dose_records` (`medicineId`)")
    }
}

/** v3: 주기 제안 거절 여부. 기존 약은 거절하지 않은 것으로 둔다. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `medicines` ADD COLUMN `intervalSuggestionDismissed` INTEGER NOT NULL DEFAULT 0")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
