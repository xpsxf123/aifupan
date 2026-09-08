package com.jiuyu.replay.third.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.sql.SQLResultSet;
import com.alicloud.openservices.tablestore.model.sql.SQLRow;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.enums.words.VideoPlatformEnum;
import com.jiuyu.replay.generic.enums.words.VideoSliceTypeEnum;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.video.VideoSliceVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.generic.vo.words.WordListItemVo;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.third.bo.QueryOtherDanMuBo;
import com.jiuyu.replay.third.bo.UploadLocalDanMuDataBo;
import com.jiuyu.replay.third.tablestore.TableStoreServiceUtils;
import com.jiuyu.replay.third.tablestore.entity.AnchorUserBo;
import com.jiuyu.replay.third.tablestore.entity.BarrageBo;
import com.jiuyu.replay.third.tablestore.entity.PrimaryKeyEntity;
import com.jiuyu.replay.third.tablestore.entity.RangeBase;
import com.jiuyu.replay.third.vo.DanMuVo;
import com.jiuyu.replay.third.vo.QueryDanMuVo;
import com.jiuyu.replay.third.vo.QueryOtherDanMuVo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/14 上午11:42
 */
@Component
public class TableStoreBll {

    @Resource
    private SyncClient syncClient;

    @Value("${third.ali.tablestore.barrage-table-name}")
    public String barrageTableName;

    @Value("${third.ali.tablestore.anchor-user-table-name}")
    public String anchorUserTableName;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private AnchorVideoFeign anchorVideoFeign;


    public R<String> uploadDanMuData(UploadLocalDanMuDataBo danMuData, List<BarrageBo> list) throws Exception {
        if (ObjectUtil.isNotEmpty(list)) {
            // 从file中获取弹幕数据
            Map<String, Map<String, Object>> map = new HashMap<>();

            for (BarrageBo barrageBo : list) {
                // 获取分数团等级范围，用户等级范围
                putMap(map, barrageBo);
            }

            // 检查当前的用户是否新的
            if (!map.isEmpty()) {
                //  去查询，判断用户是否是新用户
                List<String> nickNames = map.values().stream().map(item -> (String) item.get("nickName")).distinct().toList();
                List<AnchorUserBo> anchorUserList = TableStoreServiceUtils.queryAnchorUser(nickNames, danMuData.getTenantId(), danMuData.getSecUid(), anchorUserTableName, syncClient);
                if (ObjectUtil.isNotEmpty(anchorUserList)) {
                    List<AnchorUserBo> anchorUserBoList = anchorUserList.stream()
                            .filter(item -> !ObjectUtil.defaultIfNull(item.getBatchNumber(), "0").toString().equals(danMuData.getBatchNumber()))
                            .toList();
                    anchorUserBoList.forEach(item -> {
                        if (ObjectUtil.isNotEmpty(item.getRealNickName())){
                            String key = setNickName(item.getRealNickName(), item.getUserLevel());
                            Map<String, Object> tempMap = map.get(key);
                            if (ObjectUtil.isNotEmpty(tempMap)) {
                                tempMap.put("isNew", false);
                            }
                        }
                    });
                }
            }

            int index = 0;
            // 获取用户等级范围，判断用户是否为新的
            for (int i = 0; i < list.size(); i++) {
                BarrageBo item = list.get(i);
                Map<String, Object> objectMap = map.get(setNickName(item.getNickName(), item.getLevel()));
                if (objectMap == null){
                    objectMap = new HashMap<>();
                }
                item.setFansLevelMin((Long) objectMap.getOrDefault("minFansLevel", 0L));
                item.setFansLevelMax((Long) objectMap.getOrDefault("maxFansLevel", 0L));
                item.setIsNew(item.getFansLevelMin() == 0 && (Boolean) objectMap.getOrDefault("isNew", false));
                boolean isBlessBag = ObjectUtil.equal(item.getIsBlessBag(), true);
                if (isBlessBag) {
                    item.setSort(0L);
                } else {
                    index++;
                    item.setSort((long) index);
                }
                item.setIsBlessBag(item.getIsBlessBag() != null && item.getIsBlessBag());
            }

            // 批量保存到表格中-主播和用户表
            List<AnchorUserBo> anchorUserBoList = map.values().stream().filter(item -> (Boolean) item.get("isNew")).map(item -> {
                AnchorUserBo anchorUserBo = new AnchorUserBo();
                anchorUserBo.setTenantId(danMuData.getTenantId());
                anchorUserBo.setSecUid(danMuData.getSecUid());
                anchorUserBo.setNickName((String) item.get("nickName"));
                anchorUserBo.setBatchNumber(NumberUtil.parseLong(danMuData.getBatchNumber(), 0L));
                anchorUserBo.setRecordDate((long) item.get("time"));
                anchorUserBo.setRealNickName((String) item.get("realNickName"));
                anchorUserBo.setUserLevel((long) item.get("level"));
                return anchorUserBo;
            }).toList();

            // 批量保存到表格中-主播和用户表
            if (ObjectUtil.isNotEmpty(anchorUserBoList)) {
                List<List<AnchorUserBo>> partition = ListUtil.partition(anchorUserBoList, 200);
                for (List<AnchorUserBo> anchorUserBos : partition) {
                    TableStoreServiceUtils.saveBatch(anchorUserBos, anchorUserTableName, syncClient);
                }
            }

            // 批量保存到表格中-弹幕表
            if (ObjectUtil.isNotEmpty(list)) {
                updateBarrageBatch(list);
            }
        }
        return R.ok("上传成功");
    }

    /**
     * 查询弹幕数据-全局二级索引
     *
     * @param queryDanMuBo
     * @return
     */
    public R<QueryDanMuVo> queryDanMuData(QueryDanMuBo queryDanMuBo) throws Exception {
        RRException.isNotEmpty(queryDanMuBo.getBatchNumber(), "场次号不能为空");
        RRException.isNotEmpty(queryDanMuBo.getUserId(), "userid不能为空");
        RRException.isNotEmpty(queryDanMuBo.getVideoId(), "视频id不能为空");
        RRException.isNotEmpty(queryDanMuBo.getTenantId(), "租户id不能为空");

        // 判断查询类型-上下
        List<Integer> types = new ArrayList<>();
        if (queryDanMuBo.getQueryType() == 0) {
            types.add(0);
        } else if (queryDanMuBo.getQueryType() == 1) {
            types.add(1);
        } else {
            types.add(0);
            types.add(1);
        }
        QueryDanMuVo data = new QueryDanMuVo();

        for (Integer type : types) {
            // 查询条数，查询方向
            RangeBase base = new RangeBase();
            base.setLimit(queryDanMuBo.getLimit());
            base.setQueryType(type);

            // 封装索引条件
            List<PrimaryKeyEntity> startKeyList = new ArrayList<>();
            List<PrimaryKeyEntity> endKeyList = new ArrayList<>();

            // 租户id搜索
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getTenantId())) {
                startKeyList.add(PrimaryKeyEntity.builder()
                        .name("tenantId")
                        .value(queryDanMuBo.getTenantId())
                        .build()
                );
                endKeyList.add(PrimaryKeyEntity.builder()
                        .name("tenantId")
                        .value(queryDanMuBo.getTenantId())
                        .build()
                );
            }

            // 用户id搜索
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getUserId())) {
                startKeyList.add(PrimaryKeyEntity.builder()
                        .name("userId")
                        .value(queryDanMuBo.getUserId())
                        .build()
                );
                endKeyList.add(PrimaryKeyEntity.builder()
                        .name("userId")
                        .value(queryDanMuBo.getUserId())
                        .build()
                );
            }

            // 直播场次号搜索
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getBatchNumber())) {
                startKeyList.add(PrimaryKeyEntity.builder()
                        .name("batchNumber")
                        .value(queryDanMuBo.getBatchNumber())
                        .build()
                );
                endKeyList.add(PrimaryKeyEntity.builder()
                        .name("batchNumber")
                        .value(queryDanMuBo.getBatchNumber())
                        .build()
                );
            }

//            if (ObjectUtil.isNotEmpty(queryDanMuBo.getMsgId())) {
//                startKeyList.add(PrimaryKeyEntity.builder()
//                        .name("msgId")
//                        .value(type == 1 ? (queryDanMuBo.getMsgId() + 1) : Long.MAX_VALUE)
//                        .build()
//                );
//                endKeyList.add(PrimaryKeyEntity.builder()
//                        .name("msgId")
//                        .value(type == 0 ? queryDanMuBo.getMsgId() : Long.MAX_VALUE)
//                        .build()
//                );
//            } else {
//                startKeyList.add(PrimaryKeyEntity.builder()
//                        .name("msgId")
//                        .value(type == 1 ? Long.MIN_VALUE : Long.MAX_VALUE)
//                        .build()
//                );
//                endKeyList.add(PrimaryKeyEntity.builder()
//                        .name("msgId")
//                        .value(type == 1 ? Long.MAX_VALUE : Long.MIN_VALUE)
//                        .build()
//                );
//            }

            Map<String, Object> objectMap = getParams(queryDanMuBo, type);
            Object[] objects = TableStoreServiceUtils.queryRange(base, startKeyList, endKeyList, objectMap, BarrageBo.class, barrageTableName, syncClient);
            if (ObjectUtil.isNotEmpty(objects)) {
                List<BarrageBo> list = (List<BarrageBo>) objects[1];
                if (type == 0) {
                    data.setPreviousHash((Boolean) objects[0]);
                } else if (type == 1) {
                    data.setNextHash((Boolean) objects[0]);
                }
                if (data.getList() == null) data.setList(new ArrayList<>());
                if (ObjectUtil.isNotEmpty(list)) {
                    data.getList().addAll(BeanUtil.copyToList(list, DanMuVo.class));
                }
            }
        }

        return R.ok(data);
    }

    /**
     * 搜索弹幕数据-多元索引-sql查询
     *
     * @param queryDanMuBo
     * @return
     * @throws Exception
     */
    public R<QueryDanMuVo> queryDanMuSearchData(QueryDanMuBo queryDanMuBo) {
        RRException.isNotEmpty(queryDanMuBo.getBatchNumber(), "场次号不能为空");
        RRException.isNotEmpty(queryDanMuBo.getUserId(), "userid不能为空");
        RRException.isNotEmpty(queryDanMuBo.getVideoId(), "视频id不能为空");
        RRException.isNotEmpty(queryDanMuBo.getTenantId(), "租户id不能为空");
        RRException.isNotEmpty(queryDanMuBo.getLimit(), "limit不能为空");

        QueryDanMuVo result = new QueryDanMuVo();
        result.setNextHash(false);
        result.setPreviousHash(false);
        result.setList(new ArrayList<>());

        AnchorVideoInfoVo video = anchorVideoFeign.getParentVideoByVideoId(queryDanMuBo.getVideoId());
        if (video != null) {
            queryDanMuBo.setVideoId(video.getVideoId());
            queryDanMuBo.setBatchNumber(video.getBatchNumber());
            queryDanMuBo.setUserId(video.getUserId());
            queryDanMuBo.setTenantId(video.getTenantId());
        }

        // 判断查询类型-上下 0上，1下
        List<Integer> types = new ArrayList<>();
        if (queryDanMuBo.getQueryType() == 0) {
            types.add(0);
        } else if (queryDanMuBo.getQueryType() == 1) {
            types.add(1);
        } else if (queryDanMuBo.getQueryType() == 2) {
            types.add(0);
            types.add(1);
        } else if (queryDanMuBo.getQueryType() == 3) {
            types.add(1);
            queryDanMuBo.setLimit(1000);
            queryDanMuBo.setPage(1);
        } else {
            types.add(0);
            types.add(1);
        }

        for (Integer type : types) {
            String startSql = " select ";
            String select = " nick_name, is_new, level, fans_level_min, fans_level_max, fans_level_current, content, record_date, sort, is_bless_bag ";
            String formSql = " from " + barrageTableName;
            String whereSql = " where 1 = 1 ";
            String sql = "";
            String limit = "";

            sql += concatSqlWhere(queryDanMuBo, type);

            if (type == 0) {
                sql += " order by record_date, sort desc ";
            } else if (type == 1) {
                sql += " order by record_date, sort asc ";
            }

            // 判断有无分页
            if (queryDanMuBo.getLimit() != -1){
                limit = StrUtil.format(" LIMIT {} OFFSET {} ", queryDanMuBo.getLimit(), ((queryDanMuBo.getPage() - 1) * queryDanMuBo.getLimit()));
            }
            String sqlAll = startSql + select + formSql + whereSql + sql + limit;
            SQLResultSet sqlResultSet = TableStoreServiceUtils.querySearchSql(sqlAll, syncClient);

            List<BarrageBo> list = new ArrayList<>();
            while (sqlResultSet.hasNext()) {
                SQLRow sqlRow = sqlResultSet.next();
                BarrageBo danMuVo = TableStoreServiceUtils.sqlResultToObject(sqlResultSet.getSQLTableMeta().getColumnsMap(), sqlRow, BarrageBo.class);

                list.add(danMuVo);
            }
            // 添加到结果中
            result.getList().addAll(BeanUtil.copyToList(list, DanMuVo.class));

            // 判断是否还有下一页
            if (queryDanMuBo.getLimit() == list.size()) {
                if (type == 0) result.setPreviousHash(true);
                if (type == 1) result.setNextHash(true);
            } else {
                if (type == 0) result.setPreviousHash(false);
                if (type == 1) result.setNextHash(false);
            }

        }

        // 排序
        result.getList().sort(Comparator.comparing(DanMuVo::getRecordDate).thenComparing(DanMuVo::getSort));

        // 查询用户发言的条数
        if (ObjectUtil.isNotEmpty(result.getList())) {
            List<String> nickNameList = result.getList().stream().map(e -> "'" + escapeSqlValue(e.getNickName()) + "'").distinct().toList();

            String sql = "select " +
                    " nick_name, level, count(*) as fans_level_current " +
                    " from " + barrageTableName +
                    " where 1 = 1 ";
            // 租户id搜索
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getTenantId())) {
                sql += StrUtil.format(" and tenant_id = {} ", escapeSqlValue(queryDanMuBo.getTenantId()));
            }
            // 用户id搜索
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getUserId())) {
                sql += StrUtil.format(" and user_id = {} ", escapeSqlValue(queryDanMuBo.getUserId()));
            }
            // 直播场次号搜索
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getBatchNumber())) {
                sql += StrUtil.format(" and batch_number = {} ", escapeSqlValue(queryDanMuBo.getBatchNumber()));
            }
            // 视频id搜索
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getVideoId())) {
                sql += StrUtil.format(" and video_id = '{}' ", escapeSqlValue(queryDanMuBo.getVideoId()));
            }
            sql += " and nick_name in (" + StrUtil.join(",", nickNameList) + ") " +
                    " GROUP BY nick_name,level ";
            SQLResultSet sqlResultSet = TableStoreServiceUtils.querySearchSql(sql, syncClient);

            List<DanMuVo> tempList = new ArrayList<>();
            while (sqlResultSet.hasNext()) {
                SQLRow sqlRow = sqlResultSet.next();
                DanMuVo muVo = new DanMuVo();
                muVo.setNickName(sqlRow.getString("nick_name"));
                muVo.setLevel(sqlRow.getLong("level"));
                muVo.setFansLevelCurrent(sqlRow.getLong("fans_level_current"));
                tempList.add(muVo);
            }

            if (ObjectUtil.isNotEmpty(tempList)) {
                result.getList().forEach(e -> {
                    if (isDesensitization(e.getNickName())){
                        int sum = tempList.stream()
                                .filter(v -> v.getNickName().equals(e.getNickName()) && v.getLevel().equals(e.getLevel()))
                                .mapToInt(item -> item.getFansLevelCurrent().intValue()).sum();
                        e.setCountSendNum(sum);
                    }else{
                        int sum = tempList.stream()
                                .filter(v -> v.getNickName().equals(e.getNickName()))
                                .mapToInt(item -> item.getFansLevelCurrent().intValue()).sum();
                        e.setCountSendNum(sum);
                    }
                });
            }

        }

        return R.ok(result);
    }

    /**
     * 拼接where条件
     *
     * @param queryDanMuBo 条件
     * @param type         类型
     * @return where sql
     */
    private String concatSqlWhere(QueryDanMuBo queryDanMuBo, Integer type) {
        String sql = "";
        // 租户id搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getTenantId())) {
            sql += StrUtil.format(" and tenant_id = {} ", escapeSqlValue(queryDanMuBo.getTenantId()));
        }

        // 用户id搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getUserId())) {
            sql += StrUtil.format(" and user_id = {} ", escapeSqlValue(queryDanMuBo.getUserId()));
        }

        // 直播场次号搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getBatchNumber())) {
            sql += StrUtil.format(" and batch_number = {} ", escapeSqlValue(queryDanMuBo.getBatchNumber()));
        }

        // 视频id搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getVideoId())) {
            sql += StrUtil.format(" and video_id = '{}' ", escapeSqlValue(queryDanMuBo.getVideoId()));
        }

        // 弹幕用户昵称搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getNickName())) {
            sql += StrUtil.format(" and nick_name = '{}' ", escapeSqlValue(queryDanMuBo.getNickName()));

            // 用户等级搜索
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getLevel()) && isDesensitization(queryDanMuBo.getNickName())) {
                sql += StrUtil.format(" and level = {} ", escapeSqlValue(queryDanMuBo.getLevel()));
            }
        }

        // 时间搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getRecordDate())) {
            if (type == 0) {
                sql += StrUtil.format(" and record_date < {} ", escapeSqlValue(queryDanMuBo.getRecordDate()));


            } else if (type == 1) {
                sql += StrUtil.format(" and record_date >= {} ", escapeSqlValue(queryDanMuBo.getRecordDate()));


            }
        }

        // 开始时间
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getStartTime())) {
            sql += StrUtil.format(" and record_date >= {} ", escapeSqlValue(queryDanMuBo.getStartTime()));
        }
        // 结束时间
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getEndTime())) {
            sql += StrUtil.format(" and record_date <= {} ", escapeSqlValue(queryDanMuBo.getEndTime()));
        }

        // 是否新用户搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getIsNew())) {
            sql += StrUtil.format(" and is_new = {} ", escapeSqlValue(queryDanMuBo.getIsNew() != 0));
        }

        // 弹幕内容搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getContentLike())) {
            sql += StrUtil.format(" and content like '%{}%' ", escapeSqlValue(queryDanMuBo.getContentLike()));
        }

        // 弹幕用户昵称搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getNickNameLike())) {
            sql += StrUtil.format(" and nick_name like '%{}%' ", escapeSqlValue(queryDanMuBo.getNickNameLike()));
        }

        // 用户等级搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getMinLevel())) {
            sql += StrUtil.format(" and level >= {} ", escapeSqlValue(queryDanMuBo.getMinLevel()));
        }
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getMaxLevel())) {
            sql += StrUtil.format(" and level < {} ", escapeSqlValue(queryDanMuBo.getMaxLevel()));
        }

        // 粉丝团等级搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getMinFansLevel())) {
            sql += StrUtil.format(" and fans_level_current >= {} ", escapeSqlValue(queryDanMuBo.getMinFansLevel()));
        }
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getMaxFansLevel())) {
            sql += StrUtil.format(" and fans_level_current < {} ", escapeSqlValue(queryDanMuBo.getMaxFansLevel()));
        }

        // 最初粉丝团等级搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getMinInitFansLevel())) {
            sql += StrUtil.format(" and fans_level_min >= {} ", escapeSqlValue(queryDanMuBo.getMinInitFansLevel()));
        }
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getMaxInitFansLevel())) {
            sql += StrUtil.format(" and fans_level_min < {} ", escapeSqlValue(queryDanMuBo.getMaxInitFansLevel()));
        }

        // 最终粉丝团等级搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getMinFinallyFansLevel())) {
            sql += StrUtil.format(" and fans_level_max >= {} ", escapeSqlValue(queryDanMuBo.getMinFinallyFansLevel()));
        }
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getMaxFinallyFansLevel())) {
            sql += StrUtil.format(" and fans_level_max < {} ", escapeSqlValue(queryDanMuBo.getMaxFinallyFansLevel()));
        }

        // 是否重要弹幕搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getImportant()) && queryDanMuBo.getImportant() == 1) {
            sql += StrUtil.format(" and important = {} ", escapeSqlValue(queryDanMuBo.getImportant()));
        }

        // 是否福袋弹幕搜索
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getIsBlessBag())) {
            if (queryDanMuBo.getIsBlessBag() == 0) {
                sql += StrUtil.format(" and (is_bless_bag = 0 or is_bless_bag is null) ", 0);
            } else {
                sql += StrUtil.format(" and is_bless_bag = {} ", 1);
            }
        }

        return sql;
    }

    /**
     * 查询弹幕数据总条数
     *
     * @param queryDanMuBo 查询条件
     * @return 总条数
     */
    public Long queryDanMuSearchCount(QueryDanMuBo queryDanMuBo) {
        RRException.isNotEmpty(queryDanMuBo.getBatchNumber(), "场次号不能为空");
        RRException.isNotEmpty(queryDanMuBo.getUserId(), "userid不能为空");
        RRException.isNotEmpty(queryDanMuBo.getVideoId(), "视频id不能为空");
        RRException.isNotEmpty(queryDanMuBo.getTenantId(), "租户id不能为空");

        String startSql = " select count(*) as count ";
        String formSql = " from " + barrageTableName;
        String whereSql = " where 1 = 1 ";
        String sql = concatSqlWhere(queryDanMuBo, 0);

        String sqlAll = startSql + formSql + whereSql + sql;
        SQLResultSet sqlResultSet = TableStoreServiceUtils.querySearchSql(sqlAll, syncClient);

        long count = 0L;
        if (sqlResultSet.hasNext()) {
            SQLRow sqlRow = sqlResultSet.next();
            Object countObj = sqlRow.get(0);
            if (ObjectUtil.isNotEmpty(countObj)) {
                count = Long.parseLong(countObj.toString());
            }
        }

        return count;
    }


    public R<List<QueryOtherDanMuVo>> queryOtherSearchData(QueryOtherDanMuBo danMu) throws Exception {
        RRException.isNotEmpty(danMu.getUserId(), "userid不能为空");
        RRException.isNotEmpty(danMu.getTenantId(), "租户id不能为空");
        RRException.isNotEmpty(danMu.getRecordDate(), "记录时间戳不能为空");
        RRException.isNotEmpty(danMu.getNickName(), "用户昵称不能为空");

        String sql = "select " +
//                    " tenant_id, user_id, batch_number, msg_id, sec_uid, video_id, " +
                " batch_number, video_id," +
                " nick_name, is_new, level, fans_level_min, fans_level_max, fans_level_current, content, record_date, sort, is_bless_bag " +
                " from " + barrageTableName +
                " where 1 = 1 ";

        // 租户id搜索
        if (ObjectUtil.isNotEmpty(danMu.getTenantId())) {
            sql += StrUtil.format(" and tenant_id = {} ", escapeSqlValue(danMu.getTenantId()));
        }

        // 用户id搜索
        if (ObjectUtil.isNotEmpty(danMu.getUserId())) {
            sql += StrUtil.format(" and user_id = {} ", escapeSqlValue(danMu.getUserId()));
        }

//        // 直播场次号搜索
//        if (ObjectUtil.isNotEmpty(danMu.getBatchNumber())) {
//            sql += StrUtil.format(" and batch_number = {} ", danMu.getBatchNumber());
//        }

//        // 视频id搜索
//        if (ObjectUtil.isNotEmpty(danMu.getVideoId())) {
//            sql += StrUtil.format(" and video_id = '{}' ", danMu.getVideoId());
//        }

        // 主播id搜索
        if (ObjectUtil.isNotEmpty(danMu.getSecUid())){
            sql += StrUtil.format(" and sec_uid = '{}' ", escapeSqlValue(danMu.getSecUid()));
        }

        // 弹幕用户昵称搜索
        if (ObjectUtil.isNotEmpty(danMu.getNickName())) {
            sql += StrUtil.format(" and nick_name = '{}' ", escapeSqlValue(danMu.getNickName()));

            // 用户等级搜索
            if (ObjectUtil.isNotEmpty(danMu.getLevel()) && isDesensitization(danMu.getNickName())){
                sql += StrUtil.format(" and level = {} ", escapeSqlValue(danMu.getLevel()));
            }
        }
        // 是否福袋弹幕搜索
        if (ObjectUtil.isNotEmpty(danMu.getIsBlessBag())) {
            if (danMu.getIsBlessBag() == 0) {
                sql += StrUtil.format(" and (is_bless_bag = 0 or is_bless_bag is null) ");
            } else {
                sql += StrUtil.format(" and is_bless_bag = {} ", escapeSqlValue(danMu.getIsBlessBag()));
            }
        }



        // 设置时间条件
        if (ObjectUtil.isNotEmpty(danMu.getRecordDate())) {
            Long recordDate = danMu.getRecordDate();
            long startTime = recordDate - (1000 * 60 * 60 * 24 * 7);
            long endTime = recordDate + (1000 * 60 * 60 * 24 * 7);
            if (endTime > System.currentTimeMillis()) endTime = System.currentTimeMillis();
            sql += StrUtil.format(" and record_date >= {} ", escapeSqlValue(startTime));
            sql += StrUtil.format(" and record_date <= {} ", escapeSqlValue(endTime));
        }


        sql += " order by record_date, sort asc ";

        sql += StrUtil.format(" LIMIT {} ", danMu.getLimit());
        SQLResultSet sqlResultSet = TableStoreServiceUtils.querySearchSql(sql, syncClient);

        List<BarrageBo> list = new ArrayList<>();
        while (sqlResultSet.hasNext()) {
            SQLRow sqlRow = sqlResultSet.next();
            BarrageBo danMuVo = TableStoreServiceUtils.sqlResultToObject(sqlResultSet.getSQLTableMeta().getColumnsMap(), sqlRow, BarrageBo.class);
            list.add(danMuVo);
        }
        // 添加到结果中
        ArrayList<QueryOtherDanMuVo> result = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(list)) {
            list.stream()
                    .collect(Collectors.groupingBy(BarrageBo::getVideoId)) // 以视频维度
//                    .collect(Collectors.groupingBy(BarrageBo::getBatchNumber)) // 以场次维度
                    .forEach((videoId, boList) -> {
                        QueryOtherDanMuVo vo = new QueryOtherDanMuVo();
                        vo.setBatchNumber(videoId);
                        vo.setList(BeanUtil.copyToList(boList, DanMuVo.class));
                        result.add(vo);
                    });
        }
        return R.ok(result);
    }

    /**
     * 查询其他场次弹幕数据-全局二级索引
     *
     * @param danMu
     * @return
     */
    public R<List<QueryOtherDanMuVo>> queryOtherDanMuData(QueryOtherDanMuBo danMu) throws Exception {
        RRException.isNotEmpty(danMu.getUserId(), "userid不能为空");
        RRException.isNotEmpty(danMu.getTenantId(), "租户id不能为空");
        RRException.isNotEmpty(danMu.getRecordDate(), "记录时间戳不能为空");
        RRException.isNotEmpty(danMu.getNickName(), "用户昵称不能为空");
        List<QueryOtherDanMuVo> result = new ArrayList<>();
        // 查询条数，查询方向
        RangeBase base = new RangeBase();
        base.setLimit(danMu.getLimit());
        base.setQueryType(0);

        // 封装索引条件
        List<PrimaryKeyEntity> startKeyList = new ArrayList<>();
        List<PrimaryKeyEntity> endKeyList = new ArrayList<>();

        // 租户id搜索
        if (ObjectUtil.isNotEmpty(danMu.getTenantId())) {
            startKeyList.add(PrimaryKeyEntity.builder()
                    .name("tenantId")
                    .value(danMu.getTenantId())
                    .build()
            );
            endKeyList.add(PrimaryKeyEntity.builder()
                    .name("tenantId")
                    .value(danMu.getTenantId())
                    .build()
            );
        }

        // 用户id搜索
        if (ObjectUtil.isNotEmpty(danMu.getUserId())) {
            startKeyList.add(PrimaryKeyEntity.builder()
                    .name("userId")
                    .value(danMu.getUserId())
                    .build()
            );
            endKeyList.add(PrimaryKeyEntity.builder()
                    .name("userId")
                    .value(danMu.getUserId())
                    .build()
            );
        }

        startKeyList.add(PrimaryKeyEntity.builder()
                .name("msgId")
                .value(String.valueOf(Long.MAX_VALUE))
                .build()
        );
        endKeyList.add(PrimaryKeyEntity.builder()
                .name("msgId")
                .value(String.valueOf(Long.MIN_VALUE))
                .build()
        );

        HashMap<String, Object> params = new HashMap<>();
        ArrayList<Map<String, Object>> value = new ArrayList<>();

        HashMap<String, Object> map = new HashMap<>();
        map.put("name", "nickName");
        map.put("value", danMu.getNickName());
        map.put("type", "=");
        value.add(map);

        // 设置时间条件
        Long recordDate = danMu.getRecordDate();
        long startTime = recordDate - (1000 * 60 * 60 * 24 * 7);
        long endTime = recordDate + (1000 * 60 * 60 * 24 * 7);
        if (endTime > System.currentTimeMillis()) endTime = System.currentTimeMillis();
        HashMap<String, Object> map1 = new HashMap<>();
        map1.put("name", "recordDate");
        map1.put("value", startTime);
        map1.put("type", ">=");
        value.add(map1);
        HashMap<String, Object> map2 = new HashMap<>();
        map2.put("name", "recordDate");
        map2.put("value", endTime);
        map2.put("type", "<=");
        value.add(map2);


        params.put("value", value);
        params.put("type", "AND");
        Object[] objects = TableStoreServiceUtils.queryRange(base, startKeyList, endKeyList, params, BarrageBo.class, barrageTableName, syncClient);
        if (ObjectUtil.isNotEmpty(objects)) {
            List<BarrageBo> list = (List<BarrageBo>) objects[1];
            if (ObjectUtil.isNotEmpty(list)) {
                list.stream()
                        .collect(Collectors.groupingBy(BarrageBo::getBatchNumber))
                        .forEach((batchNumber, boList) -> {
                            QueryOtherDanMuVo vo = new QueryOtherDanMuVo();
                            vo.setBatchNumber(batchNumber.toString());
                            vo.setList(BeanUtil.copyToList(boList, DanMuVo.class));
                            result.add(vo);
                        });
            }
        }

        return R.ok(result);
    }


    /**
     * 设置查询参数
     *
     * @param queryDanMuBo
     * @return
     */
    public Map<String, Object> getParams(QueryDanMuBo queryDanMuBo, Integer type) {
        HashMap<String, Object> result = new HashMap<>();

        if (ObjectUtil.isNotEmpty(queryDanMuBo)) {
            result.put("type", "AND");
            List<Map<String, Object>> list = new ArrayList<>();

            // 视频id搜索
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getVideoId())) {
                HashMap<String, Object> temp = new HashMap<>();
                temp.put("name", "videoId");
                temp.put("type", "=");
                temp.put("value", queryDanMuBo.getVideoId());
                list.add(temp);
            }

            // 是否新用户搜索
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getIsNew())) {
                HashMap<String, Object> temp = new HashMap<>();
                temp.put("name", "isNew");
                temp.put("type", "=");
                temp.put("value", queryDanMuBo.getIsNew() != 0);
                list.add(temp);
            }

            if (ObjectUtil.isNotEmpty(queryDanMuBo.getNickName())) {
                HashMap<String, Object> temp = new HashMap<>();
                temp.put("name", "nickName");
                temp.put("type", "=");
                temp.put("value", queryDanMuBo.getNickName());
                list.add(temp);
            }

            // 时间搜索
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getRecordDate())) {
                if (type == 0) {
                    HashMap<String, Object> temp1 = new HashMap<>();
                    temp1.put("name", "recordDate");
                    temp1.put("type", "<=");
                    temp1.put("value", queryDanMuBo.getRecordDate());
                    list.add(temp1);
                    HashMap<String, Object> temp = new HashMap<>();
                    temp.put("name", "recordDate");
                    temp.put("type", ">=");
                    temp.put("value", Long.MIN_VALUE);
                    list.add(temp);
                } else if (type == 1) {
                    HashMap<String, Object> temp = new HashMap<>();
                    temp.put("name", "recordDate");
                    temp.put("type", ">=");
                    temp.put("value", queryDanMuBo.getRecordDate());
                    list.add(temp);
                    HashMap<String, Object> temp1 = new HashMap<>();
                    temp1.put("name", "recordDate");
                    temp1.put("type", "<=");
                    temp1.put("value", Long.MAX_VALUE);
                    list.add(temp1);
                }
            }

            // 用户等级--小于
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getMinLevel())) {
                HashMap<String, Object> temp = new HashMap<>();
                temp.put("name", "level");
                temp.put("type", ">");
                temp.put("value", queryDanMuBo.getMinLevel());
                list.add(temp);
            }

            // 用户等级--大于
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getMaxLevel())) {
                HashMap<String, Object> temp = new HashMap<>();
                temp.put("name", "level");
                temp.put("type", "<=");
                temp.put("value", queryDanMuBo.getMaxLevel());
                list.add(temp);
            }

            // 粉丝团等级--小于
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getMinFansLevel())) {
                HashMap<String, Object> temp = new HashMap<>();
                temp.put("name", "fansLevelCurrent");
                temp.put("type", ">");
                temp.put("value", queryDanMuBo.getMinFansLevel());
                list.add(temp);
            }

            // 粉丝团等级--大于
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getMaxFansLevel())) {
                HashMap<String, Object> temp = new HashMap<>();
                temp.put("name", "fansLevelCurrent");
                temp.put("type", "<=");
                temp.put("value", queryDanMuBo.getMaxFansLevel());
                list.add(temp);
            }

            // 弹幕用户昵称
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getNickNameLike())) {
                HashMap<String, Object> temp = new HashMap<>();
                temp.put("name", "nickName");
                temp.put("type", "LIKE");
                temp.put("value", queryDanMuBo.getNickNameLike());
                list.add(temp);
            }

            // 弹幕内容
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getContentLike())) {
                HashMap<String, Object> temp = new HashMap<>();
                temp.put("name", "content");
                temp.put("type", "LIKE");
                temp.put("value", queryDanMuBo.getContentLike());
                list.add(temp);
            }
            result.put("value", list);
        }

        return result;
    }

    /**
     * 设置用户昵称
     * @param nickName
     * @param level
     * @return
     */
    private String setNickName(String nickName, Long level){
        if (isDesensitization(nickName)){
            return nickName + "$" + level;
        }
        return nickName;
    }

    /**
     * 判断昵称是否脱敏
     * @param nickName
     * @return
     */
    private boolean isDesensitization(String nickName){
        return nickName.matches(".\\*{3,5}");
    }

    /**
     * 获取分数团等级范围，用户等级范围
     *
     * @param map
     * @param barrageBo
     */
    private void putMap(Map<String, Map<String, Object>> map, BarrageBo barrageBo) {
        String nickName = setNickName(barrageBo.getNickName(), barrageBo.getLevel());
        Map<String, Object> tempMap = map.get(nickName);
        if (tempMap == null) {
            tempMap = new HashMap<>();
            tempMap.put("isNew", true);
            tempMap.put("realNickName", barrageBo.getNickName());
            tempMap.put("time", barrageBo.getRecordDate());

            tempMap.put("minLevel", barrageBo.getLevel());
            tempMap.put("maxLevel", barrageBo.getLevel());

            tempMap.put("minFansLevel", barrageBo.getFansLevelCurrent());
            tempMap.put("maxFansLevel", barrageBo.getFansLevelCurrent());
        }

        if (barrageBo.getLevel() < (Long) tempMap.get("minLevel")) {
            tempMap.put("minLevel", barrageBo.getLevel());
        }
        if (barrageBo.getLevel() > (Long) tempMap.get("maxLevel")) {
            tempMap.put("maxLevel", barrageBo.getLevel());
        }
        if (barrageBo.getFansLevelCurrent() < (Long) tempMap.get("minFansLevel")) {
            tempMap.put("minFansLevel", barrageBo.getFansLevelCurrent());
        }
        if (barrageBo.getFansLevelCurrent() > (Long) tempMap.get("maxFansLevel")) {
            tempMap.put("maxFansLevel", barrageBo.getFansLevelCurrent());
        }
        tempMap.put("level", barrageBo.getLevel());
        tempMap.put("nickName", nickName);
        map.put(nickName, tempMap);
    }

    private String escapeSqlValue(Object value){
        if (value == null) {
            return "NULL"; // 如果值为空，直接返回 NULL
        }
        String strValue = value.toString();
        // 转义特殊字符
        return strValue.replace("'", "''")  // 转义单引号
                .replace("\\", "\\\\") // 转义反斜杠
                .replace("\"", "\\\"") // 转义双引号
                .replace("\b", "\\b")  // 转义退格符
                .replace("\n", "\\n")  // 转义换行符
                .replace("\r", "\\r")  // 转义回车符
                .replace("\t", "\\t")  // 转义制表符
                .replace("\0", "\\0")  // 转义空字符
                ;
    }

    /**
     * 判断弹幕是否存在
     * @param bo
     * @return
     */
    public R<Boolean> existsBarrage(UploadLocalDanMuDataBo bo) {

        String sql = StrUtil.format("SELECT 1 FROM {} WHERE tenant_id = {} and user_id = {} and batch_number = {} and video_id = '{}' LIMIT 1 OFFSET 0",
                barrageTableName, bo.getTenantId(), bo.getUserId(), bo.getBatchNumber(), bo.getVideoId());

        SQLResultSet sqlResultSet = TableStoreServiceUtils.querySearchSql(sql, syncClient);
        return R.ok(sqlResultSet.rowCount() > 0);
    }

    /**
     * 设置弹幕缓存
     * @param videoId
     * @param list
     */
    public void setBarrageRedisCache(String videoId, List<BarrageBo> list) {
        String redisKey = RedisCacheKey.getRedisKey(RedisCacheKey.barrageDataCacheKey, videoId);
        redisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(list), 2, TimeUnit.HOURS);
    }

    /**
     * 根据视频id查询弹幕
     * @param videoId
     * @return
     */
    public R<List<BarrageBo>> listVideoBarrageByVideoId(Long userId, Long tenantId, String batchNumber, String videoId) {

        String redisKey = RedisCacheKey.getRedisKey(RedisCacheKey.barrageDataCacheKey, videoId);
        Object o = redisTemplate.opsForValue().get(redisKey);
        if (ObjectUtil.isNotEmpty(o)) {
            return R.ok(BeanUtil.copyToList(JSONUtil.parseArray((String) o), BarrageBo.class));
        }

        if (ObjectUtil.isEmpty(videoId)) {
            return R.ok(new ArrayList<>());
        }
        AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoFeign.GetByVideoId(videoId));
        if (video == null || video.getPlatformType() == null || !ObjectUtil.equals(video.getPlatformType(), VideoPlatformEnum.DOUYIN.getCode())) {
            return R.ok(new ArrayList<>());
        }

        String sql = "select " +
                " tenant_id, user_id, batch_number, msg_id, sec_uid, video_id, " +
                " nick_name, is_new, level, fans_level_min, fans_level_max, fans_level_current, content, record_date, sort, is_bless_bag " +
                " from " + barrageTableName +
                " where 1 = 1 ";

        // 租户id搜索
        if (ObjectUtil.isNotEmpty(tenantId)) {
            sql += StrUtil.format(" and tenant_id = {} ", escapeSqlValue(tenantId));
        }

        // 用户id搜索
        if (ObjectUtil.isNotEmpty(userId)) {
            sql += StrUtil.format(" and user_id = {} ", escapeSqlValue(userId));
        }

        // 直播场次号搜索
        if (ObjectUtil.isNotEmpty(batchNumber)) {
            sql += StrUtil.format(" and batch_number = {} ", escapeSqlValue(batchNumber));
        }

        // 视频id搜索
        if (ObjectUtil.isNotEmpty(videoId)) {
            sql += StrUtil.format(" and video_id = '{}' ", escapeSqlValue(videoId));
        }

        sql += " order by record_date asc ";

//        int limit = -1;
//        int page = 1;
//        // 判断有无分页
//        if (limit != -1) {
//            sql += StrUtil.format(" LIMIT {} OFFSET {} ", limit, ((page - 1) * limit));
//        }
        SQLResultSet sqlResultSet = TableStoreServiceUtils.querySearchSql(sql, syncClient);

        List<BarrageBo> list = new ArrayList<>();
        while (sqlResultSet.hasNext()) {
            SQLRow sqlRow = sqlResultSet.next();
            BarrageBo danMuVo = TableStoreServiceUtils.sqlResultToObject(sqlResultSet.getSQLTableMeta().getColumnsMap(), sqlRow, BarrageBo.class);
            list.add(danMuVo);
        }
        // 缓存弹幕数据
        setBarrageRedisCache(videoId, list);
        return R.ok(list);
    }

    /**
     * 获取弹幕数据
     *
     * @param videoId
     * @param audioaAlyses
     * @return
     */
    public List<Map<String, Object>> getBarrageDataList(String videoId, List<String> audioaAlyses, Integer isBlessBag) {
        List<Map<String, Object>> result = new ArrayList<>();
        String key = StrUtil.format("replay:words:barrageAnnotation:{}", videoId + (isBlessBag == null ? "" : "_" + isBlessBag));

        // 先查询缓存存在
        String o = (String) redisTemplate.opsForValue().get(key);
        if (ObjectUtil.isNotEmpty(o)) {
            return JSON.parseObject(o, new TypeReference<>() {
            });
        }

        if (ObjectUtil.isEmpty(audioaAlyses)) {
            return result;
        }
        R<AnchorVideoInfoVo> anchorVideoInfoVoR = anchorVideoFeign.GetByVideoId(videoId);
        if (anchorVideoInfoVoR.getCode() != 0 && !ObjectUtil.isNotEmpty(anchorVideoInfoVoR.getData())) {
            return result;
        }else if(anchorVideoInfoVoR.getData().getPlatformType().equals(VideoPlatformEnum.KUAISHOU.getCode())) {
            return result;
        }
        AnchorVideoInfoVo videoInfo = anchorVideoInfoVoR.getData();
        if (!ObjectUtil.equals(videoInfo.getVideoSliceType(), VideoSliceTypeEnum.VIDEO.getCode())) {
            VideoSliceVo videoSlice = anchorVideoFeign.getVideoSliceBySourceId(videoId, WordsEnum.sourceType.VIDEO.getCode());
            if (videoSlice != null) {
                AnchorVideoInfoVo videoInfoOld = ResultUtil.getResult(anchorVideoFeign.GetByVideoId(videoSlice.getSourceParentId()));
                if (videoInfoOld == null) {
                    return result;
                }
                videoInfo.setUserId(videoInfoOld.getUserId());
                videoInfo.setTenantId(videoInfoOld.getTenantId());
                videoInfo.setVideoId(videoInfoOld.getVideoId());
                videoInfo.setBatchNumber(videoInfoOld.getBatchNumber());
            }
        }

        // 只有抖音才有弹幕
        if (ObjectUtil.equals(videoInfo.getPlatformType(), String.valueOf(WordsEnum.platformType.DOU_YIN.getCode()))) {
            R<List<BarrageBo>> barrageR = this.listVideoBarrageByVideoId(videoInfo.getUserId(), videoInfo.getTenantId(), videoInfo.getBatchNumber(), videoInfo.getVideoId());
            if (barrageR.getCode() == 0 && ObjectUtil.isNotEmpty(barrageR.getData())) {
                List<BarrageBo> barrageBoList = barrageR.getData().stream().filter(bo -> {
                    if (isBlessBag == null) return true;
                    if (isBlessBag == 0) {
                        return bo.getIsBlessBag() == null || ObjectUtil.equals(bo.getIsBlessBag(), false);
                    }
                    return ObjectUtil.equals(isBlessBag == 1, bo.getIsBlessBag());
                }).toList();
                int barrageIndex = 0;
                long videoStartTime = videoInfo.getStartTime().getTime();
                for (int i = 0; i < audioaAlyses.size(); i++) {

                    long startTime, endTime;
                    // 获取开始时间
                    startTime = videoStartTime + getStart(JSONObject.parseObject(audioaAlyses.get(i), SentenceMarkVo.class));

                    // 获取结束时间
                    // 判断是否最后一个段落
                    if (i == audioaAlyses.size() - 1) {
                        endTime = videoInfo.getEndTime().getTime() + 1L;
                    } else {
                        endTime = videoStartTime + getStart(JSONObject.parseObject(audioaAlyses.get(i + 1), SentenceMarkVo.class));
                    }

                    if (startTime != endTime) {
                        int temp = 0;
                        for (int i1 = barrageIndex; i1 < barrageBoList.size(); i1++) {
                            BarrageBo barrageBo = barrageBoList.get(i1);
                            if (barrageBo.getRecordDate() >= startTime && barrageBo.getRecordDate() < endTime) {
                                temp++;
                            } else {
                                if (barrageBo.getRecordDate() >= endTime) {
                                    break;
                                }
                            }
                            barrageIndex = i1;
                        }
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("barrageNum", temp);
                        map.put("date", DateUtil.formatDateTime(new DateTime(startTime)));
                        map.put("dateTime", startTime);
                        result.add(map);
                    }
                }
            }
        }
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(result), 14, TimeUnit.DAYS);
        return result;
    }

    private long getStart(SentenceMarkVo sentenceMarkVo) {
        if (ObjectUtil.isNotEmpty(sentenceMarkVo) && ObjectUtil.isNotEmpty(sentenceMarkVo.getItems())) {
            List<WordListItemVo> items = sentenceMarkVo.getItems();
            return items.get(0).getStartTime();
        }
        return 0L;
    }

    /**
     * 批量修改弹幕
     *
     * @param list 弹幕列表
     */
    public void updateBarrageBatch(List<BarrageBo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // 将字符串写入文件（覆盖模式）
        List<List<BarrageBo>> partition = ListUtil.partition(list, 200);
        for (List<BarrageBo> barrageBos : partition) {
            TableStoreServiceUtils.saveBatch(barrageBos, barrageTableName, syncClient);
        }
    }
}
