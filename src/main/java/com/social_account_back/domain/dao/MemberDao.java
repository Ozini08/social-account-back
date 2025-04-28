package com.social_account_back.domain.dao;

import com.social_account_back.domain.dao.mapper.MemberMapper;
import com.social_account_back.domain.dto.MemberAddDto;
import com.social_account_back.domain.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberDao {
    private final MemberMapper memberMapper;

    public Member findByKakaoId(Long kakaoId) {
        return memberMapper.findByKakaoId(kakaoId);
    }

    public void insertMember(MemberAddDto member) {
        memberMapper.insertMember(member);
    }
}
