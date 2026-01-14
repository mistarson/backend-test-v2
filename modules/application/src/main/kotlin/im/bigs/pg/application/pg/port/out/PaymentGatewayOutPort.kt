package im.bigs.pg.application.pg.port.out

import im.bigs.pg.domain.pg.PaymentGateway

interface PaymentGatewayOutPort {
    fun count(): Long
    
    fun save(code: String, name: String, priority: Int, active: Boolean): PaymentGateway
}

