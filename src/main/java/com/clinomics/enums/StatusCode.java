package com.clinomics.enums;

public enum StatusCode {
	S000_INPUT_REG("등록"),
	S020_INPUT_RCV("입고"),
	S040_INPUT_APPROVE("입고승인"),
	S200_EXP_READY("실험대기"),
	S210_EXP_STEP1("STEP1"),
	S220_EXP_STEP2("STEP2"),
	S230_EXP_STEP3("STEP3"),
	S400_ANLS_READY("분석대기"),
	S410_ANLS_RUNNING("분석중"),
	S420_ANLS_SUCC("분석성공"),
	S430_ANLS_FAIL("분석실패"),
	S440_ANLS_SUCC_CMPL("분석성공완료"),
	S450_ANLS_FAIL_CMPL("분석실패완료"),
	S460_ANLS_CMPL("분석최종완료"),
	S600_JDGM_APPROVE("판정완료"),
	S700_OUTPUT_WAIT("출고대기"),
	S710_OUTPUT_CMPL("출고완료"),
	S800_RE_OUTPUT_WAIT("재발행대기"),
	S810_RE_OUTPUT_CMPL("재발행완료"),
	S900_OUTPUT_CMPL("출고완료") /* 마이그레이션 전용 */
	;

	private final String value;
	
	StatusCode(String value) {
		this.value = value;
	}
	public String getKey() {
		return name();
	}
	public String getValue() {
		return value;
	}
}
