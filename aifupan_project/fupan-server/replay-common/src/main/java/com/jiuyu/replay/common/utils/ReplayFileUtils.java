package com.jiuyu.replay.common.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 关于文件的util
 */
@Slf4j
public class ReplayFileUtils {

    /**
     * 保存文件
     * @param pathPrefix
     * @param file
     * @return
     * @throws IOException
     */
    public static String saveUploadSocketFile(String pathPrefix, MultipartFile file) throws IOException {
        // 获取当前日期，格式为 yyyy/MM/dd
        String datePath = DateUtil.format(DateUtil.date(), "/yyyy/MM/dd/");

        // 创建日期文件夹路径
        String fullPath = pathPrefix + datePath;

        // 创建文件夹（如果文件夹不存在）
        File directory = new File(fullPath);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                log.error("文件夹创建失败，path={}", directory);
                RRException.create("文件夹创建失败");
            }
        }

        // 获取上传的文件名
        String uuidName = UUID.randomUUID() + ".zip"; // 生成 UUID 文件名

        // 创建最终文件路径
        compressFileToNewZip(file, fullPath + uuidName);
        return datePath + uuidName;  // 返回文件的相对路径
    }


    /**
     * 将 MultipartFile 文件直接压缩到一个新的 zip 文件中
     *
     * @param file 上传的文件
     * @param zipFile 输出的 zip 文件路径
     * @throws IOException
     */
    public static void compressFileToNewZip(MultipartFile file, String zipFile) throws IOException {
        // 创建输出文件流，指向目标 zip 文件
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // 创建一个新的 ZipEntry，表示要压缩的文件
            ZipEntry zipEntry = new ZipEntry(Objects.requireNonNull(file.getOriginalFilename()));
            zos.putNextEntry(zipEntry);

            // 获取 MultipartFile 的输入流
            try (InputStream is = file.getInputStream()) {
                byte[] buffer = new byte[1024*2];
                int length;
                // 将文件内容从 MultipartFile 输入流写入 zip 输出流
                while ((length = is.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
            }

            // 完成当前的 zip 条目
            zos.closeEntry();
        }
    }

    /**
     * 获取文件的扩展名
     */
    private static String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return "";  // 如果没有扩展名，则返回空字符串
    }


    /**
     * 一次性读取 zip 文件中的第一个 .txt 文件的全部内容
     * @param zipFilePath
     * @return
     * @throws IOException
     */
    public static String readFirstTxtFromZip(String zipFilePath) {
        // 位置为空和文件不存在直接返回空
        if (zipFilePath == null || !FileUtil.exist(zipFilePath)) return "";
        // 创建文件输入流和 ZipInputStream
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(zipFilePath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        ZipInputStream zis = new ZipInputStream(fis);

        ZipEntry entry;
        StringBuilder contentBuilder = new StringBuilder(); // 用于存储文件内容

        try {
            // 遍历 zip 中的每个条目
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".txt")) {
                    // 找到第一个 .txt 文件，读取文件内容
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        contentBuilder.append(line).append(System.lineSeparator()); // 将每行内容加入 StringBuilder
                    }

                    zis.closeEntry();
                    break; // 只读取第一个 .txt 文件，跳出循环
                }
            }
        }catch (Exception e){
            log.error("报错，报错={}", e.getMessage());
        }finally {
            try {
                zis.close();
            } catch (IOException e) {
                log.error("关闭ZipInputStream报错");
            }
            try {
                fis.close();
            } catch (IOException e) {
                log.error("关闭FileInputStream报错");
            }
        }
        // 如果有内容返回，否则返回 null
        return !contentBuilder.isEmpty() ? contentBuilder.toString() : "";
    }

    /**
     * 将字符串直接压缩为 zip 包
     * @param text
     * @param fileName
     * @param zipFilePath
     * @throws IOException
     */
    public static void zipStringToFile(String text, String fileName, String zipFilePath) throws IOException {
        FileUtil.mkParentDirs(zipFilePath);
        // 创建输出 zip 文件
        FileOutputStream fos = new FileOutputStream(zipFilePath);
        ZipOutputStream zos = new ZipOutputStream(fos);

        // 将字符串转换为字节数组
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

        // 创建一个 zip 条目
        zos.putNextEntry(new ZipEntry(fileName)); // 在 zip 包中命名该文件为 example.txt

        // 写入字节数组到 zip 文件中
        zos.write(textBytes);

        // 关闭条目和流
        zos.closeEntry();
        zos.close();
    }

    /**
     * 创建一个ZIP文件流
     * @param txtContent
     * @param txtFileName
     * @return
     * @throws IOException
     */
    public static ByteArrayOutputStream createZipStream(String txtContent, String txtFileName) throws IOException {
        // 创建一个ByteArrayOutputStream来存储ZIP文件的内容
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
            // 创建一个新的ZIP条目
            ZipEntry zipEntry = new ZipEntry(txtFileName);
            zipOut.putNextEntry(zipEntry);

            // 将TXT文件内容写入ZIP条目
            zipOut.write(txtContent.getBytes());

            // 关闭当前条目
            zipOut.closeEntry();
        }
        return baos;
    }

    /**
     * 根据本地文件路径获取压缩包里面的分析数据
     * @param filepath 文件路径
     * @return
     */
    public static String getFileContent(String filepath) {

        String content = "";
        if (FileUtil.exist(filepath)) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(filepath))) {

                // 遍历 ZIP 文件中的条目
                while ((zis.getNextEntry()) != null) {

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, length);
                    }
                    content = baos.toString(StandardCharsets.UTF_8.name());

                    zis.closeEntry();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return content;
    }

    /**
     * 通过下载地址下载zip文件，并获取压缩包里面的文本数据
     * @param downloadUrl zip下载地址
     * @return
     */
    public static String getFileContentByZipDownloadUrl(String downloadUrl) {

        StringBuilder content = new StringBuilder("");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .build();

        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {

                try (InputStream inputStream = response.body();
                     ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

                    ZipEntry entry;
                    while ((entry = zipInputStream.getNextEntry()) != null) {
                        if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".txt")) {
                            // 读取每个TXT文件的内容
                            ByteArrayOutputStream contentBytes = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = zipInputStream.read(buffer)) != -1) {
                                contentBytes.write(buffer, 0, length);
                            }

                            content.append(contentBytes.toString(StandardCharsets.UTF_8.name()));
                        }
                        zipInputStream.closeEntry();
                    }
                }catch (Exception e) {
                    log.info("解析zip文件失败");
                    e.printStackTrace();
                }
            } else {
                log.info("通过下载地址下载zip文件，并获取压缩包里面的文本数据请求失败");
            }
        }catch (Exception e) {
            log.info("通过下载地址下载zip文件，并获取压缩包里面的文本数据请求失败");
            e.printStackTrace();
        }


        return content.toString();

    }

    /**
     * 从 ZIP 包中读取第一个 TXT 文件的内容，并将其按行存储到 List<String> 中
     *
     * @param multipartFile 上传的 ZIP 文件
     * @return 包含 TXT 文件每行内容的 List<String>
     * @throws IOException 如果读取文件失败
     */
    public static List<String> readTxtFileContentFromZip(MultipartFile multipartFile){
        List<String> lines = new ArrayList<>();

        // 将 MultipartFile 转换为 InputStream
        try (InputStream inputStream = multipartFile.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

            ZipEntry zipEntry;
            // 遍历 ZIP 包中的每个文件
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                // 检查文件是否是 TXT 文件
                if (zipEntry.getName().toLowerCase().endsWith(".txt")) {
                    // 读取 TXT 文件内容
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            lines.add(line); // 将每一行内容添加到 List
                        }
                    }
                    // 找到第一个 TXT 文件后直接返回
                    return lines;
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            log.info("读取文件失败");
        }

        // 如果没有找到 TXT 文件，返回空列表或抛出异常
        return lines; // 或者 throw new IllegalArgumentException("ZIP 包中没有 TXT 文件");
    }

}
