package im.bigs.pg.application.pg.service

import im.bigs.pg.application.log.LoggingPort
import im.bigs.pg.application.payment.factory.ApplicationTestDataFactory
import im.bigs.pg.application.pg.exception.PgApprovalException
import im.bigs.pg.application.pg.exception.PgClientNotFoundException
import im.bigs.pg.application.pg.port.out.BasePgApproveRequest
import im.bigs.pg.application.pg.port.out.PgClient
import im.bigs.pg.application.pg.registry.PgClientRegistry
import im.bigs.pg.application.pg.resolver.PgClientResolver
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.pg.PgCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PgApprovalServiceTest {
    private val pgClientResolver = mockk<PgClientResolver>()
    private val pgClientRegistry = mockk<PgClientRegistry>()
    private val logger = mockk<LoggingPort>(relaxed = true)
    private val service = PgApprovalService(pgClientResolver, pgClientRegistry, logger)

    @Test
    @DisplayName("단일 PG 승인 성공 시 결과 반환")
    fun `단일 PG 승인 성공`() {
        // given
        val partnerId = 1L
        val request = ApplicationTestDataFactory.defaultPgApproveRequest(partnerId = partnerId)
        val pgCode = PgCode.TEST_PG
        val expectedResult = ApplicationTestDataFactory.defaultPgApproveResult(
            approvalCode = "APPROVAL-123",
            approvedAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0)
        )

        val pgClient = mockk<PgClient>()
        val requestSlot = slot<BasePgApproveRequest>()

        every { pgClientResolver.resolve(partnerId) } returns listOf(pgCode)
        every { pgClientRegistry.getClient(pgCode) } returns pgClient
        every { pgClient.approve(capture(requestSlot)) } returns expectedResult

        // when
        val result = service.approve(request)

        // then
        assertEquals(expectedResult.approvalCode, result.approvalCode)
        assertEquals(expectedResult.approvedAt, result.approvedAt)
        assertEquals(expectedResult.status, result.status)

        val captured = requestSlot.captured
        assertEquals(request.partnerId, captured.partnerId)
        assertEquals(request.amount, captured.amount)
        assertEquals(request.cardBin, captured.cardBin)
        assertEquals(request.cardLast4, captured.cardLast4)
        assertEquals(request.productName, captured.productName)
    }

    @Test
    @DisplayName("첫 번째 PG 실패 시 두 번째 PG로 폴백하여 성공")
    fun `폴백 메커니즘 동작 확인`() {
        // given
        val partnerId = 2L
        val request = ApplicationTestDataFactory.defaultPgApproveRequest(partnerId = partnerId)
        val pgCodes = listOf(PgCode.TEST_PG, PgCode.MOCK)
        val expectedResult = ApplicationTestDataFactory.defaultPgApproveResult(
            approvalCode = "APPROVAL-789",
            approvedAt = LocalDateTime.of(2024, 2, 1, 12, 0, 0)
        )

        val firstPgClient = mockk<PgClient>()
        val secondPgClient = mockk<PgClient>()
        val firstFailure = RuntimeException("First PG connection failed")

        every { pgClientResolver.resolve(partnerId) } returns pgCodes
        every { pgClientRegistry.getClient(pgCodes[0]) } returns firstPgClient
        every { pgClientRegistry.getClient(pgCodes[1]) } returns secondPgClient
        every { firstPgClient.approve(any()) } throws firstFailure
        every { secondPgClient.approve(any()) } returns expectedResult

        // when
        val result = service.approve(request)

        // then
        assertEquals(expectedResult.approvalCode, result.approvalCode)
        assertEquals(expectedResult.approvedAt, result.approvedAt)
        assertEquals(expectedResult.status, result.status)

        verify(exactly = 1) { pgClientRegistry.getClient(pgCodes[0]) }
        verify(exactly = 1) { pgClientRegistry.getClient(pgCodes[1]) }
        verify(exactly = 1) { firstPgClient.approve(any()) }
        verify(exactly = 1) { secondPgClient.approve(any()) }
    }

    @Test
    @DisplayName("모든 PG 실패 시 PgApprovalException 발생")
    fun `모든 PG 실패 시 예외 발생`() {
        // given
        val partnerId = 3L
        val request = ApplicationTestDataFactory.defaultPgApproveRequest(partnerId = partnerId)
        val pgCodes = listOf(PgCode.TEST_PG, PgCode.MOCK)
        val failures = listOf(
            RuntimeException("First PG failed"),
            RuntimeException("Second PG failed")
        )

        val firstPgClient = mockk<PgClient>()
        val secondPgClient = mockk<PgClient>()

        every { pgClientResolver.resolve(partnerId) } returns pgCodes
        every { pgClientRegistry.getClient(pgCodes[0]) } returns firstPgClient
        every { pgClientRegistry.getClient(pgCodes[1]) } returns secondPgClient
        every { firstPgClient.approve(any()) } throws failures[0]
        every { secondPgClient.approve(any()) } throws failures[1]

        // when & then
        val exception = assertFailsWith<PgApprovalException> {
            service.approve(request)
        }

        assertEquals(
            "Failed to approve payment for partner $partnerId. Tried ${pgCodes.size} PG(s): ${pgCodes.joinToString(", ")}",
            exception.message
        )
        assertEquals(failures.last(), exception.cause)

        verify(exactly = 1) { pgClientResolver.resolve(partnerId) }
        verify(exactly = 1) { pgClientRegistry.getClient(pgCodes[0]) }
        verify(exactly = 1) { pgClientRegistry.getClient(pgCodes[1]) }
        verify(exactly = 1) { firstPgClient.approve(any()) }
        verify(exactly = 1) { secondPgClient.approve(any()) }
    }

    @Test
    @DisplayName("PG 코드 목록이 비어있으면 PgClientNotFoundException 발생")
    fun `PG 코드 목록이 비어있을 때 예외 발생`() {
        // given
        val partnerId = 4L
        val request = ApplicationTestDataFactory.defaultPgApproveRequest(partnerId = partnerId)

        every { pgClientResolver.resolve(partnerId) } returns emptyList()

        // when & then
        val exception = assertFailsWith<PgClientNotFoundException> {
            service.approve(request)
        }

        assertEquals("No supported PG found for partner", exception.message)
        verify(exactly = 1) { pgClientResolver.resolve(partnerId) }
        verify(exactly = 0) { pgClientRegistry.getClient(any()) }
    }

    @Test
    @DisplayName("모든 PG 클라이언트가 레지스트리에 없으면 PgApprovalException 발생")
    fun `레지스트리에 PG 클라이언트가 없을 때 예외 발생`() {
        // given
        val partnerId = 5L
        val request = ApplicationTestDataFactory.defaultPgApproveRequest(partnerId = partnerId)
        val pgCodes = listOf(PgCode.TEST_PG, PgCode.MOCK)

        every { pgClientResolver.resolve(partnerId) } returns pgCodes
        pgCodes.forEach { pgCode ->
            every { pgClientRegistry.getClient(pgCode) } returns null
        }

        // when & then
        val exception = assertFailsWith<PgApprovalException> {
            service.approve(request)
        }

        assertEquals(
            "Failed to approve payment for partner $partnerId. Tried ${pgCodes.size} PG(s): ${pgCodes.joinToString(", ")}",
            exception.message
        )

        verify(exactly = 1) { pgClientResolver.resolve(partnerId) }
        pgCodes.forEach { pgCode ->
            verify(exactly = 1) { pgClientRegistry.getClient(pgCode) }
        }
    }

    @Test
    @DisplayName("지원하지 않는 PG 코드로 요청 생성 실패 시 다음 PG로 폴백")
    fun `PgApproveRequestFactory 예외 발생 시 폴백`() {
        // given
        val partnerId = 6L
        val request = ApplicationTestDataFactory.defaultPgApproveRequest(partnerId = partnerId)
        val pgCodes = listOf(PgCode.TOSSPAY, PgCode.MOCK)
        val expectedResult = ApplicationTestDataFactory.defaultPgApproveResult(
            approvalCode = "APPROVAL-SUCCESS"
        )

        val supportedPgClient = mockk<PgClient>()

        every { pgClientResolver.resolve(partnerId) } returns pgCodes
        every { pgClientRegistry.getClient(pgCodes[0]) } returns mockk<PgClient>()
        every { pgClientRegistry.getClient(pgCodes[1]) } returns supportedPgClient
        every { supportedPgClient.approve(any()) } returns expectedResult

        // when
        val result = service.approve(request)

        // then
        assertEquals(expectedResult.approvalCode, result.approvalCode)
        verify(exactly = 1) { pgClientRegistry.getClient(pgCodes[0]) }
        verify(exactly = 1) { pgClientRegistry.getClient(pgCodes[1]) }
        verify(exactly = 1) { supportedPgClient.approve(any()) }
    }

    @Test
    @DisplayName("APPROVED 상태 반환 확인")
    fun `APPROVED 상태 반환`() {
        // given
        val partnerId = 8L
        val request = ApplicationTestDataFactory.defaultPgApproveRequest(partnerId = partnerId)
        val pgCode = PgCode.TEST_PG
        val expectedResult = ApplicationTestDataFactory.defaultPgApproveResult(status = PaymentStatus.APPROVED)
        val pgClient = mockk<PgClient>()

        every { pgClientResolver.resolve(partnerId) } returns listOf(pgCode)
        every { pgClientRegistry.getClient(pgCode) } returns pgClient
        every { pgClient.approve(any()) } returns expectedResult

        // when
        val result = service.approve(request)

        // then
        assertEquals(PaymentStatus.APPROVED, result.status)
    }

    @Test
    @DisplayName("CANCELED 상태 반환 확인")
    fun `CANCELED 상태 반환`() {
        // given
        val partnerId = 9L
        val request = ApplicationTestDataFactory.defaultPgApproveRequest(partnerId = partnerId)
        val pgCode = PgCode.MOCK
        val expectedResult = ApplicationTestDataFactory.defaultPgApproveResult(
            approvalCode = "APPROVAL-CANCELED",
            status = PaymentStatus.CANCELED
        )
        val pgClient = mockk<PgClient>()

        every { pgClientResolver.resolve(partnerId) } returns listOf(pgCode)
        every { pgClientRegistry.getClient(pgCode) } returns pgClient
        every { pgClient.approve(any()) } returns expectedResult

        // when
        val result = service.approve(request)

        // then
        assertEquals(PaymentStatus.CANCELED, result.status)
        assertEquals("APPROVAL-CANCELED", result.approvalCode)
    }

    @Test
    @DisplayName("레지스트리에 없는 PG는 approve() 호출 안 됨")
    fun `레지스트리에 없는 PG는 approve 호출 안 됨`() {
        // given
        val partnerId = 13L
        val request = ApplicationTestDataFactory.defaultPgApproveRequest(partnerId = partnerId)
        val pgCodes = listOf(PgCode.TEST_PG, PgCode.MOCK)
        val secondPgClient = mockk<PgClient>()

        every { pgClientResolver.resolve(partnerId) } returns pgCodes
        every { pgClientRegistry.getClient(pgCodes[0]) } returns null
        every { pgClientRegistry.getClient(pgCodes[1]) } returns secondPgClient
        every { secondPgClient.approve(any()) } returns ApplicationTestDataFactory.defaultPgApproveResult()

        // when
        service.approve(request)

        // then
        verify(exactly = 1) { secondPgClient.approve(any()) }
    }

    @Test
    @DisplayName("여러 PG 실패 시 모든 예외가 수집되고 마지막 예외가 cause로 설정")
    fun `실패 예외 수집 검증`() {
        // given
        val partnerId = 14L
        val request = ApplicationTestDataFactory.defaultPgApproveRequest(partnerId = partnerId)
        val pgCodes = listOf(PgCode.TEST_PG, PgCode.MOCK, PgCode.MOCK)
        val failures = listOf(
            RuntimeException("First PG failed"),
            IllegalArgumentException("Second PG failed"),
            IllegalStateException("Third PG failed")
        )

        val firstPgClient = mockk<PgClient>()
        val secondPgClient = mockk<PgClient>()
        val thirdPgClient = mockk<PgClient>()

        every { pgClientResolver.resolve(partnerId) } returns pgCodes
        every { pgClientRegistry.getClient(pgCodes[0]) } returns firstPgClient
        every { pgClientRegistry.getClient(pgCodes[1]) } returns secondPgClient
        every { pgClientRegistry.getClient(pgCodes[2]) } returns thirdPgClient
        every { firstPgClient.approve(any()) } throws failures[0]
        every { secondPgClient.approve(any()) } throws failures[1]
        every { thirdPgClient.approve(any()) } throws failures[2]

        // when & then
        val exception = assertFailsWith<PgApprovalException> {
            service.approve(request)
        }

        val cause = exception.cause
        assertNotNull(cause, "Exception cause should not be null")
        assertEquals<Class<*>>(failures.last().javaClass, cause.javaClass)
        assertEquals(failures.last().message, cause.message)
    }

    @Test
    @DisplayName("예외 메시지에 상세 정보 포함 확인")
    fun `예외 메시지 상세성 검증`() {
        // given
        val partnerId = 15L
        val request = ApplicationTestDataFactory.defaultPgApproveRequest(partnerId = partnerId)
        val pgCodes = listOf(PgCode.TEST_PG, PgCode.MOCK, PgCode.TOSSPAY, PgCode.NHN_KCP)

        pgCodes.forEach { pgCode ->
            val client = mockk<PgClient>()
            every { pgClientRegistry.getClient(pgCode) } returns client
            every { client.approve(any()) } throws RuntimeException("$pgCode failed")
        }

        every { pgClientResolver.resolve(partnerId) } returns pgCodes

        // when & then
        val exception = assertFailsWith<PgApprovalException> {
            service.approve(request)
        }

        val message = exception.message
        assertNotNull(message)
        assert(message.contains("partner $partnerId"))
        assert(message.contains("Tried 4 PG(s)"))
        pgCodes.forEach { pgCode ->
            assert(message.contains(pgCode.name))
        }
    }
}
