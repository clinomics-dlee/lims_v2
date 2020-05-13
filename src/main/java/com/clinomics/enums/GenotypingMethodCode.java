package com.clinomics.enums;

public enum GenotypingMethodCode {
	QRT_PCR("qRT-PCR"),
	CHIP("Chip")
	;

	private final String value;
	
	GenotypingMethodCode(String value) {
		this.value = value;
	}
	public String getKey() {
		return name();
	}
	public String getValue() {
		return value;
	}
}
