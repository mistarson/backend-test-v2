package im.bigs.pg.application.partner.port.out

import im.bigs.pg.domain.partner.FeePolicy
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

interface FeePolicyOutPort {
    fun findEffectivePolicy(partnerId: Long, at: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)): FeePolicy?
    
    fun save(partnerId: Long, effectiveFrom: LocalDateTime, percentage: BigDecimal, fixedFee: BigDecimal?): FeePolicy
}
