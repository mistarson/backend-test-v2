package im.bigs.pg.application.payment.service

import im.bigs.pg.application.log.LoggingPort
import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.service.PgApprovalService
import im.bigs.pg.domain.calculation.FeeCalculator
import im.bigs.pg.domain.payment.Payment
import org.springframework.stereotype.Service

/**
 * 결제 생성 유스케이스 구현체.
 */
@Service
class PaymentService(
    private val partnerRepository: PartnerOutPort,
    private val feePolicyRepository: FeePolicyOutPort,
    private val paymentRepository: PaymentOutPort,
    private val pgApprovalService: PgApprovalService,
    private val logger: LoggingPort,
) : PaymentUseCase {
    override fun pay(command: PaymentCommand): Payment {
        logger.debug("결제 처리 시작: partnerId={}, amount={}", command.partnerId, command.amount)

        val partner = partnerRepository.findById(command.partnerId)
            ?: throw IllegalArgumentException("Partner not found: ${command.partnerId}")
        require(partner.active) { "Partner is inactive: ${partner.id}" }

        val feePolicy = feePolicyRepository.findEffectivePolicy(partner.id)
            ?: throw IllegalStateException("No effective fee policy for partner ${partner.id}")

        logger.debug("수수료 정책 적용: partnerId={}, rate={}, fixedFee={}", partner.id, feePolicy.percentage, feePolicy.fixedFee)

        val approve = pgApprovalService.approve(
            PgApproveRequest(
                partnerId = partner.id,
                amount = command.amount,
                cardBin = command.cardBin,
                cardLast4 = command.cardLast4,
                productName = command.productName,
            ),
        )

        val (fee, net) = FeeCalculator.calculateFee(
            amount = command.amount,
            rate = feePolicy.percentage,
            fixed = feePolicy.fixedFee
        )

        val payment = Payment(
            partnerId = partner.id,
            amount = command.amount,
            appliedFeeRate = feePolicy.percentage,
            feeAmount = fee,
            netAmount = net,
            cardBin = command.cardBin,
            cardLast4 = command.cardLast4,
            approvalCode = approve.approvalCode,
            approvedAt = approve.approvedAt,
            status = approve.status,
        )

        val saved = paymentRepository.save(payment)
        logger.info(
            "결제 처리 완료: paymentId={}, partnerId={}, amount={}, fee={}, netAmount={}, status={}",
            saved.id,
            saved.partnerId,
            saved.amount,
            saved.feeAmount,
            saved.netAmount,
            saved.status,
        )
        return saved
    }
}
