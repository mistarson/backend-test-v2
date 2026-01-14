package im.bigs.pg.application.pg.registry

import im.bigs.pg.application.pg.port.out.PgClient
import im.bigs.pg.domain.pg.PgCode
import org.springframework.stereotype.Component

/**
 * PG 클라이언트 레지스트리.
 * - 등록된 모든 PgClient를 PG 코드별로 매핑하여 관리합니다.
 */
@Component
class PgClientRegistry(
    private val pgClients: List<PgClient>,
) {
    private val clientMap: Map<PgCode, PgClient> = run {
        pgClients.associateBy { extractPgCode(it) }
    }

    fun getClient(pgCode: PgCode): PgClient? {
        return clientMap[pgCode]
    }

    private fun extractPgCode(client: PgClient): PgCode {
        return client.getPgCode()
    }
}
