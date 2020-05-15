package com.clinomics.enums;

public enum ChipTypeCode {
	APMRA_CHIP("APMRA Chip"),
	CUSTOM_CHIP("Custom Chip")
	;

	private final String value;
	
	ChipTypeCode(String value) {
		this.value = value;
	}
	public String getKey() {
		return name();
	}
	public String getValue() {
		return value;
	}
}
