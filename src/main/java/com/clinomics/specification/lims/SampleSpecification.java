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

public class SampleSpecification {

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
	
	public static Specification<Sample> notExistsResult() {
		return (root, query, criteriaBuilder) -> {
			// Subquery<Result> subquery = query.subquery(Result.class);
			// Root<Result> subqueryRoot = subquery.from(Result.class);
			// subquery.select(subqueryRoot);
			
			// Predicate predicate = criteriaBuilder.equal(subqueryRoot.get("sample").get("id"), root.get("id"));
			// subquery.select(subqueryRoot).where(predicate);
			
			// return criteriaBuilder.not(criteriaBuilder.exists(subquery));
			return null;
		};
	}
	
	public static Specification<Sample> existsResultStatusIn(List<StatusCode> statusCodes) {
		return (root, query, criteriaBuilder) -> {
			// Subquery<Result> subquery = query.subquery(Result.class);
			// Root<Result> subqueryRoot = subquery.from(Result.class);
			// subquery.select(subqueryRoot);
			
			// List<Predicate> predicatesAnds = new ArrayList<>();
			// predicatesAnds.add(criteriaBuilder.equal(subqueryRoot.get("sample").get("id"), root.get("id")));
			// predicatesAnds.add(subqueryRoot.get("statusCode").in(statusCodes));
			// subquery.select(subqueryRoot).where(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			
			// return criteriaBuilder.exists(subquery);
			return null;
		};
	}
	
	public static Specification<Sample> notExistsResultNotEqualStatus(StatusCode statusCode) {
		return (root, query, criteriaBuilder) -> {
			// Subquery<Result> subquery = query.subquery(Result.class);
			// Root<Result> subqueryRoot = subquery.from(Result.class);
			// subquery.select(subqueryRoot);
			
			// List<Predicate> predicatesAnds = new ArrayList<>();
			// predicatesAnds.add(criteriaBuilder.equal(subqueryRoot.get("sample").get("id"), root.get("id")));
			// predicatesAnds.add(criteriaBuilder.not(criteriaBuilder.equal(subqueryRoot.get("statusCode"), statusCode)));
			// subquery.select(subqueryRoot).where(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			
			// return criteriaBuilder.not(criteriaBuilder.exists(subquery));
			return null;
		};
	}
}
