package com.clinomics.entity.lims;

import java.io.Serializable;
import java.util.LinkedHashSet;
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


/**
 * The persistent class for the admin_user database table.
 * 
 */
@Entity
@Table(name="product")
public class Product implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(length = 100)
	private String name;

	@Column(length = 20)
	private String type;

	@Column(length = 100)
	private String dept;
	
	private boolean isActive = true;

	@ManyToMany(cascade =  CascadeType.ALL)
	@JoinTable(
		name="product_sample_item",
			joinColumns = @JoinColumn(name = "productId", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "sampleItemId", referencedColumnName = "id")
	)
	private Set<SampleItem> sampleItem = new LinkedHashSet<SampleItem>();
	
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

	public String getDept() {
		return dept;
	}

	public void setDept(String dept) {
		this.dept = dept;
	}

	public Set<SampleItem> getSampleItem() {
		return sampleItem;
	}

	public void setSampleItem(Set<SampleItem> sampleItem) {
		this.sampleItem = sampleItem;
	}
	
	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	
	@Override
	public String toString() {
		return "Product [id=" + id + ", name=" + name + ", type=" + type + ", dept=" + dept + "]";
	}

}