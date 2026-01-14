package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.domain.partner.FeePolicy
import im.bigs.pg.domain.partner.Partner
import im.bigs.pg.domain.payment.PaymentStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * PaymentService 테스트를 위한 테스트 데이터 팩토리.
 * 기본값을 제공하여 테스트 코드를 간결하게 유지합니다.
 */
object TestDataFactory {
    /**
     * 기본 FeePolicy를 생성합니다.
     */
    fun defaultFeePolicy(
        id: Long = 10L,
        partnerId: Long = 1L,
        percentage: BigDecimal = BigDecimal("0.0250"),
        fixedFee: BigDecimal = BigDecimal("200"),
        effectiveFrom: LocalDateTime = LocalDateTime.ofInstant(
            Instant.parse("2020-01-01T00:00:00Z"),
            ZoneOffset.UTC
        )
    ): FeePolicy = FeePolicy(
        id = id,
        partnerId = partnerId,
        effectiveFrom = effectiveFrom,
        percentage = percentage,
        fixedFee = fixedFee
    )

    /**
     * 기본 Partner를 생성합니다.
     */
    fun defaultPartner(
        id: Long = 1L,
        code: String = "TEST",
        name: String = "Test Partner",
        active: Boolean = true
    ): Partner = Partner(
        id = id,
        code = code,
        name = name,
        active = active
    )

    /**
     * 기본 PaymentCommand를 생성합니다.
     */
    fun defaultPaymentCommand(
        partnerId: Long = 1L,
        amount: BigDecimal = BigDecimal("10000"),
        cardBin: String? = "123456",
        cardLast4: String? = "4242",
        productName: String? = "테스트 상품"
    ): PaymentCommand = PaymentCommand(
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
        status: PaymentStatus = PaymentStatus.APPROVED
    ): PgApproveResult = PgApproveResult(
        approvalCode = approvalCode,
        approvedAt = approvedAt,
        status = status
    )
}
