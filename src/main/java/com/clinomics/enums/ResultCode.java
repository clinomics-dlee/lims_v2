package com.clinomics.enums;

public enum ResultCode {
	SUCCESS("00", "정상 처리되었습니다"),
	SUCCESS_NOT_USE_ALERT("01", ""),
	SUCCESS_APPROVED("02", "승인이 완료되었습니다"),
	SUCCESS_SAVE("03", "저장되었습니다"),
	SUCCESS_DELETE("04", "삭제되었습니다"),
	NO_PERMISSION("61", "권한이 없습니다"),
	EXCEL_EMPTY("71", "엑셀 파일에 내용이 존재하지 않습니다"),
	EXCEL_FILE_TYPE("72", "엑셀 파일 형식과 다릅니다"),
	FAIL_UPLOAD("81", "파일 업로드를 실패했습니다"),
	FAIL_NOT_EXISTS("91", "파일이 존재하지 않습니다"),
	FAIL_EXISTS_VALUE("92", "존재하지 않는 값입니다"),
	FAIL_FILE_READ("93", "파일 읽기에 실패했습니다"),
	FAIL_DUPL_VALUE("94", "중복된 값이 있습니다"),
	FAIL_UNKNOWN("99", "알 수 없는 오류입니다")
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
