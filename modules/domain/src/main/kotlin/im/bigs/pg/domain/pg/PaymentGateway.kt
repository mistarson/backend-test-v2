package im.bigs.pg.domain.pg

/**
 * PaymentGateway(PG사)
 * - 도메인 계층의 순수 데이터 모델로서 프레임워크에 의존하지 않습니다.
 * - 활성화 여부(active)에 따라 사용 가능/불가를 구분할 수 있습니다.
 *
 * @property id 내부 식별자(PK)
 * @property code PG사 코드
 * @property name PG사 명칭
 * @property priority 우선순위
 * @property active 사용 가능 여부
 */
data class PaymentGateway(
    val id: Long? = null,
    val code: String,
    val name: String,
    val priority: Int = 0,
    val active: Boolean = true,
)
