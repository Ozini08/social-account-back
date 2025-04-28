package com.social_account_back.domain.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class testController {

    @GetMapping("/list")
    public List<HashMap<String, String>> testControl() {
        List<HashMap<String, String>> list = new ArrayList<>();

        HashMap<String, String> map1 = new HashMap<>();
        map1.put("id", "1");
        map1.put("name", "홍길동");

        HashMap<String, String> map2 = new HashMap<>();
        map2.put("id", "2");
        map2.put("name", "이몽룡");

        list.add(map1);
        list.add(map2);

        return list;
    }

}
