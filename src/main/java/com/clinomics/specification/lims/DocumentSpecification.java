package com.clinomics.specification.lims;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.jpa.domain.Specification;

import com.clinomics.entity.lims.Document;
import com.google.common.collect.Lists;

public class DocumentSpecification {
	public static Specification<Document> isReg() {
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicatesAnds = new ArrayList<>();

			predicatesAnds.add(criteriaBuilder.equal(root.get("isReg"), true));

			rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			return rtn;
		};
	}

	public static Specification<Document> isNotReg() {
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicatesAnds = new ArrayList<>();

			predicatesAnds.add(criteriaBuilder.equal(root.get("isReg"), false));

			rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			return rtn;
		};
	}

	public static Specification<Document> betweenDate(Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			if (params.containsKey("sDate") && params.containsKey("fDate")) {
				if (params.get("sDate").length() > 0 && params.get("fDate").length() > 0) {
					List<Predicate> predicatesAnds = new ArrayList<>();
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					LocalDateTime start = LocalDateTime.parse(params.get("sDate") + " 00:00:00", formatter);
					LocalDateTime end = LocalDateTime.parse(params.get("fDate") + " 23:59:59", formatter);
					predicatesAnds.add(criteriaBuilder.between(root.get("createdDate"), start, end));
					rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
				}
			}
			return rtn;

		};
	}

	public static Specification<Document> betweenModifiedDate(Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			if (params.containsKey("sDate") && params.containsKey("fDate")) {
				if (params.get("sDate").length() > 0 && params.get("fDate").length() > 0) {
					List<Predicate> predicatesAnds = new ArrayList<>();
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					LocalDateTime start = LocalDateTime.parse(params.get("sDate") + " 00:00:00", formatter);
					LocalDateTime end = LocalDateTime.parse(params.get("fDate") + " 23:59:59", formatter);
					predicatesAnds.add(criteriaBuilder.between(root.get("lastModifiedDate"), start, end));
					rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
				}
			}
			return rtn;

		};
	}

	public static Specification<Document> bundleId(Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicatesAnds = new ArrayList<>();
			if (params.containsKey("bundleId") && !params.get("bundleId").toString().isEmpty()) {
				
				List<String> bundles = Arrays.asList((params.get("bundleId") + "").split(","));
				List<Integer> bundleIds = bundles.stream().map(b -> NumberUtils.toInt(b)).collect(Collectors.toList());
				
				predicatesAnds.add(criteriaBuilder.and(root.get("bundle").get("id").in(bundleIds)));
			}
			rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			return rtn;
		};
	}

	public static Specification<Document> hNameIn(Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicatesAnds = new ArrayList<>();

			if (params.containsKey("agencies") && !params.get("agencies").toString().isEmpty()) {
				
				List<String> hospitals = Arrays.asList((params.get("agencies") + "").split(","));

				hospitals.stream().forEach(h -> {
					predicatesAnds.add(criteriaBuilder.or(root.get("agency").get("name").in(h)));
				});

				rtn = criteriaBuilder.or(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			}
			return rtn;

		};
	}

	public static Specification<Document> keywordLike(Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicateLikes = new ArrayList<>();

			if (params.containsKey("keyword") && !params.get("keyword").toString().isEmpty()) {
				String text = "%" + params.get("keyword") + "%";

				predicateLikes.add(criteriaBuilder.like(root.get("doctorName"), text));
				predicateLikes.add(criteriaBuilder.like(root.get("name"), text));
				predicateLikes.add(criteriaBuilder.like(root.get("tel"), text));
				predicateLikes.add(criteriaBuilder.like(root.get("address"), text));
				predicateLikes.add(criteriaBuilder.like(root.get("sex"), text));
				predicateLikes.add(criteriaBuilder.like(root.get("birthday"), text));
				predicateLikes.add(criteriaBuilder.like(root.get("bundle").get("name"), text));
				// predicates.add(criteriaBuilder.like(root.get("member"), "%" + text + "%"));

				rtn = criteriaBuilder.or(predicateLikes.toArray(new Predicate[predicateLikes.size()]));
			}

			return rtn;

		};
	}

	public static Specification<Document> bundleIsActive() {
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicatesAnds = new ArrayList<>();

			predicatesAnds.add(criteriaBuilder.isTrue(root.get("bundle").get("isActive")));
			rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));

			return rtn;
		};
	}

	public static Specification<Document> orderBy(Map<String, String> params) {
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			if (params.containsKey("order") && !params.get("order").isEmpty()) {
				List<Order> orderList = Lists.newArrayList();
				String order = params.get("order");

				List<String> sorting = Arrays.asList(order.substring(1).split(","));

				for (String sort : sorting) {

					String key = sort.replaceAll("^([\\S]+):([\\S]+)$", "$1");
					String type = sort.replaceAll("^([\\S]+):([\\S]+)$", "$2");

					Expression<?> expression = null;
					Order objQuery = null;

					if (key.contains("items")) {
						expression = criteriaBuilder.function(
							"JSON_EXTRACT"
							, String.class
							, root.get("items")
							, criteriaBuilder.literal("$." + key.replace("items.", ""))
						);
					} else {
						int dots = StringUtils.countMatches(key, ".");
						if (dots == 0) {
							expression = root.get(key);
						} else if (dots == 1) {
							String key1 = key.replaceAll("^([\\S]+)\\.([\\S]+)$", "$1");
							String key2 = key.replaceAll("^([\\S]+)\\.([\\S]+)$", "$2");
							expression = root.get(key1).get(key2);
						}
					}

					if ("asc".equals(type)) {
						objQuery = criteriaBuilder.asc(expression);					
					} else {
						objQuery = criteriaBuilder.desc(expression);
					}
					orderList.add(objQuery);
				}
				if (orderList.size() > 0) query.orderBy(orderList);
				
			} else {
				query.orderBy(criteriaBuilder.desc(root.get("id")));
			}
			return rtn;
		
		};
	}
}
