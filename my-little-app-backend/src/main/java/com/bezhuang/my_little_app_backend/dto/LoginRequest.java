package com.bezhuang.my_little_app_backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 登录请求 DTO
 */
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码（明文）
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 登录类型：admin-管理员，user-普通用户
     */
    private String loginType;

    public LoginRequest() {
    }

    public LoginRequest(String username, String password, String loginType) {
        this.username = username;
        this.password = password;
        this.loginType = loginType;
    }

    // ==================== Getter 和 Setter ====================

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                ", loginType='" + loginType + '\'' +
                '}';
    }
}







