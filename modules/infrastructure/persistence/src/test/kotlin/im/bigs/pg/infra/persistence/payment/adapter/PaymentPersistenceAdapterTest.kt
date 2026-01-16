package im.bigs.pg.infra.persistence.payment.adapter

import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.infra.persistence.config.JpaConfig
import im.bigs.pg.infra.persistence.factory.PersistenceTestDataFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DataJpaTest
@ContextConfiguration(classes = [PaymentPersistenceAdapter::class, JpaConfig::class, PersistenceTestDataFactory::class])
class PaymentPersistenceAdapterTest @Autowired constructor(
    val adapter: PaymentPersistenceAdapter,
    val testData: PersistenceTestDataFactory,
) {
    @Test
    @DisplayName("hasNext가 false일 때 nextCursor는 null이어야 한다")
    fun `hasNext와 nextCursor 일관성 hasNext가 false일 때 nextCursor는 null이어야 한다`() {
        // Given: 정확히 limit 개수만큼의 결제 생성 (마지막 페이지)
        val pageLimit = 2
        testData.createPaymentsForPaging(count = pageLimit)

        // When: 첫 페이지 조회
        val query = PaymentQuery(
            partnerId = 1L,
            status = PaymentStatus.APPROVED,
            limit = pageLimit,
        )
        val result = adapter.findPageWithSummary(query)

        // Then: hasNext가 false이면 nextCursor도 null이어야 함
        assertEquals(false, result.hasNext)
        assertNull(result.nextCursorCreatedAt)
        assertNull(result.nextCursorId)
        assertNull(result.toNextCursor())
    }

    @Test
    @DisplayName("hasNext가 true일 때 nextCursor는 null이 아니어야 한다")
    fun `hasNext와 nextCursor 일관성 hasNext가 true일 때 nextCursor는 null이 아니어야 한다`() {
        // Given: limit보다 많은 결제 생성
        val pageLimit = 2
        testData.createPaymentsForPaging(count = 5)

        // When: 첫 페이지 조회
        val query = PaymentQuery(
            partnerId = 1L,
            status = PaymentStatus.APPROVED,
            limit = pageLimit,
        )
        val result = adapter.findPageWithSummary(query)

        // Then: hasNext가 true이면 nextCursor도 null이 아니어야 함
        assertEquals(true, result.hasNext)
        assertNotNull(result.nextCursorCreatedAt)
        assertNotNull(result.nextCursorId)
        assertNotNull(result.toNextCursor())

        // nextCursor는 마지막 항목의 createdAt과 id와 일치해야 함
        val lastItem = result.items.last()
        assertEquals(lastItem.createdAt.toInstant(java.time.ZoneOffset.UTC), result.nextCursorCreatedAt)
        assertEquals(lastItem.id, result.nextCursorId)
    }
}
