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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Product;
import com.clinomics.entity.lims.SampleItem;
import com.clinomics.enums.ResultCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.ProductRepository;
import com.clinomics.repository.lims.SampleItemRepository;
import com.clinomics.service.DataTableService;
import com.google.common.collect.Maps;

@Service
public class ProductService {

	@Autowired
	ProductRepository productRepository;

	@Autowired
	SampleItemRepository sampleItemRepository;

	@Autowired
	BundleRepository bundleRepository;
	
	@Autowired
	DataTableService dataTableService;
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public Optional<Product> selectOne(int id) {
		return productRepository.findById(id);
	}

	public List<Product> selectAll() {
		return productRepository.findAll();
	}
	
	public Map<String, Object> selectBundleAll(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);
		long total = bundleRepository.countByIsSingleAndIsActiveTrue(true);
		long filtered = total;
		
		List<Order> orders = Arrays.asList(new Order[] {
			Order.asc("id")
		});
		// #. paging 관련 객체
		Pageable pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		
		List<Bundle> list = bundleRepository.findByIsSingleAndIsActiveTrue(true, pageable);
		
		Map<String, Object> rtn = dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list);
		
		return rtn;
	}
	
	public Map<String, Object> selectProductAll(Map<String, String> params) {
		int draw = 1; // #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10); // #.count 조회
		long total = productRepository.count();
		long filtered = total;

		List<Order> orders = Arrays.asList(new Order[] { Order.asc("id") }); // #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}

		Page<Product> page = productRepository.findAll(pageable);
		List<Product> list = page.getContent();

		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list);
	}
	
	@Transactional
	public Map<String, String> insert(Map<String, String> datas) {
		Map<String, String> rtn = Maps.newHashMap();
		String name = datas.get("name");
		
		List<Product> existsProduct =  productRepository.findByName(name);
		if (existsProduct.size() > 0 ) {
			rtn.put("result", ResultCode.FAIL_EXISTS_VALUE.get());
		}
		
		Product newProduct = new Product();
		newProduct.setName(name);
		newProduct.setActive(true);
		
		Bundle newBundle = new Bundle();
		newBundle.setName(name);
		newBundle.setSingle(true);
		if (datas.containsKey("autoBarcode")) {
			newBundle.setAutoBarcode(BooleanUtils.toBooleanObject(datas.get("autoBarcode")));
		} else {
			newBundle.setAutoBarcode(false);
		}
		if (datas.containsKey("autoSequence")) {
			newBundle.setAutoSequence(BooleanUtils.toBooleanObject(datas.get("autoSequence")));
		} else {
			newBundle.setAutoSequence(false);
		}
		if (datas.containsKey("barcodeRole")) {
			newBundle.setBarcodeRole(datas.get("barcodeRole"));
		}
		if (datas.containsKey("sequenceRole")) {
			newBundle.setSequenceRole(datas.get("sequenceRole"));
		}
		if (datas.containsKey("type")) {
			newBundle.setType(datas.get("type"));
		}
		Set<Product> sp = new HashSet<Product>();
		sp.add(newProduct);
		newBundle.setProduct(sp);
		
		bundleRepository.save(newBundle);
		productRepository.save(newProduct);
		
//		Optional.ofNullable(a).

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}
	
	@Transactional
	public Map<String, String> save(Map<String, String> datas) {
		int id = NumberUtils.toInt(datas.get("id"));
		Map<String, String> rtn = Maps.newHashMap();
		/*
		 * if (id == 0) { rtn.put("result", ResultCode.FAIL_NOT_EXISTS.get()); return
		 * rtn; }
		 */
		Optional<Bundle> oBundle = bundleRepository.findById(id);
		Bundle bundle = oBundle.orElse(new Bundle());

		Optional<Product> oProduct = bundle.getProduct().stream().findFirst();
		Product product = oProduct.orElse(new Product());

		if (datas.containsKey("name")) {
			String name = datas.get("name");
			product.setName(name);
			bundle.setName(name);
		}
		if (datas.containsKey("dept")) {
			product.setDept(datas.get("dept"));
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
			product.setType(datas.get("type"));
		}
		
		bundle.setSingle(true);
		
		rtn.put("result", ResultCode.SUCCESS.get());

		if (datas.containsKey("active")) {
			boolean inUse = BooleanUtils.toBooleanObject(datas.get("active"));
			product.setActive(inUse);

			rtn.put("result", ResultCode.SUCCESS_NOT_USE_ALERT.get());
		}
		Set<Product> setProduct = bundle.getProduct();
		setProduct.add(product);
		bundle.setProduct(setProduct);

		bundleRepository.save(bundle);
		productRepository.save(product);

		return rtn;
	}
	
	@Transactional
	public Map<String, String> addSampleItem(Map<String, String> datas) {
		int id = NumberUtils.toInt(datas.get("id"));
		Map<String, String> rtn = Maps.newHashMap();
		if (id == 0) {
			rtn.put("result", ResultCode.FAIL_NOT_EXISTS.get());
			return rtn;
		}
		Optional<Product> oProduct = productRepository.findById(id);

		Product product = oProduct.orElse(new Product());
		if (datas.containsKey("sampleItems") && datas.containsKey("isAdd")) {
			String sampleItems = datas.get("sampleItems");
			boolean isAdd = BooleanUtils.toBooleanObject(datas.get("isAdd"));
			if (!sampleItems.isEmpty()) {
				
				Set<SampleItem> sampleItem = product.getSampleItem();
				
				Stream.of(sampleItems.split("\\|")).forEach(p -> {
					Optional<SampleItem> si = sampleItemRepository.findById(NumberUtils.toInt(p));
					si.ifPresent(s -> {
						if (isAdd) {
							sampleItem.add(s);
						} else {
							sampleItem.remove(s);
						}
					});
				});
				product.setSampleItem(sampleItem);
			}
		}
		
		productRepository.save(product);
		
		rtn.put("result", ResultCode.SUCCESS_NOT_USE_ALERT.get());
		return rtn;
	}
	
}
