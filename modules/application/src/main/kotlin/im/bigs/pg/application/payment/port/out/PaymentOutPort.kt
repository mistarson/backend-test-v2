package im.bigs.pg.application.payment.port.out

import im.bigs.pg.domain.payment.Payment

/**
 * 영속성 어댑터가 구현할 출력 포트.
 * - 조회 결과는 `createdAt desc, id desc` 정렬을 전제로 합니다.
 * - findPageWithSummary는 items와 summary를 함께 반환하여 동일한 집합을 보장합니다.
 */
interface PaymentOutPort {
    fun save(payment: Payment): Payment

    fun findPageWithSummary(query: PaymentQuery): PaymentPageWithSummary
}
