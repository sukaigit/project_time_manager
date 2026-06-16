package com.timemanager.dto;

public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String name;
    private String role;
    private Boolean firstLogin;

    public LoginResponse() {}

    public LoginResponse(String token, Long userId, String username, String name, String role, Boolean firstLogin) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.role = role;
        this.firstLogin = firstLogin;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getFirstLogin() { return firstLogin; }
    public void setFirstLogin(Boolean firstLogin) { this.firstLogin = firstLogin; }
}
