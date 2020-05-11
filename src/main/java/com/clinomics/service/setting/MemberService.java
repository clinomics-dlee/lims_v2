package com.clinomics.service.setting;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Role;
import com.clinomics.enums.ResultCode;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.RoleRepository;
import com.clinomics.service.DataTableService;
import com.google.common.collect.Maps;

@Service
public class MemberService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	MemberRepository memberRepository;
	
	@Autowired
	RoleRepository roleRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	DataTableService dataTableService;
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Transactional
	public void insert(Member users) {
		Member newUsers = new Member();
		newUsers = users;
		newUsers.setPassword(passwordEncoder.encode(newUsers.getPassword()));
		memberRepository.save(newUsers);
		//entityManager.persist(newUsers);
	}
	
	public Optional<Member> selectOne(String memberId) {
		return memberRepository.findById(memberId);
	}
	
	/**
	 * paging 조회
	 * @param paramMap
	 * @return
	 */
	public Map<String, Object> selectAll(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 1);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);
		// #. 검색어
		String searchValue = StringUtils.trimToEmpty(String.valueOf(params.get("srchVal")));
		// #. count 조회
		long total = memberRepository.countBySearchValue(searchValue);
		long filtered = total;
		
		// #. 소팅 처리
		List<Order> orders = Arrays.asList(new Order[] {
			Order.asc("name")
		});
		// #. paging 관련 객체
		Pageable pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		// #. 조회
		List<Member> list = memberRepository.findBySearchValue(searchValue, pageable);
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list);
	}
	
	/**
	 * 등록
	 * @param datas
	 * @return
	 */
	@Transactional
	public Map<String, String> insert(Map<String, String> datas) {
		Map<String, String> rtn = Maps.newHashMap();
		String result = "";
		String id = datas.get("id");
		
		// #. id로 멤버 조회. 중복체크
		Optional<Member> existMember = memberRepository.findById(id);
		
		// #. id로 조회시 member가 없는 경우 생성
		if (existMember.isPresent()) {
			result = "99";
		} else {
			// #. id로 조회시 member가 없는 경우 생성
			Member newMember = new Member();
			newMember.setId(datas.get("id"));
			newMember.setPassword(passwordEncoder.encode(datas.get("password")));
			newMember.setName(datas.get("name"));
			newMember.setEmail(datas.get("email"));
			newMember.setDept(datas.get("dept"));
			
			// #. 생성
			memberRepository.save(newMember);
			
			// #. 결과 셋팅
			result = "00";
		}
		
		rtn.put("result", result);
		
		return rtn;
	}
	
	/**
	 * 수정
	 * @param datas
	 * @return
	 */
	@Transactional
	public Map<String, String> save(Map<String, String> datas) {
		Map<String, String> rtn = Maps.newHashMap();
		String id = datas.get("id");
		
		// #. id로 멤버 조회. 중복체크
		Optional<Member> oMember = memberRepository.findById(id);
		
		// #. id로 조회시 member가 있는 경우 수정
		if (oMember.isPresent()) {
			Member newMember = new Member();
			Member member = oMember.orElse(newMember);
			// #. password는 값이 넘어오면 변경
			if (datas.containsKey("password") && datas.get("password").length() > 0) {
				member.setPassword(passwordEncoder.encode(datas.get("password")));
			}
			
			if (datas.containsKey("name")) {
				member.setName(datas.get("name"));
			}
			if (datas.containsKey("email")) {
				member.setEmail(datas.get("email"));
			}
			if (datas.containsKey("dept")) {
				member.setDept(datas.get("dept"));
			}
			// #. 결과 셋팅
			rtn.put("result", ResultCode.SUCCESS.get());
						
			if (datas.containsKey("inUse")) {
				boolean inUse = BooleanUtils.toBooleanObject(datas.get("inUse"));
				member.setInUse(inUse);
				// #. 사용여부 저장
				rtn.put("result", ResultCode.SUCCESS_NOT_USE_ALERT.get());
			}
			
			// #. 수정
			memberRepository.save(member);
		} else {
			// #. member가 없는 경우
			rtn.put("result", ResultCode.FAIL_NOT_EXISTS.get());
		}
		
		return rtn;
	}
	
	/**
	 * Role 전체조회
	 * @param paramMap
	 * @return
	 */
	public Map<String, Object> selectAllRoles(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 1);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);
		
		// #. 검색어
		String searchValue = StringUtils.trimToEmpty(params.get("srchVal"));
		String memberId = StringUtils.trimToEmpty(params.get("mbrId"));
		
		// #. count 조회
		long total = roleRepository.countBySearchValue(searchValue);
		long filtered = total;
		
		// #. 소팅 처리
		List<Order> orders = Arrays.asList(new Order[] {
			Order.asc("name")
		});
		// #. paging 관련 객체
		Pageable pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		// #. 조회
		List<Role> list = roleRepository.findBySearchValue(searchValue, pageable);
		
		Map<String, Object> rtn = Maps.newHashMap();
		
		// #. memberId를 가지고 member 조회
		Optional<Member> sMember = memberRepository.findById(memberId);
		// #. id로 조회시 member가 있는 경우
		if (sMember.isPresent()) {
			Member newMember = new Member();
			Member member = sMember.orElse(newMember);
			Set<Role> roleSet =  member.getRole();
			rtn.put("existRoles", roleSet);
		}
		
		rtn.put("draw", draw);
		rtn.put("pageNumber", pageNumber);
		rtn.put("recordsTotal", total);
		rtn.put("recordsFiltered", filtered);
		rtn.put("data", list);
		
		return rtn;
	}
	
	/**
	 * member role 변경
	 * @param datas
	 * @return
	 */
	@Transactional
	public Map<String, String> changeRole(Map<String, String> datas) {
		Map<String, String> rtn = Maps.newHashMap();
		int id = NumberUtils.toInt(datas.get("id"), 0);
		String memberId = datas.get("mbrId");
		
		// #. id로 role 조회.
		Optional<Role> oRole = roleRepository.findById(id);
		// #. id로 멤버 조회.
		Optional<Member> oMember = memberRepository.findById(memberId);
		logger.info(">> datas=" + datas.toString());
		// #. id로 조회시 member가 있는 경우 수정
		if (oMember.isPresent() && oRole.isPresent()) {
			Member member = oMember.orElse(new Member());
			Role role = oRole.orElse(new Role());
			// #. 추가여부. 아닌경우는 삭제
			boolean isAdd = BooleanUtils.toBooleanObject(datas.get("isAdd"));
			// #. 현재 member에 role목록을 조회해서 변경하려는 role과 동일한 role값을 추가 또는 삭제
			Set<Role> memberRoles = member.getRole();
			// #. 수정. save하지 않아도 자동 반영
			if (isAdd) {
				memberRoles.add(role);
			} else {
				memberRoles.remove(role);
			}
			
			rtn.put("result", ResultCode.SUCCESS_NOT_USE_ALERT.get());
		} else {
			// #. member가 없는 경우
			rtn.put("result", ResultCode.FAIL_NOT_EXISTS.get());
		}
		
		return rtn;
	}
}
