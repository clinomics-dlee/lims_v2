
package com.clinomics.service;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.clinomics.entity.lims.Role;
import com.clinomics.repository.lims.RoleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class RoleService {
    
	@Autowired
	RoleRepository roleRepository;

	public boolean checkPersonalView() {
		String userRoles = getRoles();
		List<Role> roles = roleRepository.findByIsPersonalViewTrue();

		for (Role r : roles) {
			if (userRoles.contains(r.getCode())) {
				return true;
			}
		}
		return false;
	}

	private String getRoles() {
		// 시큐리티 컨텍스트 객체를 얻습니다.
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		// 사용자가 가진 모든 롤 정보를 얻습니다.
		Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
		Iterator<? extends GrantedAuthority> iter = authorities.iterator();
		String userRoles = "";
		while (iter.hasNext()) {
			GrantedAuthority auth = iter.next();
			userRoles += "," + auth.getAuthority();
		}
		return userRoles;
	}
}