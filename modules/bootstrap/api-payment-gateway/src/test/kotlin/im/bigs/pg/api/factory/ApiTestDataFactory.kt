package im.bigs.pg.api.factory

import im.bigs.pg.domain.partner.Partner
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.pg.PaymentGateway
import im.bigs.pg.infra.persistence.partner.entity.FeePolicyEntity
import im.bigs.pg.infra.persistence.partner.entity.PartnerEntity
import im.bigs.pg.infra.persistence.partner.repository.FeePolicyJpaRepository
import im.bigs.pg.infra.persistence.partner.repository.PartnerJpaRepository
import im.bigs.pg.infra.persistence.payment.entity.PaymentEntity
import im.bigs.pg.infra.persistence.payment.repository.PaymentJpaRepository
import im.bigs.pg.infra.persistence.pg.entity.PartnerPgSupportEntity
import im.bigs.pg.infra.persistence.pg.entity.PaymentGatewayEntity
import im.bigs.pg.infra.persistence.pg.repository.PartnerPgSupportJpaRepository
import im.bigs.pg.infra.persistence.pg.repository.PaymentGatewayJpaRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

/**
 * API 통합 테스트 데이터 생성을 위한 Factory 클래스
 */
@Component
class ApiTestDataFactory(
    private val partnerRepo: PartnerJpaRepository,
    private val feePolicyRepo: FeePolicyJpaRepository,
    private val paymentGatewayRepo: PaymentGatewayJpaRepository,
    private val partnerPgSupportRepo: PartnerPgSupportJpaRepository,
    private val paymentRepo: PaymentJpaRepository,
) {
    /**
     * 파트너를 생성하고 반환합니다.
     */
    fun createPartner(code: String, name: String, active: Boolean = true): Partner {
        val entity = partnerRepo.save(PartnerEntity(code = code, name = name, active = active))
        return Partner(
            id = entity.id!!,
            code = entity.code,
            name = entity.name,
            active = entity.active,
        )
    }

    /**
     * 수수료 정책을 생성하고 저장합니다.
     */
    fun createFeePolicy(
        partnerId: Long,
        effectiveFrom: String,
        percentage: BigDecimal,
        fixedFee: BigDecimal
    ): FeePolicyEntity {
        val instant = Instant.parse(effectiveFrom)
        val entity = feePolicyRepo.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = instant,
                percentage = percentage,
                fixedFee = fixedFee,
            )
        )
        return entity
    }

    /**
     * PaymentGateway를 생성하고 반환합니다.
     */
    fun createPaymentGateway(
        code: String = "TEST-PG",
        name: String = "테스트 PG사",
        priority: Int = 0,
        active: Boolean = true,
    ): PaymentGateway {
        val entity = paymentGatewayRepo.save(
            PaymentGatewayEntity(
                code = code,
                name = name,
                priority = priority,
                active = active,
            )
        )
        return PaymentGateway(
            id = entity.id,
            code = entity.code,
            name = entity.name,
            priority = entity.priority,
            active = entity.active,
        )
    }

    /**
     * PartnerPgSupport를 생성합니다.
     */
    fun createPartnerPgSupport(partnerId: Long, pgId: Long) {
        partnerPgSupportRepo.save(
            PartnerPgSupportEntity(
                partnerId = partnerId,
                paymentGatewayId = pgId,
            )
        )
    }

    /**
     * Payment를 생성하고 저장합니다.
     * 테스트 데이터 생성을 위해 Repository를 직접 사용합니다.
     */
    fun createPayment(
        partnerId: Long,
        amount: BigDecimal,
        appliedFeeRate: BigDecimal,
        feeAmount: BigDecimal,
        netAmount: BigDecimal,
        approvalCode: String = "APPROVAL-${System.currentTimeMillis()}",
        approvedAt: Instant = Instant.now(),
        createdAt: Instant = Instant.now(),
        status: String = "APPROVED",
        cardBin: String? = null,
        cardLast4: String? = "4242",
    ): PaymentEntity {
        return paymentRepo.save(
            PaymentEntity(
                partnerId = partnerId,
                amount = amount,
                appliedFeeRate = appliedFeeRate,
                feeAmount = feeAmount,
                netAmount = netAmount,
                cardBin = cardBin,
                cardLast4 = cardLast4,
                approvalCode = approvalCode,
                approvedAt = approvedAt,
                status = status,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        )
    }

    /**
     * Payment를 생성하고 저장합니다 (간편 버전).
     * 수수료율 3%로 고정하여 자동 계산합니다.
     */
    fun createPayment(
        partnerId: Long,
        amount: BigDecimal,
        createdAt: Instant,
        status: PaymentStatus = PaymentStatus.APPROVED,
    ): PaymentEntity {
        val appliedFeeRate = BigDecimal("0.0300")
        val feeAmount = amount.multiply(appliedFeeRate).setScale(0, java.math.RoundingMode.HALF_UP)
        val netAmount = amount - feeAmount
        
        return createPayment(
            partnerId = partnerId,
            amount = amount,
            appliedFeeRate = appliedFeeRate,
            feeAmount = feeAmount,
            netAmount = netAmount,
            approvalCode = if (status == PaymentStatus.APPROVED) "APPROVAL-${System.currentTimeMillis()}" else "CANCEL-${System.currentTimeMillis()}",
            approvedAt = createdAt,
            createdAt = createdAt,
            status = status.name,
            cardLast4 = "4242",
        )
    }
}
