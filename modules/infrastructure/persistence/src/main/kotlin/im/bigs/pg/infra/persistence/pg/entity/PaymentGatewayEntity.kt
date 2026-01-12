package im.bigs.pg.infra.persistence.pg.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "payment_gateway")
class PaymentGatewayEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false, unique = true, length = 32)
    var code: String,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var priority: Int = 0,
    @Column(nullable = false)
    var active: Boolean = true,
)

