package com.clinomics.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.clinomics.entity.lims.Member;
import com.clinomics.service.setting.MemberService;

@Controller
public class LoginController {

	@Autowired
	MemberService userService;
	
	@GetMapping("/login")
	public String login(Model model, String error, String logout) {
		if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() != "anonymousUser") {
			
			// 인증 사용자
			return "redirect:/";
		}

        if (error != null) {
            model.addAttribute("error", "Your username and password is invalid.");
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }
        
		return "login";
	}
	
	@GetMapping("/registration")
	public String getUsers(Model model) {
		model.addAttribute("Member", new Member()); 
		return "registration";
	}
	
	@PostMapping("/registration")
	public String postUsers(Member member) {
		userService.insert(member);
		
		return "redirect:/login";
	}
}
