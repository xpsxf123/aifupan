package com.jiuyu.governance.plugins.useragent;

import com.blueconic.browscap.*;
import com.jiuyu.framework.util.EmptyUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;

/**
 * User-Agent解析处理工具类
 * <p>
 * 该工具类基于BrowsCap库提供强大的User-Agent解析功能，能够准确识别客户端设备类型、
 * 操作系统、浏览器等详细信息。支持PC、移动端、平板等多种设备类型的智能识别。
 * </p>
 *
 * <p><strong>主要功能：</strong></p>
 * <ul>
 *   <li>解析User-Agent字符串，提取客户端详细信息</li>
 *   <li>智能识别设备类型（PC、iOS、Android、iPad等）</li>
 *   <li>提取操作系统及版本信息</li>
 *   <li>识别浏览器类型及主版本号</li>
 *   <li>获取设备品牌和市场名称</li>
 *   <li>判断是否为平板设备</li>
 * </ul>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15";
 * UserAgentInfo info = UserAgentHandler.parseClientInfo(userAgent);
 * ClientDeviceType deviceType = info.getDeviceType();
 * String deviceName = info.getDeviceName();
 * }</pre>
 *
 * <p><strong>线程安全性：</strong>该类是线程安全的，所有方法都是静态方法，
 * 内部的UserAgentParser在类加载时初始化一次，可被多个线程并发使用。
 * </p>
 *
 * @author HeHui
 * @version 1.0
 * @since 2025-10-27
 * @see UserAgentInfo 客户端信息数据传输对象
 * @see ClientDeviceType 设备类型枚举
 * @see Capabilities BrowsCap能力对象
 */
@Slf4j
public class UserAgentHandler {


    /**UserAgentHandler
     * User-Agent解析器实例
     * <p>
     * 静态初始化的BrowsCap解析器，在类加载时创建一次。
     * 配置了必要的字段以支持设备类型识别和信息提取。
     * </p>
     */
    private static UserAgentParser parser;

    static {
        try {
            parser = new UserAgentService().loadParser(Arrays.asList(
                BrowsCapField.BROWSER,
                BrowsCapField.DEVICE_TYPE,
                BrowsCapField.PLATFORM,
                BrowsCapField.PLATFORM_VERSION,
                BrowsCapField.DEVICE_BRAND_NAME,
                BrowsCapField.IS_TABLET,
                BrowsCapField.DEVICE_NAME,
                BrowsCapField.BROWSER_MAJOR_VERSION
            ));
        } catch (IOException | ParseException e) {
            log.error("init user-agent parser error", e);
        }
    }


    /**
     * 私有构造函数，防止实例化
     * <p>
     * 该类只提供静态方法，不应被实例化。
     * </p>
     *
     * @throws IllegalStateException 总是抛出此异常，因为该类不应该被实例化
     */
    private UserAgentHandler() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 解析User-Agent字符串并返回结构化的客户端信息对象
     *
     * <p>该方法使用BrowsCap库解析User-Agent字符串，提取客户端的详细信息，
     * 包括设备类型、操作系统、浏览器等，并根据设备类型生成相应的设备名称描述。
     * </p>
     *
     * <p><strong>解析逻辑：</strong></p>
     * <ol>
     *   <li>首先检测设备类型（PC、移动设备等）</li>
     *   <li>对于PC设备：组合操作系统+版本号+浏览器+浏览器版本</li>
     *   <li>对于移动设备：组合设备类型+品牌+市场名称</li>
     *   <li>返回包含设备类型和设备名称的结构化对象</li>
     * </ol>
     *
     * <p><strong>异常处理：</strong>当解析过程中发生异常时，会记录警告日志并返回空的客户端信息对象，
     * 不会抛出异常，保证调用方的稳定性。
     * </p>
     *
     * @param userAgent 待解析的User-Agent字符串，可以为null或空字符串
     * @return UserAgentInfo 客户端信息对象，包含设备类型和设备名称；
     *         如果输入为空或解析失败，返回各字段为空的默认对象
     * @throws NullPointerException 如果内部parser未正确初始化（理论上不会发生）
     *
     * @see #detectDeviceType(Capabilities) 设备类型检测逻辑
     * @see UserAgentInfo 客户端信息数据结构
     */
    public static UserAgentInfo parse(String userAgent) {
        UserAgentInfo clientInfo = new UserAgentInfo();
        if (parser == null || EmptyUtil.isEmpty(userAgent)) {
            String prefix = userAgent.split("/")[0];
            if (prefix.length() > 15) {
                clientInfo.setDeviceName(prefix.substring(0, 15));
            } else {
                clientInfo.setDeviceName(prefix);
            }
            return clientInfo;
        }

        try {
            Capabilities capabilities = parser.parse(userAgent);

            // 识别设备类型
            ClientDeviceType deviceType = detectDeviceType(capabilities);
            clientInfo.setDeviceType(deviceType);
            String deviceName = "";

            if (deviceType == ClientDeviceType.OTHER) {
                String prefix = userAgent.split("/")[0];
                if (prefix.length() > 15) {
                    deviceName = prefix.substring(0, 15);
                } else {
                    deviceName = prefix;
                }
            } else if (deviceType == ClientDeviceType.PC) {
                // 操作系统
                String platform = capabilities.getPlatform();
                if (EmptyUtil.isNotEmpty(platform) && !"unknown".equalsIgnoreCase(platform)) {
                    deviceName = platform;
                    String platformVersion = capabilities.getPlatformVersion();
                    if (EmptyUtil.isNotEmpty(platformVersion) && !"unknown".equalsIgnoreCase(platformVersion)) {
                        deviceName = deviceName + " " + platformVersion;
                    }
                }

                // 获取浏览器信息
                String browser = capabilities.getBrowser();
                if (EmptyUtil.isNotEmpty(browser) && !"unknown".equalsIgnoreCase(browser)) {
                    deviceName = deviceName + "/" + browser;
                    String browserMajorVersion = capabilities.getBrowserMajorVersion();
                    if (EmptyUtil.isNotEmpty(browserMajorVersion) && !"unknown".equalsIgnoreCase(browserMajorVersion)) {
                        deviceName = deviceName + " " + browserMajorVersion;
                    }
                }
            } else {
//                String device = capabilities.getDeviceType();
//                if (EmptyUtil.isNotEmpty(device) && !"unknown".equalsIgnoreCase(device)) {
//                    deviceName = device;
//                }
                String deviceBrandName = capabilities.getValue(BrowsCapField.DEVICE_BRAND_NAME);
                if (EmptyUtil.isNotEmpty(deviceBrandName) && !"unknown".equalsIgnoreCase(deviceBrandName)) {
                    if (deviceName.isEmpty()) {
                        deviceName = deviceBrandName;
                    } else {
                        deviceName = deviceName + "/" + deviceBrandName;
                    }
                    String deviceMarketName = capabilities.getValue(BrowsCapField.DEVICE_NAME);
                    if (EmptyUtil.isNotEmpty(deviceMarketName) && !"unknown".equalsIgnoreCase(deviceMarketName)) {
                        deviceName = deviceName + " " + deviceMarketName;
                    }
                }
            }
            if (EmptyUtil.isNotEmpty(deviceName)) {
                clientInfo.setDeviceName(deviceName);
            }
            return clientInfo;
        } catch (Exception e) {
            log.warn("解析User-Agent失败: {}", userAgent, e);
        }
        return clientInfo;
    }

    /**
     * 根据浏览器能力对象检测客户端设备类型
     *
     * <p>该方法通过分析Capabilities对象中的平台信息、设备类型和tablet标识，
     * 智能判断客户端的具体设备类型。支持多种设备类型的精确识别。
     * </p>
     *
     * <p><strong>识别优先级：</strong></p>
     * <ol>
     *   <li>iOS设备（iPhone/iPad）</li>
     *   <li>Android设备（包括HarmonyOS）</li>
     *   <li>PC设备（Windows/Mac/Linux）</li>
     *   <li>其他未知设备</li>
     * </ol>
     *
     * <p><strong>特殊处理：</strong></p>
     * <ul>
     *   <li>iOS设备会进一步区分iPhone和iPad</li>
     *   <li>Android设备同样区分手机和平板</li>
     *   <li>通过IS_TABLET字段和设备类型描述双重判断平板设备</li>
     * </ul>
     *
     * @param capabilities BrowsCap解析后的浏览器能力对象，包含各种客户端信息
     * @return ClientDeviceType 识别出的设备类型枚举值，never null
     *
     * @see ClientDeviceType 设备类型枚举定义
     * @see Capabilities BrowsCap能力对象
     * @see BrowsCapField#IS_TABLET 平板设备标识字段
     */
    private static ClientDeviceType detectDeviceType(Capabilities capabilities) {
        String platform = capabilities.getPlatform();
        String deviceType = capabilities.getDeviceType();
        boolean isTablet = Boolean.parseBoolean(capabilities.getValue(BrowsCapField.IS_TABLET));
        if (platform == null) {
            platform = "";
        }
        if (deviceType == null) {
            deviceType = "";
        }

        String lowerPlatform = platform.toLowerCase();
        String lowerDeviceType = deviceType.toLowerCase();

        // 检测iOS设备
        if (lowerPlatform.contains("ios") || lowerPlatform.contains("iphone") || lowerPlatform.contains("ipad")) {
            if (isTablet || lowerDeviceType.contains("tablet") || lowerPlatform.contains("ipad")) {
                return ClientDeviceType.IPAD_IOS;
            }
            return ClientDeviceType.IOS;
        }

        // 检测Android设备
        if (lowerPlatform.contains("android") || lowerPlatform.contains("harmony")) {
            if (isTablet || lowerDeviceType.contains("tablet")) {
                return ClientDeviceType.IPAD_ANDROID;
            }
            return ClientDeviceType.ANDROID;
        }

        // 检测PC设备
        if (lowerDeviceType.contains("desktop") || lowerPlatform.contains("windows")
            || lowerPlatform.contains("mac") || lowerPlatform.contains("linux")) {
            return ClientDeviceType.PC;
        }
        // 默认返回其他
        return ClientDeviceType.OTHER;
    }
}
