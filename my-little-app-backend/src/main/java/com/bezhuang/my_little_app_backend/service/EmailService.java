package com.bezhuang.my_little_app_backend.service;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送密码重置验证码
     *
     * @param email 邮箱地址
     */
    void sendResetCode(String email);

    /**
     * 验证重置码
     *
     * @param email 邮箱地址
     * @param code  验证码
     * @return 是否有效
     */
    boolean verifyResetCode(String email, String code);

    /**
     * 通过验证码重置密码
     *
     * @param email       邮箱地址
     * @param code        验证码
     * @param newPassword 新密码（明文）
     * @return 是否成功
     */
    boolean resetPasswordWithCode(String email, String code, String newPassword);

    /**
     * 发送注册验证码
     *
     * @param email 邮箱地址
     */
    void sendRegisterCode(String email);

    /**
     * 验证注册验证码
     *
     * @param email 邮箱地址
     * @param code  验证码
     * @return 是否有效
     */
    boolean verifyRegisterCode(String email, String code);

    /**
     * 发送通用邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content 内容
     */
    void sendEmail(String to, String subject, String content);
}





