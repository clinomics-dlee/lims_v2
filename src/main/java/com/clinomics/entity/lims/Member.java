package com.clinomics.entity.lims;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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
@Table(name="member")
public class Member implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(length = 40)
	private String id;

	@Column(length = 100)
	private String password;

	@Column(length = 100)
	private String email;

	@Column(length = 100)
	private String name;

	@Column(length = 80)
	private String dept;
	
	@Column(columnDefinition = "boolean default true")
	private boolean inUse;

	@ManyToMany(fetch = FetchType.EAGER, cascade =  CascadeType.ALL)
	@JoinTable(name="member_role",
				joinColumns = @JoinColumn(name = "memberId", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "roleId", referencedColumnName = "id"))
	private Set<Role> role = new HashSet<Role>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDept() {
		return dept;
	}

	public void setDept(String dept) {
		this.dept = dept;
	}

	public boolean isInUse() {
		return inUse;
	}

	public void setInUse(boolean inUse) {
		this.inUse = inUse;
	}

	public Set<Role> getRole() {
		return role;
	}

	public void setRole(Set<Role> role) {
		this.role = role;
	}

}