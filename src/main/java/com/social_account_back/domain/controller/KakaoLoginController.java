package com.social_account_back.domain.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social_account_back.domain.dto.KakaoTokenRequest;
import com.social_account_back.domain.dto.MemberAddDto;
import com.social_account_back.domain.dto.MemberRegisterRequest;
import com.social_account_back.domain.entity.Member;
import com.social_account_back.domain.service.MemberService;
import com.social_account_back.global.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/kakao")
@RequiredArgsConstructor
public class KakaoLoginController {

    private final MemberService memberService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> kakaoLogin(@RequestBody KakaoTokenRequest request, HttpSession session) {
        String accessToken = request.getAccessToken();

        String url = "https://kapi.kakao.com/v2/user/me";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String userInfo = response.getBody();

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(userInfo);

            Long kakaoId = jsonNode.get("id").asLong();
            String nickname = jsonNode.get("properties").get("nickname").asText();
            String profileImageUrl = jsonNode.get("properties").get("profile_image").asText();

            Member member = memberService.findByKakaoId(kakaoId);
            if (member != null) {
                String jwtAccessToken = jwtUtil.generateAccessToken(kakaoId, nickname, member.getAuth());
                String jwtRefreshToken = jwtUtil.generateRefreshToken(kakaoId);
                session.setAttribute("user", member);

                ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", jwtAccessToken)
                        .httpOnly(true)
                        .secure(false)  // 운영 시 true
                        .path("/")
                        .maxAge(jwtUtil.getAccessTokenExpireMs() / 1000)  // 초 단위
                        .sameSite("Lax")
                        .build();

                Map<String, Object> responseBody = Map.of(
                        "message", "로그인 성공",
                        "refreshToken", jwtRefreshToken,
                        "member", Map.of(
                                "nickname", nickname,
                                "profileImageUrl", profileImageUrl,
                                "userGroup", member.getUserGroup(),
                                "auth", member.getAuth()
                        )
                );

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                        .body(responseBody);

            } else {
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

    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        Long kakaoId = request.get("kakaoId") != null ? Long.parseLong(request.get("kakaoId")) : null;

        if (kakaoId == null || refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "잘못된 요청입니다."));
        }

        // 리프레시 토큰 유효성 체크
        if (jwtUtil.isRefreshTokenValid(kakaoId, refreshToken)) {
            Member member = memberService.findByKakaoId(kakaoId);
            if (member == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "회원 정보가 없습니다."));
            }

            // 새 Access Token, 새 Refresh Token 발급
            String newAccessToken = jwtUtil.generateAccessToken(kakaoId, member.getNickname(), member.getAuth());
            String newRefreshToken = jwtUtil.generateRefreshToken(kakaoId);  // Redis 덮어쓰기

            ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", newAccessToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(jwtUtil.getAccessTokenExpireMs() / 1000)
                    .sameSite("Lax")
                    .build();

            Map<String, Object> responseBody = Map.of(
                    "message", "토큰 재발급 성공",
                    "refreshToken", newRefreshToken
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                    .body(responseBody);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "리프레시 토큰이 유효하지 않습니다."));
        }
    }



    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(HttpSession session) {
        Member member = (Member) session.getAttribute("user");

        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("nickname", member.getNickname());
        userInfo.put("profileImageUrl", member.getProfileImageUrl());
        userInfo.put("userGroup", member.getUserGroup());
        userInfo.put("auth", member.getAuth());

        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();

        ResponseCookie clearAccessToken = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false) // 운영 시 true
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        String accessToken = jwtUtil.extractAccessTokenFromCookie(request);
        if (accessToken != null && jwtUtil.validateToken(accessToken)) {
            Long kakaoId = jwtUtil.getUserIdFromToken(accessToken);
            jwtUtil.deleteRefreshToken(kakaoId); // Redis에서 제거
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessToken.toString())
                .body(Map.of("message", "로그아웃 완료"));
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
