package im.bigs.pg.application.payment.factory

import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.domain.partner.FeePolicy
import im.bigs.pg.domain.partner.Partner
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Application 모듈 테스트를 위한 통합 테스트 데이터 팩토리.
 * 기본값을 제공하여 테스트 코드를 간결하게 유지합니다.
 */
object ApplicationTestDataFactory {
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

    /**
     * 테스트용 Payment를 생성합니다.
     */
    fun createPayment(
        id: Long,
        partnerId: Long,
        amount: BigDecimal,
        status: PaymentStatus,
        createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    ): Payment {
        return Payment(
            id = id,
            partnerId = partnerId,
            amount = amount,
            appliedFeeRate = BigDecimal("0.0300"),
            feeAmount = amount.multiply(BigDecimal("0.0300")).setScale(0, java.math.RoundingMode.HALF_UP),
            netAmount = amount.multiply(BigDecimal("0.9700")).setScale(0, java.math.RoundingMode.HALF_UP),
            cardLast4 = "4242",
            approvalCode = "APP$id",
            approvedAt = createdAt,
            status = status,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }
}
