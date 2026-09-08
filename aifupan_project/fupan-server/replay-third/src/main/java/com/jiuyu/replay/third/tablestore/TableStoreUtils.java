package com.jiuyu.replay.third.tablestore;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alicloud.openservices.tablestore.model.ColumnType;
import com.alicloud.openservices.tablestore.model.ColumnValue;
import com.alicloud.openservices.tablestore.model.filter.*;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.third.tablestore.annotation.TableStoreSaveAnnotation;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/17 上午10:21
 */
public class TableStoreUtils {

    /**
     * 获取过滤器
     * <p>params参数，最外层的typekey支持 AND、OR、NOT</p>
     *
     * @param params
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> Filter setFilter(Map<String, Object> params, Class<T> clazz) {
        if (params != null && !params.isEmpty() && params.containsKey("value")) {
            RRException.isNotEmpty(params.get("type"), "逻辑操作符不能为空");
            if (Arrays.asList("NOT", "AND", "OR").contains(params.get("type").toString())) {
                return getCompositeColumnValueFilter(params, clazz);
            } else {
                RRException.create("最外层不支持 NOT、OR、AND之外的操作");
            }
        }
        return null;
    }

    /**
     * 获取组合列值过滤器
     *
     * @param params
     * @param clazz
     * @return
     */
    public static <T> ColumnValueFilter getCompositeColumnValueFilter(Map<String, Object> params, Class<T> clazz) {

        Object o = params.get("type");
        RRException.isNotEmpty(o, "逻辑操作符不能为空");
        CompositeColumnValueFilter result = new CompositeColumnValueFilter(getLogicOperator((String) o));
        Object v = params.get("value");
        if (ObjectUtil.isNotEmpty(v) && v instanceof List<?>) {
            List<Map<String, Object>> valueList = (List<Map<String, Object>>) v;
            if (ObjectUtil.isNotEmpty(valueList) && valueList.size() == 1){
                Map<String, Object> objectMap = valueList.get(0);
                RRException.isNotEmpty(objectMap.get("type"), "逻辑操作符不能为空");
                switch ((String) objectMap.get("type")) {
                    case "=", "!=", ">", ">=", "<", "<=": {
                        TableStoreSaveAnnotation fieldType = getFieldType(objectMap.get("name").toString(), clazz);
                        return getSingleColumnValueFilter(
                                fieldType.name(),
                                objectMap.get("value"),
                                objectMap.get("type").toString(),
                                fieldType.type()
                        );
                    }
                    case "LIKE": {
                        TableStoreSaveAnnotation fieldType = getFieldType(objectMap.get("name").toString(), clazz);
                        return getSingleColumnValueRegexFilter(fieldType.name(), objectMap.get("value").toString(), fieldType.type());
                    }
                    default: {
                        RRException.create("不支持的操作符");
                    }
                }
            }
            for (Map<String, Object> objectMap : valueList) {
                Object o1 = objectMap.get("type");
                RRException.isNotEmpty(o1, "逻辑操作符不能为空");
                switch ((String) o1) {
                    case "NOT", "AND", "OR": {
                        // 递归获取之后的条件
                        result.addFilter(getCompositeColumnValueFilter(objectMap, clazz));
                        break;
                    }
                    case "=", "!=", ">", ">=", "<", "<=": {
                        TableStoreSaveAnnotation fieldType = getFieldType(objectMap.get("name").toString(), clazz);
                        result.addFilter(getSingleColumnValueFilter(
                                fieldType.name(),
                                objectMap.get("value"),
                                objectMap.get("type").toString(),
                                fieldType.type()
                        ));
                        break;
                    }
                    case "LIKE": {
                        TableStoreSaveAnnotation fieldType = getFieldType(objectMap.get("name").toString(), clazz);
                        result.addFilter(getSingleColumnValueRegexFilter(fieldType.name(), objectMap.get("value").toString(), fieldType.type()));
                        break;
                    }
                    default: {
                        RRException.create("不支持的操作符");
                    }
                }
            }
        }
        return result;
    }


    /**
     * 获取单列值过滤器
     *
     * @param name
     * @param value
     * @param type
     * @param columnValue
     * @return
     */
    public static SingleColumnValueFilter getSingleColumnValueFilter(String name, Object value, String type, ColumnType columnValue) {
        RRException.isNotEmpty(name, "name不能为空");
        RRException.isNotEmpty(value, "value不能为空");
        RRException.isNotEmpty(type, "type不能为空");
        return new SingleColumnValueFilter(name, getCompareOperator(type), new ColumnValue(value, columnValue));
    }

    /**
     * 获取单列值正则过滤器
     *
     * @param name
     * @param value
     * @param columnValue
     * @return
     */
    public static SingleColumnValueRegexFilter getSingleColumnValueRegexFilter(String name, String value, ColumnType columnValue) {
        RRException.isNotEmpty(name, "name不能为空");
        RRException.isNotEmpty(value, "value不能为空");
        return new SingleColumnValueRegexFilter(name, new RegexRule(value, getcastType(columnValue)), SingleColumnValueRegexFilter.CompareOperator.EQUAL);
    }

    /**
     * 获取列值类型
     *
     * @param name
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> TableStoreSaveAnnotation getFieldType(String name, Class<T> clazz) {

        RRException.isNotEmpty(name, "字段名称不能为空");

        Field[] fields = ReflectUtil.getFields(clazz);
        // 遍历字段
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                // 判断字段是否有 TableField 注解
                if (field.isAnnotationPresent(TableStoreSaveAnnotation.class)) {
                    // 获取注解实例
                    return field.getAnnotation(TableStoreSaveAnnotation.class);
                }
            }
        }

        RRException.create("字段类型不能为空");

        return null;
    }

    /**
     * 获取正则类型
     *
     * @param columnValue
     * @return
     */
    public static RegexRule.CastType getcastType(ColumnType columnValue) {
        RegexRule.CastType castType = switch (columnValue) {
            case INTEGER -> RegexRule.CastType.VT_INTEGER;
            case STRING -> RegexRule.CastType.VT_STRING;
            case DOUBLE -> RegexRule.CastType.VT_DOUBLE;
            default -> null;
        };
        RRException.isNotEmpty(castType, "castType非法");
        return castType;
    }

    /**
     * 获取比较操作符类型
     *
     * @param compareOperator
     * @return
     */
    public static SingleColumnValueFilter.CompareOperator getCompareOperator(String compareOperator) {
        RRException.isNotEmpty(compareOperator, "比较操作符不能为空");
        SingleColumnValueFilter.CompareOperator compareOperator1 = switch (compareOperator) {
            case "=" -> SingleColumnValueFilter.CompareOperator.EQUAL;
            case "!=" -> SingleColumnValueFilter.CompareOperator.NOT_EQUAL;
            case ">" -> SingleColumnValueFilter.CompareOperator.GREATER_THAN;
            case ">=" -> SingleColumnValueFilter.CompareOperator.GREATER_EQUAL;
            case "<" -> SingleColumnValueFilter.CompareOperator.LESS_THAN;
            case "<=" -> SingleColumnValueFilter.CompareOperator.LESS_EQUAL;
            default -> null;
        };
        RRException.isNotEmpty(compareOperator1, "比较操作符非法");
        return compareOperator1;
    }

    /**
     * 获取逻辑操作符类型
     *
     * @param logicOperator
     * @return
     */
    public static CompositeColumnValueFilter.LogicOperator getLogicOperator(String logicOperator) {
        RRException.isNotEmpty(logicOperator, "逻辑操作符不能为空");
        return switch (logicOperator) {
            case "NOT" -> CompositeColumnValueFilter.LogicOperator.NOT;
            case "OR" -> CompositeColumnValueFilter.LogicOperator.OR;
            default -> CompositeColumnValueFilter.LogicOperator.AND;
        };
    }

}