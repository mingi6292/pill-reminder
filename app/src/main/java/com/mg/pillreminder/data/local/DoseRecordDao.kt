package com.mg.pillreminder.data.local

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface DoseRecordDao {
    @Insert
    suspend fun insert(record: DoseRecordEntity): Long
}
