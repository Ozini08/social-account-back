package com.social_account_back.domain.dao.mapper;

import com.social_account_back.domain.dto.MemberAddDto;
import com.social_account_back.domain.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberMapper {
    Member findByKakaoId(
            @Param("kakaoId") Long kakaoId
    );

    void insertMember(MemberAddDto member);
}
