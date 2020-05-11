package com.clinomics.specification.lims;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.jpa.domain.Specification;

import com.clinomics.enums.StatusCode;

public class ResultSpecification {

	// public static Specification<Result> betweenDate(Map<String, String> params) {
	// 	return (root, query, criteriaBuilder) -> {
	// 		List<Predicate> predicatesAnds = new ArrayList<>();
	// 		if (params.containsKey("sDate") && params.containsKey("fDate")) {
	// 			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	// 			LocalDateTime start = LocalDateTime.parse(params.get("sDate") + " 00:00:00", formatter);
	// 			LocalDateTime end = LocalDateTime.parse(params.get("fDate") + " 23:59:59", formatter);
	// 			predicatesAnds.add(criteriaBuilder.between(root.get("createdDate"), start, end));
	// 		}
	// 		return criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
	// 	};
	// }

	// public static Specification<Result> modifiedDateOneMonth(Map<String, String> params) {
	// 	return new Specification<Result>() {
	// 		/**
	// 		 * 
	// 		 */
	// 		private static final long serialVersionUID = 1L;

	// 		@Override
	// 		public Predicate toPredicate(Root<Result> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
	// 			List<Predicate> predicatesAnds = new ArrayList<>();
	// 			if (params.containsKey("yyyymm")) {
	// 				String yyyymm = params.get("yyyymm");
					
	// 				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	// 				LocalDateTime start = LocalDateTime.parse(yyyymm + "-01 00:00:00", formatter);
	// 				LocalDateTime end = start.plusMonths(1).minusSeconds(1);

	// 				predicatesAnds.add(criteriaBuilder.between(root.get("modifiedDate"), start, end));
	// 			}
	// 			return criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
	// 		}
	// 	};
	// }
	
	// public static Specification<Result> bundleId(Map<String, String> params) {
	// 	return (root, query, criteriaBuilder) -> {
	// 		Predicate rtn = null;
	// 		List<Predicate> predicatesAnds = new ArrayList<>();
	// 		if (params.containsKey("bundleId") && !params.get("bundleId").isEmpty()) {
	// 			int bundleId = NumberUtils.toInt(params.get("bundleId"));
	// 			predicatesAnds.add(criteriaBuilder.equal(root.get("sample").get("bundle").get("id"), bundleId));
				
	// 			rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
	// 		}
	// 		return rtn;
	// 	};
	// }
	
	// public static Specification<Result> keywordLike(Map<String, String> params) {
	// 	return (root, query, criteriaBuilder) -> {
	// 		Predicate rtn = null;
	// 		List<Predicate> predicateLikes = new ArrayList<>();
			
	// 		if (params.containsKey("keyword") && !params.get("keyword").isEmpty()) {
	// 			String text = "%" + params.get("keyword") + "%";
				
	// 			predicateLikes.add(criteriaBuilder.like(root.get("chipNumber"), text));
	// 			predicateLikes.add(criteriaBuilder.like(root.get("sample").get("id"), text));
	// 			predicateLikes.add(criteriaBuilder.like(root.get("sample").get("barcode"), text));
	// 			predicateLikes.add(criteriaBuilder.like(criteriaBuilder.function("JSON_EXTRACT", String.class, root.get("sample").get("items"), criteriaBuilder.literal("$.*")), text));
	// 			predicateLikes.add(criteriaBuilder.like(root.get("member").get("id"), text));
	// 			predicateLikes.add(criteriaBuilder.like(root.get("member").get("name"), text));
				
	// 			rtn = criteriaBuilder.or(predicateLikes.toArray(new Predicate[predicateLikes.size()]));
	// 		}
			
	// 		return rtn;
	// 	};
	// }
	
	// public static Specification<Result> statusCodeIn(List<StatusCode> statusCodes) {
	// 	return (root, query, criteriaBuilder) -> {
	// 		Predicate rtn = null;
	// 		if (statusCodes != null && statusCodes.size() > 0) {
	// 			rtn = root.get("statusCode").in(statusCodes);
	// 		}
			
	// 		return rtn;
	// 	};
	// }

	// public static Specification<Result> lastResult(String sampleId) {
	// 	return (root, query, criteriaBuilder) -> {
	// 		Predicate rtn = null;
	// 		List<Predicate> predicatesAnds = new ArrayList<>();
	// 		predicatesAnds.add(criteriaBuilder.equal(root.get("sample").get("id"), sampleId));
	// 		criteriaBuilder.max(root.get("createdDate"));
	// 		rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
	// 		return rtn;
	// 	};
	// }
}
