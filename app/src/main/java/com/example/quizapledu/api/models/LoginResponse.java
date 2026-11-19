package com.example.quizapledu.api.models;

public class LoginResponse {
    public boolean success;
    public String token;   // JWT token returnat de backend
    public User user;

    public static class User {
        public int id;
        public String email;
        public String name;
        public String role;
    }
}