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

    // 같은 이름을 시각별로 여러 번 등록할 수 있다. 1회 알림이 약마다 하나라 매번 같은 약을 골라야
    // "다시 예약"이 이전 예약을 바꾼다. 음성과 AppFunctions가 같은 약을 고르도록 여기서 정한다.
    fun findFirst(query: String, medicines: List<Medicine>): Medicine? =
        matches(query, medicines).minByOrNull { it.id }
}
