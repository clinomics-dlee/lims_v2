INSERT INTO role (code, name, is_personal_view, in_use)
SELECT 'ROLE_INPUT_20' AS code, '검사서비스관리 담당자', 1, 1 AS name FROM DUAL WHERE NOT EXISTS (SELECT * FROM role WHERE code = 'ROLE_INPUT_20');
INSERT INTO role (code, name, is_personal_view, in_use)
SELECT 'ROLE_OUTPUT_20' AS code, '결과전달 담당자', 1, 1 AS name FROM DUAL WHERE NOT EXISTS (SELECT * FROM role WHERE code = 'ROLE_OUTPUT_20');
INSERT INTO role (code, name, is_personal_view, in_use)
SELECT 'ROLE_EXP_20' AS code, '결과분석 담당자', 0, 1 AS name FROM DUAL WHERE NOT EXISTS (SELECT * FROM role WHERE code = 'ROLE_EXP_20');
INSERT INTO role (code, name, is_personal_view, in_use)
SELECT 'ROLE_EXP_40' AS code, '검사 담당자', 0, 1 AS name FROM DUAL WHERE NOT EXISTS (SELECT * FROM role WHERE code = 'ROLE_EXP_40');
INSERT INTO role (code, name, is_personal_view, in_use)
SELECT 'ROLE_EXP_80' AS code, '총괄 책임자', 1, 1 AS name FROM DUAL WHERE NOT EXISTS (SELECT * FROM role WHERE code = 'ROLE_EXP_80');
INSERT INTO role (code, name, is_personal_view, in_use)
SELECT 'ROLE_TEST_90' AS code, 'TESTER', 0, 1 AS name FROM DUAL WHERE NOT EXISTS (SELECT * FROM role WHERE code = 'ROLE_TEST_90');
INSERT INTO role (code, name, is_personal_view, in_use)
SELECT 'ROLE_IT_99' AS code, '결과정보처리 담당자', 1, 1 AS name FROM DUAL WHERE NOT EXISTS (SELECT * FROM role WHERE code = 'ROLE_IT_99');
INSERT INTO member (id, dept, email, is_failed_mail_sent, in_use, name, password)
SELECT 'admin', 'IT Infra', 'info@clinomics.com', 1, 1, 'Administrator', '$10$8y7E2JJg7d68OQSFKw5rmePUCEd5NtyCKhoX5Ue.0n46veUaHw6Oq' FROM DUAL WHERE NOT EXISTS (SELECT * FROM member WHERE id = 'admin');
INSERT INTO member_role (member_id, role_id)
SELECT 'admin', id FROM role WHERE code = 'ROLE_IT_99' AND NOT EXISTS (SELECT * FROM member_role WHERE member_id = 'admin');