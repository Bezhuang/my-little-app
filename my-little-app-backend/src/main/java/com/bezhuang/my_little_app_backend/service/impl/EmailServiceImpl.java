package com.bezhuang.my_little_app_backend.service.impl;

import com.bezhuang.my_little_app_backend.entity.Admin;
import com.bezhuang.my_little_app_backend.entity.User;
import com.bezhuang.my_little_app_backend.mapper.AdminMapper;
import com.bezhuang.my_little_app_backend.mapper.UserMapper;
import com.bezhuang.my_little_app_backend.service.EmailService;
import com.bezhuang.my_little_app_backend.util.MD5Util;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮件服务实现类
 */
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final UserMapper userMapper;
    private final AdminMapper adminMapper;

    /**
     * 用户验证码缓存
     * Key: "user:" + email, Value: ResetCodeInfo
     */
    private final Map<String, ResetCodeInfo> userResetCodeCache = new ConcurrentHashMap<>();

    /**
     * 管理员验证码缓存
     * Key: "admin:" + email, Value: ResetCodeInfo
     */
    private final Map<String, ResetCodeInfo> adminResetCodeCache = new ConcurrentHashMap<>();

    /**
     * 注册验证码缓存
     * Key: "register:" + email, Value: ResetCodeInfo
     */
    private final Map<String, ResetCodeInfo> registerCodeCache = new ConcurrentHashMap<>();

    /**
     * 验证码有效期（5分钟）
     */
    private static final long CODE_EXPIRE_TIME = 5 * 60 * 1000;

    /**
     * 发件人邮箱
     */
    private String fromEmail = "1586906111@qq.com";

    public EmailServiceImpl(JavaMailSender mailSender, UserMapper userMapper, AdminMapper adminMapper) {
        this.mailSender = mailSender;
        this.userMapper = userMapper;
        this.adminMapper = adminMapper;
    }

    @Override
    public void sendResetCode(String email) {
        // 生成6位数字验证码
        String code = generateCode();

        // 保存验证码（用户）
        String cacheKey = "user:" + email;
        userResetCodeCache.put(cacheKey, new ResetCodeInfo(code, System.currentTimeMillis()));

        // 发送邮件
        String subject = "【My Little App】密码重置验证码";
        String content = buildResetEmailContent(code);

        try {
            sendEmail(email, subject, content);
        } catch (Exception e) {
            // 如果邮件发送失败，移除缓存的验证码
            userResetCodeCache.remove(cacheKey);
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    /**
     * 发送管理员重置验证码
     */
    public void sendAdminResetCode(String email) {
        // 生成6位数字验证码
        String code = generateCode();

        // 保存验证码（管理员）
        String cacheKey = "admin:" + email;
        adminResetCodeCache.put(cacheKey, new ResetCodeInfo(code, System.currentTimeMillis()));

        // 发送邮件
        String subject = "【My Little App】管理员密码重置验证码";
        String content = buildResetEmailContent(code);

        try {
            sendEmail(email, subject, content);
        } catch (Exception e) {
            // 如果邮件发送失败，移除缓存的验证码
            adminResetCodeCache.remove(cacheKey);
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    @Override
    public boolean verifyResetCode(String email, String code) {
        String cacheKey = "user:" + email;
        ResetCodeInfo info = userResetCodeCache.get(cacheKey);

        if (info == null) {
            return false;
        }

        // 检查是否过期
        if (System.currentTimeMillis() - info.getCreateTime() > CODE_EXPIRE_TIME) {
            userResetCodeCache.remove(cacheKey);
            return false;
        }

        // 验证码匹配
        return info.getCode().equals(code);
    }

    /**
     * 验证管理员重置码
     */
    public boolean verifyAdminResetCode(String email, String code) {
        String cacheKey = "admin:" + email;
        ResetCodeInfo info = adminResetCodeCache.get(cacheKey);

        if (info == null) {
            return false;
        }

        // 检查是否过期
        if (System.currentTimeMillis() - info.getCreateTime() > CODE_EXPIRE_TIME) {
            adminResetCodeCache.remove(cacheKey);
            return false;
        }

        // 验证码匹配
        return info.getCode().equals(code);
    }

    @Override
    public boolean resetPasswordWithCode(String email, String code, String newPassword) {
        // 验证码验证
        if (!verifyResetCode(email, code)) {
            return false;
        }

        // 获取用户
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            return false;
        }

        // 加密新密码并更新
        String encryptedPassword = MD5Util.md5(newPassword);
        int result = userMapper.updatePassword(user.getId(), encryptedPassword);

        if (result > 0) {
            // 重置成功，移除验证码
            userResetCodeCache.remove("user:" + email);
            return true;
        }

        return false;
    }

    /**
     * 通过验证码重置管理员密码
     */
    public boolean resetAdminPasswordWithCode(String email, String code, String newPassword) {
        // 验证码验证
        if (!verifyAdminResetCode(email, code)) {
            return false;
        }

        // 获取管理员
        Admin admin = adminMapper.selectByEmail(email);
        if (admin == null) {
            return false;
        }

        // 加密新密码并更新
        String encryptedPassword = MD5Util.md5(newPassword);
        int result = adminMapper.updatePassword(admin.getId(), encryptedPassword);

        if (result > 0) {
            // 重置成功，移除验证码
            adminResetCodeCache.remove("admin:" + email);
            return true;
        }

        return false;
    }

    @Override
    public void sendRegisterCode(String email) {
        // 检查邮箱是否已被注册
        User existingUser = userMapper.selectByEmail(email);
        if (existingUser != null) {
            throw new RuntimeException("该邮箱已被注册");
        }

        // 生成6位数字验证码
        String code = generateCode();

        // 保存验证码（注册）
        String cacheKey = "register:" + email;
        registerCodeCache.put(cacheKey, new ResetCodeInfo(code, System.currentTimeMillis()));

        // 发送邮件
        String subject = "【My Little App】注册验证码";
        String content = buildRegisterEmailContent(code);

        try {
            sendEmail(email, subject, content);
        } catch (Exception e) {
            // 如果邮件发送失败，移除缓存的验证码
            registerCodeCache.remove(cacheKey);
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    @Override
    public boolean verifyRegisterCode(String email, String code) {
        String cacheKey = "register:" + email;
        ResetCodeInfo info = registerCodeCache.get(cacheKey);

        if (info == null) {
            return false;
        }

        // 检查是否过期
        if (System.currentTimeMillis() - info.getCreateTime() > CODE_EXPIRE_TIME) {
            registerCodeCache.remove(cacheKey);
            return false;
        }

        // 验证码匹配
        return info.getCode().equals(code);
    }

    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
        } catch (Exception e) {
            // 开发环境下，如果邮件发送失败，只打印日志
            System.out.println("========================================");
            System.out.println("邮件发送模拟（开发环境）");
            System.out.println("收件人: " + to);
            System.out.println("主题: " + subject);
            System.out.println("内容: \n" + content);
            System.out.println("========================================");
        }
    }

    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 构建重置密码邮件内容
     */
    private String buildResetEmailContent(String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("您好！\n\n");
        sb.append("您正在重置 My Little App 账号密码。\n\n");
        sb.append("您的验证码是：").append(code).append("\n\n");
        sb.append("验证码有效期为5分钟，请尽快使用。\n\n");
        sb.append("如果这不是您的操作，请忽略此邮件。\n\n");
        sb.append("---\n");
        sb.append("My Little App 团队");
        return sb.toString();
    }

    /**
     * 构建注册验证码邮件内容
     */
    private String buildRegisterEmailContent(String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("您好！\n\n");
        sb.append("感谢您注册 My Little App。\n\n");
        sb.append("您的验证码是：").append(code).append("\n\n");
        sb.append("验证码有效期为5分钟，请尽快完成注册。\n\n");
        sb.append("如果这不是您的操作，请忽略此邮件。\n\n");
        sb.append("---\n");
        sb.append("My Little App 团队");
        return sb.toString();
    }

    /**
     * 验证码信息内部类
     */
    private static class ResetCodeInfo {
        private final String code;
        private final long createTime;

        public ResetCodeInfo(String code, long createTime) {
            this.code = code;
            this.createTime = createTime;
        }

        public String getCode() {
            return code;
        }

        public long getCreateTime() {
            return createTime;
        }
    }
}




