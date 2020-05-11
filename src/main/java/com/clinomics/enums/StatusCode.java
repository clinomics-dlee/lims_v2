package com.clinomics.enums;

public enum StatusCode {
	INPUT_READY("입고"),
	INPUT_APPROVE("입고승인"),
	EXP_READY("실험대기"),
	EXP_STEP1("STEP1"),
	EXP_STEP2("STEP2"),
	EXP_STEP3("STEP3"),
	EXP_APPROVE("실험승인"),
	ANLS_READY("분석대기"),
	ANLS_RUNNING("분석중"),
	ANLS_SUCC("분석성공"),
	ANLS_SUCC_CMPL("분석성공"),
	ANLS_FAIL("분석실패"),
	ANLS_FAIL_CMPL("분석실패"),
	ANLS_CMPL("분석완료"),
	JDGM_APPROVE("판정완료"),
	OUTPUT_WAIT("출고대기"),
	OUTPUT_CMPL("출고완료"),
	RE_OUTPUT_WAIT("재발행대기"),
	RE_OUTPUT_CMPL("재발행완료")
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
