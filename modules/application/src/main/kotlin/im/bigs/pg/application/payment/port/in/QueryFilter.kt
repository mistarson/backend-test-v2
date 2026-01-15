package im.bigs.pg.application.payment.port.`in`

import com.fasterxml.jackson.annotation.JsonFormat
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.domain.payment.PaymentStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 결제 조회 조건.
 * - cursor 는 다음 페이지를 가리키는 토큰(Base64 URL-safe)
 * - 기간은 UTC 기준 권장
 */
data class QueryFilter(
    val partnerId: Long? = null,
    val status: String? = null,
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val from: LocalDateTime? = null,
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val to: LocalDateTime? = null,
    val cursor: String? = null,
    val limit: Int = 20,
) {
    /**
     * PaymentQuery로 변환합니다.
     * @param cursorCreatedAt 디코딩된 커서의 createdAt 값
     * @param cursorId 디코딩된 커서의 id 값
     */
    fun toPaymentQuery(cursorCreatedAt: Instant?, cursorId: Long?): PaymentQuery {
        return PaymentQuery(
            partnerId = this.partnerId,
            status = this.status?.let { PaymentStatus.valueOf(it) },
            from = this.from?.toInstant(ZoneOffset.UTC),
            to = this.to?.toInstant(ZoneOffset.UTC),
            limit = this.limit,
            cursorCreatedAt = cursorCreatedAt,
            cursorId = cursorId,
        )
    }
}

