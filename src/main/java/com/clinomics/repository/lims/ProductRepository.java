package com.clinomics.repository.lims;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clinomics.entity.lims.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
	
	List<Product> findByName(String name);

}
