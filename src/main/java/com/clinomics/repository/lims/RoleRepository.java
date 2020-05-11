package com.clinomics.repository.lims;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.clinomics.entity.lims.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {
	@Query(value = "SELECT * FROM role WHERE name LIKE %?1%", nativeQuery = true)
	List<Role> findBySearchValue(String searchValue, Pageable pageable);
	
	@Query(value = "SELECT COUNT(*) FROM role WHERE name LIKE %?1%", nativeQuery = true)
	int countBySearchValue(String searchValue);
}
