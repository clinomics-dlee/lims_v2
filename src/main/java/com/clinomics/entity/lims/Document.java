package com.clinomics.entity.lims;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="document")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Document {
    
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne()
	@JoinColumn(name="agencyId")
	private Agency agency;

    @ManyToOne()
	@JoinColumn(name="bundleId")
	private Bundle bundle = new Bundle();

    private String doctorName;

    private String chartNumber;

    private String name;

    private String tel;

    private String address;

    private String sex;

    private String birthday;

    private String height;

    private String weight;

    private String smoking;

    private String alcohol;

    private String meat;

    private String instant;

    private String fried;

    private String salt;

    private String exercise;

    private String depression;

    private String stress;

    private boolean isReg = false;

    @CreatedDate
	private LocalDateTime createdDate;

    @ManyToOne()
	@JoinColumn(name = "lastModifiedMemberId")
	private Member lastModifiedMember;

    @LastModifiedDate
	private LocalDateTime lastModifiedDate;
}
