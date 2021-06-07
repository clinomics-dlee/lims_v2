package com.clinomics.entity.lims;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="agency")
@Getter
@Setter
public class Agency {
    
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    private String type;

    private int unitPrice;

    private String etc;

    private int sort;
}
