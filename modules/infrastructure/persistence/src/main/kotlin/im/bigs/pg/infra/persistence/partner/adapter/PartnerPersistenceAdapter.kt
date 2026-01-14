package im.bigs.pg.infra.persistence.partner.adapter

import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.domain.partner.Partner
import im.bigs.pg.infra.persistence.partner.entity.PartnerEntity
import im.bigs.pg.infra.persistence.partner.repository.PartnerJpaRepository
import org.springframework.stereotype.Component

@Component
class PartnerPersistenceAdapter(
    private val repo: PartnerJpaRepository,
) : PartnerOutPort {
    override fun findById(id: Long): Partner? =
        repo.findById(id).orElse(null)?.toDomain()

    override fun save(code: String, name: String, active: Boolean): Partner =
        repo.save(PartnerEntity(code = code, name = name, active = active)).toDomain()

    private fun PartnerEntity.toDomain() =
        Partner(id = this.id!!, code = this.code, name = this.name, active = this.active)
}
