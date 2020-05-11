package com.clinomics.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.clinomics.entity.lims.Sample;
import com.clinomics.entity.lims.SampleHistory;
import com.clinomics.entity.lims.SampleItem;
import com.clinomics.enums.ResultCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.ProductRepository;
import com.clinomics.repository.lims.SampleItemRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.google.common.collect.Maps;

@Service
public class SampleItemService {

	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	SampleItemRepository sampleItemRepository;

	@Autowired
	BundleRepository bundleRepository;

	@Autowired
	ProductRepository productRepository;
	
	@Autowired
	DataTableService dataTableService;
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public Optional<SampleItem> selectOne(int id) {
		return sampleItemRepository.findById(id);
	}
	
	public Map<String, Object> findSampleItemBySample(String id) {
		Optional<Sample> oSample = sampleRepository.findById(id);
		Sample sample = oSample.orElse(new Sample());
		
		Optional<Bundle> oBundle = Optional.of(sample.getBundle());
		Bundle bundle = oBundle.orElse(new Bundle());
		Set<SampleItem> sampleItems = new HashSet<SampleItem>();
		bundle.getProduct().stream().forEach(p -> {
			
			Optional<Product> oProduct = productRepository.findById(p.getId());
			Product product = oProduct.orElse(new Product());
			
			sampleItems.addAll(product.getSampleItem());
			
		});
		
		List<SampleItem> sortedSampleItems = sampleItems.stream()
				.sorted(Comparator.comparing(SampleItem::getOrd))
				.collect(Collectors.toList());
		
		Map<String, Object> rtn = Maps.newHashMap();
		rtn.put("sampleItem", sortedSampleItems);
		rtn.put("sample", sample);
		
		return rtn;
	}

	public List<Map<String, Object>> filterItemsAndOrdering(List<Sample> list, boolean all) {
		
		Set<SampleItem> sampleItems = new HashSet<SampleItem>();
		List<Map<String, Object>> header = new ArrayList<>();
		list.stream().forEach(s -> {
			
			s.getBundle().getProduct().stream().forEach(p -> {
				sampleItems.addAll(p.getSampleItem());
			});
		});

		List<SampleItem> filteredSampleItems = sampleItems.stream()
				.filter(fs -> fs.isVisible() || all)
				.sorted(Comparator.comparing(SampleItem::getOrd))
				.collect(Collectors.toList());

		filteredSampleItems.forEach(f -> {
			Map<String, Object> tt = Maps.newLinkedHashMap();
			tt.put("title", f.getName());
			tt.put("data", "items." + f.getNameCode());
			if ("date".equals(f.getType())) {
				tt.put("type", "date");
			}
			header.add(tt);
		});
		
		return header;
	}

	public List<Map<String, Object>> filterItemsAndOrderingForMap(List<Map<String, Object>> list, boolean all) {
		
		Set<SampleItem> sampleItems = new HashSet<SampleItem>();
		List<Map<String, Object>> header = new ArrayList<>();
		list.stream().forEach(s -> {
			Sample sample = (Sample)s.get("sample");
			sample.getBundle().getProduct().stream().forEach(p -> {
				sampleItems.addAll(p.getSampleItem());
			});
		});

		List<SampleItem> filteredSampleItems = sampleItems.stream()
				.filter(fs -> fs.isVisible() || all)
				.sorted(Comparator.comparing(SampleItem::getOrd))
				.collect(Collectors.toList());

		filteredSampleItems.forEach(f -> {
			Map<String, Object> tt = Maps.newLinkedHashMap();
			tt.put("title", f.getName());
			tt.put("data", "sample.items." + f.getNameCode());
			if ("date".equals(f.getType())) {
				tt.put("type", "date");
			}
			header.add(tt);
		});
		
		return header;
	}
	
	public List<Map<String, Object>> filterItemsAndOrderingForResult(List<Sample> list, boolean all) {
		
		Set<SampleItem> sampleItems = new HashSet<SampleItem>();
		List<Map<String, Object>> header = new ArrayList<>();
		list.stream().forEach(s -> {
			
			s.getBundle().getProduct().stream().forEach(p -> {
				sampleItems.addAll(p.getSampleItem());
			});
		});

		List<SampleItem> filteredSampleItems = sampleItems.stream()
				.filter(fs -> fs.isVisible() || all)
				.sorted(Comparator.comparing(SampleItem::getOrd))
				.collect(Collectors.toList());

		filteredSampleItems.forEach(f -> {
			Map<String, Object> tt = Maps.newLinkedHashMap();
			tt.put("title", f.getName());
			tt.put("data", "sample.items." + f.getNameCode());
			if ("date".equals(f.getType())) {
				tt.put("type", "date");
			}
			header.add(tt);
		});
		
		return header;
	}
	
	public void filterItemsAndOrdering(List<SampleHistory> list) {
		
		list.stream().forEach(s -> {
			Map<String, Object> t = Maps.newLinkedHashMap();
			
			Set<SampleItem> sampleItems = new HashSet<SampleItem>();
			s.getSample().getBundle().getProduct().stream().forEach(p -> {
				sampleItems.addAll(p.getSampleItem());
			});
			
			List<SampleItem> filteredSampleItems = sampleItems.stream()
					.sorted(Comparator.comparing(SampleItem::getOrd))
					.collect(Collectors.toList());
			
			filteredSampleItems.forEach(f -> {
				Map<String, Object> tf = s.getItems();
				String key = f.getNameCode();
				if (tf.containsKey(key)) {
					t.put(f.getName(), tf.get(key));
				}
				
			});
			s.setItems(t);
		});
	}
	
	public Map<String, Object> findSampleItemByBundle(String id) {
		Map<String, Object> rtn = Maps.newHashMap();
		
		Optional<Bundle> oBundle = bundleRepository.findById(NumberUtils.toInt(id));
		Bundle Bundle = oBundle.orElse(new Bundle());
		Set<SampleItem> sampleItems = new HashSet<SampleItem>();
		Bundle.getProduct().stream().forEach(p -> {
			
			Optional<Product> oProduct = productRepository.findById(p.getId());
			Product product = oProduct.orElse(new Product());
			
			sampleItems.addAll(product.getSampleItem());
			
		});
		
		List<SampleItem> sortedSampleItems = sampleItems.stream()
				.sorted(Comparator.comparing(SampleItem::getOrd))
				.collect(Collectors.toList());
		
		rtn.put("bundle", Bundle);
		rtn.put("sampleItem", sortedSampleItems);
		
		return rtn;
	}
	
	public Map<String, Object> selectAll(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 1);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);
		// #. count 조회
		long total = sampleItemRepository.count();
		long filtered = total;
		
		List<Order> orders = Arrays.asList(new Order[] {
			Order.asc("ord")
		});
		// #. paging 관련 객체
		Pageable pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		
		Page<SampleItem> page = sampleItemRepository.findAll(pageable);
		List<SampleItem> list = page.getContent();
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list);
	}
	
	@Transactional
	public Map<String, String> save(Map<String, String> datas) {
		int id = NumberUtils.toInt(datas.get("id"));
		Map<String, String> rtn = Maps.newHashMap();
		
		Optional<SampleItem> oSampleItem = sampleItemRepository.findById(id);
		SampleItem sampleItem = oSampleItem.orElse(new SampleItem());

		rtn.put("result", ResultCode.SUCCESS.get());
		if (datas.containsKey("active")) {
			boolean isActive = BooleanUtils.toBooleanObject(datas.get("active"));
			sampleItem.setActive(isActive);
			rtn.put("result", ResultCode.SUCCESS_NOT_USE_ALERT.get());
		}
		if (datas.containsKey("visible")) {
			sampleItem.setVisible(BooleanUtils.toBooleanObject(datas.get("visible")));
			rtn.put("result", ResultCode.SUCCESS_NOT_USE_ALERT.get());
		}
		if (datas.containsKey("notNull")) {
			sampleItem.setNotNull(BooleanUtils.toBooleanObject(datas.get("notNull")));
			rtn.put("result", ResultCode.SUCCESS_NOT_USE_ALERT.get());
		}
//		if (datas.containsKey("autoBarcode")) {
//			sampleItem.setAutoBarcode(BooleanUtils.toBooleanObject(datas.get("autoBarcode")));
//			rtn.put("result", ResultCode.SUCCESS_NOT_USE_ALERT.get());
//		}
//		if (datas.containsKey("autoSequence")) {
//			sampleItem.setAutoSequence(BooleanUtils.toBooleanObject(datas.get("autoSequence")));
//			rtn.put("result", ResultCode.SUCCESS_NOT_USE_ALERT.get());
//		}
		if (datas.containsKey("name")) {
			rtn.put("result", ResultCode.SUCCESS.get());
			sampleItem.setName(datas.get("name"));
		}
		if (datas.containsKey("nameCode")) {
			sampleItem.setNameCode(datas.get("nameCode"));
		}
		if (datas.containsKey("exampleValue")) {
			sampleItem.setExampleValue(datas.get("exampleValue"));
		}
		if (datas.containsKey("selectValue")) {
			sampleItem.setSelectValue(datas.get("selectValue"));
		}
		
		if (datas.containsKey("width")) {
			sampleItem.setWidth(datas.get("width"));
		}
		if (datas.containsKey("type")) {
			sampleItem.setType(datas.get("type"));
		}
		if (datas.containsKey("ord")) {
			sampleItem.setOrd(NumberUtils.toInt(datas.get("ord")));
		}
		
		sampleItemRepository.save(sampleItem);
		
		return rtn;
	}
}
