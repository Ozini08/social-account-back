package com.social_account_back.domain.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social_account_back.domain.dto.KakaoTokenRequest;
import com.social_account_back.domain.dto.MemberAddDto;
import com.social_account_back.domain.dto.MemberRegisterRequest;
import com.social_account_back.domain.entity.Member;
import com.social_account_back.domain.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController
@RequestMapping("/api/kakao")
@RequiredArgsConstructor
public class KakaoLoginController {

    private final MemberService memberService;

    @PostMapping("/login")
    public ResponseEntity<?> kakaoLogin(@RequestBody KakaoTokenRequest request) {
        String accessToken = request.getAccessToken();

        String url = "https://kapi.kakao.com/v2/user/me";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String userInfo = response.getBody();

            // userInfo 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(userInfo);

            Long kakaoId = jsonNode.get("id").asLong();
            String nickname = jsonNode.get("properties").get("nickname").asText();
            String profileImageUrl = jsonNode.get("properties").get("profile_image").asText();

            Member member = memberService.findByKakaoId(kakaoId);

            if (member != null) {
                // 로그인 성공, 회원 정보 반환
                Map<String, Object> responseData = Map.of(
                        "message", "로그인 성공",
                        "member", Map.of(
//                                "kakaoId", kakaoId,
                                "nickname", nickname,
                                "profileImageUrl", profileImageUrl,
                                "userGroup", member.getUserGroup()
                        )
                );
                return ResponseEntity.ok(responseData);
            } else {
                // 회원 가입 필요 메시지와 함께 회원 정보 반환
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "회원 가입 후 로그인 필요"));
            }

        } catch (RestClientException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "카카오 API 호출 실패"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "사용자 정보 파싱 실패"));
        }
    }

    // 회원가입 API 추가
    @PostMapping("/register")
    public ResponseEntity<?> registerMember(@RequestBody MemberRegisterRequest request) {
        MemberAddDto newMember = new MemberAddDto();
        newMember.setKakaoId(request.getKakaoId());
        newMember.setNickname(request.getNickname());
        newMember.setBirthDate(request.getBirthDate());
        newMember.setGenderCode(request.getGenderCode());
        newMember.setGroup(request.getGroup());
        newMember.setTeamMinistry1(request.getTeamMinistry1());
        newMember.setTeamMinistry2(request.getTeamMinistry2());
        newMember.setTeamMinistry3(request.getTeamMinistry3());
        memberService.registerMember(newMember);

        return ResponseEntity.ok(Map.of("message", "회원가입 성공"));
    }

}
