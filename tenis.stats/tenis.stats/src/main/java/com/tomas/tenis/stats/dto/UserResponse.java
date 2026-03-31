package com.tomas.tenis.stats.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserResponse {
    private List<User> results;

    @Data
    public static class User {
        private String gender;
        private Name name;
        private String email;
    }

    @Data
    public static class Name {
        private String first;
        private String last;
    }
}