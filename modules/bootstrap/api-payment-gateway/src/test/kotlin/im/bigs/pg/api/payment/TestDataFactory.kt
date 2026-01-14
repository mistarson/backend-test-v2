package im.bigs.pg.api.payment

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.pg.port.out.PartnerPgSupportOutPort
import im.bigs.pg.application.pg.port.out.PaymentGatewayOutPort
import im.bigs.pg.domain.partner.FeePolicy
import im.bigs.pg.domain.partner.Partner
import im.bigs.pg.domain.pg.PaymentGateway
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 테스트 데이터 생성을 위한 Factory 클래스.
 */
class TestDataFactory(
    private val partnerPort: PartnerOutPort,
    private val feePolicyPort: FeePolicyOutPort,
    private val partnerPgSupportPort: PartnerPgSupportOutPort,
    private val paymentGatewayPort: PaymentGatewayOutPort
) {
    /**
     * 파트너를 생성하고 반환합니다.
     */
    fun createPartner(code: String, name: String, active: Boolean = true): Partner {
        return partnerPort.save(code, name, active)
    }

    /**
     * 수수료 정책을 생성하고 저장합니다.
     */
    fun createFeePolicy(
        partnerId: Long,
        effectiveFrom: String,
        percentage: BigDecimal,
        fixedFee: BigDecimal
    ): FeePolicy {
        val instant = java.time.Instant.parse(effectiveFrom)
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        return feePolicyPort.save(
            partnerId = partnerId,
            effectiveFrom = localDateTime,
            percentage = percentage,
            fixedFee = fixedFee
        )
    }


    /**
     * PaymentGateway를 생성합니다.
     */
    fun createPaymentGateway(pgCode: String): PaymentGateway {
        return paymentGatewayPort.save(
            code = pgCode,
            name = pgCode,
            priority = 0,
            active = true
        )
    }

    /**
     * Partner와 PaymentGateway를 연결합니다.
     */
    fun createPartnerPgSupport(
        partnerId: Long,
        paymentGatewayId: Long
    ) {
        partnerPgSupportPort.save(partnerId, paymentGatewayId)
    }

}
