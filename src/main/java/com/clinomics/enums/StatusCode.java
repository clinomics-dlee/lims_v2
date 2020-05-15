package com.clinomics.enums;

public enum StatusCode {
	S000("등록"),
	S020("입고"),
	S040("입고승인"),
	S200("실험대기"),
	S210("STEP1"),
	S220("STEP2"),
	S230("STEP3"),
	S250("실험승인"),
	S400("분석대기"),
	S410("분석중"),
	S420("분석성공"),
	S430("분석성공완료"),
	S440("분석실패"),
	S450("분석실패완료"),
	S460("분석최종완료"),
	S600("판정완료"),
	S700("출고대기"),
	S710("출고완료"),
	S800("재발행대기"),
	S810("재발행완료")
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
