package com.jiuyu.governance.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IP查询工具类
 *
 * @author linfeng
 * @date 2022/2/7 19:18
 */
@Slf4j
public class RequestUtil {

    private static final String UNKNOWN = "unknown";


    /** ip过滤器正则表达式列表 */
    static List<Pattern> ipFilterRegexList = new ArrayList<>();

    private final static String LOCALHOS;


    static {

        Set<String> ipFilter = new HashSet<>();
        ipFilter.add("^10\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[0-9])"
            + "\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[0-9])" + "\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[0-9])$");
        // B类地址范围: 172.16.0.0---172.31.255.255
        ipFilter.add("^172\\.(1[6789]|2[0-9]|3[01])\\" + ".(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[0-9])\\"
            + ".(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[0-9])$");
        // C类地址范围: 192.168.0.0---192.168.255.255
        ipFilter.add("^192\\.168\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[0-9])\\"
            + ".(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[0-9])$");
        ipFilter.add("127.0.0.1");
        ipFilter.add("0.0.0.0");
        ipFilter.add("localhost");
        for (String tmp : ipFilter) {
            ipFilterRegexList.add(Pattern.compile(tmp));
        }
        String s;
        try {
            s = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            s = "127.0.0.1";
        }

        LOCALHOS = s;
    }

    /**
     * 判断给定的IP地址是否为内网IP。
     * 内网IP通常指私有IP地址，这些地址不直接在互联网上公开，而是在内部网络中使用。
     * 本方法通过匹配预先定义的一组IP地址正则表达式来判断IP地址是否为内网IP。
     *
     * @param ip 待检查的IP地址字符串。
     *
     * @return 如果给定的IP地址是内网IP，则返回true；否则返回false。
     */
    public static boolean ipIsInner(String ip) {
        // 默认假设IP地址不是内网IP
        boolean isInnerIp = false;

        // 遍历预定义的IP地址正则表达式列表
        for (Pattern tmp : ipFilterRegexList) {
            // 使用正则表达式匹配当前的IP地址
            Matcher matcher = tmp.matcher(ip);
            // 如果IP地址匹配正则表达式，即为内网IP，跳出循环
            if (matcher.find()) {
                isInnerIp = true;
                break;
            }
        }
        // 返回IP地址是否为内网IP的判断结果
        return isInnerIp;
    }


    /**
     * 获取ip地址
     */
    public static String getIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!ipIsInner(remoteAddr)) {
            return remoteAddr;
        }

        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        String comma = ",";
        String localhost = "127.0.0.1";
        if (ip == null) {
            return localhost;
        }
        if (ip.contains(comma)) {
            ip = ip.split(",")[0];
        }
        if (localhost.equals(ip)) {
            // 获取本机真正的ip地址
            return LOCALHOS;
        }
        return ip;
    }


    /**
     * 响应
     *
     * @param response     响应
     * @param bodySupplier body数据提供
     */
    public static void response(HttpServletResponse response, Supplier<String> bodySupplier) {
        if (response.isCommitted()) {
            return;
        }
        String body = bodySupplier.get();
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        try {
            if (body != null) {
                response.setContentLengthLong(body.length());
                response.getWriter().write(body);
            }
            response.getWriter().flush();
            response.getWriter().close();
        } catch (IOException responseWriter) {
            responseWriter.printStackTrace();
        }
    }


    /**
     * 获取上下文
     *
     * @param context 上下文
     * @param key     名称
     * @param other   另外
     *
     * @return {@link Optional }<{@link T }>
     */
    public static <T> T getContext(HttpServletRequest context, String key, Supplier<T> other) {
        T attribute = (T) context.getAttribute(key);
        if (attribute == null) {
            attribute = other.get();
            context.setAttribute(key, attribute);
        }
        return attribute;

    }
}
