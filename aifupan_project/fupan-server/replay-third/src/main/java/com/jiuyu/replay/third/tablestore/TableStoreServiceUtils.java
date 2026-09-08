package com.jiuyu.replay.third.tablestore;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.*;
import com.alicloud.openservices.tablestore.model.sql.SQLQueryRequest;
import com.alicloud.openservices.tablestore.model.sql.SQLQueryResponse;
import com.alicloud.openservices.tablestore.model.sql.SQLResultSet;
import com.alicloud.openservices.tablestore.model.sql.SQLRow;
import com.jiuyu.replay.third.tablestore.annotation.TableStoreSaveAnnotation;
import com.jiuyu.replay.third.tablestore.entity.AnchorUserBo;
import com.jiuyu.replay.third.tablestore.entity.PrimaryKeyEntity;
import com.jiuyu.replay.third.tablestore.entity.RangeBase;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：处理表格存储的服务
 * @date ：2025/1/13 下午2:58
 */
public class TableStoreServiceUtils {

    /**
     * 批量保存
     *
     * @param list
     * @param tableName
     * @param syncClient
     * @param <T>
     * @return
     */
    public static <T> boolean saveBatch(List<T> list, String tableName, SyncClient syncClient) {
        // 创建批量写入请求
        BatchWriteRowRequest batchWriteRowRequest = new BatchWriteRowRequest();

        // 添加多行数据
        for (int i = 0; i < list.size(); i++) {
            T item = list.get(i);

            // 获取对象的 Class 对象
            Class<?> clazz = item.getClass();
            // 使用 Hutool 获取所有字段
            Field[] fields = ReflectUtil.getFields(clazz);

            // 定义属性列
            List<Column> columns = new ArrayList<>();
            // 创建主键列
            List<PrimaryKeyColumn> primaryKeyColumns = new ArrayList<>();
            // 遍历字段
            for (Field field : fields) {
                // 判断字段是否有 TableField 注解
                if (field.isAnnotationPresent(TableStoreSaveAnnotation.class)) {
                    // 获取注解实例
                    TableStoreSaveAnnotation tableField = field.getAnnotation(TableStoreSaveAnnotation.class);
                    if (tableField.isHasTableStore()) {
                        // 使用 Hutool 获取字段值
                        Object value = ReflectUtil.getFieldValue(item, field);
                        if (ObjectUtil.isEmpty(value)) continue;
                        // 添加索引
                        if (tableField.isIndex()) {
                            primaryKeyColumns.add(new PrimaryKeyColumn(tableField.name(), PrimaryKeyValue.fromColumn(new ColumnValue(value, tableField.type()))));
                        } else {
                            // 添加字段-value
                            columns.add(new Column(tableField.name(), new ColumnValue(value, tableField.type())));
                        }
                    }
                }
            }

            // 创建主键
            PrimaryKey primaryKey = new PrimaryKey(primaryKeyColumns);
            // 创建行数据
            RowPutChange rowPutChange = new RowPutChange(tableName, primaryKey);
            rowPutChange.addColumns(columns);
            // 添加到批量写入请求
            batchWriteRowRequest.addRowChange(rowPutChange);
        }

        // 执行批量写入
        BatchWriteRowResponse response = syncClient.batchWriteRow(batchWriteRowRequest);

        // 检查写入结果
        if (!response.isAllSucceed()) {
            System.out.println("部分行写入失败，失败的行信息如下：");
            for (BatchWriteRowResponse.RowResult rowResult : response.getFailedRows()) {
                System.out.println("失败的行: " + rowResult.getRow());
                System.out.println("失败原因: " + rowResult.getError());
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * 根据昵称批量查询
     *
     * @param nickNameList
     * @param tenantId
     * @param secUid
     * @param tableName
     * @param syncClient
     * @return
     * @throws Exception
     */
    public static List<AnchorUserBo> queryAnchorUser(List<String> nickNameList, Long tenantId, String secUid, String tableName, SyncClient syncClient) throws Exception {
        ArrayList<AnchorUserBo> result = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(nickNameList)) {
            List<List<String>> partition = ListUtil.partition(nickNameList, 99);
            for (List<String> partitionList : partition) {
                List<AnchorUserBo> anchorUserBoList = partitionList.stream().map(item -> {
                    AnchorUserBo userBo = new AnchorUserBo();
                    userBo.setTenantId(tenantId);
                    userBo.setSecUid(secUid);
                    userBo.setNickName(item);
                    return userBo;
                }).toList();
                List<Row> rows = queryBatchGetRow(anchorUserBoList, tableName, syncClient);
                if (ObjectUtil.isNotEmpty(rows)) {
                    List<AnchorUserBo> objects = rowToObject(rows, AnchorUserBo.class);
                    result.addAll(objects);
                }
            }
        }
        return result;
    }

    /**
     * 根据条件批量查询--当前只能查询主键字段
     *
     * @param list
     * @param tableName
     * @param syncClient
     * @param <T>
     * @return
     */
    public static <T> List<Row> queryBatchGetRow(List<T> list, String tableName, SyncClient syncClient) {
        // 创建批量查询请求
        BatchGetRowRequest batchRequest = new BatchGetRowRequest();

        MultiRowQueryCriteria criteria = new MultiRowQueryCriteria(tableName);

        ArrayList<PrimaryKey> primaryKeys = new ArrayList<>();
        // 遍历 userid 列表
        for (T item : list) {
            Field[] fields = ReflectUtil.getFields(item.getClass());
            // 遍历字段
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            for (Field field : fields) {
                // 判断字段是否有 TableField 注解
                if (field.isAnnotationPresent(TableStoreSaveAnnotation.class)) {
                    // 获取注解实例
                    TableStoreSaveAnnotation tableField = field.getAnnotation(TableStoreSaveAnnotation.class);
                    Object value = ReflectUtil.getFieldValue(item, field);

                    if (tableField.isHasTableStore() && ObjectUtil.isNotEmpty(value)) {
                        // 定义主键
                        if (tableField.isIndex() && ObjectUtil.isNotEmpty(value)) {
                            primaryKeyBuilder.addPrimaryKeyColumn(tableField.name(), PrimaryKeyValue.fromColumn(new ColumnValue(value, tableField.type())));
                        }

                        // TODO 不是主键单前不支持查询
                    }
                }
            }
            primaryKeys.add(primaryKeyBuilder.build());
        }
        criteria.setRowKeys(primaryKeys);

        // 设置返回的最大版本数
        criteria.setMaxVersions(1); // 每行最多返回 1 个版本
        // 添加到批量查询请求
        batchRequest.addMultiRowQueryCriteria(criteria);

        // 执行异步批量查询
        //批量读取的所有行采用相同的参数条件，例如ColumnsToGet=[colA]，则要读取的所有行都只读取colA列。
        //
        //由于批量读取可能存在部分行失败的情况，失败行的错误信息在返回的BatchGetRowResponse中，但并不抛出异常。因此调用BatchGetRow接口时，需要检查返回值，可通过BatchGetRowResponse的isAllSucceed方法判断是否所有行都获取成功；通过BatchGetRowResponse的getFailedRows方法获取失败行的信息。
        //
        //BatchGetRow操作单次支持读取的最大行数为100行。
        BatchGetRowResponse batchResponse = syncClient.batchGetRow(batchRequest);

        // 收集结果
        List<Row> result = new ArrayList<>();
        for (BatchGetRowResponse.RowResult rowResult : batchResponse.getSucceedRows()) {
            if (rowResult.isSucceed() && rowResult.getRow() != null && !rowResult.getRow().isEmpty()) {
                result.add(rowResult.getRow());
            }
        }

        return result;
    }

    /**
     * 根据条件批量查询
     *
     * @param base         查询数量
     * @param startKeyList 左边的条件
     * @param endKeyList   右边的条件
     * @param params       查询条件
     * @param clazz        查询对象
     * @param tableName    表名
     * @param syncClient   客户端
     * @param <T>
     * @return 查询结果， 第一个参数是否还有更多数据，第二个参数是查询到的数据
     */
    public static <T> Object[] queryRange(RangeBase base,
                                          List<PrimaryKeyEntity> startKeyList, List<PrimaryKeyEntity> endKeyList,
                                          Map<String, Object> params, Class<T> clazz,
                                          String tableName, SyncClient syncClient) throws Exception {

        RangeRowQueryCriteria criteria = new RangeRowQueryCriteria(tableName);

        // 添加索引条件
        if (ObjectUtil.isNotEmpty(startKeyList) || ObjectUtil.isNotEmpty(endKeyList)){
            List<List<PrimaryKeyEntity>> list = Arrays.asList(startKeyList, endKeyList);
            for (int i = 0; i < list.size(); i++) {
                List<PrimaryKeyEntity> item = list.get(i);
                if (ObjectUtil.isNotEmpty(item)){
                    // 构造分区键
                    PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
                    for (PrimaryKeyEntity key : item) {
                        TableStoreSaveAnnotation fieldType = TableStoreUtils.getFieldType(key.getName(), clazz);
                        primaryKeyBuilder.addPrimaryKeyColumn(fieldType.name(), PrimaryKeyValue.fromColumn(new ColumnValue(key.getValue(), fieldType.type()))); // 分区键
                    }
                    PrimaryKey startPrimaryKey = primaryKeyBuilder.build();

                    // i == 0是左边索引，i == 1是右边索引
                    if (i == 0){
                        criteria.setInclusiveStartPrimaryKey(startPrimaryKey);
                    }else{
                        criteria.setExclusiveEndPrimaryKey(startPrimaryKey);
                    }
                }
            }
        }

        // 添加查询条件
        if (ObjectUtil.isNotEmpty(params)) {
            criteria.setFilter(TableStoreUtils.setFilter(params, clazz));
        }

        // 设置查询方向 FORWARD 或 BACKWARD
        criteria.setDirection(base.getQueryType() == 0 ? Direction.BACKWARD : Direction.FORWARD);

        // 设置查询数量
        criteria.setLimit(base.getLimit());

        criteria.setMaxVersions(1);

        // 执行查询
        GetRangeRequest getRangeRequest = new GetRangeRequest(criteria);


        GetRangeResponse range = syncClient.getRange(getRangeRequest);


        List<Row> rows = range.getRows();

        List<T> ts = rowToObject(rows, clazz);

        return new Object[]{ObjectUtil.isNotEmpty(range.getNextStartPrimaryKey()), ts};
    }

    /**
     * 执行sql查询
     * @param sql
     * @param syncClient
     * @return
     */
    public static SQLResultSet querySearchSql(String sql, SyncClient syncClient){
        // 创建SQL请求。
        SQLQueryRequest request = new SQLQueryRequest(sql);

        SQLQueryResponse sqlQueryResponse = syncClient.sqlQuery(request);

        return sqlQueryResponse.getSQLResultSet();
    }

    /**
     * sql查询结果转对象
     * @param columnsMap
     * @param sqlRow
     * @param clazz
     * @return
     * @param <T>
     * @throws Exception
     */
    public static <T> T sqlResultToObject(Map<String, Integer> columnsMap, SQLRow sqlRow, Class<T> clazz) {

        T obj = null;
        try {
            obj = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        // 使用 Hutool 获取所有字段
        Field[] fields = ReflectUtil.getFields(clazz);
        // 遍历字段
        for (Field field : fields) {

            // 判断字段是否有 TableField 注解
            if (field.isAnnotationPresent(TableStoreSaveAnnotation.class)) {
                // 获取注解实例
                TableStoreSaveAnnotation tableField = field.getAnnotation(TableStoreSaveAnnotation.class);

                // 判断是否有单前字段
                if (columnsMap.containsKey(tableField.name())) {

                    // 获取字段的值
                    Object value = sqlRow.get(columnsMap.get(tableField.name()));

                    // 把字段的值保存到对象中
                    ReflectUtil.setFieldValue(obj, field, value);
                }
            }
        }
        return obj;
    }

    /**
     * 把row转成对象，对象上要加上TableStoreSaveAnnotation注解
     *
     * @param list
     * @param clazz
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> List<T> rowToObject(List<Row> list, Class<T> clazz) {
        ArrayList<T> result = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(list)) {
            for (Row row : list) {
                T obj = null;
                try {
                    obj = clazz.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                // 使用 Hutool 获取所有字段
                Field[] fields = ReflectUtil.getFields(clazz);
                // 遍历字段
                for (Field field : fields) {
                    // 判断字段是否有 TableField 注解
                    if (field.isAnnotationPresent(TableStoreSaveAnnotation.class)) {
                        // 获取注解实例
                        TableStoreSaveAnnotation tableField = field.getAnnotation(TableStoreSaveAnnotation.class);
                        List<Column> column = row.getColumn(tableField.name());
                        if (ObjectUtil.isNotEmpty(column)) {
                            ReflectUtil.setFieldValue(obj, field, column.get(0).getValue().getValue());
                        }
                        PrimaryKeyColumn primaryKeyColumn = row.getPrimaryKey().getPrimaryKeyColumn(tableField.name());
                        if (ObjectUtil.isNotEmpty(primaryKeyColumn)) {
                            ReflectUtil.setFieldValue(obj, field, primaryKeyColumn.getValue());
                        }
                    }
                }
                result.add(obj);
            }
        }
        return result;
    }
}
