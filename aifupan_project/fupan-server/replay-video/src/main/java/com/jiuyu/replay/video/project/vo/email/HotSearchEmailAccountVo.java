package com.jiuyu.replay.video.project.vo.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热搜邮箱账号返回VO
 *
 * @author RayChou
 * @since 2025-11-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热搜邮箱账号返回VO")
public class HotSearchEmailAccountVo {

    /**
     * 账号ID
     */
    @Schema(description = "账号ID", example = "1234567890")
    private Long accountId;

    /**
     * 邮箱账号
     */
    @Schema(description = "邮箱账号", example = "test@gmail.com")
    private String email;

    /**
     * 邮箱密码（AES-CBC加密）
     *
     * 解密说明：
     * 1. 加密算法：AES-CBC模式，PKCS5Padding填充
     * 2. 密钥(key)：当前邮箱账号(email字段)，UTF-8编码后取前16字节，不足16字节补0
     * 3. 偏移量(IV)：当前邮箱账号(email字段)，UTF-8编码后取前16字节，不足16字节补0
     * 4. 加密结果：Base64编码
     *
     * 解密步骤：
     * - 将email字段UTF-8编码转字节数组
     * - 如果字节长度>16，截取前16字节作为key和IV
     * - 如果字节长度<16，补0至16字节作为key和IV
     * - 使用AES/CBC/PKCS5Padding算法解密
     * - Base64解码emailPassword字段后进行解密
     *
     * 示例(JavaScript)：
     * const CryptoJS = require('crypto-js');
     * function decryptPassword(emailPassword, email) {
     *   const emailBytes = CryptoJS.enc.Utf8.parse(email);
     *   let keyBytes = CryptoJS.lib.WordArray.create();
     *   if (emailBytes.sigBytes >= 16) {
     *     keyBytes = CryptoJS.lib.WordArray.create(emailBytes.words.slice(0, 4));
     *   } else {
     *     keyBytes = emailBytes.clone();
     *     while (keyBytes.sigBytes < 16) {
     *       keyBytes.concat(CryptoJS.lib.WordArray.create([0]));
     *     }
     *   }
     *   const decrypted = CryptoJS.AES.decrypt(emailPassword, keyBytes, {
     *     iv: keyBytes,
     *     mode: CryptoJS.mode.CBC,
     *     padding: CryptoJS.pad.Pkcs7
     *   });
     *   return decrypted.toString(CryptoJS.enc.Utf8);
     * }
     */
    @Schema(description = "邮箱密码（AES-CBC加密，Base64编码）。解密：使用email字段作为key和IV（UTF-8编码取前16字节，超过16字节截断，不足16字节补0），AES/CBC/PKCS5Padding算法解密", example = "U2FsdGVkX1...")
    private String emailPassword;

    /**
     * 账号类型：1-Gmail 2-Outlook 3-QQ邮箱 4-163邮箱 5-其他
     */
    @Schema(description = "账号类型：1-Gmail 2-Outlook 3-QQ邮箱 4-163邮箱 5-其他", example = "1")
    private Integer accountType;

    /**
     * 账号类型名称
     */
    @Schema(description = "账号类型名称", example = "Gmail")
    private String accountTypeName;

    /**
     * 账号归属城市
     */
    @Schema(description = "账号归属城市", example = "上海")
    private String accountCity;

    /**
     * 客户端城市
     */
    @Schema(description = "客户端城市", example = "上海")
    private String clientCity;

    /**
     * 是否同城
     */
    @Schema(description = "是否同城", example = "true")
    private Boolean isSameCity;

    /**
     * 使用超时时间（分钟）
     */
    @Schema(description = "使用超时时间（分钟），超过此时间账号将自动释放", example = "30")
    private Integer useTimeoutMinutes;
}

