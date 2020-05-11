package com.clinomics.service.setting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Product;
import com.clinomics.enums.ResultCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.ProductRepository;
import com.clinomics.service.DataTableService;
import com.google.common.collect.Maps;

@Service
public class BundleService {

	@Autowired
	ProductRepository productRepository;

	@Autowired
	BundleRepository bundleRepository;

	@Autowired
	DataTableService dataTableService;

	@PersistenceContext
	private EntityManager entityManager;

	public Bundle selectOne(int id) {
		return bundleRepository.findById(id).orElse(new Bundle());
	}

	public List<Bundle> selectAll() {
		return bundleRepository.findAll();
	}

	public Map<String, Object> selectAll(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 1);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);
		long total = bundleRepository.countByIsSingleAndIsActiveTrue(false);
		long filtered = total;
		
		List<Order> orders = Arrays.asList(new Order[] {
			Order.asc("id")
		});
		// #. paging 관련 객체
		Pageable pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		
		List<Bundle> list = bundleRepository.findByIsSingleAndIsActiveTrue(false, pageable);
		
		Map<String, Object> rtn = dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list);
		
		rtn.put("products", productRepository.findAll());
		
		return rtn;
	}

	/*
	 * @Transactional public Map<String, String> insert(Map<String, String> datas) {
	 * Map<String, String> rtn = Maps.newHashMap(); String name = datas.get("name");
	 * String products = datas.get("products");
	 * 
	 * List<Bundle> existsBundle = bundleRepository.findByName(name); if
	 * (existsBundle.size() > 0) { rtn.put("result", "91"); }
	 * 
	 * Bundle newBundle = new Bundle(); newBundle.setName(name);
	 * newBundle.setSingle(false); Set<Product> sp = new HashSet<Product>();
	 * 
	 * Stream.of(products.split("\\|")).forEach(p -> { Optional<Product> mProduct =
	 * productRepository.findById(NumberUtils.toInt(p)); mProduct.ifPresent(mp ->
	 * sp.add(mp));
	 * 
	 * }); newBundle.setProduct(sp); if (datas.containsKey("useAutoBarcode")) {
	 * newBundle.setAutoBarcode(BooleanUtils.toBooleanObject(datas.get(
	 * "useAutoBarcode"))); } else { newBundle.setAutoBarcode(false); } if
	 * (datas.containsKey("useAutoSequence")) {
	 * newBundle.setAutoSequence(BooleanUtils.toBooleanObject(datas.get(
	 * "useAutoSequence"))); } else { newBundle.setAutoSequence(false); } if
	 * (datas.containsKey("barcodeRole")) {
	 * newBundle.setBarcodeRole(datas.get("barcodeRole")); } if
	 * (datas.containsKey("sequenceRole")) {
	 * newBundle.setSequenceRole(datas.get("sequenceRole")); } if
	 * (datas.containsKey("type")) { newBundle.setType(datas.get("type")); }
	 * 
	 * bundleRepository.save(newBundle);
	 * 
	 * rtn.put("result", ResultCode.SUCCESS.get()); return rtn; }
	 */
	
	@Transactional
	public Map<String, String> save(Map<String, String> datas) {
		int id = NumberUtils.toInt(datas.get("id"));
		Map<String, String> rtn = Maps.newHashMap();
		
		Optional<Bundle> oBundle = bundleRepository.findById(id);
		Bundle bundle = oBundle.orElse(new Bundle());

		if (datas.containsKey("name")) {
			String name = datas.get("name");
			bundle.setName(name);
		}
		if (datas.containsKey("autoBarcode")) {
			bundle.setAutoBarcode(BooleanUtils.toBooleanObject(datas.get("autoBarcode")));
		} else {
			bundle.setAutoBarcode(false);
		}
		if (datas.containsKey("autoSequence")) {
			bundle.setAutoSequence(BooleanUtils.toBooleanObject(datas.get("autoSequence")));
		} else {
			bundle.setAutoSequence(false);
		}
		if (datas.containsKey("barcodeRole")) {
			bundle.setBarcodeRole(datas.get("barcodeRole"));
		}
		if (datas.containsKey("sequenceRole")) {
			bundle.setSequenceRole(datas.get("sequenceRole"));
		}
		if (datas.containsKey("type")) {
			bundle.setType(datas.get("type"));
		}
		if (datas.containsKey("single")) {
			bundle.setSingle(BooleanUtils.toBooleanObject(datas.get("single")));
		}
		rtn.put("result", ResultCode.SUCCESS.get());
		
		if (datas.containsKey("active")) {
			boolean active = BooleanUtils.toBooleanObject(datas.get("active"));
			bundle.setActive(active);
			rtn.put("result", ResultCode.SUCCESS_NOT_USE_ALERT.get());
		}
		if (datas.containsKey("products")) {
			String products = datas.get("products");
			if (!products.isEmpty()) {
				Set<Product> sp = new HashSet<Product>();
				Stream.of(products.split("\\|")).forEach(p -> {
					Optional<Product> mProduct = productRepository.findById(NumberUtils.toInt(p));
					mProduct.ifPresent(mp -> sp.add(mp));
				});
				bundle.setProduct(sp);
			}
		}
		
		bundleRepository.save(bundle);
		
		return rtn;
	}
}
