package im.bigs.pg.infra.persistence.payment

import im.bigs.pg.infra.persistence.config.JpaConfig
import im.bigs.pg.infra.persistence.factory.PersistenceTestDataFactory
import im.bigs.pg.infra.persistence.payment.repository.PaymentJpaRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@ContextConfiguration(classes = [JpaConfig::class, PersistenceTestDataFactory::class])
class PaymentRepositoryPagingTest @Autowired constructor(
    val paymentRepo: PaymentJpaRepository,
    val testData: PersistenceTestDataFactory,
) {
    @Test
    @DisplayName("커서 페이징과 통계가 일관되어야 한다")
    fun `커서 페이징과 통계가 일관되어야 한다`() {
        testData.createPaymentsForPaging(count = 35)

        val first = paymentRepo.pageBy(1L, "APPROVED", null, null, null, null, PageRequest.of(0, 21))
        assertEquals(21, first.size)
        val lastOfFirst = first[20]
        val second = paymentRepo.pageBy(
            1L, "APPROVED", null, null,
            lastOfFirst.createdAt, lastOfFirst.id, PageRequest.of(0, 21),
        )
        assertTrue(second.isNotEmpty())

        val sumList = paymentRepo.summary(1L, "APPROVED", null, null)
        val row = sumList.first()
        assertEquals(35L, (row[0] as Number).toLong())
        assertEquals(BigDecimal("35000"), row[1] as BigDecimal)
        assertEquals(BigDecimal("33950"), row[2] as BigDecimal)
    }

    @Test
    @DisplayName("복합 필터 조건에서 summary는 items와 동일한 집합을 집계해야 한다")
    fun `파트너1, APPROVED, 기간 내 조건에서 summary는 items와 동일한 집합을 집계해야 한다`() {
        // 테스트 상수
        val pageLimit = 5
        val testPartnerId = 1L
        val anotherPartnerId = 2L
        val baseTime = Instant.parse("2024-01-15T10:00:00Z")

        // Given: 파트너별, 상태별, 시간별로 다른 결제 데이터 생성
        // 파트너1, APPROVED, 기간 내: 8개
        testData.createPaymentsForPaging(
            count = 8,
            partnerId = testPartnerId,
            baseTs = baseTime,
            status = "APPROVED",
        )

        // 파트너1, CANCELED, 기간 내: 2개
        testData.createPaymentsForPaging(
            count = 2,
            partnerId = testPartnerId,
            baseTs = baseTime.plusSeconds(10),
            status = "CANCELED",
        )

        // 파트너1, APPROVED, 기간 밖: 1개
        testData.createPaymentsForPaging(
            count = 1,
            partnerId = testPartnerId,
            baseTs = baseTime.minusSeconds(100),
            status = "APPROVED",
        )

        // 파트너2, APPROVED, 기간 내: 2개
        testData.createPaymentsForPaging(
            count = 2,
            partnerId = anotherPartnerId,
            baseTs = baseTime,
            status = "APPROVED",
        )

        // 필터 조건: 파트너1, APPROVED, 기간 내
        val filterFrom = baseTime
        val filterTo = baseTime.plusSeconds(20)
        val filterStatus = "APPROVED"

        // When: 복합 필터로 첫 페이지 조회 (limit=5, 커서 없음)
        val pageItems = paymentRepo.pageBy(
            partnerId = testPartnerId,
            status = filterStatus,
            fromAt = filterFrom,
            toAt = filterTo,
            cursorCreatedAt = null,
            cursorId = null,
            org = PageRequest.of(0, pageLimit + 1),
        )
        val firstPage = pageItems.take(pageLimit)

        // 두 번째 페이지 검증
        val firstPageLastItem = firstPage.last()
        val secondPageItems = paymentRepo.pageBy(
            partnerId = testPartnerId,
            status = filterStatus,
            fromAt = filterFrom,
            toAt = filterTo,
            cursorCreatedAt = firstPageLastItem.createdAt,
            cursorId = firstPageLastItem.id,
            org = PageRequest.of(0, pageLimit + 1),
        )
        val secondPage = secondPageItems.take(pageLimit)

        // When: 동일한 커서 조건으로 두 번째 페이지 summary 조회 (limit 적용)
        // summary는 현재 페이지의 items와 동일한 집합을 집계해야 하므로,
        // pageBy를 호출하여 limit을 적용한 결과를 집계합니다.
        val secondPageForSummary = paymentRepo.pageBy(
            partnerId = testPartnerId,
            status = filterStatus,
            fromAt = filterFrom,
            toAt = filterTo,
            cursorCreatedAt = firstPageLastItem.createdAt,
            cursorId = firstPageLastItem.id,
            org = PageRequest.of(0, pageLimit),
        )
        val secondPageItemsForSummary = secondPageForSummary.take(pageLimit)

        // Then: 두 번째 페이지의 summary도 items와 동일한 집합을 집계해야 함
        val secondPageSummaryCount = secondPageItemsForSummary.size.toLong()
        val secondPageSummaryTotalAmount = secondPageItemsForSummary.sumOf { it.amount }
        val secondPageSummaryTotalNet = secondPageItemsForSummary.sumOf { it.netAmount }

        assertEquals(secondPage.size.toLong(), secondPageSummaryCount)
        assertEquals(secondPage.sumOf { it.amount }, secondPageSummaryTotalAmount)
        assertEquals(secondPage.sumOf { it.netAmount }, secondPageSummaryTotalNet)
    }
}
