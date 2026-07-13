package com.kdt.shoppingmall.security;

import com.kdt.shoppingmall.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MemberUserDetailsService implements UserDetailsService {

  private final MemberRepository memberRepository;

  public MemberUserDetailsService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return memberRepository
        .findByEmail(email)
        .map(MemberPrincipal::new)
        .orElseThrow(() -> new UsernameNotFoundException("가입되지 않은 이메일입니다: " + email));
  }
}
