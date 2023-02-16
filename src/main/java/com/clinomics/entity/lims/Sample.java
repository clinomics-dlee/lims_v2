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
import javax.persistence.Transient;

import com.clinomics.config.StringMapConverter;
import com.clinomics.enums.ChipTypeCode;
import com.clinomics.enums.GenotypingMethodCode;
import com.clinomics.enums.StatusCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="sample")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Sample implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
    @Column(length = 30)
    private String laboratoryId;

	@Column(length = 30)
	private String managementNumber;

	private int version;

	private boolean isLastVersion = true;

	@ManyToOne()
	@JoinColumn(name="bundleId")
	private Bundle bundle = new Bundle();
	
	@Column(columnDefinition = "json")
	@Convert(converter = StringMapConverter.class)
	private Map<String, Object> items = new HashMap<>();

	@Column(columnDefinition = "json")
	@Convert(converter = StringMapConverter.class)
	private Map<String, Object> docInfos = new HashMap<>();

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
	private GenotypingMethodCode genotypingMethodCode;

	@Transient
	private String genotypingId;

	@Column(length = 100)
	private String mappingNo;

	@Column(length = 100)
	private String wellPosition;

	@Column(length = 100)
	private String chipBarcode;
	
	@Enumerated(EnumType.STRING)
	private ChipTypeCode chipTypeCode;

	@Enumerated(EnumType.STRING)
	private StatusCode statusCode;

	@Column(columnDefinition = "TEXT")
	private String statusMessage;

	@Column(columnDefinition = "json")
	@Convert(converter = StringMapConverter.class)
	private Map<String, Object> data = new HashMap<>();

	@Column(length = 100)
	private String filePath;
	
	@Column(length = 100)
	private String fileName;

	// #. cel 파일 존재여부 판단 컬럼( NULL : celFile 확인중, PASS : CelFile 존재, FAIL : CelFile이 없음)
	@Column(length = 100)
	private String checkCelFile;

	// #. interface api 요청된 product에 type값 목록에 "_"를 붙임 ex> _GS_GSX_
	private String outputProductTypes;

	// #. test sample 체크용, 기본 false
	private boolean isTest = false;

	// #. 중복 검체여부를 판단하는 값( ○ : 중복된 검체가 있고 필요한 결과값도 있는 경우, △ : 중복된 검체는 있으나 결과값이 없는 경우)
	@Column(length = 10)
	private String checkDuplicationSample;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime createdDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime modifiedDate;

	@ManyToOne()
	@JoinColumn(name = "createdMemberId")
	private Member createdMember;
	
	// #. 입고 승인일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime inputApproveDate;
	// #. 입고 승인자
	@ManyToOne()
	@JoinColumn(name = "inputApproveMemberId")
	private Member inputApproveMember;

	// #. 입고 중간관리자 승인일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime inputMngApproveDate;
	// #. 입고 중간관리자 승인자
	@ManyToOne()
	@JoinColumn(name = "inputMngApproveMemberId")
	private Member inputMngApproveMember;

	// #. 입고 검사실책임자 승인일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime inputDrctApproveDate;
	// #. 입고 검사실책임자 승인자
	@ManyToOne()
	@JoinColumn(name = "inputDrctMemberId")
	private Member inputDrctMember;

	// #. 실험 시작일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime expStartDate;
	// #. 실험 시작 담당자
	@ManyToOne()
	@JoinColumn(name = "expStartMemberId")
	private Member expStartMember;

	// #. STEP1 완료일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime expStep1Date;
	// #. STEP1 담당자
	@ManyToOne()
	@JoinColumn(name = "expStep1MemberId")
	private Member expStep1Member;

	// #. STEP2 완료일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime expStep2Date;
	// #. STEP2 담당자
	@ManyToOne()
	@JoinColumn(name = "expStep2MemberId")
	private Member expStep2Member;

	// #. STEP3 완료일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime expStep3Date;
	// #. STEP3 담당자
	@ManyToOne()
	@JoinColumn(name = "expStep3MemberId")
	private Member expStep3Member;

	// #. 분석 시작일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime anlsStartDate;
	// #. 분석 시작 담당자
	@ManyToOne()
	@JoinColumn(name = "anlsStartMemberId")
	private Member anlsStartMember;
	// #. 분석 종료일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime anlsEndDate;

	// #. 분석 완료일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime anlsCmplDate;
	// #. 분석 완료 담당자
	@ManyToOne()
	@JoinColumn(name = "anlsCmplMemberId")
	private Member anlsCmplMember;

	// #. 판정 검사 담당자 승인일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime jdgmApproveDate;
	// #. 판정 검사 담당자
	@ManyToOne()
	@JoinColumn(name = "jdgmApproveMemberId")
	private Member jdgmApproveMember;

	// #. 판정 중간관리자 승인일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime jdgmMngApproveDate;
	// #. 판정 중간관리자
	@ManyToOne()
	@JoinColumn(name = "jdgmMngApproveMemberId")
	private Member jdgmMngApproveMember;

	// #. 판정 검사실책임자 승인일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime jdgmDrctApproveDate;
	// #. 판정 검사실책임자
	@ManyToOne()
	@JoinColumn(name = "jdgmDrctApproveMemberId")
	private Member jdgmDrctApproveMember;

	// #. PDF출고 대기일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime outputWaitDate;
	// #. PDF출고 담당자
	@ManyToOne()
	@JoinColumn(name = "outputWaitMemberId")
	private Member outputWaitMember;
	// #. PDF출고 완료일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime outputCmplDate;

	// #. 재발행 대기일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime reOutputWaitDate;
	// #. 재발행 담당자
	@ManyToOne()
	@JoinColumn(name = "reOutputWaitMemberId")
	private Member reOutputWaitMember;
	// #. 재발행 완료일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime reOutputCmplDate;

	// #. 실제 출고 담당자
	@ManyToOne()
	@JoinColumn(name = "deliveryMemberId")
	private Member deliveryCmplMember;
	// #. 실제 출고 완료일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime deliveryCmplDate;

	// #. 출고 예정일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime outputScheduledDate;

	// #. interface api 요청된 product에 type값 목록에 "_"를 붙임 ex> _GS_GSX_
	private String approvedOutputProductTypes;

	// #. 입고 승인 후 데이터 조회 완료일
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private LocalDateTime approvedOutputCmplDate;
	
	public String getGenotypingId() {
		return this.laboratoryId + "-V" + this.version;
	}

}
