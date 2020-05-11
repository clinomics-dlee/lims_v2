package com.clinomics.entity.lims;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.clinomics.config.StringMapConverter;

@Entity
@Table(name="sample_history")
@EntityListeners(AuditingEntityListener.class)
public class SampleHistory implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @ManyToOne
	@JoinColumn(name = "sampleId")
	private Sample sample;
	
	private String barcode;
    
	@Column(nullable = false)
	private int numbering;
	
	@Column(columnDefinition = "json")
	@Convert(converter = StringMapConverter.class)
	private Map<String, Object> items = new HashMap<>();
	
	@ManyToOne(cascade =  CascadeType.ALL)
	@JoinColumn(name = "createdMemberId")
	private Member member;
	
	@CreatedDate
	private LocalDateTime createdDate;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Sample getSample() {
		return sample;
	}

	public void setSample(Sample sample) {
		this.sample = sample;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public int getNumbering() {
		return numbering;
	}

	public void setNumbering(int numbering) {
		this.numbering = numbering;
	}

	public Map<String, Object> getItems() {
		return items;
	}

	public void setItems(Map<String, Object> items) {
		this.items = items;
	}

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}

}
