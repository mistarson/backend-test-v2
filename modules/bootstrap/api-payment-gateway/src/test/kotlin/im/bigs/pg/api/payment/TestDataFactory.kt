package im.bigs.pg.api.payment

import im.bigs.pg.infra.persistence.partner.entity.FeePolicyEntity
import im.bigs.pg.infra.persistence.partner.entity.PartnerEntity
import im.bigs.pg.infra.persistence.partner.repository.FeePolicyJpaRepository
import im.bigs.pg.infra.persistence.partner.repository.PartnerJpaRepository
import java.math.BigDecimal
import java.time.Instant

/**
 * 테스트 데이터 생성을 위한 Factory 클래스.
 * 테스트 데이터 설정을 위해 리포지토리를 직접 의존합니다.
 */
class TestDataFactory(
    private val partnerRepo: PartnerJpaRepository,
    private val feePolicyRepo: FeePolicyJpaRepository,
) {
    /**
     * 파트너를 생성하고 반환합니다.
     */
    fun createPartner(code: String, name: String, active: Boolean = true): PartnerEntity {
        val entity = partnerRepo.save(PartnerEntity(code = code, name = name, active = active))
        return entity
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
}
