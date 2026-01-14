package im.bigs.pg.application.pg.service

import im.bigs.pg.application.pg.exception.PgApprovalException
import im.bigs.pg.application.pg.exception.PgClientNotFoundException
import im.bigs.pg.application.pg.factory.PgApproveRequestFactory
import im.bigs.pg.application.pg.port.out.BasePgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClient
import im.bigs.pg.application.pg.registry.PgClientRegistry
import im.bigs.pg.application.pg.resolver.PgClientResolver
import im.bigs.pg.domain.pg.PgCode
import org.springframework.stereotype.Service

/**
 * PG 승인 처리를 담당하는 서비스.
 * - PG Client 선택 및 승인 요청을 수행합니다.
 * - PG Provider Code 목록을 순차적으로 시도하고, 실패 시 다음 PG사로 폴백합니다.
 */
@Service
class PgApprovalService(
    private val pgClientResolver: PgClientResolver,
    private val pgClientRegistry: PgClientRegistry,
) {
    fun approve(request: PgApproveRequest): PgApproveResult {
        val pgCodes = pgClientResolver.resolve(request.partnerId)
            .ifEmpty { throw PgClientNotFoundException("No supported PG found for partner") }

        val failures = mutableListOf<Throwable>()
        val approvalResult = pgCodes.firstNotNullOfOrNull { pgCode ->
            tryApprove(pgCode, request, failures)
        }

        return approvalResult ?: throw PgApprovalException(
            "Failed to approve payment for partner ${request.partnerId}. Tried ${pgCodes.size} PG(s): ${pgCodes.joinToString(", ")}",
            failures.lastOrNull()
        )
    }

    private fun tryApprove(
            pgCode: PgCode,
            request: PgApproveRequest,
            failures: MutableList<Throwable>
    ): PgApproveResult? {
        return runCatching {
            val client = pgClientRegistry.getClient(pgCode) ?: return null

            // pgCode에 따라 적절한 BasePgApproveRequest 구현체 생성
            val pgRequest = PgApproveRequestFactory.create(pgCode, request)

            client.approve(pgRequest)
        }
            .onFailure { failures.add(it) }
            .getOrNull()
    }
}
