package com.clinomics.controller.setting;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.clinomics.service.setting.MemberService;

@RequestMapping("/set/mbr")
@Controller
public class MemberController {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	MemberService memberService;
	
	@GetMapping("/get")
	@ResponseBody
	public Map<String, Object> get(@RequestParam Map<String, String> params) {
		return memberService.selectAll(params);
	}

	@PostMapping("/add")
	@ResponseBody
	public Map<String, String> add(@RequestBody Map<String, String> datas) {
		System.out.println(datas.toString());
		return memberService.insert(datas);
	}

	@PostMapping("/save")
	@ResponseBody
	public Map<String, String> save(@RequestBody Map<String, String> datas) {
		System.out.println(datas.toString());
		return memberService.save(datas);
	}
	
	@GetMapping("/role/get")
	@ResponseBody
	public Map<String, Object> getRoles(@RequestParam Map<String, String> params) {
		return memberService.selectAllRoles(params);
	}
	
	@PostMapping("/role/change")
	@ResponseBody
	public Map<String, String> changeRole(@RequestBody Map<String, String> datas) {
		System.out.println(datas.toString());
		return memberService.changeRole(datas);
	}

	@PostMapping("/password/change")
	@ResponseBody
	public Map<String, String> changePassword(@RequestBody Map<String, String> datas) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String memberId = userDetails.getUsername();
		datas.put("id", memberId);
		System.out.println(datas.toString());
		return memberService.save(datas);
	}
}
