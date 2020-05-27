package com.clinomics.enums;

public enum ChipTypeCode {
	APMRA_CHIP("APMRA Chip", "(Axiom_KORV1-1)", "/BiO/Research/Cel2GSSInput/Script/runAnalysis_GSK.py"),
	CUSTOM_CHIP("Custom Chip", "(Axiom_GSChip-1)", "/BiO/Research/Cel2GSSInput/Script/runAnalysis.py")
	;

	private final String value;
	private final String desc;
	private final String cmd;
	
	ChipTypeCode(String value, String desc, String cmd) {
		this.value = value;
		this.desc = desc;
		this.cmd = cmd;
	}
	public String getKey() {
		return name();
	}
	public String getValue() {
		return value;
	}
	public String getDesc() {
		return desc;
	}
	public String getCmd() {
		return cmd;
	}
}
