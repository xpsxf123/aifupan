package com.jiuyu.replay.system.util;

import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.vo.FileUploadVo;
import com.jiuyu.replay.generic.vo.common.R;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图片抓取的 SSRF 防护单测。
 *
 * <p>只覆盖不发起真实网络请求的部分：协议校验、内网地址判定、已托管地址短路。
 * 跳转跟随、Content-Type 校验、大小截断需要一个可控的 HTTP 服务端，
 * 属集成测试范畴（当前项目无该基础设施），已在类末列出。
 *
 * @author claude
 * @date 2026-08-13
 */
@DisplayName("SEO 图片抓取 SSRF 防护")
class SeoImageFetcherTest {

    private SeoImageFetcher fetcherWithAccessUrl(String accessUrl) {
        ImgOssUtils imgOssUtils = Mockito.mock(ImgOssUtils.class);
        Mockito.when(imgOssUtils.getAccessUrl()).thenReturn(accessUrl);
        return new SeoImageFetcher(imgOssUtils);
    }

    @Nested
    @DisplayName("内网地址判定")
    class BlockedAddress {

        @Test
        @DisplayName("云元数据地址 169.254.169.254 被拦——isSiteLocalAddress 覆盖不到它")
        void isBlockedAddress_cloudMetadata_blocked() throws UnknownHostException {
            InetAddress metadata = InetAddress.getByName("169.254.169.254");

            assertFalse(metadata.isSiteLocalAddress(),
                    "前提变了：若 isSiteLocalAddress 已覆盖链路本地地址，本类的逐项补齐可以简化");
            assertTrue(SeoImageFetcher.isBlockedAddress(metadata),
                    "云元数据服务未被拦截，服务器凭证可被读走");
        }

        @Test
        @DisplayName("私有网段与回环全部被拦")
        void isBlockedAddress_privateRanges_blocked() throws UnknownHostException {
            for (String ip : new String[]{
                    "10.0.0.1", "172.16.0.1", "172.31.255.254", "192.168.1.1",
                    "127.0.0.1", "0.0.0.0", "::1"}) {
                assertTrue(SeoImageFetcher.isBlockedAddress(InetAddress.getByName(ip)), ip + " 未被拦截");
            }
        }

        @Test
        @DisplayName("IPv6 唯一本地地址 fc00::/7 被拦")
        void isBlockedAddress_ipv6UniqueLocal_blocked() throws UnknownHostException {
            assertTrue(SeoImageFetcher.isBlockedAddress(InetAddress.getByName("fd00::1")));
            assertTrue(SeoImageFetcher.isBlockedAddress(InetAddress.getByName("fc00::1")));
        }

        @Test
        @DisplayName("公网地址放行——拦得过头会让正常图片全部抓取失败")
        void isBlockedAddress_publicAddress_allowed() throws UnknownHostException {
            for (String ip : new String[]{"1.1.1.1", "8.8.8.8", "104.16.1.1", "2001:4860:4860::8888"}) {
                assertFalse(SeoImageFetcher.isBlockedAddress(InetAddress.getByName(ip)), ip + " 被误拦");
            }
        }

        @Test
        @DisplayName("阿里云元数据 100.100.100.200 被拦——它不属于 JDK 任何一个内网判定方法")
        void isBlockedAddress_aliyunMetadata_blocked() throws UnknownHostException {
            InetAddress metadata = InetAddress.getByName("100.100.100.200");

            assertFalse(metadata.isSiteLocalAddress(), "前提变了：100.64/10 已被 JDK 覆盖，逐段补齐可以简化");
            assertFalse(metadata.isLinkLocalAddress(), "前提变了：同上");
            assertTrue(SeoImageFetcher.isBlockedAddress(metadata),
                    "阿里云 ECS 元数据入口未被拦截，临时 AK/SK 可被读走。本项目全栈跑在阿里云");
            // 阿里云内网 DNS 同段
            assertTrue(SeoImageFetcher.isBlockedAddress(InetAddress.getByName("100.100.2.136")));
        }

        @Test
        @DisplayName("IPv4 特殊用途段被拦：共享地址、文档用、基准测试、保留段")
        void isBlockedAddress_specialPurposeRanges_blocked() throws UnknownHostException {
            for (String ip : new String[]{
                    "100.64.0.1", "100.127.255.254",   // RFC 6598 共享地址
                    "192.0.0.1", "192.0.2.1",          // IETF 协议分配 / TEST-NET-1
                    "198.51.100.1", "203.0.113.1",     // TEST-NET-2 / TEST-NET-3
                    "198.18.0.1", "198.19.255.254",    // 基准测试
                    "240.0.0.1", "255.255.255.255"}) { // 保留段
                assertTrue(SeoImageFetcher.isBlockedAddress(InetAddress.getByName(ip)), ip + " 未被拦截");
            }
        }

        @Test
        @DisplayName("IPv6 内嵌 IPv4 的变形被拦：::127.0.0.1 与 NAT64")
        void isBlockedAddress_ipv6EmbeddedIpv4_blocked() throws UnknownHostException {
            // 这两种在 Java 里都是 Inet6Address，isLoopbackAddress 等方法一概返回 false
            assertTrue(SeoImageFetcher.isBlockedAddress(InetAddress.getByName("::127.0.0.1")),
                    "IPv4-compatible 形式的回环未被拦截");
            assertTrue(SeoImageFetcher.isBlockedAddress(InetAddress.getByName("64:ff9b::7f00:1")),
                    "NAT64 前缀内嵌的回环未被拦截，部署 NAT64/DNS64 的环境会真实翻译过去");
            // 内嵌公网地址不该被误拦
            assertFalse(SeoImageFetcher.isBlockedAddress(InetAddress.getByName("64:ff9b::8080:808")));
        }
    }

    @Nested
    @DisplayName("协议与地址格式")
    class SchemeValidation {

        @Test
        @DisplayName("file:// 被拒——否则等于让 md 文件读服务器本地文件")
        void fetchAndUpload_fileScheme_rejected() {
            SeoImageFetcher fetcher = fetcherWithAccessUrl("https://img.example.com");

            SeoImageFetcher.ImageFetchException e = assertThrows(
                    SeoImageFetcher.ImageFetchException.class,
                    () -> fetcher.fetchAndUpload("file:///etc/passwd"));

            assertTrue(e.getMessage().contains("http/https"), e.getMessage());
        }

        @Test
        @DisplayName("非 http 协议（gopher / ftp）一律被拒")
        void fetchAndUpload_otherSchemes_rejected() {
            SeoImageFetcher fetcher = fetcherWithAccessUrl("https://img.example.com");

            for (String url : new String[]{
                    "gopher://127.0.0.1:6379/_INFO", "ftp://example.com/a.jpg",
                    "jar:file:///tmp/a.zip!/b.jpg"}) {
                assertThrows(SeoImageFetcher.ImageFetchException.class,
                        () -> fetcher.fetchAndUpload(url), url + " 未被拒绝");
            }
        }

        @Test
        @DisplayName("localhost 域名被拒，不是只看字面 IP")
        void fetchAndUpload_localhostHostname_rejected() {
            SeoImageFetcher fetcher = fetcherWithAccessUrl("https://img.example.com");

            SeoImageFetcher.ImageFetchException e = assertThrows(
                    SeoImageFetcher.ImageFetchException.class,
                    () -> fetcher.fetchAndUpload("http://localhost/a.jpg"));

            // 对外文案统一成「不可用」：区分「解析不了」与「指向内网」等于送出一个
            // 内网 DNS 名枚举器，而这些消息会原样出现在导入结果里
            assertTrue(e.getMessage().contains("不可用"), e.getMessage());
        }

        @Test
        @DisplayName("非 80/443 端口被拒——否则一次导入能对内网任意端口发上千次 GET")
        void fetchAndUpload_nonStandardPort_rejected() {
            SeoImageFetcher fetcher = fetcherWithAccessUrl("https://img.example.com");

            SeoImageFetcher.ImageFetchException e = assertThrows(
                    SeoImageFetcher.ImageFetchException.class,
                    () -> fetcher.fetchAndUpload("http://example.com:6379/a.jpg"));

            assertTrue(e.getMessage().contains("端口"), e.getMessage());
        }

        @Test
        @DisplayName("空地址与无域名地址被拒")
        void fetchAndUpload_blankOrHostless_rejected() {
            SeoImageFetcher fetcher = fetcherWithAccessUrl("https://img.example.com");

            assertThrows(SeoImageFetcher.ImageFetchException.class, () -> fetcher.fetchAndUpload(""));
            assertThrows(SeoImageFetcher.ImageFetchException.class, () -> fetcher.fetchAndUpload("   "));
            assertThrows(SeoImageFetcher.ImageFetchException.class, () -> fetcher.fetchAndUpload("http:///a.jpg"));
        }
    }

    @Nested
    @DisplayName("data: 内联图片")
    class DataUri {

        /** 1x1 透明 PNG 的 base64 */
        private static final String PNG_BASE64 =
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

        private SeoImageFetcher fetcherUploadingTo(String uploadedUrl) {
            ImgOssUtils imgOssUtils = Mockito.mock(ImgOssUtils.class);
            Mockito.when(imgOssUtils.getAccessUrl()).thenReturn("https://img.example.com");
            FileUploadVo vo = new FileUploadVo();
            vo.setUrl(uploadedUrl);
            vo.setKey("img/seo/x.png");
            Mockito.when(imgOssUtils.uploadToImg(Mockito.anyString(), Mockito.any())).thenReturn(vo);
            // 与站内既有上传路径一致，转存后要过内容安全审核
            Mockito.when(imgOssUtils.imageModerationWithOptions(Mockito.anyString()))
                    .thenReturn(R.ok("检测通过", true));
            return new SeoImageFetcher(imgOssUtils);
        }

        @Test
        @DisplayName("base64 内联 PNG 被解码并上传，返回图床地址")
        void fetchAndUpload_base64Png_decodedAndUploaded() {
            SeoImageFetcher fetcher = fetcherUploadingTo("https://img.example.com/img/seo/x.png");

            String result = fetcher.fetchAndUpload("data:image/png;base64," + PNG_BASE64);

            assertEquals("https://img.example.com/img/seo/x.png", result);
        }

        @Test
        @DisplayName("非 base64 的 data URI 被拒——百分号编码的 SVG 不能落进图床")
        void fetchAndUpload_nonBase64DataUri_rejected() {
            SeoImageFetcher fetcher = fetcherUploadingTo("https://img.example.com/img/seo/x.png");

            SeoImageFetcher.ImageFetchException e = assertThrows(
                    SeoImageFetcher.ImageFetchException.class,
                    () -> fetcher.fetchAndUpload("data:image/svg+xml,%3Csvg onload%3Dalert(1)%3E"));

            assertTrue(e.getMessage().contains("base64"), e.getMessage());
        }

        @Test
        @DisplayName("白名单外的内联类型被拒，即使写成 base64")
        void fetchAndUpload_dataUriWithDisallowedMime_rejected() {
            SeoImageFetcher fetcher = fetcherUploadingTo("https://img.example.com/img/seo/x.png");

            assertThrows(SeoImageFetcher.ImageFetchException.class,
                    () -> fetcher.fetchAndUpload("data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg=="));
            assertThrows(SeoImageFetcher.ImageFetchException.class,
                    () -> fetcher.fetchAndUpload("data:image/svg+xml;base64,PHN2Zy8+"));
        }

        @Test
        @DisplayName("base64 内容损坏时给出可读原因，而不是抛 IllegalArgumentException")
        void fetchAndUpload_brokenBase64_reportsReadableReason() {
            SeoImageFetcher fetcher = fetcherUploadingTo("https://img.example.com/img/seo/x.png");

            SeoImageFetcher.ImageFetchException e = assertThrows(
                    SeoImageFetcher.ImageFetchException.class,
                    () -> fetcher.fetchAndUpload("data:image/png;base64,@@@不是base64@@@"));

            assertTrue(e.getMessage().contains("解码失败"), e.getMessage());
        }
    }

    @Nested
    @DisplayName("已托管地址短路")
    class AlreadyHosted {

        @Test
        @DisplayName("已是本站图床地址的原样返回，不做无意义的下载再上传")
        void fetchAndUpload_alreadyHosted_returnsAsIs() {
            SeoImageFetcher fetcher = fetcherWithAccessUrl("https://img.example.com");
            String url = "https://img.example.com/img/seo/abc.jpg";

            assertEquals(url, fetcher.fetchAndUpload(url));
        }

        @Test
        @DisplayName("图床域名未配置时不短路，走正常抓取校验")
        void fetchAndUpload_blankAccessUrl_doesNotShortCircuit() {
            SeoImageFetcher fetcher = fetcherWithAccessUrl("");

            // 走到协议校验被拒，说明没有因为 accessUrl 为空而误判成「已托管」
            assertThrows(SeoImageFetcher.ImageFetchException.class,
                    () -> fetcher.fetchAndUpload("file:///etc/passwd"));
        }
    }

    /*
     * 以下场景需要一个可控 HTTP 服务端（当前项目无集成测试基础设施），本层无法覆盖：
     *
     * 1. 302 跳转到 169.254.169.254 被逐跳校验拦下（自动跟随时最容易漏的一条）；
     * 2. 相对路径 Location（/a/b.jpg）能被正确解析而不是误判为非法地址；
     * 3. 跳转次数超过 MAX_IMAGE_REDIRECTS 时报错而非无限跟随；
     * 4. Content-Type 为 text/html 时被拒，image/jpeg 时落 .jpg 扩展名；
     * 5. 响应体超过 5MB 时按字节截断报错（Content-Length 撒谎也拦得住）；
     * 6. 连接/读取超时生效。
     */
}
