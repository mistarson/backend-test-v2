package im.bigs.pg.application.payment.port.out

import im.bigs.pg.application.payment.util.CursorEncoder
import im.bigs.pg.domain.payment.Payment
import java.time.Instant

/** 페이지 결과와 통계를 함께 반환. */
data class PaymentPageWithSummary(
    val items: List<Payment>,
    val summary: PaymentSummaryProjection,
    val hasNext: Boolean,
    val nextCursorCreatedAt: Instant?,
    val nextCursorId: Long?,
) {
    /**
     * 다음 페이지 커서를 생성합니다.
     * hasNext가 true일 때만 커서를 반환하고, false이면 null을 반환합니다.
     */
    fun toNextCursor(): String? =
        takeIf { hasNext }?.let {
            CursorEncoder.encode(nextCursorCreatedAt, nextCursorId)
        }
}
