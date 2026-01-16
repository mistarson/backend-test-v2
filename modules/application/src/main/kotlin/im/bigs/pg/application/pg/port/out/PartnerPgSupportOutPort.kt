package im.bigs.pg.application.pg.port.out

import im.bigs.pg.domain.pg.PgCode

interface PartnerPgSupportOutPort {
    /**
     * 제휴사가 지원하는 PG사 코드를 우선순위 순으로 조회합니다.
     *
     * @param partnerId 제휴사 ID
     * @return 우선순위 순으로 정렬된 PG사 코드 목록 (없으면 빈 리스트)
     */
    fun findPgCodesByPriority(partnerId: Long): List<PgCode>

    fun save(partnerId: Long, paymentGatewayId: Long)
}
