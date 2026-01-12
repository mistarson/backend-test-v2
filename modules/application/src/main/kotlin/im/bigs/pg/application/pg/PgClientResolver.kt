package im.bigs.pg.application.pg

import im.bigs.pg.domain.pg.PgCode

/**
 * PG Provider Code 선택을 담당하는 인터페이스.
 * - Application 계층 내부에서 PG Provider 선택 로직을 추상화합니다.
 * - 제휴사 ID를 기반으로 적절한 PG Provider Code 목록을 반환합니다.
 * - 다양한 선택 전략(우선순위, 조건별 등)을 구현할 수 있습니다.
 */
interface PgClientResolver {
    /**
     * 제휴사 ID에 맞는 PG Provider Code 목록을 반환합니다.
     * - 반환된 목록의 순서는 시도 순서를 의미합니다.
     *
     * @param partnerId 제휴사 ID
     * @return 시도할 PG Provider Code 목록 (없으면 빈 리스트)
     */
    fun resolve(partnerId: Long): List<PgCode>
}

