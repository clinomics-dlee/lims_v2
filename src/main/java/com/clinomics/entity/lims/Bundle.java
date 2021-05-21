package com.clinomics.entity.lims;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name="bundle")
public class Bundle implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(length = 200)
	private String name;

	@Column(length = 20)
	private String type;

	@Column(length = 100)
	private String barcode;

	@Column(length = 100)
	private String barcodeRole;

	@Column(length = 100)
	private String sequence;

	@Column(length = 100)
	private String sequenceRole;

	private int tatDay;

	private boolean isTatTueThu;

	private boolean isHospital;

	private boolean isAutoBarcode;

	private boolean isAutoSequence;
	
	private boolean isSingle;
	
	private boolean isActive = true;
	
	private int sort;

	@ManyToMany(cascade =  CascadeType.ALL)
	@JoinTable(name="bundle_product",
				joinColumns = @JoinColumn(name = "bundleId", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "productId", referencedColumnName = "id"))
	private Set<Product> product = new HashSet<Product>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public String getBarcodeRole() {
		return barcodeRole;
	}

	public void setBarcodeRole(String barcodeRole) {
		this.barcodeRole = barcodeRole;
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	public String getSequenceRole() {
		return sequenceRole;
	}

	public void setSequenceRole(String sequenceRole) {
		this.sequenceRole = sequenceRole;
	}

	public boolean isAutoBarcode() {
		return isAutoBarcode;
	}

	public void setAutoBarcode(boolean isAutoBarcode) {
		this.isAutoBarcode = isAutoBarcode;
	}

	public boolean isAutoSequence() {
		return isAutoSequence;
	}

	public void setAutoSequence(boolean isAutoSequence) {
		this.isAutoSequence = isAutoSequence;
	}

	public boolean isSingle() {
		return isSingle;
	}

	public void setSingle(boolean isSingle) {
		this.isSingle = isSingle;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public Set<Product> getProduct() {
		return product;
	}

	public void setProduct(Set<Product> product) {
		this.product = product;
	}

	public int getTatDay() {
		return tatDay;
	}

	public void setTatDay(int tatDay) {
		this.tatDay = tatDay;
	}

	public boolean isTatTueThu() {
		return isTatTueThu;
	}

	public void setTatTueThu(boolean isTatTueThu) {
		this.isTatTueThu = isTatTueThu;
	}

	public boolean isHospital() {
		return isHospital;
	}

	public void setHospital(boolean isHospital) {
		this.isHospital = isHospital;
	}
	
	public int getSort() {
		return sort;
	}

	public void setSort(int sort) {
		this.sort = sort;
	}

}
