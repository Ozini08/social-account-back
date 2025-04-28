package com.social_account_back.domain.service;

import com.social_account_back.domain.dao.MemberDao;
import com.social_account_back.domain.dto.MemberAddDto;
import com.social_account_back.domain.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberDao memberDao;

    public Member findByKakaoId(Long kakaoId) {
        return memberDao.findByKakaoId(kakaoId);
    }

    public void registerMember(MemberAddDto member) {
        memberDao.insertMember(member);
    }
}
