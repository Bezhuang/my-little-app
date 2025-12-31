package com.bezhuang.my_little_app_backend.config.security;

import com.bezhuang.my_little_app_backend.util.MD5Util;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * MD5 密码编码器
 * 实现 Spring Security 的 PasswordEncoder 接口
 */
public class MD5PasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return MD5Util.md5(rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return MD5Util.verifyPassword(rawPassword.toString(), encodedPassword);
    }
}





