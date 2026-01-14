package im.bigs.pg.application.pg.service

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.domain.payment.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * PgApprovalService 테스트를 위한 테스트 데이터 팩토리.
 * 기본값을 제공하여 테스트 코드를 간결하게 유지합니다.
 */
object TestDataFactory {
    /**
     * 기본 PgApproveRequest를 생성합니다.
     */
    fun defaultPgApproveRequest(
        partnerId: Long = 1L,
        amount: BigDecimal = BigDecimal("10000"),
        cardBin: String? = "123456",
        cardLast4: String? = "4242",
        productName: String? = "테스트 상품"
    ): PgApproveRequest = PgApproveRequest(
        partnerId = partnerId,
        amount = amount,
        cardBin = cardBin,
        cardLast4 = cardLast4,
        productName = productName
    )

    /**
     * 기본 PgApproveResult를 생성합니다.
     */
    fun defaultPgApproveResult(
        approvalCode: String = "APPROVAL-456",
        approvedAt: LocalDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0),
        status: PaymentStatus = PaymentStatus.APPROVED,
        maskedCardLast4: String? = null,
        amount: BigDecimal? = null
    ): PgApproveResult = PgApproveResult(
        approvalCode = approvalCode,
        approvedAt = approvedAt,
        status = status,
        maskedCardLast4 = maskedCardLast4,
        amount = amount
    )
}
