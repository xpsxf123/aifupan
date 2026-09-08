package com.jiuyu.replay.system.util;

import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.vo.FileUploadVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.system.constant.SeoConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 导入文章时把外链图片转存到自有图床（阿里云 OSS，bucket replay-images）。
 *
 * <p><b>图片 URL 来自导入的 md 文件，是不可信外部输入</b>，因此抓取前后都要做 SSRF 校验。
 * 服务端能访问的内网地址（数据库、Redis、云厂商元数据 169.254.169.254）远多于公网，
 * 一个「帮我下载这张图」的功能等于把服务器变成内网探测代理。
 *
 * @author claude
 * @date 2026-08-13
 */
@Component
public class SeoImageFetcher {

    private static final Logger log = LoggerFactory.getLogger(SeoImageFetcher.class);

    /** 允许的图片 Content-Type 前缀 → 落 OSS 时用的扩展名 */
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    /** OSS 存放路径前缀，与站内图片区分，便于日后单独治理 */
    private static final String OSS_KEY_PREFIX = "img/seo/";

    /** 内联图片前缀，形如 data:image/png;base64,xxxx */
    private static final String DATA_URI_PREFIX = "data:";

    /**
     * 对外统一的「取不到」文案。
     *
     * <p>连接失败 / 超时 / 404 / 403 / 域名解析不了 / 指向内网——这些差异对运营毫无用处，
     * 对攻击者却是精确的内网探测信道（端口开闭、DNS 名是否存在、服务是否存活）。
     * 这些结果会原样出现在 importProgress 响应里，所以对外必须收敛成一句话，细节只进日志。
     */
    private static final String MSG_UNAVAILABLE = "图片地址不可用";

    /** 对外统一的超限文案 */
    private static final String MSG_TOO_LARGE = "图片超过 5MB 上限";

    /** 允许的目标端口。图床都用标准端口，放开任意端口等于送出一个内网端口扫描器 */
    private static final Set<Integer> ALLOWED_PORTS = Set.of(80, 443);

    private final ImgOssUtils imgOssUtils;

    public SeoImageFetcher(ImgOssUtils imgOssUtils) {
        this.imgOssUtils = imgOssUtils;
    }

    /** 抓取失败，消息直接进导入结果的 notes */
    public static class ImageFetchException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public ImageFetchException(String message) {
            super(message);
        }
    }

    /**
     * 抓取一张外链图片并转存到 OSS。
     *
     * <p>已经是本站图床地址的直接原样返回，不做无意义的「下载再上传」。
     *
     * @param imageUrl 原始图片地址
     * @return 转存后的可访问 URL
     * @throws ImageFetchException 地址非法、目标为内网、超限、类型不符或网络失败
     */
    public String fetchAndUpload(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ImageFetchException("图片地址为空");
        }
        String url = imageUrl.trim();
        if (isAlreadyHosted(url)) {
            return url;
        }

        // data: 内联图片不发网络请求，直接解码。放在协议校验之前处理——
        // validateAndParse 会把它当成不支持的协议拒掉
        Fetched fetched = url.regionMatches(true, 0, DATA_URI_PREFIX, 0, DATA_URI_PREFIX.length())
                ? decodeDataUri(url)
                : fetch(url);
        // 魔数校验：Content-Type 是对方服务器随口声明的，可以对着任意二进制写 image/png。
        // OSS 那侧按扩展名推断类型，所以这不构成图床域名上的 XSS，但会让图床沦为任意内容的
        // 匿名存储（违规内容托管、恶意载荷分发）
        assertMagicMatches(fetched);

        String key = OSS_KEY_PREFIX + UUID.randomUUID().toString().replace("-", "") + fetched.extension();
        FileUploadVo uploaded = imgOssUtils.uploadToImg(key, fetched.data());
        if (uploaded == null) {
            throw new ImageFetchException("图片转存失败，请稍后重试");
        }
        // 与站内既有图片上传路径（CommonLogicImpl.uploadImg / ThirdLogicImpl）对齐：
        // 都要过内容安全审核。SEO 文章会发到公开站，漏掉这一步是合规问题
        R<Boolean> moderation = imgOssUtils.imageModerationWithOptions(uploaded.getKey());
        if (moderation == null || moderation.getCode() != 0 || !Boolean.TRUE.equals(moderation.getData())) {
            imgOssUtils.deleteObject(uploaded.getKey());
            throw new ImageFetchException("图片未通过内容安全检测");
        }
        return uploaded.getUrl();
    }

    /**
     * 判断是否已是本站图床地址。
     *
     * <p>按 host 精确比对而非字符串前缀：前缀匹配下 {@code https://<图床域名>.evil.com/x.jpg}
     * 会命中短路，直接原样返回——协议校验、内网校验、类型与大小限制<b>全部被跳过</b>，
     * 「外链图片必转存自有图床」这个核心保证就此失效。
     */
    private boolean isAlreadyHosted(String url) {
        String accessUrl = imgOssUtils.getAccessUrl();
        if (accessUrl == null || accessUrl.isBlank()) {
            return false;
        }
        try {
            String hostedHost = new URI(accessUrl).getHost();
            String targetHost = new URI(url).getHost();
            return hostedHost != null && hostedHost.equalsIgnoreCase(targetHost);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /** 图片格式魔数。与 ALLOWED_CONTENT_TYPES 的扩展名一一对应 */
    private static void assertMagicMatches(Fetched fetched) {
        byte[] data = fetched.data();
        boolean matched = switch (fetched.extension()) {
            case ".jpg" -> startsWith(data, 0xFF, 0xD8, 0xFF);
            case ".png" -> startsWith(data, 0x89, 0x50, 0x4E, 0x47);
            case ".gif" -> startsWith(data, 0x47, 0x49, 0x46, 0x38);
            // WebP 是 RIFF 容器：前 4 字节 RIFF，第 8-11 字节 WEBP
            case ".webp" -> startsWith(data, 0x52, 0x49, 0x46, 0x46)
                    && data.length >= 12 && startsWithAt(data, 8, 0x57, 0x45, 0x42, 0x50);
            default -> false;
        };
        if (!matched) {
            throw new ImageFetchException("文件内容不是有效图片");
        }
    }

    private static boolean startsWith(byte[] data, int... expected) {
        return startsWithAt(data, 0, expected);
    }

    private static boolean startsWithAt(byte[] data, int offset, int... expected) {
        if (data.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((data[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private record Fetched(byte[] data, String extension) {
    }

    /**
     * 解码 data:image/png;base64,xxxx 形式的内联图片。
     *
     * <p>只接受 base64 编码：非 base64 的 data URI（百分号编码的 SVG 之类）落进图床后
     * 可能被当作可执行内容，而白名单本就不含 svg。
     */
    private Fetched decodeDataUri(String uri) {
        int comma = uri.indexOf(',');
        if (comma < 0) {
            throw new ImageFetchException("内联图片格式非法，缺少数据部分");
        }
        String meta = uri.substring(DATA_URI_PREFIX.length(), comma);
        if (!meta.toLowerCase(Locale.ROOT).contains("base64")) {
            throw new ImageFetchException("内联图片仅支持 base64 编码");
        }
        String mime = meta.split(";")[0].trim();
        String extension = resolveExtension(mime);

        byte[] data;
        try {
            data = Base64.getDecoder().decode(uri.substring(comma + 1).replaceAll("\\s", ""));
        } catch (IllegalArgumentException e) {
            throw new ImageFetchException("内联图片 base64 解码失败");
        }
        if (data.length == 0) {
            throw new ImageFetchException("内联图片内容为空");
        }
        if (data.length > SeoConstant.MAX_IMAGE_SIZE) {
            throw new ImageFetchException(MSG_TOO_LARGE);
        }
        return new Fetched(data, extension);
    }

    /**
     * 逐跳抓取。
     *
     * <p>之所以自己处理跳转而不用 {@code setInstanceFollowRedirects(true)}：
     * JDK 自动跟随时不会给我们回调，一个外网域名 302 到 169.254.169.254 就能完全绕过
     * 首次校验——这是 SSRF 防护里最常漏的一条。这里每一跳都重新解析、重新校验。
     */
    private Fetched fetch(String originalUrl) {
        // 整体预算，跳转与读取共用。setReadTimeout 只管「两次 read 之间」的间隔，
        // 对方每 9 秒回 1 个字节就能让 readNBytes(5MB) 跑上十几个小时而不触发任何超时——
        // 慢速投喂几个 URL 就能占死整个导入线程池
        long deadline = System.currentTimeMillis() + SeoConstant.IMAGE_FETCH_DEADLINE_MS;
        String current = originalUrl;
        for (int hop = 0; hop <= SeoConstant.MAX_IMAGE_REDIRECTS; hop++) {
            URL url = validateAndParse(current);
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                // 关掉自动跟随，跳转由本方法逐跳校验后手动继续
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(SeoConstant.IMAGE_FETCH_TIMEOUT_MS);
                connection.setReadTimeout(SeoConstant.IMAGE_FETCH_TIMEOUT_MS);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "image/*");

                int code = connection.getResponseCode();
                if (isRedirect(code)) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.isBlank()) {
                        throw new ImageFetchException(MSG_UNAVAILABLE);
                    }
                    // 相对地址要按当前 URL 解析，否则 /a/b.jpg 这种会被当成非法地址误拒
                    current = resolveLocation(url, location);
                    continue;
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    // 不回显 HTTP 状态码：连接成功但 401/403/404 与连接失败的区别，
                    // 就足以把这个接口变成内网端口扫描器（结果整齐列在导入结果里）
                    log.warn("[SEO导入] 图片响应异常 url={} code={}", current, code);
                    throw new ImageFetchException(MSG_UNAVAILABLE);
                }
                return readBody(connection, deadline);
            } catch (IOException e) {
                // 同理不回显异常类名：ConnectException（端口关闭）与 SocketTimeoutException
                // （被防火墙丢包）的差异是一个精确的探测信道。细节只进日志
                log.warn("[SEO导入] 图片抓取失败 url={}", current, e);
                throw new ImageFetchException(MSG_UNAVAILABLE);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        throw new ImageFetchException(MSG_UNAVAILABLE);
    }

    private Fetched readBody(HttpURLConnection connection, long deadline) throws IOException {
        String contentType = connection.getContentType();
        String extension = resolveExtension(contentType);

        // Content-Length 只是声明值，可以撒谎；真正的限制靠下面按字节数截断
        long declaredLength = connection.getContentLengthLong();
        if (declaredLength > SeoConstant.MAX_IMAGE_SIZE) {
            throw new ImageFetchException(MSG_TOO_LARGE);
        }

        try (InputStream in = connection.getInputStream()) {
            // 分块读而不是 readNBytes 一把梭：只有分块才能在每块之间检查整体耗时，
            // 否则慢速投喂能把单张图片的下载拖到数小时（见 fetch 里的 deadline 注释）
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                if (System.currentTimeMillis() > deadline) {
                    throw new ImageFetchException(MSG_UNAVAILABLE);
                }
                buffer.write(chunk, 0, read);
                // 超过上限即刻停，不把剩下的读完——否则限制形同虚设
                if (buffer.size() > SeoConstant.MAX_IMAGE_SIZE) {
                    throw new ImageFetchException(MSG_TOO_LARGE);
                }
            }
            if (buffer.size() == 0) {
                throw new ImageFetchException("图片内容为空");
            }
            return new Fetched(buffer.toByteArray(), extension);
        }
    }

    private static boolean isRedirect(int code) {
        return code == HttpURLConnection.HTTP_MOVED_PERM
                || code == HttpURLConnection.HTTP_MOVED_TEMP
                || code == HttpURLConnection.HTTP_SEE_OTHER
                || code == 307
                || code == 308;
    }

    private static String resolveLocation(URL base, String location) {
        try {
            return base.toURI().resolve(location).toString();
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new ImageFetchException("图片跳转目标地址非法");
        }
    }

    /**
     * Content-Type 白名单。不能只看扩展名——扩展名由攻击者控制，
     * 而白名单外的类型（text/html、application/octet-stream）落进图床后
     * 可能被浏览器按 HTML 渲染，等于在自有域名下开了个 XSS 落点。
     */
    private static String resolveExtension(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new ImageFetchException("图片响应未声明类型，无法确认是图片");
        }
        // Content-Type 可能带参数：image/jpeg; charset=binary
        String mime = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        String extension = ALLOWED_CONTENT_TYPES.get(mime);
        if (extension == null) {
            // 不回显对方声明的 mime：application/json、text/html 这类回显等于服务指纹识别
            log.warn("[SEO导入] 图片类型不支持 mime={}", mime);
            throw new ImageFetchException("不支持的图片类型，仅支持 jpg/png/webp/gif");
        }
        return extension;
    }

    /**
     * 协议 + 目标 IP 校验。
     *
     * <p>用 {@link InetAddress#getAllByName} 而不是只解析一个地址：域名可以同时返回
     * 一个公网 A 记录和一个内网 A 记录，只查第一个就可能放行。任意一个落在内网就整体拒绝。
     *
     * <p><b>已知残留风险</b>：这里解析一次、连接时 JDK 再解析一次，中间存在 DNS
     * rebinding 窗口。彻底封堵需要自定义 SocketFactory 或走固定出口代理，
     * 成本远高于本功能收益——运营手工导入自己写的 md，攻击面是「文件里带条恶意链接」，
     * 不是「有人持续控制 DNS 做时序攻击」。
     */
    private static URL validateAndParse(String rawUrl) {
        URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException e) {
            throw new ImageFetchException("图片地址格式非法");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new ImageFetchException("图片地址仅支持 http/https，当前为 " + (scheme.isEmpty() ? "无协议" : scheme));
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new ImageFetchException("图片地址缺少域名");
        }
        // 端口白名单。协议限死 http/https 但端口不限的话，一次导入可以对内网
        // 任意主机的任意端口发 1550 次 GET——scheme 的限制就没多少意义了
        int port = uri.getPort();
        if (port != -1 && !ALLOWED_PORTS.contains(port)) {
            throw new ImageFetchException("图片地址端口不被允许，仅支持 80/443");
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            // 不回显 host：「解析不了」与「指向内网」的差异可用来枚举内网 DNS 名，
            // 所以两者统一成同一句对外文案
            log.warn("[SEO导入] 图片域名无法解析 host={}", host);
            throw new ImageFetchException(MSG_UNAVAILABLE);
        }
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                log.warn("[SEO导入] 图片地址指向内网，已拒绝 host={}", host);
                throw new ImageFetchException(MSG_UNAVAILABLE);
            }
        }

        try {
            return uri.toURL();
        } catch (Exception e) {
            throw new ImageFetchException("图片地址格式非法");
        }
    }

    /**
     * 内网 / 特殊用途地址判定。
     *
     * <p>JDK 自带的判定方法覆盖不全，必须逐项补齐——只写 {@code isSiteLocalAddress}
     * 是这类校验最典型的漏法，它<b>只</b>覆盖 10/8、172.16/12、192.168/16：
     * <ul>
     *   <li>169.254/16 由 {@code isLinkLocalAddress} 覆盖，AWS 元数据在这里；</li>
     *   <li><b>100.64/10（RFC 6598）不属于上述任何一个</b>，而本项目全栈跑在阿里云，
     *       阿里云 ECS 的元数据入口正是 {@code 100.100.100.200}（返回临时 AK/SK）、
     *       内网 DNS 是 {@code 100.100.2.136}。漏掉这一段等于把云凭证入口留在门外；</li>
     *   <li>IPv4-compatible（{@code ::127.0.0.1}）与 NAT64（{@code 64:ff9b::/96}）
     *       在 Java 里都是 Inet6Address，上面所有方法一概返回 false，需要拆出内嵌的
     *       IPv4 再判一次。（IPv4-mapped {@code ::ffff:x.x.x.x} 会被 JDK 直接转成
     *       Inet4Address，不必特殊处理。）</li>
     * </ul>
     */
    static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()           // 0.0.0.0 / ::
                || address.isLoopbackAddress()    // 127/8 / ::1
                || address.isSiteLocalAddress()   // 10/8、172.16/12、192.168/16
                || address.isLinkLocalAddress()   // 169.254/16（AWS 元数据）、fe80::/10
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return isBlockedIpv4(bytes);
        }
        if ((bytes[0] & 0xFE) == 0xFC) {          // fc00::/7 唯一本地地址
            return true;
        }
        byte[] embedded = extractEmbeddedIpv4(bytes);
        return embedded != null && isBlockedIpv4(embedded);
    }

    /**
     * IPv4 特殊用途段。取「默认拒绝已知非全球单播段」的思路，
     * 逐段列出而不是只信 JDK 的三个方法。
     *
     * <p><b>必须自带私有段与回环判定</b>，不能假设调用方已用 JDK 方法过滤过：
     * IPv6 内嵌 IPv4（{@code ::127.0.0.1}）走的就是这条路径，
     * 而那种地址在 Java 里是 Inet6Address，{@code isLoopbackAddress()} 返回 false。
     */
    private static boolean isBlockedIpv4(byte[] b) {
        int first = b[0] & 0xFF;
        int second = b[1] & 0xFF;
        int third = b[2] & 0xFF;
        // 0/8 本网络、127/8 回环
        if (first == 0 || first == 127) {
            return true;
        }
        // 10/8、172.16/12、192.168/16 私有段
        if (first == 10
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168)) {
            return true;
        }
        // 169.254/16 链路本地（AWS 元数据）
        if (first == 169 && second == 254) {
            return true;
        }
        // 224/4 组播
        if (first >= 224 && first <= 239) {
            return true;
        }
        // 100.64/10 共享地址段（RFC 6598）：阿里云元数据与内网 DNS
        if (first == 100 && second >= 64 && second <= 127) {
            return true;
        }
        // 192.0.0/24 IETF 协议分配、192.0.2/24 与 198.51.100/24 与 203.0.113/24 文档用
        if (first == 192 && second == 0 && (third == 0 || third == 2)) {
            return true;
        }
        if (first == 198 && second == 51 && third == 100) {
            return true;
        }
        if (first == 203 && second == 0 && third == 113) {
            return true;
        }
        // 198.18/15 基准测试段
        if (first == 198 && (second == 18 || second == 19)) {
            return true;
        }
        // 240/4 保留段（含 255.255.255.255）
        return first >= 240;
    }

    /**
     * 取出 IPv6 地址里内嵌的 IPv4。
     *
     * @return 4 字节 IPv4，不含内嵌 IPv4 时返回 null
     */
    private static byte[] extractEmbeddedIpv4(byte[] b) {
        // 64:ff9b::/96 NAT64：部署了 NAT64/DNS64 的环境会真实翻译到 IPv4
        boolean nat64 = (b[0] & 0xFF) == 0x00 && (b[1] & 0xFF) == 0x64
                && (b[2] & 0xFF) == 0xFF && (b[3] & 0xFF) == 0x9B;
        if (nat64) {
            return new byte[]{b[12], b[13], b[14], b[15]};
        }
        // ::x.x.x.x IPv4-compatible（已废弃但仍可解析）
        for (int i = 0; i < 12; i++) {
            if (b[i] != 0) {
                return null;
            }
        }
        return new byte[]{b[12], b[13], b[14], b[15]};
    }
}
