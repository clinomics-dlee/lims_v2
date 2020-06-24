package com.clinomics.entity.lims;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name="sample")
@EntityListeners(AuditingEntityListener.class)
public class Sample implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
    @Column(length = 30)
    private String laboratoryId;

	private int version;

	private boolean isLastVersion = true;

	@ManyToOne()
	@JoinColumn(name="bundleId")
	private Bundle bundle = new Bundle();
	
	@Column(columnDefinition = "json")
	@Convert(converter = StringMapConverter.class)
	private Map<String, Object> items = new HashMap<>();

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
	
	private LocalDateTime createdDate;

	private LocalDateTime modifiedDate;

	@ManyToOne()
	@JoinColumn(name = "createdMemberId")
	private Member createdMember;
	
	// #. 입고 승인일
	private LocalDateTime inputApproveDate;
	// #. 입고 승인자
	@ManyToOne()
	@JoinColumn(name = "inputApproveMemberId")
	private Member inputApproveMember;

	// #. 입고 중간관리자 승인일
	private LocalDateTime inputMngApproveDate;
	// #. 입고 중간관리자 승인자
	@ManyToOne()
	@JoinColumn(name = "inputMngApproveMemberId")
	private Member inputMngApproveMember;

	// #. 입고 검사실책임자 승인일
	private LocalDateTime inputDrctApproveDate;
	// #. 입고 검사실책임자 승인자
	@ManyToOne()
	@JoinColumn(name = "inputDrctMemberId")
	private Member inputDrctMember;

	// #. 실험 시작일
	private LocalDateTime expStartDate;
	// #. 실험 시작 담당자
	@ManyToOne()
	@JoinColumn(name = "expStartMemberId")
	private Member expStartMember;

	// #. STEP1 완료일
	private LocalDateTime expStep1Date;
	// #. STEP1 담당자
	@ManyToOne()
	@JoinColumn(name = "expStep1MemberId")
	private Member expStep1Member;

	// #. STEP2 완료일
	private LocalDateTime expStep2Date;
	// #. STEP2 담당자
	@ManyToOne()
	@JoinColumn(name = "expStep2MemberId")
	private Member expStep2Member;

	// #. STEP3 완료일
	private LocalDateTime expStep3Date;
	// #. STEP3 담당자
	@ManyToOne()
	@JoinColumn(name = "expStep3MemberId")
	private Member expStep3Member;

	// #. 분석 시작일
	private LocalDateTime anlsStartDate;
	// #. 분석 시작 담당자
	@ManyToOne()
	@JoinColumn(name = "anlsStartMemberId")
	private Member anlsStartMember;
	// #. 분석 종료일
	private LocalDateTime anlsEndDate;

	// #. 분석 완료일
	private LocalDateTime anlsCmplDate;
	// #. 분석 완료 담당자
	@ManyToOne()
	@JoinColumn(name = "anlsCmplMemberId")
	private Member anlsCmplMember;

	// #. 판정 검사 담당자 승인일
	private LocalDateTime jdgmApproveDate;
	// #. 판정 검사 담당자
	@ManyToOne()
	@JoinColumn(name = "jdgmApproveMemberId")
	private Member jdgmApproveMember;

	// #. 판정 중간관리자 승인일
	private LocalDateTime jdgmMngApproveDate;
	// #. 판정 중간관리자
	@ManyToOne()
	@JoinColumn(name = "jdgmMngApproveMemberId")
	private Member jdgmMngApproveMember;

	// #. 판정 검사실책임자 승인일
	private LocalDateTime jdgmDrctApproveDate;
	// #. 판정 검사실책임자
	@ManyToOne()
	@JoinColumn(name = "jdgmDrctApproveMemberId")
	private Member jdgmDrctApproveMember;

	// #. 출고 대기일
	private LocalDateTime outputWaitDate;
	// #. 출고 담당자
	@ManyToOne()
	@JoinColumn(name = "outputWaitMemberId")
	private Member outputWaitMember;
	// #. 출고 완료일
	private LocalDateTime outputCmplDate;

	// #. 재발행 대기일
	private LocalDateTime reOutputWaitDate;
	// #. 재발행 담당자
	@ManyToOne()
	@JoinColumn(name = "reOutputWaitMemberId")
	private Member reOutputWaitMember;
	// #. 재발행 완료일
	private LocalDateTime reOutputCmplDate;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getLaboratoryId() {
		return laboratoryId;
	}

	public void setLaboratoryId(String laboratoryId) {
		this.laboratoryId = laboratoryId;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public boolean isLastVersion() {
		return isLastVersion;
	}

	public void setLastVersion(boolean isLastVersion) {
		this.isLastVersion = isLastVersion;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	public Map<String, Object> getItems() {
		return items;
	}

	public void setItems(Map<String, Object> items) {
		this.items = items;
	}

	public LocalDate getCollectedDate() {
		return collectedDate;
	}

	public void setCollectedDate(LocalDate collectedDate) {
		this.collectedDate = collectedDate;
	}

	public LocalDate getReceivedDate() {
		return receivedDate;
	}

	public void setReceivedDate(LocalDate receivedDate) {
		this.receivedDate = receivedDate;
	}

	public String getSampleType() {
		return sampleType;
	}

	public void setSampleType(String sampleType) {
		this.sampleType = sampleType;
	}

	public String getA260280() {
		return a260280;
	}

	public void setA260280(String a260280) {
		this.a260280 = a260280;
	}

	public String getCncnt() {
		return cncnt;
	}

	public void setCncnt(String cncnt) {
		this.cncnt = cncnt;
	}

	public String getDnaQc() {
		return dnaQc;
	}

	public void setDnaQc(String dnaQc) {
		this.dnaQc = dnaQc;
	}

	public GenotypingMethodCode getGenotypingMethodCode() {
		return genotypingMethodCode;
	}

	public void setGenotypingMethodCode(GenotypingMethodCode genotypingMethodCode) {
		this.genotypingMethodCode = genotypingMethodCode;
	}

	public String getMappingNo() {
		return mappingNo;
	}

	public void setMappingNo(String mappingNo) {
		this.mappingNo = mappingNo;
	}

	public String getWellPosition() {
		return wellPosition;
	}

	public void setWellPosition(String wellPosition) {
		this.wellPosition = wellPosition;
	}

	public String getChipBarcode() {
		return chipBarcode;
	}

	public void setChipBarcode(String chipBarcode) {
		this.chipBarcode = chipBarcode;
	}

	public ChipTypeCode getChipTypeCode() {
		return chipTypeCode;
	}

	public void setChipTypeCode(ChipTypeCode chipTypeCode) {
		this.chipTypeCode = chipTypeCode;
	}

	public StatusCode getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(StatusCode statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getCheckCelFile() {
		return checkCelFile;
	}

	public void setCheckCelFile(String checkCelFile) {
		this.checkCelFile = checkCelFile;
	}

	public String getOutputProductTypes() {
		return outputProductTypes;
	}

	public void setOutputProductTypes(String outputProductTypes) {
		this.outputProductTypes = outputProductTypes;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}

	public LocalDateTime getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(LocalDateTime modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public Member getCreatedMember() {
		return createdMember;
	}

	public void setCreatedMember(Member createdMember) {
		this.createdMember = createdMember;
	}

	public LocalDateTime getInputApproveDate() {
		return inputApproveDate;
	}

	public void setInputApproveDate(LocalDateTime inputApproveDate) {
		this.inputApproveDate = inputApproveDate;
	}

	public Member getInputApproveMember() {
		return inputApproveMember;
	}

	public void setInputApproveMember(Member inputApproveMember) {
		this.inputApproveMember = inputApproveMember;
	}

	public LocalDateTime getInputMngApproveDate() {
		return inputMngApproveDate;
	}

	public void setInputMngApproveDate(LocalDateTime inputMngApproveDate) {
		this.inputMngApproveDate = inputMngApproveDate;
	}

	public Member getInputMngApproveMember() {
		return inputMngApproveMember;
	}

	public void setInputMngApproveMember(Member inputMngApproveMember) {
		this.inputMngApproveMember = inputMngApproveMember;
	}

	public LocalDateTime getInputDrctApproveDate() {
		return inputDrctApproveDate;
	}

	public void setInputDrctApproveDate(LocalDateTime inputDrctApproveDate) {
		this.inputDrctApproveDate = inputDrctApproveDate;
	}

	public Member getInputDrctMember() {
		return inputDrctMember;
	}

	public void setInputDrctMember(Member inputDrctMember) {
		this.inputDrctMember = inputDrctMember;
	}

	public LocalDateTime getExpStartDate() {
		return expStartDate;
	}

	public void setExpStartDate(LocalDateTime expStartDate) {
		this.expStartDate = expStartDate;
	}

	public Member getExpStartMember() {
		return expStartMember;
	}

	public void setExpStartMember(Member expStartMember) {
		this.expStartMember = expStartMember;
	}

	public LocalDateTime getExpStep1Date() {
		return expStep1Date;
	}

	public void setExpStep1Date(LocalDateTime expStep1Date) {
		this.expStep1Date = expStep1Date;
	}

	public Member getExpStep1Member() {
		return expStep1Member;
	}

	public void setExpStep1Member(Member expStep1Member) {
		this.expStep1Member = expStep1Member;
	}

	public LocalDateTime getExpStep2Date() {
		return expStep2Date;
	}

	public void setExpStep2Date(LocalDateTime expStep2Date) {
		this.expStep2Date = expStep2Date;
	}

	public Member getExpStep2Member() {
		return expStep2Member;
	}

	public void setExpStep2Member(Member expStep2Member) {
		this.expStep2Member = expStep2Member;
	}

	public LocalDateTime getExpStep3Date() {
		return expStep3Date;
	}

	public void setExpStep3Date(LocalDateTime expStep3Date) {
		this.expStep3Date = expStep3Date;
	}

	public Member getExpStep3Member() {
		return expStep3Member;
	}

	public void setExpStep3Member(Member expStep3Member) {
		this.expStep3Member = expStep3Member;
	}

	public LocalDateTime getAnlsStartDate() {
		return anlsStartDate;
	}

	public void setAnlsStartDate(LocalDateTime anlsStartDate) {
		this.anlsStartDate = anlsStartDate;
	}

	public Member getAnlsStartMember() {
		return anlsStartMember;
	}

	public void setAnlsStartMember(Member anlsStartMember) {
		this.anlsStartMember = anlsStartMember;
	}

	public LocalDateTime getAnlsEndDate() {
		return anlsEndDate;
	}

	public void setAnlsEndDate(LocalDateTime anlsEndDate) {
		this.anlsEndDate = anlsEndDate;
	}

	public LocalDateTime getAnlsCmplDate() {
		return anlsCmplDate;
	}

	public void setAnlsCmplDate(LocalDateTime anlsCmplDate) {
		this.anlsCmplDate = anlsCmplDate;
	}

	public Member getAnlsCmplMember() {
		return anlsCmplMember;
	}

	public void setAnlsCmplMember(Member anlsCmplMember) {
		this.anlsCmplMember = anlsCmplMember;
	}

	public LocalDateTime getJdgmApproveDate() {
		return jdgmApproveDate;
	}

	public void setJdgmApproveDate(LocalDateTime jdgmApproveDate) {
		this.jdgmApproveDate = jdgmApproveDate;
	}

	public Member getJdgmApproveMember() {
		return jdgmApproveMember;
	}

	public void setJdgmApproveMember(Member jdgmApproveMember) {
		this.jdgmApproveMember = jdgmApproveMember;
	}

	public LocalDateTime getJdgmMngApproveDate() {
		return jdgmMngApproveDate;
	}

	public void setJdgmMngApproveDate(LocalDateTime jdgmMngApproveDate) {
		this.jdgmMngApproveDate = jdgmMngApproveDate;
	}

	public Member getJdgmMngApproveMember() {
		return jdgmMngApproveMember;
	}

	public void setJdgmMngApproveMember(Member jdgmMngApproveMember) {
		this.jdgmMngApproveMember = jdgmMngApproveMember;
	}

	public LocalDateTime getJdgmDrctApproveDate() {
		return jdgmDrctApproveDate;
	}

	public void setJdgmDrctApproveDate(LocalDateTime jdgmDrctApproveDate) {
		this.jdgmDrctApproveDate = jdgmDrctApproveDate;
	}

	public Member getJdgmDrctApproveMember() {
		return jdgmDrctApproveMember;
	}

	public void setJdgmDrctApproveMember(Member jdgmDrctApproveMember) {
		this.jdgmDrctApproveMember = jdgmDrctApproveMember;
	}

	public LocalDateTime getOutputWaitDate() {
		return outputWaitDate;
	}

	public void setOutputWaitDate(LocalDateTime outputWaitDate) {
		this.outputWaitDate = outputWaitDate;
	}

	public Member getOutputWaitMember() {
		return outputWaitMember;
	}

	public void setOutputWaitMember(Member outputWaitMember) {
		this.outputWaitMember = outputWaitMember;
	}

	public LocalDateTime getOutputCmplDate() {
		return outputCmplDate;
	}

	public void setOutputCmplDate(LocalDateTime outputCmplDate) {
		this.outputCmplDate = outputCmplDate;
	}

	public LocalDateTime getReOutputWaitDate() {
		return reOutputWaitDate;
	}

	public void setReOutputWaitDate(LocalDateTime reOutputWaitDate) {
		this.reOutputWaitDate = reOutputWaitDate;
	}

	public Member getReOutputWaitMember() {
		return reOutputWaitMember;
	}

	public void setReOutputWaitMember(Member reOutputWaitMember) {
		this.reOutputWaitMember = reOutputWaitMember;
	}

	public LocalDateTime getReOutputCmplDate() {
		return reOutputCmplDate;
	}

	public void setReOutputCmplDate(LocalDateTime reOutputCmplDate) {
		this.reOutputCmplDate = reOutputCmplDate;
	}

	public String getGenotypingId() {
		return this.laboratoryId + "-V" + this.version;
	}

}
