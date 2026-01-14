package im.bigs.pg.infra.persistence.pg.adapter

import im.bigs.pg.application.pg.port.out.PaymentGatewayOutPort
import im.bigs.pg.domain.pg.PaymentGateway
import im.bigs.pg.infra.persistence.pg.entity.PaymentGatewayEntity
import im.bigs.pg.infra.persistence.pg.repository.PaymentGatewayJpaRepository
import org.springframework.stereotype.Component

@Component
class PaymentGatewayPersistenceAdapter(
    private val repository: PaymentGatewayJpaRepository,
) : PaymentGatewayOutPort {
    override fun count(): Long = repository.count()

    override fun save(code: String, name: String, priority: Int, active: Boolean): PaymentGateway {
        return repository.save(
            PaymentGatewayEntity(
                code = code,
                name = name,
                priority = priority,
                active = active
            )
        ).toDomain()
    }

    private fun PaymentGatewayEntity.toDomain() =
        PaymentGateway(
            id = this.id,
            code = this.code,
            name = this.name,
            priority = this.priority,
            active = this.active
        )
}

