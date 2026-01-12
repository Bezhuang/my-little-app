package com.bezhuang.my_little_app_backend.config.security;

import com.bezhuang.my_little_app_backend.config.jwt.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Security 配置类 - JWT 认证
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 密码编码器 - 使用 MD5
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new MD5PasswordEncoder();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 安全过滤链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（前后端分离项目）
            .csrf(AbstractHttpConfigurer::disable)

            // 启用 CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 会话管理 -  Stateless，不创建 Session
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // 请求授权配置
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/thoughts/**").permitAll()
                .requestMatchers("/error").permitAll()

                // 公开图片数据接口（无需认证，用于前端显示）
                .requestMatchers("/api/admin/images/public/**").permitAll()

                // SiliconFlow AI 接口无需登录（默认模型）
                .requestMatchers("/api/ai/siliconflow/**").permitAll()

                // Admin 接口需要 ADMIN 角色
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // User 接口需要登录
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")

                // AI 接口需要登录（DeepSeek 等）
                .requestMatchers("/api/ai/**").hasAnyRole("USER", "ADMIN")

                // 其他请求需要认证
                .anyRequest().authenticated()
            )

            // 设置 UserDetailsService
            .userDetailsService(customUserDetailsService)

            // 异常处理
            .exceptionHandling(ex -> ex
                // 未认证处理
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    PrintWriter writer = response.getWriter();

                    Map<String, Object> result = new HashMap<>();
                    result.put("code", 401);
                    result.put("message", "请先登录");
                    result.put("success", false);

                    writer.write(new ObjectMapper().writeValueAsString(result));
                    writer.flush();
                })
                // 权限不足处理
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    PrintWriter writer = response.getWriter();

                    Map<String, Object> result = new HashMap<>();
                    result.put("code", 403);
                    result.put("message", "权限不足");
                    result.put("success", false);

                    writer.write(new ObjectMapper().writeValueAsString(result));
                    writer.flush();
                })
            );

        return http.build();
    }

    /**
     * CORS 配置 - 允许跨域请求
     * 从配置文件 cors.allowed-origins 读取允许的域名
     * 支持通配符 * 表示任意端口
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 从配置文件读取允许的域名
        boolean allowAll = false;
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            for (String origin : origins) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    if (trimmed.equals("*")) {
                        allowAll = true;
                        configuration.addAllowedOriginPattern("*");
                    } else if (trimmed.endsWith(":*")) {
                        // 支持通配符模式（如 http://localhost:*）
                        configuration.addAllowedOriginPattern(trimmed);
                    } else {
                        configuration.addAllowedOrigin(trimmed);
                    }
                }
            }
        } else {
            // 默认允许所有来源
            configuration.addAllowedOriginPattern("*");
            allowAll = true;
        }

        // 允许携带 Cookie（但 * 通配符时不能设置）
        if (!allowAll) {
            configuration.setAllowCredentials(true);
        }
        // 允许所有请求头
        configuration.addAllowedHeader("*");
        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // 暴露的响应头（允许前端访问）
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        // 预检请求缓存时间
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
