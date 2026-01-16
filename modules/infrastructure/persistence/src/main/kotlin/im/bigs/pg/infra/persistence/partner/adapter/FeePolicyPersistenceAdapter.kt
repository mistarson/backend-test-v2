package im.bigs.pg.infra.persistence.partner.adapter

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.domain.partner.FeePolicy
import im.bigs.pg.infra.persistence.partner.entity.FeePolicyEntity
import im.bigs.pg.infra.persistence.partner.repository.FeePolicyJpaRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class FeePolicyPersistenceAdapter(
    private val repo: FeePolicyJpaRepository,
) : FeePolicyOutPort {
    override fun findEffectivePolicy(partnerId: Long, at: LocalDateTime): FeePolicy? =
        repo.findTop1ByPartnerIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(partnerId, at.toInstant(ZoneOffset.UTC))?.toDomain()

    override fun save(partnerId: Long, effectiveFrom: LocalDateTime, percentage: BigDecimal, fixedFee: BigDecimal?): FeePolicy =
        repo.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = effectiveFrom.toInstant(ZoneOffset.UTC),
                percentage = percentage,
                fixedFee = fixedFee
            )
        ).toDomain()

    private fun FeePolicyEntity.toDomain() =
        FeePolicy(
            id = this.id,
            partnerId = this.partnerId,
            effectiveFrom = LocalDateTime.ofInstant(this.effectiveFrom, ZoneOffset.UTC),
            percentage = this.percentage,
            fixedFee = this.fixedFee
        )
}
