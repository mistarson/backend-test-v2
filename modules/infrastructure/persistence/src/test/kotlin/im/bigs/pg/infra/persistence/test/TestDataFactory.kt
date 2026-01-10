package im.bigs.pg.infra.persistence.test

import im.bigs.pg.infra.persistence.partner.entity.FeePolicyEntity
import im.bigs.pg.infra.persistence.partner.entity.PartnerEntity
import im.bigs.pg.infra.persistence.partner.repository.FeePolicyJpaRepository
import im.bigs.pg.infra.persistence.partner.repository.PartnerJpaRepository
import java.math.BigDecimal
import java.time.Instant

/**
 * 테스트 데이터 생성을 위한 Factory 클래스.
 * - 테스트에서 필요한 엔티티(Partner, FeePolicy)를 동적으로 생성합니다.
 * - 상속 대신 객체 인스턴스로 사용하여 테스트 독립성을 유지합니다.
 */
class TestDataFactory(
    private val partnerRepo: PartnerJpaRepository,
    private val feePolicyRepo: FeePolicyJpaRepository
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
}
