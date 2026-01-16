## 간단 사용 가이드

### 빠른 시작

**필수 요구사항:**
- JDK 21 이상
- Docker & Docker Compose (Docker 실행 시)
- Gradle Wrapper (프로젝트에 포함됨)

### 실행 방법

#### 방법 1: Docker Compose로 실행 (권장)

```bash
# 전체 서비스 실행 (애플리케이션 + MariaDB)
# 처음 실행하거나 코드 변경 후 재빌드가 필요한 경우
docker-compose up --build -d

# 이미 빌드된 이미지가 있는 경우 (빠른 실행)
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 서비스 중지
docker-compose down
```

애플리케이션은 `http://localhost:8080`에서 실행됩니다.  

#### 방법 2: 로컬에서 Gradle로 실행

**주의:** 로컬 실행 시 MariaDB가 필요합니다. 다음 중 하나를 선택하세요:

**옵션 A: Docker Compose로 MariaDB만 실행하고, 애플리케이션은 로컬에서 실행**
```bash
# MariaDB 서비스만 실행
docker-compose up -d mariadb

# MariaDB가 준비될 때까지 대기 후, 애플리케이션을 로컬에서 실행
./gradlew :modules:bootstrap:api-payment-gateway:bootRun
```

**옵션 B: 로컬에 MariaDB 설치 후 실행**
```bash
# 로컬 MariaDB 설치 후 실행 (설정: localhost:3306, DB: pgdb, User: pguser, Password: pgpassword)
# 애플리케이션 실행
./gradlew :modules:bootstrap:api-payment-gateway:bootRun
```

기본 포트: `8080`  
데이터베이스 연결: `localhost:3306/pgdb` (User: `pguser`, Password: `pgpassword`)

##개발 도구 접근 주소

애플리케이션 실행 후 다음 주소로 접근할 수 있습니다:

- **Swagger UI (API 문서)**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Actuator Health**: http://localhost:8080/actuator/health
- **Actuator Metrics**: http://localhost:8080/actuator/metrics


## 추가 PG사 연동 구현 내용

PG 연동은 다음과 같은 흐름으로 동작합니다:

**특정 PgClient 획득 흐름:**

```
1. PgApprovalService.approve() 호출
   │
   ├─→ 2. PgClientResolver.resolve(partnerId) - 해당 파트너에 맞는 PG사 목록 찾기
   │      │
   │      ├─→ 2-1. PartnerPgSupportOutPort.findPgCodesByPriority(partnerId) - 현재는 우선 순위순으로 PG사를 정렬하여 가져오기
   │      │      │
   │      │      └─→ 2-1-1. DB 조회: partner_pg_support + payment_gateway
   │      │                (priority 기준 정렬)
   │      │
   │      └─→ 2-2. List<PgCode> 반환 (예: [TEST_PG, MOCK])
   │
   ├─→ 3. 각 PgCode 순회하며 PG 시도 - 한 PG사의 결제가 실패하더라도 다른 PG사와의 결제를 시도함으로써 결제의 지속성을 지킴
   │      │
   │      └─→ 4. PgClientRegistry.getClient(pgCode)
   │             │
   │             ├─→ 4-1. Map<PgCode, PgClient>에서 조회
   │             │      (레지스트리는 생성 시점에 모든 @Component PgClient를 수집)
   │             │
   │             └─→ 4-2. PgClient 반환 (예: TestPgClient)
   │
   └─→ 5. PgClient.approve(request) 호출
          │
          └─→ 6. 실제 PG사 API 호출
```

**핵심 컴포넌트:**

1. **PgClient 인터페이스로 추상화** (`modules/application/src/main/kotlin/im/bigs/pg/application/pg/port/out/PgClient.kt`)
   - 각 PG사(TestPg, Mock 등)를 `PgClient` 인터페이스로 추상화
   - `approve()`: 결제 승인 요청 처리
   - `getPgCode()`: 지원하는 PG 코드 반환
   - 새로운 PG사 추가 시 `PgClient` 인터페이스를 구현하는 클래스만 생성하면 자동으로 시스템에 통합됨

2. **PgClientRegistry** (`modules/application/src/main/kotlin/im/bigs/pg/application/pg/registry/PgClientRegistry.kt`)
   - Spring의 의존성 주입을 통해 모든 `PgClient` 구현체를 자동 수집
   - `Map<PgCode, PgClient>` 형태로 관리하여 PG 코드를 통해 해당 PG사의 클라이언트를 빠르게 조회
   - `getClient(pgCode: PgCode)` 메서드로 PG 코드에 해당하는 클라이언트를 얻을 수 있음
   - `@Component`로 등록된 모든 `PgClient` 구현체가 자동으로 레지스트리에 등록됨

3. **PgClientResolver 인터페이스로 전략 추상화** (`modules/application/src/main/kotlin/im/bigs/pg/application/pg/resolver/PgClientResolver.kt`)
   - `PgClientResolver` 인터페이스를 통해 PG 선택 전략을 추상화
   - 현재 구현: `PriorityBasedPgClientResolver` (`modules/application/src/main/kotlin/im/bigs/pg/application/pg/resolver/PriorityBasedPgClientResolver.kt`)
     - 제휴사별로 지원하는 PG 목록을 데이터베이스에서 조회
     - `payment_gateway.priority` 기준으로 우선순위 결정하여 반환
   - **확장 가능성**: Resolver가 인터페이스로 추상화되어 있어, 추후 다른 전략으로 쉽게 교체 가능
     - 우선순위 순으로 정렬하여 가져오는 `PriorityBasedPgClientResolver`
     - 추후에 수수료가 가장 낮은 PG사를 우선적으로 가져오는 `FeeBasedPgClientResolver`를 개발하여 교체만 하면 됨

4. **폴백 메커니즘**: `PgApprovalService` (`modules/application/src/main/kotlin/im/bigs/pg/application/pg/service/PgApprovalService.kt`)
   - `PgClientResolver`로부터 받은 PG 목록을 순서대로 시도
   - 첫 번째 PG 실패 시 자동으로 다음 PG로 폴백
   - 모든 PG가 실패한 경우 `PgApprovalException` 발생
---

# 백엔드 사전 과제 – 결제 도메인 서버

본 과제는 나노바나나 페이먼츠의 “결제 도메인 서버”를 주제로, 백엔드 개발자의 설계·구현·테스트 역량을 평가하기 위한 사전 과제입니다. 제공된 멀티모듈 + 헥사고널 아키텍처 기반 코드를 바탕으로 요구사항을 충족하는 기능을 완성해 주세요.

주의: 이 디렉터리(`backend-test-v1`)만 압축/전달됩니다. 외부 경로를 참조하지 않도록 README/코드/스크립트를 유지해 주세요.

## 1. 배경 시나리오
- 본 서비스는 결제대행사 “나노바나나 페이먼츠”의 결제 도메인 서버입니다.
- 현재는 제휴사가 없어 “목업 PG”만 연동되어 있으며, 결제는 항상 성공합니다.
- 정산금 계산식은 임시로 “하드코드(3% + 100원)” 되어 있습니다.

여러 제휴사와 연동을 시작하면서 다음이 필요합니다.
1) 새로운 결제 제휴사 연동(기본 스켈레톤 제공)
2) 결제 내역 조회 API 제공(통계 포함, 커서 기반 페이지네이션)
3) 제휴사별 수수료 정책 적용(하드코드 제거, 정책 테이블 기반)

## 2. 과제 목표
아래 항목을 모두 구현/보강하고, 테스트로 증명해 주세요.

1) 결제 생성
- 엔드포인트: POST `/api/v1/payments`
- 내용: 결제 승인(외부 PG 연동) 후, 수수료/정산금 계산 결과를 포함하여 저장
- 주의: 현재 `PaymentService`는 하드코드된 수수료(3% + 100원)를 사용합니다. 제휴사별 정책(percentage, fixedFee, effective_from)에 따라 계산하도록 리팩터링하세요.  
  또한 반드시 [11. 참고자료](#11-참고자료) 의 과제 내 연동 대상 API 문서를 참고하여 TestPg 와 Rest API 를 통한 연동을 진행해야 합니다. 

2) 결제 내역 조회 + 통계
- 엔드포인트: GET `/api/v1/payments`
- 쿼리: `partnerId`, `status`, `from`, `to`, `cursor`, `limit`
- 응답: `items[]`, `summary{count,totalAmount,totalNetAmount}`, `nextCursor`, `hasNext`
- 요구: 통계는 반드시 필터와 동일한 집합을 대상으로 계산되어야 하며, 커서 기반 페이지네이션을 사용해야 합니다.

3) 제휴사별 수수료 정책
- 스키마: `sql/scheme.sql` 의 `partner`, `partner_fee_policy`, `payment` 참조(필요시 보완/수정 가능)
- 규칙: `effective_from` 기준 가장 최근(<= now) 정책을 적용, 금액은 HALF_UP로 반올림
- 보안: 카드번호 등 민감정보는 저장/로깅 금지(제공 코드도 마스킹/부분 저장만 수행)

## 3. 제공 코드 개요(헥사고널)
- `modules/domain`: 순수 도메인 모델/유틸(FeePolicy, Payment, FeeCalculator 등)
- `modules/application`: 유스케이스/포트(PaymentUseCase, QueryPaymentsUseCase, Repository/PgClient 포트, PaymentService 등)
  - 의도적으로 PaymentService에 “하드코드 수수료 계산”이 남아 있습니다. 이를 정책 기반으로 개선하세요.
- `modules/infrastructure/persistence`: JPA 엔티티·리포지토리·어댑터(pageBy/summary 제공)
- `modules/external/pg-client`: PG 연동 어댑터(Mock, TestPay 예시)
- `modules/bootstrap/api-payment-gateway`: 실행 가능한 Spring Boot API(Controller, 시드 데이터)

아키텍처 제약
- 멀티모듈 경계/의존 역전/포트-어댑터 패턴을 유지할 것
- `domain`은 프레임워크 의존 금지(순수 Kotlin)

## 4. 필수 요구 사항
- 결제 생성 시 저장 레코드에 다음 필드가 정확히 기록됨: 금액, 적용 수수료율, 수수료, 정산금, 카드 식별(마스킹), 승인번호, 승인시각, 상태
- 조회 API에서 필터 조합별 `summary`가 `items`와 동일 집합을 정확히 집계
- 커서 페이지네이션이 정렬 키(`createdAt desc, id desc`) 기반으로 올바르게 동작(다음 페이지 유무/커서 일관성)
- 제휴사별 수수료 정책(비율/고정/시점)이 적용되어 계산 결과가 맞음
- 모든 신규/수정 로직에 대해 의미 있는 단위/통합 테스트 존재, 빠르고 결정적

## 5. 개발 환경 & 실행 방법
- JDK 21, Gradle Wrapper 사용
- H2 인메모리 DB 기본 실행(필요 시 schema/data/migration 구성 변경 가능)

명령어
```bash
./gradlew build                  # 컴파일 + 모든 테스트
./gradlew test                   # 테스트만
./gradlew :modules:bootstrap:api-payment-gateway:bootRun   # API 실행
./gradlew ktlintCheck | ktlintFormat  # 코드 스타일 검사/자동정렬
```
기본 포트: 8080

## 6. API 사양(요약)
1) 결제 생성
```
POST /api/v1/payments
{
  "partnerId": 1,
  "amount": 10000,
  "cardBin": "123456",
  "cardLast4": "4242",
  "productName": "샘플"
}

200 OK
{
  "id": 99,
  "partnerId": 1,
  "amount": 10000,
  "appliedFeeRate": 0.0300,
  "feeAmount": 400,
  "netAmount": 9600,
  "cardLast4": "4242",
  "approvalCode": "...",
  "approvedAt": "2025-01-01T00:00:00Z",
  "status": "APPROVED",
  "createdAt": "2025-01-01T00:00:00Z"
}
```

2) 결제 조회(통계+커서)
```
GET /api/v1/payments?partnerId=1&status=APPROVED&from=2025-01-01T00:00:00Z&to=2025-01-02T00:00:00Z&limit=20&cursor=

200 OK
{
  "items": [ { ... }, ... ],
  "summary": { "count": 35, "totalAmount": 35000, "totalNetAmount": 33950 },
  "nextCursor": "ey1...",
  "hasNext": true
}
```

## 7. 데이터베이스 가이드
- 기준 테이블(예시):
  - `partner(id, code, name, active)`
  - `partner_fee_policy(id, partner_id, effective_from, percentage, fixed_fee)`
  - `payment(id, partner_id, amount, applied_fee_rate, fee_amount, net_amount, card_bin, card_last4, approval_code, approved_at, status, created_at, updated_at)`
- 인덱스 권장: `payment(created_at desc, id desc)`, `payment(partner_id, created_at desc)`, 검색 조건 컬럼
- 정확한 스키마/인덱스는 요구사항을 만족하는 선에서 자유롭게 보완 가능

## 8. 제출물
- github 저장소 링크를 사전과제 전달 메일로 회신. (메일 본문에 채용공고 명 / 실명 기재 필수)
- 포함 사항: 구현 코드, 테스트, 간단 사용가이드(필요 시 README 보강), 변경이력, 추가 선택 구현 설명(선택)

## 9. 평가 기준
- 아키텍처 일관성(모듈 경계, 포트-어댑터, 의존 역전)
- 도메인 모델링 적절성 및 가독성(KDoc, 네이밍)
- 기능 정확성(통계 일치, 커서 페이징 동작, 수수료 계산)
- 테스트 품질(결정적/빠름/커버리지)
- 보안/개인정보 처리(민감정보 최소 저장, 로깅 배제)
- 변경 이력 품질(의미 있는 커밋 메시지, 작은 단위 변경)

## 10. 선택 과제(가산점)
- 추가 제휴사 연동(Adapter 추가 및 전략 선택)
- 오픈API 문서화(springdoc 등) 또는 간단한 운영지표(로그/메트릭)
- MariaDB 등 외부 DB로 전환(docker-compose 포함) 및 마이그레이션 도구 적용

## 11. 참고자료
- [과제 내 연동 대상 API 문서](https://api-test-pg.bigs.im/docs/index.html)

## 12. 주의사항
- 전달한 본 프로젝트는 정상동작하지 않습니다. 요구사항을 포함해, 정상 동작을 목표로 진행하세요.
- 본 과제와 관련한 어떠한 질문도 받지 않습니다.
- 제출물을 기준으로 면접시 코드리뷰를 진행합니다. 이를 고려해주세요. 

행운을 빕니다. 읽기 쉬운 코드, 일관된 설계, 신뢰할 수 있는 테스트를 기대합니다.
