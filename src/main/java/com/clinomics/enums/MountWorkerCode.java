package com.clinomics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MountWorkerCode {
	WORKER_128("/BiO/Serve/mount_128"),
	WORKER_238("/BiO/Serve/mount_238")
	;

	private final String value;
	
	public String getKey() {
		return name();
	}
	
}
