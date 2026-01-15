package im.bigs.pg.application.payment.port.out

import im.bigs.pg.domain.payment.PaymentStatus
import java.time.Instant

/**
 * 결제 페이지 조회용 입력 값.
 * - 정렬 키(createdAt desc, id desc)를 커서로 사용합니다.
 */
data class PaymentQuery(
    val partnerId: Long? = null,
    val status: PaymentStatus? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = 20,
    val cursorCreatedAt: Instant? = null,
    val cursorId: Long? = null,
)
