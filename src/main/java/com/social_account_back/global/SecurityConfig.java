package com.social_account_back.global;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 보호 끔
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/**").permitAll() // 카카오 로그인은 인증 없이 허용
                        .anyRequest().authenticated() // 그 외 나머지는 인증 필요
                )
                .formLogin(form -> form.disable()) // 기본 로그인 폼 끔
                .httpBasic(httpBasic -> httpBasic.disable()); // 기본 인증 끔

        return http.build();
    }
}
