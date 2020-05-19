package com.clinomics.enums;

public enum RoleCode {
	ROLE_GUEST("ROLE_GUEST_00"),
	ROLE_INPUT_20("ROLE_INPUT_20"),
	ROLE_INPUT_40("ROLE_INPUT_40"),
	ROLE_EXP_20("ROLE_EXP_20"),
	ROLE_EXP_40("ROLE_EXP_40"),
	ROLE_EXP_80("ROLE_EXP_80"),
	ROLE_IT_99("ROLE_IT_99")
	;

	private final String value;
	
	RoleCode(String value) {
		this.value = value;
	}
	public String getKey() {
		return name();
	}
	public String getValue() {
		return value;
	}
}
