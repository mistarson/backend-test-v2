package im.bigs.pg.application.payment.port.out

import im.bigs.pg.domain.payment.PaymentSummary

/** 저장 계층에서 계산된 집계 결과. */
data class PaymentSummaryProjection(
    val count: Long,
    val totalAmount: java.math.BigDecimal,
    val totalNetAmount: java.math.BigDecimal,
) {
    /**
     * 도메인 모델인 PaymentSummary로 변환합니다.
     */
    fun toPaymentSummary(): PaymentSummary {
        return PaymentSummary(
            count = this.count,
            totalAmount = this.totalAmount,
            totalNetAmount = this.totalNetAmount,
        )
    }
}
