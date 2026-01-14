package im.bigs.pg.external.pg.testpg.dto

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * TestPG API 응답 데이터 모델 (평문 JSON).
 * API 문서: https://api-test-pg.bigs.im/docs/index.html
 */
data class TestPgResponse(
    val approvalCode: String,
    val approvedAt: String, // ISO-8601 형식 문자열 (예: "2025-10-08T03:31:34.181568")
    val maskedCardLast4: String,
    val amount: Long,
    val status: String,
) {
    /**
     * approvedAt 문자열을 LocalDateTime으로 변환합니다.
     */
    fun toLocalDateTime(): LocalDateTime {
        return LocalDateTime.parse(approvedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}

