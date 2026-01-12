package im.bigs.pg.infra.persistence.pg.adapter

import im.bigs.pg.application.pg.port.out.PartnerPgSupportOutPort
import im.bigs.pg.domain.pg.PgCode
import im.bigs.pg.infra.persistence.pg.repository.PartnerPgSupportJpaRepository
import org.springframework.stereotype.Component

/**
 * 제휴사-PG사 지원 조회 어댑터.
 */
@Component
class PartnerPgSupportPersistenceAdapter(
    private val repository: PartnerPgSupportJpaRepository,
) : PartnerPgSupportOutPort {
    override fun findPgCodesByPriority(partnerId: Long): List<PgCode> {
        return repository.findPgCodesByPriority(partnerId)
            .map { PgCode.valueOf(it) }
    }
}

