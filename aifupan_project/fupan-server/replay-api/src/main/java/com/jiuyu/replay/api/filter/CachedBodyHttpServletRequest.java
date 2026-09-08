package com.jiuyu.replay.api.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

/**
 * 可重复读取请求体的HttpServletRequest包装器
 *
 * @author RayChou
 * @date 2025/6/9 14:28
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        // 只有在有请求体的情况下才缓存
        if (isBodyRequest(request)) {
            InputStream requestInputStream = request.getInputStream();
            this.cachedBody = StreamUtils.copyToByteArray(requestInputStream);
        } else {
            this.cachedBody = new byte[0];
        }
    }

    /**
     * 判断请求是否包含请求体
     */
    private boolean isBodyRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method) ||
                "PUT".equalsIgnoreCase(method) ||
                "PATCH".equalsIgnoreCase(method);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
        return new BufferedReader(new InputStreamReader(byteArrayInputStream));
    }

    /**
     * 获取请求体内容，转为字符串
     */
    public String getBody() {
        if (cachedBody.length == 0) {
            return "";
        }
        return new String(this.cachedBody);
    }

    /**
     * 获取请求内容类型
     */
    public String getContentType() {
        return getHeader("Content-Type");
    }

    /**
     * 判断是否是JSON请求
     */
    public boolean isJsonRequest() {
        String contentType = getContentType();
        return contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * 判断是否是表单请求
     */
    public boolean isFormRequest() {
        String contentType = getContentType();
        return contentType != null &&
                (contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE) ||
                        contentType.contains(MediaType.MULTIPART_FORM_DATA_VALUE));
    }

    /**
     * 获取URL查询参数
     */
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = new TreeMap<>();
        String queryString = getQueryString();

        if (queryString != null && !queryString.isEmpty()) {
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                try {
                    String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name()) : pair;
                    String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name()) : "";
                    queryParams.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    // 不应该发生，因为UTF-8总是支持的
                }
            }
        }

        return queryParams;
    }

    /**
     * 获取表单数据
     */
    public Map<String, String> getFormParams() {
        Map<String, String> formParams = new TreeMap<>();

        if (isFormRequest() && cachedBody.length > 0) {
            String body = getBody();
            if (body.contains(MediaType.MULTIPART_FORM_DATA_VALUE)) {
                // 对于multipart/form-data，使用getParameter方法
                Enumeration<String> paramNames = getParameterNames();
                while (paramNames.hasMoreElements()) {
                    String name = paramNames.nextElement();
                    String value = getParameter(name);
                    formParams.put(name, value);
                }
            } else {
                // 对于application/x-www-form-urlencoded，解析请求体
                String[] pairs = body.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf('=');
                    try {
                        String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name()) : pair;
                        String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name()) : "";
                        formParams.put(key, value);
                    } catch (UnsupportedEncodingException e) {
                        // 不应该发生，因为UTF-8总是支持的
                    }
                }
            }
        }

        return formParams;
    }

    /**
     * 获取需要签名的内容
     * 1. POST-JSON请求：签名body中的JSON
     * 2. POST-FORM请求：签名form-data数据
     * 3. GET请求：签名query参数
     *
     * @return 需要签名的内容
     */
    public String getContentToSign() {
        String method = getMethod();

        // 对于POST请求
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            // JSON请求，直接返回请求体
            if (isJsonRequest()) {
                return getBody();
            }
            // 表单请求，返回表单参数
            else if (isFormRequest()) {
                return mapToString(getFormParams());
            }
        }

        // 对于GET请求，返回查询参数
        if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            return mapToString(getQueryParams());
        }

        return getBody(); // 默认返回请求体
    }

    /**
     * 将Map转换为字符串
     */
    private String mapToString(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }

        return sb.toString();
    }

    /**
     * 自定义ServletInputStream，提供对缓存请求体的访问
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final InputStream cachedBodyInputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.cachedBodyInputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            try {
                return cachedBodyInputStream.available() == 0;
            } catch (IOException e) {
                return true;
            }
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("不支持设置ReadListener");
        }

        @Override
        public int read() throws IOException {
            return cachedBodyInputStream.read();
        }
    }
} 