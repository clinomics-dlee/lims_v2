package com.clinomics.enums;

public enum ResultCode {
	SUCCESS("00", "정상처리되었습니다"),
	SUCCESS_NOT_USE_ALERT("01", "완료"),
	SUCCESS_APPROVED("02", "완료"),
	NO_PERMISSION("61", "완료"),
	EXCEL_EMPTY("71", "완료"),
	EXCEL_FILE_TYPE("72", "완료"),
	FAIL_UPLOAD("81", "완료"),
	FAIL_NOT_EXISTS("91", "완료"),
	FAIL_EXISTS_VALUE("92", "완료"),
	FAIL_FILE_READ("93", "완료")
	;

	private final String value;
	private final String msg;
	
	ResultCode(String value, String msg) {
		this.value = value;
		this.msg = msg;
	}
	public String get() {
		return value;
	}
	public String getMsg() {
		return msg;
	}
}
