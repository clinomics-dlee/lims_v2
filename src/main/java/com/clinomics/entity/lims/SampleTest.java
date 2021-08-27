package com.clinomics.entity.lims;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.clinomics.config.StringMapConverter;
import com.clinomics.enums.StatusCode;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="sample_test")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class SampleTest implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
    @Column(length = 30)
    private String laboratoryId;

	@ManyToOne()
	@JoinColumn(name="bundleId")
	private Bundle bundle = new Bundle();
	
	@Column(columnDefinition = "json")
	@Convert(converter = StringMapConverter.class)
	private Map<String, Object> items = new HashMap<>();

	@Column(length = 10)
	@JsonIgnore
	private String barcodeLetter;

	@Column(length = 20)
	@JsonIgnore
	private String barcodeNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="agencyId")
	@JsonIgnore
	private Agency agency;

	private int unitPrice;

	private LocalDate collectedDate;

	private LocalDate receivedDate;
	
	private String sampleType;

	private String a260280;

	private String cncnt;
	
	private String dnaQc;
	
	@Enumerated(EnumType.STRING)
	private StatusCode statusCode;

	@Column(columnDefinition = "TEXT")
	private String statusMessage;

	@Column(columnDefinition = "json")
	@Convert(converter = StringMapConverter.class)
	private Map<String, Object> data = new HashMap<>();

	// #. interface api 요청된 product에 type값 목록에 "_"를 붙임 ex> _GS_GSX_
	private String outputProductTypes;
	
	private LocalDateTime createdDate;

	private LocalDateTime modifiedDate;

	@ManyToOne()
	@JoinColumn(name = "createdMemberId")
	private Member createdMember;
	
	// #. PDF출고 대기일
	private LocalDateTime outputWaitDate;
	// #. PDF출고 담당자
	@ManyToOne()
	@JoinColumn(name = "outputWaitMemberId")
	private Member outputWaitMember;
	// #. PDF출고 완료일
	private LocalDateTime outputCmplDate;

	// #. 재발행 대기일
	private LocalDateTime reOutputWaitDate;
	// #. 재발행 담당자
	@ManyToOne()
	@JoinColumn(name = "reOutputWaitMemberId")
	private Member reOutputWaitMember;
	// #. 재발행 완료일
	private LocalDateTime reOutputCmplDate;

}
