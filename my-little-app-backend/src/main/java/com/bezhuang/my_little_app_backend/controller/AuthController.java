package com.bezhuang.my_little_app_backend.controller;

import com.bezhuang.my_little_app_backend.config.jwt.JwtUtil;
import com.bezhuang.my_little_app_backend.config.security.CustomUserDetails;
import com.bezhuang.my_little_app_backend.dto.LoginRequest;
import com.bezhuang.my_little_app_backend.dto.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 - JWT 登录
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthController(JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    /**
     * JWT 登录接口
     */
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public void login(@RequestBody LoginRequest loginRequest,
                      HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        try {
            // 认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取用户详情
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // 生成 JWT
            String token = jwtUtil.generateToken(
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getRole()
            );

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "登录成功");
            result.put("success", true);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("id", userDetails.getId());
            data.put("username", userDetails.getUsername());
            data.put("email", userDetails.getEmail());
            data.put("phone", userDetails.getPhone());
            data.put("avatar", userDetails.getAvatar());
            data.put("userType", userDetails.getUserType());
            data.put("role", userDetails.getRole());
            result.put("data", data);

            PrintWriter writer = response.getWriter();
            writer.write(new ObjectMapper().writeValueAsString(result));
            writer.flush();

        } catch (AuthenticationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "用户名或密码错误");
            result.put("success", false);

            PrintWriter writer = response.getWriter();
            writer.write(new ObjectMapper().writeValueAsString(result));
            writer.flush();
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getPrincipal())) {
            return Result.unauthorized("未登录");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Map<String, Object> data = new HashMap<>();
        data.put("id", userDetails.getId());
        data.put("username", userDetails.getUsername());
        data.put("email", userDetails.getEmail());
        data.put("phone", userDetails.getPhone());
        data.put("avatar", userDetails.getAvatar());
        data.put("userType", userDetails.getUserType());
        data.put("role", userDetails.getRole());
        data.put("status", userDetails.getStatus());

        return Result.success(data);
    }

    /**
     * 检查登录状态
     */
    @GetMapping("/check")
    public Result<Boolean> checkLogin(Authentication authentication) {
        boolean isLoggedIn = authentication != null
            && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getPrincipal());

        return Result.success(isLoggedIn);
    }
}


