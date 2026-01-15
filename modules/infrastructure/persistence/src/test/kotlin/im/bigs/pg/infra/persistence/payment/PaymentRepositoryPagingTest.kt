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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@ContextConfiguration(classes = [JpaConfig::class, PersistenceTestDataFactory::class])
class 결제저장소커서페이징Test @Autowired constructor(
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
}
