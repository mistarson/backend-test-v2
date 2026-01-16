package im.bigs.pg.application.payment.service

import im.bigs.pg.application.log.LoggingPort
import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.factory.ApplicationTestDataFactory
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.exception.PgClientNotFoundException
import im.bigs.pg.application.pg.service.PgApprovalService
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PaymentServiceTest {
    private val partnerRepo = mockk<PartnerOutPort>()
    private val feeRepo = mockk<FeePolicyOutPort>()
    private val paymentRepo = mockk<PaymentOutPort>()
    private val pgApprovalService = mockk<PgApprovalService>()
    private val logger = mockk<LoggingPort>(relaxed = true)
    private val service = PaymentService(partnerRepo, feeRepo, paymentRepo, pgApprovalService, logger)

    @Test
    @DisplayName("정상 결제 승인 및 저장 - 모든 필드 검증")
    fun `정상 결제 승인 및 저장 시 모든 필드가 올바르게 설정되어야 한다`() {
        val partnerId = 1L
        val amount = BigDecimal("10000")
        val cardBin = "123456"
        val cardLast4 = "4242"
        val productName = "테스트 상품"
        val approvalCode = "APPROVAL-456"
        val approvedAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0)
        val feePolicy = ApplicationTestDataFactory.defaultFeePolicy(partnerId = partnerId)

        every { partnerRepo.findById(partnerId) } returns ApplicationTestDataFactory.defaultPartner(partnerId)
        every { feeRepo.findEffectivePolicy(partnerId, any()) } returns feePolicy
        every { pgApprovalService.approve(any()) } returns ApplicationTestDataFactory.defaultPgApproveResult(
            approvalCode = approvalCode,
            approvedAt = approvedAt
        )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 100L) }

        val cmd = ApplicationTestDataFactory.defaultPaymentCommand(
            partnerId = partnerId,
            amount = amount,
            cardBin = cardBin,
            cardLast4 = cardLast4,
            productName = productName
        )
        val result = service.pay(cmd)

        // Payment 필드 검증
        assertEquals(100L, result.id)
        assertEquals(partnerId, result.partnerId)
        assertEquals(amount, result.amount)
        assertEquals(feePolicy.percentage, result.appliedFeeRate)
        assertEquals(BigDecimal("450"), result.feeAmount) // 10000 * 0.025 + 200 = 450
        assertEquals(BigDecimal("9550"), result.netAmount) // 10000 - 450 = 9550
        assertEquals(cardBin, result.cardBin)
        assertEquals(cardLast4, result.cardLast4)
        assertEquals(approvalCode, result.approvalCode)
        assertEquals(approvedAt, result.approvedAt)
        assertEquals(PaymentStatus.APPROVED, result.status)
    }

    @Test
    @DisplayName("선택적 필드가 null인 경우 - null 값이 올바르게 처리되어야 한다")
    fun `선택적 필드가 null인 경우 null 값이 올바르게 처리되어야 한다`() {
        val partnerId = 2L
        val amount = BigDecimal("5000")
        val feePolicy = ApplicationTestDataFactory.defaultFeePolicy(
            id = 20L,
            partnerId = partnerId,
            percentage = BigDecimal("0.0200"),
            fixedFee = BigDecimal("100")
        )

        every { partnerRepo.findById(partnerId) } returns ApplicationTestDataFactory.defaultPartner(partnerId)
        every { feeRepo.findEffectivePolicy(partnerId, any()) } returns feePolicy
        every { pgApprovalService.approve(any()) } returns ApplicationTestDataFactory.defaultPgApproveResult(
            approvalCode = "APPROVAL-789",
            approvedAt = LocalDateTime.of(2024, 2, 1, 12, 0, 0)
        )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 200L) }

        val cmd = ApplicationTestDataFactory.defaultPaymentCommand(
            partnerId = partnerId,
            amount = amount,
            cardBin = null,
            cardLast4 = null,
            productName = null
        )
        val result = service.pay(cmd)

        assertEquals(200L, result.id)
        assertNull(result.cardBin)
        assertNull(result.cardLast4)
        assertEquals(partnerId, result.partnerId)
        assertEquals(amount, result.amount)
    }

    @Test
    @DisplayName("승인 결과가 CANCELED인 경우 - Payment의 status가 CANCELED로 저장되어야 한다")
    fun `PG 승인 결과가 CANCELED인 경우 Payment의 status가 CANCELED로 저장되어야 한다`() {
        val partnerId = 3L
        val amount = BigDecimal("8000")
        val feePolicy = ApplicationTestDataFactory.defaultFeePolicy(
            id = 30L,
            partnerId = partnerId,
            percentage = BigDecimal("0.0150"),
            fixedFee = BigDecimal("50")
        )

        every { partnerRepo.findById(partnerId) } returns ApplicationTestDataFactory.defaultPartner(partnerId)
        every { feeRepo.findEffectivePolicy(partnerId, any()) } returns feePolicy
        every { pgApprovalService.approve(any()) } returns ApplicationTestDataFactory.defaultPgApproveResult(
            approvalCode = "APPROVAL-CANCELED",
            approvedAt = LocalDateTime.of(2024, 3, 1, 15, 45, 0),
            status = PaymentStatus.CANCELED
        )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 300L) }

        val cmd = ApplicationTestDataFactory.defaultPaymentCommand(partnerId = partnerId, amount = amount)
        val result = service.pay(cmd)

        assertEquals(300L, result.id)
        assertEquals(PaymentStatus.CANCELED, result.status)
        assertEquals("APPROVAL-CANCELED", result.approvalCode)
    }

    @Test
    @DisplayName("Partner가 존재하지 않는 경우 - IllegalArgumentException 발생")
    fun `Partner가 존재하지 않는 경우 IllegalArgumentException이 발생해야 한다`() {
        val partnerId = 999L

        every { partnerRepo.findById(partnerId) } returns null

        val cmd = ApplicationTestDataFactory.defaultPaymentCommand(partnerId = partnerId)
        val exception = assertFailsWith<IllegalArgumentException> {
            service.pay(cmd)
        }

        assertEquals("Partner not found: $partnerId", exception.message)
        verify(exactly = 0) { feeRepo.findEffectivePolicy(any()) }
        verify(exactly = 0) { pgApprovalService.approve(any()) }
        verify(exactly = 0) { paymentRepo.save(any()) }
    }

    @Test
    @DisplayName("Partner가 비활성화된 경우 - IllegalArgumentException 발생")
    fun `Partner가 비활성화된 경우 IllegalArgumentException이 발생해야 한다`() {
        val partnerId = 4L
        val inactivePartner = ApplicationTestDataFactory.defaultPartner(
            id = partnerId,
            code = "INACTIVE",
            name = "Inactive Partner",
            active = false
        )

        every { partnerRepo.findById(partnerId) } returns inactivePartner

        val cmd = ApplicationTestDataFactory.defaultPaymentCommand(partnerId = partnerId)
        val exception = assertFailsWith<IllegalArgumentException> {
            service.pay(cmd)
        }

        assertEquals("Partner is inactive: $partnerId", exception.message)
        verify(exactly = 0) { feeRepo.findEffectivePolicy(any()) }
        verify(exactly = 0) { pgApprovalService.approve(any()) }
        verify(exactly = 0) { paymentRepo.save(any()) }
    }

    @Test
    @DisplayName("유효한 FeePolicy가 없는 경우 - IllegalStateException 발생")
    fun `유효한 FeePolicy가 없는 경우 IllegalStateException이 발생해야 한다`() {
        val partnerId = 5L

        every { partnerRepo.findById(partnerId) } returns ApplicationTestDataFactory.defaultPartner(partnerId)
        every { feeRepo.findEffectivePolicy(partnerId, any()) } returns null

        val cmd = ApplicationTestDataFactory.defaultPaymentCommand(partnerId = partnerId)
        val exception = assertFailsWith<IllegalStateException> {
            service.pay(cmd)
        }

        assertEquals("No effective fee policy for partner $partnerId", exception.message)
        verify(exactly = 0) { pgApprovalService.approve(any()) }
        verify(exactly = 0) { paymentRepo.save(any()) }
    }

    @Test
    @DisplayName("PG Client를 찾을 수 없는 경우 - PgClientNotFoundException 전파")
    fun `PG Client를 찾을 수 없는 경우 PgClientNotFoundException이 전파되어야 한다`() {
        val partnerId = 6L
        val feePolicy = ApplicationTestDataFactory.defaultFeePolicy(
            id = 60L,
            partnerId = partnerId,
            percentage = BigDecimal("0.0100"),
            fixedFee = BigDecimal("0")
        )

        every { partnerRepo.findById(partnerId) } returns ApplicationTestDataFactory.defaultPartner(partnerId)
        every { feeRepo.findEffectivePolicy(partnerId, any()) } returns feePolicy
        val pgException = PgClientNotFoundException("No supported PG found for partner")
        every { pgApprovalService.approve(any()) } throws pgException

        val cmd = ApplicationTestDataFactory.defaultPaymentCommand(partnerId = partnerId)
        val exception = assertFailsWith<PgClientNotFoundException> {
            service.pay(cmd)
        }

        assertEquals(pgException, exception)
        verify(exactly = 0) { paymentRepo.save(any()) }
    }

    @Test
    @DisplayName("Payment 저장 실패 시나리오 - 예외가 그대로 전파되어야 한다")
    fun `Payment 저장 실패 시 예외가 그대로 전파되어야 한다`() {
        val partnerId = 7L
        val feePolicy = ApplicationTestDataFactory.defaultFeePolicy(
            id = 70L,
            partnerId = partnerId,
            percentage = BigDecimal("0.0200"),
            fixedFee = BigDecimal("100")
        )
        val saveException = RuntimeException("Database connection failed")

        every { partnerRepo.findById(partnerId) } returns ApplicationTestDataFactory.defaultPartner(partnerId)
        every { feeRepo.findEffectivePolicy(partnerId, any()) } returns feePolicy
        every { pgApprovalService.approve(any()) } returns ApplicationTestDataFactory.defaultPgApproveResult(
            approvalCode = "APPROVAL-999",
            approvedAt = LocalDateTime.of(2024, 4, 1, 9, 0, 0)
        )
        every { paymentRepo.save(any()) } throws saveException

        val cmd = ApplicationTestDataFactory.defaultPaymentCommand(partnerId = partnerId)
        val exception = assertFailsWith<RuntimeException> {
            service.pay(cmd)
        }

        assertEquals(saveException, exception)
    }
}
