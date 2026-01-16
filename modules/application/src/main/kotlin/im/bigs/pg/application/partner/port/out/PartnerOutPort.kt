package im.bigs.pg.application.partner.port.out

import im.bigs.pg.domain.partner.Partner

interface PartnerOutPort {
    fun findById(id: Long): Partner?

    fun save(code: String, name: String, active: Boolean): Partner
}
