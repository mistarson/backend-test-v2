package im.bigs.pg.infra.persistence.pg.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "partner_pg_support")
class PartnerPgSupportEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var partnerId: Long,
    @Column(nullable = false, name = "payment_gateway_id")
    var paymentGatewayId: Long,
)
