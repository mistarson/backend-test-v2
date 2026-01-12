package im.bigs.pg.infra.persistence.pg.repository

import im.bigs.pg.infra.persistence.partner.entity.PartnerEntity
import im.bigs.pg.infra.persistence.pg.entity.PartnerPgSupportEntity
import im.bigs.pg.infra.persistence.pg.entity.PaymentGatewayEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/** 제휴사-PG사 지원 조회용 JPA 리포지토리. */
interface PartnerPgSupportJpaRepository : JpaRepository<PartnerPgSupportEntity, Long> {
    /**
     * 제휴사가 지원하는 PG사 코드를 우선순위 순으로 조회합니다.
     * - partner.active = true, payment_gateway.active = true인 경우만 조회합니다.
     *
     * @param partnerId 제휴사 ID
     * @return 우선순위 순으로 정렬된 PG사 코드 목록 (없으면 빈 리스트)
     */
    @Query(
        """
        select p.code
        from PartnerPgSupportEntity s
        join PartnerEntity pt on s.partnerId = pt.id
        join PaymentGatewayEntity p on s.paymentGatewayId = p.id
        where s.partnerId = :partnerId
          and pt.active = true
          and p.active = true
        order by p.priority asc
        """,
    )
    fun findPgCodesByPriority(@Param("partnerId") partnerId: Long): List<String>
}

