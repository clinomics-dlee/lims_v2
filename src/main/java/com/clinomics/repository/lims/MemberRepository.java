package com.clinomics.repository.lims;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.clinomics.entity.lims.Member;

public interface MemberRepository extends JpaRepository<Member, String> {
	
	@Query(value = "SELECT * FROM member WHERE id LIKE %?1% OR name LIKE %?1%", nativeQuery = true)
	List<Member> findBySearchValue(String searchValue, Pageable pageable);
	
	@Query(value = "SELECT COUNT(*) FROM member WHERE id LIKE %?1% OR name LIKE %?1%", nativeQuery = true)
	int countBySearchValue(String searchValue);
}
