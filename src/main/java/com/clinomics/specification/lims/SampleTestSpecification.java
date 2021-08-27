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

import com.clinomics.entity.lims.SampleTest;
import com.clinomics.enums.StatusCode;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.jpa.domain.Specification;

public class SampleTestSpecification {

	public static Specification<SampleTest> createdDateOneMonth(Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			if (params.containsKey("yyyymm")) {
				List<Predicate> predicatesAnds = new ArrayList<>();
				String yyyymm = params.get("yyyymm") + "";

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				LocalDateTime start = LocalDateTime.parse(yyyymm + "-01 00:00:00", formatter);
				LocalDateTime end = start.plusMonths(1).minusSeconds(1);

				predicatesAnds.add(criteriaBuilder.between(root.get("createdDate"), start, end));
				rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			}
			return rtn;

		};
	}

	public static Specification<SampleTest> modifiedDateOneMonth(Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			if (params.containsKey("yyyymm")) {
				List<Predicate> predicatesAnds = new ArrayList<>();
				String yyyymm = params.get("yyyymm") + "";

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				LocalDateTime start = LocalDateTime.parse(yyyymm + "-01 00:00:00", formatter);
				LocalDateTime end = start.plusMonths(1).minusSeconds(1);

				predicatesAnds.add(criteriaBuilder.between(root.get("modifiedDate"), start, end));
				rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			}
			return rtn;

		};
	}

	public static Specification<SampleTest> customDateOneMonth(String field, Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			if (params.containsKey("yyyymm")) {
				List<Predicate> predicatesAnds = new ArrayList<>();
				String yyyymm = params.get("yyyymm") + "";

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				LocalDateTime start = LocalDateTime.parse(yyyymm + "-01 00:00:00", formatter);
				LocalDateTime end = start.plusMonths(1).minusSeconds(1);

				predicatesAnds.add(criteriaBuilder.between(root.get(field), start, end));
				rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			}
			return rtn;

		};
	}

	public static Specification<SampleTest> customDateBetween(String field, Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			if (params.containsKey("sDate") && params.containsKey("fDate")) {
				if (params.get("sDate").length() > 0 && params.get("fDate").length() > 0) {
					List<Predicate> predicatesAnds = new ArrayList<>();
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					LocalDateTime start = LocalDateTime.parse(params.get("sDate") + " 00:00:00", formatter);
					LocalDateTime end = LocalDateTime.parse(params.get("fDate") + " 23:59:59", formatter);
					predicatesAnds.add(criteriaBuilder.between(root.get(field), start, end));
					rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
				}
			}
			return rtn;

		};
	}

	public static Specification<SampleTest> betweenDate(Map<String, String> params) {

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

	public static Specification<SampleTest> betweenModifiedDate(Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			if (params.containsKey("sDate") && params.containsKey("fDate")) {
				if (params.get("sDate").length() > 0 && params.get("fDate").length() > 0) {
					List<Predicate> predicatesAnds = new ArrayList<>();
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					LocalDateTime start = LocalDateTime.parse(params.get("sDate") + " 00:00:00", formatter);
					LocalDateTime end = LocalDateTime.parse(params.get("fDate") + " 23:59:59", formatter);
					predicatesAnds.add(criteriaBuilder.between(root.get("modifiedDate"), start, end));
					rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
				}
			}
			return rtn;

		};
	}

	// public static Specification<SampleTest> bundleId(Map<String, String> params) {

	// 	return (root, query, criteriaBuilder) -> {
	// 		Predicate rtn = null;
	// 		List<Predicate> predicatesAnds = new ArrayList<>();
	// 		if (params.containsKey("bundleId") && !params.get("bundleId").toString().isEmpty()) {
	// 			int bundleId = NumberUtils.toInt(params.get("bundleId") + "");
	// 			predicatesAnds.add(criteriaBuilder.equal(root.get("bundle").get("id"), bundleId));
	// 		}
	// 		rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
	// 		return rtn;
	// 	};
	// }

	public static Specification<SampleTest> bundleId(Map<String, String> params) {

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

	public static Specification<SampleTest> hNameIn(Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicatesAnds = new ArrayList<>();

			if (params.containsKey("agencies") && !params.get("agencies").toString().isEmpty()) {
				
				List<String> hospitals = Arrays.asList((params.get("agencies") + "").split(","));

				hospitals.stream().forEach(h -> {
					predicatesAnds.add(criteriaBuilder.equal(criteriaBuilder.function("JSON_EXTRACT", String.class,
						root.get("items"), criteriaBuilder.literal("$.h_name")), h));
				});

				rtn = criteriaBuilder.or(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			}
			return rtn;

		};
	}

	public static Specification<SampleTest> keywordLike(Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicateLikes = new ArrayList<>();

			if (params.containsKey("keyword") && !params.get("keyword").toString().isEmpty()) {
				String text = "%" + params.get("keyword") + "%";
				predicateLikes.add(criteriaBuilder.like(criteriaBuilder.function("JSON_EXTRACT", String.class,
						root.get("items"), criteriaBuilder.literal("$.*")), text));

				predicateLikes.add(criteriaBuilder.like(root.get("laboratoryId"), text));
				predicateLikes.add(criteriaBuilder.like(root.get("bundle").get("name"), text));
				// predicates.add(criteriaBuilder.like(root.get("member"), "%" + text + "%"));

				rtn = criteriaBuilder.or(predicateLikes.toArray(new Predicate[predicateLikes.size()]));
			}

			return rtn;

		};
	}

	public static Specification<SampleTest> statusIn(List<StatusCode> statusCodes) {
		return (root, query, criteriaBuilder) -> {

			Predicate rtn = null;
			List<Predicate> predicatesAnds = new ArrayList<>();

			predicatesAnds.add(criteriaBuilder.and(root.get("statusCode").in(statusCodes)));

			rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			return rtn;
		};
	}

	public static Specification<SampleTest> statusNotIn(List<StatusCode> statusCodes) {
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicatesAnds = new ArrayList<>();

			predicatesAnds.add(criteriaBuilder.not(root.get("statusCode").in(statusCodes)));

			rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			return rtn;
		};
	}

	public static Specification<SampleTest> statusEqual(StatusCode statusCode) {
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicatesAnds = new ArrayList<>();

			predicatesAnds.add(criteriaBuilder.equal(root.get("statusCode"), statusCode));

			rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			return rtn;
		};
	}

	public static Specification<SampleTest> statusNotEqual(StatusCode statusCode) {
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicatesAnds = new ArrayList<>();

			predicatesAnds.add(criteriaBuilder.notEqual(root.get("statusCode"), statusCode));

			rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			return rtn;
		};
	}

	public static Specification<SampleTest> bundleIsActive() {
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicatesAnds = new ArrayList<>();

			predicatesAnds.add(criteriaBuilder.isTrue(root.get("bundle").get("isActive")));
			rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));

			return rtn;
		};
	}

	public static Specification<SampleTest> laboratoryIdEqual(String laboratoryId) {
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = criteriaBuilder.equal(root.get("laboratoryId"), laboratoryId);
			return rtn;
		};
	}

	public static Specification<SampleTest> statusCodeGt(int number) {
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = criteriaBuilder.greaterThanOrEqualTo(
					criteriaBuilder.substring(root.get("statusCode"), 2, 3).as(Integer.class), number);
			return rtn;
		};
	}

	public static Specification<SampleTest> productNotLike(Map<String, String> params) {

		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicateLikes = new ArrayList<>();

			if (params.containsKey("productType") && !params.get("productType").isEmpty()) {
				String text = "%\\_" + params.get("productType") + "\\_%";

				predicateLikes.add(criteriaBuilder.notLike(root.get("outputProductTypes"), text));
				predicateLikes.add(criteriaBuilder.isNull(root.get("outputProductTypes")));

				rtn = criteriaBuilder.or(predicateLikes.toArray(new Predicate[predicateLikes.size()]));
			}

			return rtn;

		};
	}

	public static Specification<SampleTest> orderBy(Map<String, String> params) {
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
				query.orderBy(criteriaBuilder.desc(root.get("laboratoryId")));
			}
			return rtn;
		
		};
	}

	public static Specification<SampleTest> barcodeEqual(String barcode) {
		return (root, query, criteriaBuilder) -> {
			Predicate rtn = null;
			List<Predicate> predicatesAnds = new ArrayList<>();

			predicatesAnds.add(criteriaBuilder.equal(criteriaBuilder.function("JSON_EXTRACT", String.class,
				root.get("items"), criteriaBuilder.literal("$.barcode")), barcode));

			rtn = criteriaBuilder.and(predicatesAnds.toArray(new Predicate[predicatesAnds.size()]));
			return rtn;
		};
	}
}
