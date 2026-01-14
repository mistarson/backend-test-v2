package im.bigs.pg.application.pg.resolver

import im.bigs.pg.application.pg.port.out.PartnerPgSupportOutPort
import im.bigs.pg.domain.pg.PgCode
import org.springframework.stereotype.Component

/**
 * 우선순위 기반 PG Provider Code 선택 전략 구현체.
 * - 우선순위 순으로 정렬된 PG Provider Code 목록을 반환합니다.
 */
@Component
class PriorityBasedPgClientResolver(
    private val partnerPgSupportOutPort: PartnerPgSupportOutPort,
) : PgClientResolver {
    override fun resolve(partnerId: Long): List<PgCode> {
        return partnerPgSupportOutPort.findPgCodesByPriority(partnerId)
    }
}
