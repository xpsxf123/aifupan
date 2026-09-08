package com.jiuyu.replay.common.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 普通的utils
 */
@Slf4j
public class CommonUtils {


    /**
     * 提取字符串中的所有数字并将其连接成一个整数（int类型）。
     *
     * @param input 输入的字符串
     * @return 连接后的整数
     */
    public static long extractAndCombineNumbers(String input) {
        // 正则表达式匹配连续的数字
        String regex = "\\d+";  // 匹配一个或多个数字

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        StringBuilder result = new StringBuilder();

        // 查找所有匹配的数字并将其连接成一个字符串
        while (matcher.find()) {
            result.append(matcher.group());
        }

        // 将结果转换为int类型
        return Long.parseLong(result.toString());
    }

    /**
     * 把string中的万+去掉
     * @param str
     * @return
     */
    public static String removeWanAdd(String str) {
        // 如果是纯数字，直接返回字符串
        if (str.matches("\\d+\\.\\d+|\\d+")) {
            return String.valueOf((int) Double.parseDouble(str));  // 转为整数并返回字符串
        }

        // 如果包含"万+"，去掉"万+"并处理小数
        if (str.endsWith("万+")) {
            String numberPart = str.substring(0, str.length() - 2);  // 去掉"万+"
            try {
                // 处理可能的浮动小数
                double number = Double.parseDouble(numberPart);
                return String.valueOf((int) (number * 10000));  // 乘以10000并转为整数再转换成字符串
            } catch (NumberFormatException e) {
                System.out.println("错误: 无法解析数字 " + numberPart);
                return "0";  // 解析错误时返回"0"
            }
        }

        // 其他情况，直接返回"0"（如果需要，可以进行进一步的处理）
        return str;
    }

    /**
     * 获取异常的堆栈信息
     * @param e
     * @return
     */
    public static String getExceptionStack(Exception e) {
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        StringBuilder result = new StringBuilder(e.toString()).append("\n");
        for (StackTraceElement element : stackTraceElements) {
            result.append("\tat  ").append(element).append("\n");
        }
        return result.toString();
    }

    /**
     * 转化场观人数
     * @param observationNum 观看人数
     * @param onlineNum 在线人数
     * @return 转化后的场观人数
     */
    public static int conversionObservationNum(Integer observationNum, Integer onlineNum) {
        if (observationNum == null){
            return -1;
        }

        // 如果在线人数比观看人数多，则返回观看人数
        if (onlineNum != null && onlineNum > observationNum) {
            return observationNum;
        }

        if (observationNum <= 0){
            return observationNum;
        }
        return (int) (observationNum * 0.4);
    }

    /**
     * 处理站位符 - 站位符格式：#{xxx}
     * @param str
     * @param params
     * @return
     */
    public static String placeholderHandle(String str, Map<String, String> params){
        Set<String> replaceList = params.keySet();

        if (ObjectUtil.isNotEmpty(replaceList)){
            for (String replace : replaceList) {
                if (str.contains(StrUtil.format("#{{}}", replace))) {
                    String orDefault = params.getOrDefault(replace, "");
                    str = str.replaceAll(StrUtil.format("#\\{{}}", replace), orDefault);
                }
            }
        }
        return str;
    }

    /**
     * 处理下次更新时间，
     * 超过2点设置为明天的凌晨2点
     * 不超过2点设置为当前时间的2点
     * @param currentDate
     * @return
     */
    public static Date getResourceUpdateTime(Date currentDate){
        if (currentDate == null) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        calendar.add(Calendar.HOUR_OF_DAY, 1);
        // 设置时间为2点
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    /**
     * 测量方法调用后使用的时间
     *
     * @param supplier 方法
     * @param msg      说明
     * @param values   变量
     * @param <T>      返回值类型
     * @return 原方法的返回值
     */
    public static <T> T measureTime(Supplier<T> supplier, String msg, Object... values) {
        // 执行业务逻辑
        long start = System.currentTimeMillis();
        try {
            T res = supplier.get();
            return res;
        } finally {
            calculationTime(start, msg, values);
        }
    }

    /**
     * 测量无返回值方法的执行时间
     *
     * @param runnable 方法
     * @param msg      说明
     * @param values   变量
     */
    public static void measureTime(Runnable runnable, String msg, Object... values) {
        long start = System.currentTimeMillis();
        try {
            runnable.run();
        } finally {
            calculationTime(start, msg, values);
        }
    }

    /**
     * 计算时间
     *
     * @param start  开始时间
     * @param msg    说明
     * @param values 变量
     */
    private static void calculationTime(long start, String msg, Object... values) {
        List<Object> list = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(values)) {
            list.add(values);
        }
        long s = System.currentTimeMillis() - start;
        list.add(s);
        log.info(StrUtil.format((msg == null ? "" : msg + "__") + "耗时：{} ms", list.toArray()));
    }

    /**
     * 根据redis中的key把value转成对应的list
     *
     * @param redisTemplate redis模版
     * @param key           建
     * @param clazz         转换类型
     * @param <T>           Object
     * @return 转换后的值
     */
    public static <T> List<T> redisDataToList(RedisTemplate redisTemplate, String key, Class<T> clazz) {
        // 查询redis
        List<T> res = null;
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj != null) {
            boolean flag = true;
            try {
                res = JSONArray.parseArray(obj.toString(), clazz);
            } catch (Exception e) {
                flag = false;
            }
            if (flag) {
                return res;
            }
        }
        return null;
    }

    /**
     * 根据redis中的key把value转成对应的Object
     *
     * @param redisTemplate redis模版
     * @param key           建
     * @param clazz         转换类型
     * @param <T>           Object
     * @return 转换后的值
     */
    public static <T> T redisDataToObject(RedisTemplate redisTemplate, String key, Class<T> clazz) {
        // 查询redis
        T res = null;
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj != null) {
            boolean flag = true;
            try {
                res = JSONObject.parseObject(obj.toString(), clazz);
            } catch (Exception e) {
                flag = false;
            }
            if (flag) {
                return res;
            }
        }
        return null;
    }

    /**
     * 将对象转换为字符串
     * 基本类型/包装类/String：直接返回toString
     * 对象：字段1=xxx&字段2=xxx
     * 数组/集合：字段1=xxx&字段2=xxx__字段1=xxx&字段2=xxx
     *
     * @param obj 待转换对象
     * @return 转换后的字符串
     */
    public static String objectToString(Object obj) {
        if (obj == null) {
            return "";
        }
        if (isSimpleType(obj)) {
            return obj.toString();
        }
        if (obj.getClass().isArray()) {
            return arrayToString(obj);
        }
        if (obj instanceof Collection<?> collection) {
            return collection.stream()
                    .map(CommonUtils::singleObjectToString)
                    .collect(java.util.stream.Collectors.joining("__"));
        }
        return singleObjectToString(obj);
    }

    private static boolean isSimpleType(Object obj) {
        return obj instanceof Number
                || obj instanceof CharSequence
                || obj instanceof Boolean
                || obj instanceof Character
                || obj instanceof Date;
    }

    private static String arrayToString(Object array) {
        int length = Array.getLength(array);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            list.add(objectToString(Array.get(array, i)));
        }
        return String.join("__", list);
    }

    private static String singleObjectToString(Object obj) {
        if (obj == null) {
            return "";
        }
        if (isSimpleType(obj)) {
            return obj.toString();
        }
        java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
        return Arrays.stream(fields)
                .filter(f -> !java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                .sorted(Comparator.comparing(java.lang.reflect.Field::getName))
                .map(f -> {
                    f.setAccessible(true);
                    try {
                        Object value = f.get(obj);
                        return f.getName() + "=" + (value == null ? "" : value);
                    } catch (IllegalAccessException e) {
                        return f.getName() + "=";
                    }
                })
                .collect(java.util.stream.Collectors.joining("&"));
    }

    /**
     * 手机号脱敏处理，将中间4位替换为****
     * 例如：13812345678 -> 138****5678
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

}
