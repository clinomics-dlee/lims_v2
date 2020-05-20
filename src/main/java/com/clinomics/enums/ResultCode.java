package com.clinomics.enums;

public enum ResultCode {
	SUCCESS("00"),
	SUCCESS_NOT_USE_ALERT("01"),
	SUCCESS_APPROVED("02"),
	NO_PERMISSION("61"),
	EXCEL_EMPTY("71"),
	EXCEL_FILE_TYPE("72"),
	FAIL_UPLOAD("81"),
	FAIL_NOT_EXISTS("91"),
	FAIL_EXISTS_VALUE("92"),
	FAIL_FILE_READ("93")
	;

	private final String value;
	
	ResultCode(String value) {
		this.value = value;
	}
	public String get() {
		return value;
	}
}
