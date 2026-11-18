package com.mg.pillreminder.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DoseRecordDaoTest {

    private lateinit var db: PillReminderDatabase
    private lateinit var dao: DoseRecordDao
    private var vitamin = 0L
    private var probiotic = 0L

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, PillReminderDatabase::class.java).build()
        dao = db.doseRecordDao()
        vitamin = db.medicineDao().insert(MedicineEntity(name = "비타민", createdAt = 0))
        probiotic = db.medicineDao().insert(MedicineEntity(name = "유산균", createdAt = 1))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun findLatest_picksNewestAmongGivenMedicines() = runBlocking {
        dao.insert(DoseRecordEntity(medicineId = vitamin, takenAt = 100))
        dao.insert(DoseRecordEntity(medicineId = vitamin, takenAt = 300))
        dao.insert(DoseRecordEntity(medicineId = probiotic, takenAt = 500))

        assertEquals(300L, dao.findLatest(listOf(vitamin))?.takenAt)
        assertEquals(500L, dao.findLatest(listOf(vitamin, probiotic))?.takenAt)
    }

    @Test
    fun findLatest_returnsNullWithoutRecords() = runBlocking {
        assertNull(dao.findLatest(listOf(vitamin)))
    }

    @Test
    fun observeLastTakenAt_returnsMaxPerMedicine() = runBlocking {
        dao.insert(DoseRecordEntity(medicineId = vitamin, takenAt = 300))
        dao.insert(DoseRecordEntity(medicineId = vitamin, takenAt = 100))
        dao.insert(DoseRecordEntity(medicineId = probiotic, takenAt = 200))

        val rows = dao.observeLastTakenAt().first().associate { it.medicineId to it.takenAt }

        assertEquals(mapOf(vitamin to 300L, probiotic to 200L), rows)
    }
}
