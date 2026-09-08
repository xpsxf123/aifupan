package com.jiuyu.replay.common.utils;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class HttpUtils {
    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url   发送请求的URL
     * @param param 请求参数,请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public String sendGet(String url, String param) {

        return sendGet(url, param, 10000, 15000);
    }

    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url   发送请求的URL
     * @param param 请求参数,请求参数应该是 name1=value1&name2=value2 的形式。
     * @param connectTime 连接超时时间，毫秒
     * @param readTime 读取超时时间，毫秒
     * @return URL 所代表远程资源的响应结果
     */
    public String sendGet(String url, String param, int connectTime, int readTime) {
        StringBuilder result = new StringBuilder();
        URLConnection connection = null;
        try {
            String urlNameString = url;
            if (param != null && !"".equals(param)) {
                urlNameString = url + "?" + param;
            }

            URL realUrl = new URL(urlNameString);
            connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 设置超时时间（单位：毫秒）
            connection.setConnectTimeout(connectTime);  // 连接超时 10 秒
            connection.setReadTimeout(readTime);    // 读取超时 15 秒
            // 建立实际的连接
            connection.connect();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
            }catch (Exception e) {
                log.error("发送GET请求处理数据流出现异常: URL={}, 参数={}, 异常={}", url, param, e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("发送GET请求出现异常: URL={}, 参数={}, 异常={}", url, param, e.getMessage(), e);
        } finally {
            // 关闭 HttpURLConnection 连接
            if(connection != null) {
                if (connection instanceof HttpURLConnection) {
                    ((HttpURLConnection) connection).disconnect();
                }
            }
        }
        return result.toString();
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url   发送请求的 URL
     * @param param 请求参数,请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    public static String requestOCRForHttp(String url, Map<String, String> requestParams, String filePathAndName) throws Exception {
        String result = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        /** HttpPost */
        HttpPost httpPost = new HttpPost(url);
        MultipartEntity reqEntity = new MultipartEntity(); // 建立多文件实例
        FileBody filebody = new FileBody(new File(filePathAndName));
        reqEntity.addPart("pic", filebody);// upload为请求后台的File upload;
        for (String key : requestParams.keySet()) {
            String value = requestParams.get(key);
            reqEntity.addPart(key, new StringBody(value, Charset.forName("utf-8")));
        }
        httpPost.setEntity(reqEntity); // 设置实体
        /** HttpResponse */
        CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
        try {
            HttpEntity httpEntity = httpResponse.getEntity();
            result = EntityUtils.toString(httpEntity, "utf-8");
            EntityUtils.consume(httpEntity);
        } finally {
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            } catch (IOException e) {

            }
        }
        return result;
    }

    public static String requestOCRForHttp(String url, Map<String, String> requestParams) throws Exception {
        String result = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        /** HttpPost */
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        Iterator<Map.Entry<String, String>> it = requestParams.entrySet().iterator();
//		System.out.println(params.toString());
        while (it.hasNext()) {
            Map.Entry<String, String> en = it.next();
            String key = en.getKey();
            String value = en.getValue();
            if (value != null) {
                params.add(new BasicNameValuePair(key, value));
            }
        }
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        /** HttpResponse */
        CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
        try {
            HttpEntity httpEntity = httpResponse.getEntity();
            result = EntityUtils.toString(httpEntity, "utf-8");
            EntityUtils.consume(httpEntity);
        } finally {
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            } catch (IOException e) {
            }
        }
        return result;
    }


    public static String sendHttpPost(String url, String JSONBody) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(JSONBody, "utf-8"));
        CloseableHttpResponse response = httpClient.execute(httpPost);
//		System.out.println(response.getStatusLine().getStatusCode() + "\n");
        HttpEntity entity = response.getEntity();
        String responseContent = EntityUtils.toString(entity, "UTF-8");
//		System.out.println(responseContent);
        response.close();
        httpClient.close();
        return responseContent;
    }

    /**
     * 发送带请求头的 JSON POST 请求
     *
     * @param url      请求地址
     * @param jsonBody JSON 请求体
     * @param headers  请求头
     * @return 响应内容
     * @throws Exception 请求异常
     */
    public String sendHttpPost(String url, String jsonBody, Map<String, String> headers) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            if (headers != null) {
                headers.forEach(httpPost::addHeader);
            }
            if (headers == null || !headers.containsKey("Content-Type")) {
                httpPost.addHeader("Content-Type", "application/json");
            }
            httpPost.setEntity(new StringEntity(jsonBody, "utf-8"));
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                return entity == null ? "" : EntityUtils.toString(entity, "UTF-8");
            }
        }
    }

    /**
     * 发送post请求,主体为json格式
     * @param url  路径
     * @param jsonObject  参数(json类型)
     * @param encoding 编码格式
     * @return
     * @throws IOException
     */
    public static String sendPOSTJSON(String url, Object jsonObject, String encoding) throws Exception, IOException{
        String body = "";

        //创建httpclient对象
        CloseableHttpClient client = HttpClients.createDefault();
        //创建post方式请求对象
        HttpPost httpPost = new HttpPost(url);

        //装填参数
        StringEntity s = new StringEntity(JSONObject.toJSONString(jsonObject), "utf-8");
        s.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
                "application/json"));
        //设置参数到请求对象中
        httpPost.setEntity(s);
//        System.out.println("请求地址："+url);
//        System.out.println("请求参数："+nvps.toString());

        //设置header信息
        //指定报文头【Content-type】、【User-Agent】
//        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

        //执行请求操作，并拿到结果（同步阻塞）
        CloseableHttpResponse response = client.execute(httpPost);
        //获取结果实体
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            //按指定编码转换结果实体为String类型
            body = EntityUtils.toString(entity, encoding);
        }
        EntityUtils.consume(entity);
        //释放链接
        response.close();
        return body;
    }
}
