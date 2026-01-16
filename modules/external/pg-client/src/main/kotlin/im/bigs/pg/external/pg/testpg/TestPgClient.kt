package im.bigs.pg.external.pg.testpg

import im.bigs.pg.application.pg.port.out.BasePgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClient
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.pg.PgCode
import im.bigs.pg.external.pg.testpg.dto.TestPgRequest
import im.bigs.pg.external.pg.testpg.dto.TestPgResponse
import im.bigs.pg.external.pg.testpg.exception.TestPgException
import im.bigs.pg.external.pg.testpg.util.TestPgEncryptor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import java.math.BigDecimal

/**
 * Test PG 클라이언트 구현체.
 * - https://api-test-pg.bigs.im API와 연동하여 카드 결제 승인을 처리합니다.
 * - 요청은 AES-256-GCM으로 암호화하여 전송하고, 응답은 평문 JSON으로 수신합니다.
 * - API 문서: https://api-test-pg.bigs.im/docs/index.html
 */
@Component
class TestPgClient(
    private val encryptor: TestPgEncryptor,
    @Qualifier("testPgWebClient") private val webClient: WebClient,
) : PgClient {
    override fun getPgCode(): PgCode = PgCode.TEST_PG

    override fun approve(request: BasePgApproveRequest): PgApproveResult {
        require(request is BasePgApproveRequest.TestPgApproveRequest) {
            "TestPgClient requires TestPgApproveRequest, but got ${request::class.simpleName}"
        }

        // 1. TestPG 요청 DTO 생성
        val testPgRequest = TestPgRequest(
            cardNumber = request.extra.cardNumber,
            birthDate = request.extra.birthDate,
            expiry = request.extra.expiry,
            password = request.extra.password,
            amount = request.amount.longValueExact(),
        )

        // 2. 암호화
        val apiKey = request.extra.apiKey
        val ivBase64Url = request.extra.ivBase64Url
        val encrypted = encryptor.encrypt(testPgRequest, apiKey, ivBase64Url)

        // 3. HTTP 요청 (WebClient 사용)
        // 실패 케이스(422, 401)는 예외로 처리하고, 성공(200)만 status를 반영합니다.
        val response = try {
            webClient.post()
                .uri("/api/v1/pay/credit-card")
                .header("API-KEY", apiKey)
                .bodyValue(mapOf("enc" to encrypted))
                .retrieve()
                .onStatus({ it == HttpStatus.UNPROCESSABLE_ENTITY }) { _ ->
                    reactor.core.publisher.Mono.just(TestPgException("TestPG 결제 승인 실패", HttpStatus.UNPROCESSABLE_ENTITY))
                }
                .onStatus({ it == HttpStatus.UNAUTHORIZED }) { _ ->
                    reactor.core.publisher.Mono.just(TestPgException("TestPG 인증 실패: API-KEY가 유효하지 않습니다", HttpStatus.UNAUTHORIZED))
                }
                .bodyToMono<TestPgResponse>()
                .block()
        } catch (e: WebClientResponseException) {
            // onStatus에서 처리되지 않은 다른 HTTP 에러 처리
            val httpStatus = if (e.statusCode is HttpStatus) e.statusCode as HttpStatus else HttpStatus.resolve(e.statusCode.value())
            throw TestPgException("TestPG API 호출 실패: ${e.statusCode} - ${e.message}", httpStatus, e)
        }

        requireNotNull(response) { "TestPG API 응답이 null입니다" }

        // 4. PgApproveResult로 변환합니다.
        return PgApproveResult(
            approvalCode = response.approvalCode,
            approvedAt = response.toLocalDateTime(),
            status = mapStatus(response.status),
            maskedCardLast4 = response.maskedCardLast4,
            amount = BigDecimal(response.amount),
        )
    }

    /**
     * TestPG 응답의 status 문자열을 PaymentStatus enum으로 변환합니다.
     */
    private fun mapStatus(status: String): PaymentStatus {
        return when (status.uppercase()) {
            "APPROVED" -> PaymentStatus.APPROVED
            "CANCELED" -> PaymentStatus.CANCELED
            else -> throw IllegalArgumentException("Unknown payment status: $status")
        }
    }
}
