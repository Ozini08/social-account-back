package com.social_account_back.domain.entity;

import lombok.*;

@Getter
@ToString
@RequiredArgsConstructor
public class Member {
    private Integer memberNo;
    private Long kakaoId;
    private String nickname;
    private String profileImageUrl;
    private String userGroup;
    private String auth;
}
