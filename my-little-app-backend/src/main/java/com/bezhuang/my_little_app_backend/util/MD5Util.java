package com.bezhuang.my_little_app_backend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * MD5 加密工具类
 * 提供密码加密和验证功能
 */
public class MD5Util {

    /**
     * 盐值长度
     */
    private static final int SALT_LENGTH = 16;

    /**
     * 盐值分隔符
     */
    private static final String SALT_SEPARATOR = "$";

    /**
     * 私有构造方法，防止实例化
     */
    private MD5Util() {
    }

    /**
     * 生成随机盐值
     *
     * @return Base64编码的盐值
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 对明文密码进行MD5加密（不加盐）
     * 返回32位小写MD5字符串
     *
     * @param plainPassword 明文密码
     * @return 32位小写MD5字符串
     */
    public static String md5(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    /**
     * 对明文密码进行加盐MD5加密
     * 格式：salt$md5(salt+password)
     *
     * @param plainPassword 明文密码
     * @return 加盐后的密码字符串
     */
    public static String encryptWithSalt(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        String salt = generateSalt();
        String saltedPassword = salt + plainPassword;
        String md5Hash = md5(saltedPassword);
        return salt + SALT_SEPARATOR + md5Hash;
    }

    /**
     * 使用指定盐值对密码进行MD5加密
     *
     * @param plainPassword 明文密码
     * @param salt          盐值
     * @return 32位小写MD5字符串
     */
    public static String encryptWithSalt(String plainPassword, String salt) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("盐值不能为空");
        }
        String saltedPassword = salt + plainPassword;
        return md5(saltedPassword);
    }

    /**
     * 验证密码是否正确（加盐版本）
     *
     * @param plainPassword   明文密码
     * @param encryptedPassword 数据库中存储的加密密码（格式：salt$hash）
     * @return true-密码正确，false-密码错误
     */
    public static boolean verifySaltedPassword(String plainPassword, String encryptedPassword) {
        if (plainPassword == null || encryptedPassword == null) {
            return false;
        }
        int separatorIndex = encryptedPassword.indexOf(SALT_SEPARATOR);
        if (separatorIndex == -1) {
            // 如果没有盐值分隔符，则进行简单MD5比较
            return md5(plainPassword).equals(encryptedPassword);
        }
        String salt = encryptedPassword.substring(0, separatorIndex);
        String storedHash = encryptedPassword.substring(separatorIndex + 1);
        String computedHash = encryptWithSalt(plainPassword, salt);
        return storedHash.equals(computedHash);
    }

    /**
     * 验证密码是否正确（简单MD5版本）
     *
     * @param plainPassword    明文密码
     * @param md5Password      MD5加密后的密码
     * @return true-密码正确，false-密码错误
     */
    public static boolean verifyPassword(String plainPassword, String md5Password) {
        if (plainPassword == null || md5Password == null) {
            return false;
        }
        return md5(plainPassword).equalsIgnoreCase(md5Password);
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串（小写）
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 获取默认密码的MD5值
     * 默认密码：123456
     *
     * @return 默认密码的MD5值
     */
    public static String getDefaultPasswordMD5() {
        return md5("123456");
    }
}







