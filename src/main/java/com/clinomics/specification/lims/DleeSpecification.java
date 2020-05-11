package com.clinomics.specification.lims;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.jpa.domain.Specification;

import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.StatusCode;

public class DleeSpecification {

	public static Specification<Sample> createdDateOneMonth(Map<String, String> params) {
		
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			if (params.containsKey("yyyymm")) {
				List<Predicate> predicatesAnds = new ArrayList<>();
				String yyyymm = params.get("yyyymm");
				
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				LocalDateTime start = LocalDateTime.parse(yyyymm + "-01 00:00:00", formatter);
				LocalDateTime end = start.plusMonths(1).minusSeconds(1);
				
				predicatesAnds.add(criteriaBuilder.between(root.get("createdDate"), start, end));
				rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			}
			return rtn;
			
		};
	}

	public static Specification<Sample> betweenDate(Map<String, String> params) {
		
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			if (params.containsKey("sDate") && params.containsKey("fDate")) {
				List<Predicate> predicatesAnds = new ArrayList<>();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				LocalDateTime start = LocalDateTime.parse(params.get("sDate") + " 00:00:00", formatter);
				LocalDateTime end = LocalDateTime.parse(params.get("fDate") + " 23:59:59", formatter);
				predicatesAnds.add(criteriaBuilder.between(root.get("createdDate"), start, end));
				rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			}
			return rtn;
		
		};
	}
	
	public static Specification<Sample> bundleId(Map<String, String> params) {
		
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			if (params.containsKey("bundleId") && !params.get("bundleId").isEmpty()) {
				List<Predicate> predicatesAnds = new ArrayList<>();
				int bundleId = NumberUtils.toInt(params.get("bundleId"));
				predicatesAnds.add(criteriaBuilder.equal(root.get("bundle").get("id"), bundleId));
				rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			}
			return rtn;
		};
	}
	
	public static Specification<Sample> keywordLike(Map<String, String> params) {
		
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicateLikes = new ArrayList<>();
			
			if (params.containsKey("keyword") && !params.get("keyword").isEmpty()) {
				String text = "%" + params.get("keyword") + "%";
				predicateLikes.add(criteriaBuilder.like(criteriaBuilder.function("JSON_EXTRACT", String.class, root.get("items"), criteriaBuilder.literal("$.*")), text));
				
				predicateLikes.add(criteriaBuilder.like(root.get("id"), text));
				predicateLikes.add(criteriaBuilder.like(root.get("barcode"), text));
				predicateLikes.add(criteriaBuilder.like(root.get("bundle").get("name"), text));
				//predicates.add(criteriaBuilder.like(root.get("member"), "%" + text + "%"));
				
				rtn = criteriaBuilder.or(predicateLikes.toArray(new Predicate[predicateLikes.size()]));
			}
			
			return rtn;
			
		};
	}
	
	public static Specification<Sample> existsStatusIn(List<StatusCode> statusCodes) {
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			if (statusCodes != null && statusCodes.size() > 0) {
				rtn = root.get("statusCode").in(statusCodes);
			}
			
			return rtn;
		};
	}
}
