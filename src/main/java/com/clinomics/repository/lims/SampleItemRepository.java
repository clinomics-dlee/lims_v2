package com.clinomics.repository.lims;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clinomics.entity.lims.SampleItem;

public interface SampleItemRepository extends JpaRepository<SampleItem, Integer> {
	
	List<SampleItem> findByName(String name);

}
