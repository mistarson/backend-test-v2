package im.bigs.pg.infra.persistence.factory

import im.bigs.pg.infra.persistence.partner.entity.FeePolicyEntity
import im.bigs.pg.infra.persistence.partner.entity.PartnerEntity
import im.bigs.pg.infra.persistence.partner.repository.FeePolicyJpaRepository
import im.bigs.pg.infra.persistence.partner.repository.PartnerJpaRepository
import im.bigs.pg.infra.persistence.payment.entity.PaymentEntity
import im.bigs.pg.infra.persistence.payment.repository.PaymentJpaRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

/**
 * Persistence 모듈 테스트 데이터 생성을 위한 Factory 클래스.
 * - 테스트에서 필요한 엔티티(Partner, FeePolicy, Payment)를 동적으로 생성합니다.
 * - Spring Component로 등록되어 테스트에서 @Autowired로 주입받아 사용합니다.
 */
@Component
class PersistenceTestDataFactory(
    private val partnerRepo: PartnerJpaRepository,
    private val feePolicyRepo: FeePolicyJpaRepository,
    private val paymentRepo: PaymentJpaRepository
) {
    /**
     * 파트너를 생성하고 반환합니다.
     */
    fun createPartner(code: String, name: String, active: Boolean = true): PartnerEntity {
        return partnerRepo.save(
            PartnerEntity(
                code = code,
                name = name,
                active = active
            )
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
    ) {
        feePolicyRepo.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse(effectiveFrom),
                percentage = percentage,
                fixedFee = fixedFee
            )
        )
    }

    /**
     * 여러 수수료 정책을 한 번에 생성합니다.
     * @param partnerId 파트너 ID
     * @param policies Triple(effectiveFrom, percentage, fixedFee) 리스트
     */
    fun createFeePolicies(
        partnerId: Long,
        vararg policies: Triple<String, BigDecimal, BigDecimal>
    ) {
        policies.forEach { (effectiveFrom, percentage, fixedFee) ->
            createFeePolicy(partnerId, effectiveFrom, percentage, fixedFee)
        }
    }

    /**
     * Payment를 생성하고 저장합니다.
     */
    fun createPayment(
        partnerId: Long,
        amount: BigDecimal,
        appliedFeeRate: BigDecimal,
        feeAmount: BigDecimal,
        netAmount: BigDecimal,
        cardBin: String? = null,
        cardLast4: String? = null,
        approvalCode: String,
        approvedAt: Instant,
        status: String,
        createdAt: Instant,
        updatedAt: Instant,
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
                updatedAt = updatedAt,
            )
        )
    }

    /**
     * 여러 Payment를 순차적으로 생성합니다 (페이징 테스트용).
     * 각 Payment는 baseTs에서 i초씩 증가한 시간을 가집니다.
     */
    fun createPaymentsForPaging(
        count: Int,
        partnerId: Long = 1L,
        amount: BigDecimal = BigDecimal("1000"),
        appliedFeeRate: BigDecimal = BigDecimal("0.0300"),
        feeAmount: BigDecimal = BigDecimal("30"),
        netAmount: BigDecimal = BigDecimal("970"),
        baseTs: Instant = Instant.parse("2024-01-01T00:00:00Z"),
        status: String = "APPROVED",
    ): List<PaymentEntity> {
        return (0 until count).map { i ->
            val timestamp = baseTs.plusSeconds(i.toLong())
            createPayment(
                partnerId = partnerId,
                amount = amount,
                appliedFeeRate = appliedFeeRate,
                feeAmount = feeAmount,
                netAmount = netAmount,
                cardBin = null,
                cardLast4 = "%04d".format(i),
                approvalCode = "A$i",
                approvedAt = timestamp,
                status = status,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        }
    }
}
