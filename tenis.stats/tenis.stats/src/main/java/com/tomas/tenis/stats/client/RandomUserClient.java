package com.tomas.tenis.stats.client;

import com.tomas.tenis.stats.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

// Esta anotación le dice a Spring: "Conectate a esta URL"
@FeignClient(name = "randomUserClient", url = "https://randomuser.me/api/")
public interface RandomUserClient {

    @GetMapping("/")
    UserResponse getRandomUser();
}