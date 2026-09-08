package com.jiuyu.replay.video.project.service;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.LongByteArray;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * IP城市查询服务
 * <p>
 * 基于 ip2region xdb 离线库实现高性能 IP 地址解析
 * 采用 VectorIndex 模式，项目启动时加载 34MB xdb 文件到内存，查询性能 < 10μs
 *
 * @author RayChou
 * @date 2025-11-28
 * @description IP 地址解析服务，支持 IPv4 地址查询城市信息
 */
@Slf4j
@Service
public class IpCityService {

    /**
     * ip2region Searcher 实例（线程安全）
     */
    private Searcher searcher;

    /**
     * xdb 文件路径
     */
    private static final String XDB_FILE_PATH = "ipdata/ip2region_v4.xdb";

    /**
     * 项目启动时初始化 ip2region Searcher
     * 采用 Buffer 模式加载 xdb 文件到内存（支持 JAR 包运行）
     */
    @PostConstruct
    public void init() {
        try {
            log.info("开始加载 ip2region xdb 文件: {}", XDB_FILE_PATH);
            long startTime = System.currentTimeMillis();

            // 从 classpath 读取 xdb 文件流
            ClassPathResource resource = new ClassPathResource(XDB_FILE_PATH);
            InputStream inputStream = resource.getInputStream();

            // 读取文件内容到字节数组
            byte[] cBuff = inputStream.readAllBytes();
            inputStream.close();

            // 使用 Buffer 模式创建 Searcher（全内存加载，支持 JAR 包）
            LongByteArray longByteArray = new LongByteArray();
            longByteArray.append(cBuff);
            searcher = Searcher.newWithBuffer(Version.IPv4, longByteArray);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("ip2region xdb 文件加载成功，耗时: {} ms，文件大小: {} MB",
                    costTime, cBuff.length / 1024.0 / 1024.0);
        } catch (IOException e) {
            log.error("加载 ip2region xdb 文件失败: {}", XDB_FILE_PATH, e);
            throw new RuntimeException("IP 地址库初始化失败", e);
        }
    }

    /**
     * 项目关闭时释放资源
     */
    @PreDestroy
    public void destroy() {
        if (searcher != null) {
            try {
                searcher.close();
                log.info("ip2region Searcher 资源已释放");
            } catch (IOException e) {
                log.error("关闭 ip2region Searcher 失败", e);
            }
        }
    }

    /**
     * 根据IP获取城市
     *
     * @param ip IP地址
     * @return 城市名称
     */
    public String getCityByIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            log.warn("IP地址为空，无法查询城市");
            return null;
        }
        // 1. 从 ip2region 查询城市
        return queryCityFromIp2Region(ip);
    }

    /**
     * 从 ip2region 查询城市
     *
     * @param ip IP地址
     * @return 城市名称
     */
    private String queryCityFromIp2Region(String ip) {
        if (searcher == null) {
            log.error("ip2region Searcher 未初始化");
            return null;
        }

        try {
            // 查询 IP 地址信息
            // 返回格式: 国家|省份|城市|ISP (4段)
            // 示例1: 中国|四川省|成都市|电信
            // 示例2: 日本|东京都|东京|0
            String region = searcher.search(ip);

            if (StrUtil.isBlank(region)) {
                log.warn("IP {} 未查询到地址信息", ip);
                return null;
            }

            // 解析城市信息（取第3段，索引为2）
            String[] parts = region.split("\\|");
            if (parts.length >= 3) {
                String city = parts[2]; // 城市（第3段）

                // 去除"市"后缀，统一城市名称格式
                if (StrUtil.isNotBlank(city) && !city.equals("0")) {
                    log.debug("IP {} 解析城市: {}, 完整信息: {}", ip, city, region);
                    return city;
                }
            }

            // 如果城市为0或解析失败，尝试使用省份（第2段）
            if (parts.length >= 2) {
                String province = parts[1];
                if (StrUtil.isNotBlank(province) && !province.equals("0")) {
                    province = province.replace("省", "").replace("市", "");
                    log.debug("IP {} 城市为空，使用省份: {}, 完整信息: {}", ip, province, region);
                    return province;
                }
            }

            log.warn("IP {} 解析城市失败，原始数据: {}", ip, region);
            return null;
        } catch (Exception e) {
            log.error("查询 IP {} 地址信息失败", ip, e);
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        // 1、使用上述的 version 和 dbPath 创建 searcher 对象
        // 如果是 IPv4: 设置 xdb 路径为 v4 的 xdb 文件，IP版本指定为 Version.IPv4
        final String dbPath = "D:/ip2region_v4.xdb";  // 或者你的 ipv4 xdb 的路径
        final Version version = Version.IPv4;
        Searcher searcher = null;
        try {
            searcher = Searcher.newWithFileOnly(version, dbPath);
        } catch (IOException e) {
            System.out.printf("failed to create searcher with `%s`: %s\n", dbPath, e);
            return;
        }
        String ip = "110.185.174.118";
        // 2、查询，IPv4 或者 IPv6 的地址都支持
        try {
            // ip = "2001:4:112:ffff:ffff:ffff:ffff:ffff";  // IPv6
            long sTime = System.nanoTime();
            String region = searcher.search(ip);
            long cost = TimeUnit.NANOSECONDS.toMicros((long) (System.nanoTime() - sTime));
            System.out.printf("{region: %s, ioCount: %d, took: %d μs}\n", region, searcher.getIOCount(), cost);
        } catch (Exception e) {
            System.out.printf("failed to search(%s): %s\n", ip, e);
        }

        // 3、关闭资源
        searcher.close();

        // 备注：并发使用，每个线程需要创建一个独立的 searcher 对象单独使用。
    }
}

