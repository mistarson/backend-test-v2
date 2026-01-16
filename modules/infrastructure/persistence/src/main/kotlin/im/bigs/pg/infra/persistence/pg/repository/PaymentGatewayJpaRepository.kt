package im.bigs.pg.infra.persistence.pg.repository

import im.bigs.pg.infra.persistence.pg.entity.PaymentGatewayEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentGatewayJpaRepository : JpaRepository<PaymentGatewayEntity, Long> {
    fun findByCode(code: String): PaymentGatewayEntity?
}
