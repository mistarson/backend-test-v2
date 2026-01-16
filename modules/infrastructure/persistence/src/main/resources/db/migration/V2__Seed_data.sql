-- 초기 시드 데이터

-- PaymentGateway 데이터
INSERT INTO payment_gateway (code, name, priority, active) VALUES
('TEST_PG', '테스트 PG사', 1, true),
('MOCK', '모의 PG사', 2, true);

-- Partner 데이터
INSERT INTO partner (code, name, active) VALUES
('PARTNER_001', '테스트 제휴사 1', true),
('PARTNER_002', '테스트 제휴사 2', true),
('PARTNER_003', '비활성 제휴사', false);

-- Partner Fee Policy 데이터
INSERT INTO partner_fee_policy (partner_id, effective_from, percentage, fixed_fee) VALUES
(1, '2024-01-01 00:00:00', 0.0300, 100);

-- Partner 2: 2.5%만, 고정 수수료 없음
INSERT INTO partner_fee_policy (partner_id, effective_from, percentage, fixed_fee) VALUES
(2, '2024-01-01 00:00:00', 0.0250, NULL);

-- Partner PG Support 데이터
-- Partner 1 -> TEST_PG
INSERT INTO partner_pg_support (partner_id, payment_gateway_id) VALUES
(1, 1);

-- Partner 2 -> TEST_PG, MOCK (우선순위: TEST_PG가 먼저)
INSERT INTO partner_pg_support (partner_id, payment_gateway_id) VALUES
(2, 1),
(2, 2);
