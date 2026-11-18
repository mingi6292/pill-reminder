package com.mg.pillreminder.domain

import com.mg.pillreminder.domain.model.Medicine

/**
 * 음성 인식은 띄어쓰기와 영문 대소문자가 들쭉날쭉해서 둘 다 무시하고 완전 일치로 찾는다.
 * 부분 일치는 "비타민"이 "비타민D"를 잡는 식의 오답이 나와 쓰지 않는다.
 */
object MedicineNameMatcher {
    private val WHITESPACE = Regex("\\s+")

    fun normalize(name: String): String = name.replace(WHITESPACE, "").lowercase()

    fun matches(query: String, medicines: List<Medicine>): List<Medicine> {
        val key = normalize(query)
        if (key.isEmpty()) return emptyList()
        return medicines.filter { normalize(it.name) == key }
    }
}
