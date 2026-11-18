package com.mg.pillreminder.domain

import com.mg.pillreminder.domain.model.Medicine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MedicineNameMatcherTest {

    private val medicines = listOf(medicine(1, "생리통약"), medicine(2, "Vitamin D"), medicine(3, "비타민"))

    @Test
    fun `띄어쓰기가 달라도 찾는다`() {
        assertEquals(listOf(1L), matchedIds(" 생리 통약 "))
    }

    @Test
    fun `영문 대소문자가 달라도 찾는다`() {
        assertEquals(listOf(2L), matchedIds("vitamind"))
    }

    @Test
    fun `부분 일치는 찾지 않는다`() {
        assertEquals(emptyList<Long>(), matchedIds("비타"))
        assertEquals(emptyList<Long>(), matchedIds("비타민C"))
    }

    @Test
    fun `같은 이름이 여러 개면 모두 돌려준다`() {
        val matched = MedicineNameMatcher.matches("비타민", medicines + medicine(4, "비타 민"))

        assertEquals(listOf(3L, 4L), matched.map { it.id })
    }

    @Test
    fun `빈 이름은 아무것도 찾지 않는다`() {
        val withBlank = medicines + medicine(5, " ")

        assertEquals(emptyList<Long>(), MedicineNameMatcher.matches("  ", withBlank).map { it.id })
    }

    private fun matchedIds(query: String) = MedicineNameMatcher.matches(query, medicines).map { it.id }

    private fun medicine(id: Long, name: String) = Medicine(id = id, name = name, createdAt = Instant.EPOCH)
}
