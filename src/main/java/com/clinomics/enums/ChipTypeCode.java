package com.clinomics.enums;

public enum ChipTypeCode {
	// APMRA_CHIP("APMRA Chip", "(Axiom_APMRA_24)", "/BiO/Research/Cel2GSSInput/Script2/runAnalysis_APMRA.py"),
	APMRA_CHIP("APMRA Chip", "(Axiom_APMRA_24)", "python3", "/BiO/Research/Cel2GSSInput/Script2_APMRA_U10K/10.run_APMRA2Genotypes.py"),
	CUSTOM_CHIP("Custom Chip", "(Axiom_GSChip-1)", "python", "/BiO/Research/Cel2GSSInput/Script2/runAnalysis_GSChip.py"),
	CUSTOM_CHIP2("Custom Chip2", "(Axiom_GSChip-2)", "python", "/BiO/Research/Cel2GSSInput/Script2/runAnalysis_GSChip2.py"),
	CUSTOM_CHIP3("Custom Chip3", "(Axiom_GSChip_3)", "python3", "/BiO/Research/Cel2GSSInput_GSChip3/Script/runAnalysis_GSChip3.1.py")
	;

	private final String value;
	private final String desc;
	private final String program;
	private final String cmd;
	
	ChipTypeCode(String value, String desc, String program, String cmd) {
		this.value = value;
		this.desc = desc;
		this.program = program;
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
	public String getProgram() {
		return program;
	}
	public String getCmd() {
		return cmd;
	}
}
