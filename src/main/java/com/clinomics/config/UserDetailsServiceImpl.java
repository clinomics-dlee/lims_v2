package com.clinomics.config;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Role;
import com.clinomics.service.setting.MemberService;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	@Autowired
	MemberService memberService;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Optional<Member> oMember = memberService.selectOne(username);
		
		Member member = oMember.orElseThrow(() -> new UsernameNotFoundException(username));

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        	
    	Set<Role> roles = member.getRole();
    	roles.stream().forEach(r -> {
            grantedAuthorities.add(new SimpleGrantedAuthority(r.getCode()));
    	});
        
        return new User(member.getId(), member.getPassword(), grantedAuthorities);
	}

}
