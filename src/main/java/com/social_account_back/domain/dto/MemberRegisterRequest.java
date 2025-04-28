package com.social_account_back.domain.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class MemberRegisterRequest {

    private Long kakaoId;
    private String nickname;
    private String profileImageUrl;  // 프로필 이미지는 선택사항
    private String birthDate; // 예: "1990-05-25" 형태로 받기
    private Integer genderCode; // 보통 1~4 같은 숫자
    private String group; // 목장 (선택)
    private String teamMinistry1; // 팀사역1 (선택)
    private String teamMinistry2; // 팀사역2 (선택)
    private String teamMinistry3; // 팀사역3 (선택)
}
