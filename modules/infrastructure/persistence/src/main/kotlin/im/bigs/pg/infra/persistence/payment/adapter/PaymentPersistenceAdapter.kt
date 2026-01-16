package im.bigs.pg.infra.persistence.payment.adapter

import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentPageWithSummary
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.application.payment.port.out.PaymentSummaryProjection
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.infra.persistence.payment.entity.PaymentEntity
import im.bigs.pg.infra.persistence.payment.repository.PaymentJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.ZoneOffset

/** PaymentOutPort 구현체(JPA 기반). */
@Component
class PaymentPersistenceAdapter(
    private val repo: PaymentJpaRepository,
) : PaymentOutPort {

    override fun save(payment: Payment): Payment =
        repo.save(payment.toEntity()).toDomain()

    override fun findPageWithSummary(query: PaymentQuery): PaymentPageWithSummary {
        val pageSize = query.limit
        val list = repo.pageBy(
            partnerId = query.partnerId,
            status = query.status?.name,
            fromAt = query.from,
            toAt = query.to,
            cursorCreatedAt = query.cursorCreatedAt,
            cursorId = query.cursorId,
            org = PageRequest.of(0, pageSize + 1),
        )
        val hasNext = list.size > pageSize
        val items = list.take(pageSize)
        val last = items.lastOrNull()

        // summary는 현재 페이지의 items와 동일한 집합을 집계합니다.
        return PaymentPageWithSummary(
            items = items.map { it.toDomain() },
            summary = PaymentSummaryProjection(
                count = items.size.toLong(),
                totalAmount = items.sumOf { it.amount },
                totalNetAmount = items.sumOf { it.netAmount },
            ),
            hasNext = hasNext,
            // hasNext가 false일 때는 커서 값을 null로 설정하여 일관성 유지
            nextCursorCreatedAt = if (hasNext) last?.createdAt else null,
            nextCursorId = if (hasNext) last?.id else null,
        )
    }

    /** 도메인 → 엔티티 매핑. */
    private fun Payment.toEntity() =
        PaymentEntity(
            id = this.id,
            partnerId = this.partnerId,
            amount = this.amount,
            appliedFeeRate = this.appliedFeeRate,
            feeAmount = this.feeAmount,
            netAmount = this.netAmount,
            cardBin = this.cardBin,
            cardLast4 = this.cardLast4,
            approvalCode = this.approvalCode,
            approvedAt = this.approvedAt.toInstant(ZoneOffset.UTC),
            status = this.status.name,
            createdAt = this.createdAt.toInstant(ZoneOffset.UTC),
            updatedAt = this.updatedAt.toInstant(ZoneOffset.UTC),
        )

    /** 엔티티 → 도메인 매핑. */
    private fun PaymentEntity.toDomain() =
        Payment(
            id = this.id,
            partnerId = this.partnerId,
            amount = this.amount,
            appliedFeeRate = this.appliedFeeRate,
            feeAmount = this.feeAmount,
            netAmount = this.netAmount,
            cardBin = this.cardBin,
            cardLast4 = this.cardLast4,
            approvalCode = this.approvalCode,
            approvedAt = java.time.LocalDateTime.ofInstant(this.approvedAt, ZoneOffset.UTC),
            status = PaymentStatus.valueOf(this.status),
            createdAt = java.time.LocalDateTime.ofInstant(this.createdAt, ZoneOffset.UTC),
            updatedAt = java.time.LocalDateTime.ofInstant(this.updatedAt, ZoneOffset.UTC),
        )
}
