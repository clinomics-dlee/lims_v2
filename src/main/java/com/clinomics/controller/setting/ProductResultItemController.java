package com.clinomics.controller.setting;

import com.clinomics.service.setting.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/set/pritem")
public class ProductResultItemController {
	
	@Autowired
	ProductService productService;

	

}
