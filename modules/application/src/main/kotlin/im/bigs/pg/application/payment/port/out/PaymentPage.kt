package im.bigs.pg.application.payment.port.out

import im.bigs.pg.domain.payment.Payment
import java.time.Instant

/** 페이지 결과. */
data class PaymentPage(
    val items: List<Payment>,
    val hasNext: Boolean,
    val nextCursorCreatedAt: Instant?,
    val nextCursorId: Long?,
)
