package im.bigs.pg.infra.persistence.partner.adapter

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.infra.persistence.config.JpaConfig
import im.bigs.pg.infra.persistence.partner.entity.PartnerEntity
import im.bigs.pg.infra.persistence.partner.repository.FeePolicyJpaRepository
import im.bigs.pg.infra.persistence.partner.repository.PartnerJpaRepository
import im.bigs.pg.infra.persistence.test.TestDataFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DataJpaTest
@ContextConfiguration(classes = [FeePolicyPersistenceAdapter::class, JpaConfig::class])
@ActiveProfiles("test")
class FeePolicyPersistenceAdapterTest {

    @Autowired
    lateinit var feePolicyRepository: FeePolicyOutPort

    @Autowired
    lateinit var partnerRepo: PartnerJpaRepository

    @Autowired
    lateinit var feePolicyRepo: FeePolicyJpaRepository

    private lateinit var partner: PartnerEntity
    private lateinit var testData: TestDataFactory

    @BeforeEach
    fun setup() {
        testData = TestDataFactory(partnerRepo, feePolicyRepo)
        partner = testData.createPartner("TIME_TEST", "Time Test Partner")
    }

    // ===== 헬퍼 메서드 =====

    /**
     * 특정 시점의 정책을 조회하고 검증합니다.
     */
    private fun assertPolicyAt(
        partnerId: Long,
        at: LocalDateTime,
        expectedPercentage: BigDecimal?,
        expectedFixedFee: BigDecimal?
    ) {
        val policy = feePolicyRepository.findEffectivePolicy(partnerId, at)
        if (expectedPercentage == null) {
            assertNull(policy, "시점 $at 에는 정책이 없어야 합니다")
        } else {
            assertNotNull(policy, "시점 $at 에는 정책이 있어야 합니다")
            assertEquals(expectedPercentage, policy.percentage, "수수료율이 일치하지 않습니다")
            expectedFixedFee?.let {
                assertEquals(it, policy.fixedFee, "고정 수수료가 일치하지 않습니다")
            }
        }
    }

    @Test
    @DisplayName("정책이 시간에 따라 변경될 때 올바른 정책이 적용된다")
    fun `정책이 시간에 따라 변경될 때 올바른 정책이 적용된다`() {
        // Given: 시간순 정책 이력 생성
        testData.createFeePolicies(
            partner.id!!,
            Triple("2020-01-01T00:00:00Z", BigDecimal("0.0500"), BigDecimal("200")), // 5% + 200원
            Triple("2022-01-01T00:00:00Z", BigDecimal("0.0400"), BigDecimal("150")), // 4% + 150원
            Triple("2024-01-01T00:00:00Z", BigDecimal("0.0300"), BigDecimal("100")), // 3% + 100원
            Triple("2024-06-01T00:00:00Z", BigDecimal("0.0250"), BigDecimal("50"))   // 2.5% + 50원
        )

        // When & Then: 각 시점마다 올바른 정책이 선택됨
        assertPolicyAt(partner.id!!, LocalDateTime.of(2019, 12, 31, 23, 59, 59), null, null) // 정책 없음

        assertPolicyAt(partner.id!!, LocalDateTime.of(2020, 1, 1, 0, 0, 0), BigDecimal("0.0500"), BigDecimal("200")) // 5% + 200원
        assertPolicyAt(partner.id!!, LocalDateTime.of(2021, 6, 15, 12, 0, 0), BigDecimal("0.0500"), BigDecimal("200")) // 여전히 5% + 200원

        assertPolicyAt(partner.id!!, LocalDateTime.of(2022, 1, 1, 0, 0, 0), BigDecimal("0.0400"), BigDecimal("150")) // 4% + 150원
        assertPolicyAt(partner.id!!, LocalDateTime.of(2023, 12, 31, 23, 59, 59), BigDecimal("0.0400"), BigDecimal("150")) // 여전히 4% + 150원

        assertPolicyAt(partner.id!!, LocalDateTime.of(2024, 1, 1, 0, 0, 0), BigDecimal("0.0300"), BigDecimal("100")) // 3% + 100원
        assertPolicyAt(partner.id!!, LocalDateTime.of(2024, 5, 31, 23, 59, 59), BigDecimal("0.0300"), BigDecimal("100")) // 여전히 3% + 100원

        assertPolicyAt(partner.id!!, LocalDateTime.of(2024, 6, 1, 0, 0, 0), BigDecimal("0.0250"), BigDecimal("50")) // 2.5% + 50원
        assertPolicyAt(partner.id!!, LocalDateTime.of(2024, 12, 31, 23, 59, 59), BigDecimal("0.0250"), BigDecimal("50")) // 여전히 2.5% + 50원
    }

    @Test
    @DisplayName("정책 변경 시점의 경계값이 정확하다")
    fun `정책 변경 시점의 경계값이 정확하다`() {
        // Given: 두 개의 정책 생성 (2024-06-01 00:00:00 기준 변경)
        testData.createFeePolicies(
            partner.id!!,
            Triple("2024-01-01T00:00:00Z", BigDecimal("0.0300"), BigDecimal("100")), // 3% + 100원
            Triple("2024-06-01T00:00:00Z", BigDecimal("0.0250"), BigDecimal("50"))   // 2.5% + 50원
        )

        // When & Then: 경계값 테스트
        assertPolicyAt(partner.id!!, LocalDateTime.of(2024, 5, 31, 23, 59, 59), BigDecimal("0.0300"), BigDecimal("100")) // 변경 직전
        assertPolicyAt(partner.id!!, LocalDateTime.of(2024, 6, 1, 0, 0, 0), BigDecimal("0.0250"), BigDecimal("50"))      // 변경 시점
        assertPolicyAt(partner.id!!, LocalDateTime.of(2024, 6, 1, 0, 0, 1), BigDecimal("0.0250"), BigDecimal("50"))      // 변경 직후
    }

    @Test
    @DisplayName("미래 정책은 적용되지 않는다")
    fun `미래 정책은 적용되지 않는다`() {
        // Given: 현재 정책과 미래 정책 생성
        testData.createFeePolicies(
            partner.id!!,
            Triple("2024-01-01T00:00:00Z", BigDecimal("0.0300"), BigDecimal("100")), // 현재 정책
            Triple("2025-01-01T00:00:00Z", BigDecimal("0.0200"), BigDecimal.ZERO)    // 미래 정책
        )

        // When & Then: 현재 시점(2024-06-15)으로 조회 시 현재 정책만 반환됨
        assertPolicyAt(partner.id!!, LocalDateTime.of(2024, 6, 15, 0, 0, 0), BigDecimal("0.0300"), BigDecimal("100"))
    }
}

