package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.ThirdpartyLiveRecordVo;
import com.jiuyu.replay.words.producer.ThirdpartyLiveRecordProducer;
import com.jiuyu.replay.words.producer.TradeProducer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 第三方直播录制记录Bll
 *
 * @author System
 * @date 2026-04-09
 */
@Slf4j
@Component
@AllArgsConstructor
public class ThirdpartyLiveRecordBll {

    private final ThirdpartyLiveRecordProducer thirdpartyLiveRecordProducer;
    private final TradeProducer tradeProducer;

    /**
     * 最少返回记录数
     */
    private static final int MIN_RECORDS = 3;

    /**
     * 最多返回记录数
     */
    private static final int MAX_RECORDS = 5;

    /**
     * 按行业随机获取直播录制记录（四级降级查询）
     *
     * @param tradeId 行业ID
     * @return 3-5条记录
     */
    public R<List<ThirdpartyLiveRecordVo>> randomLiveRecords(Long tradeId) {
        // 随机生成3-5之间的数量
        int randomCount = ThreadLocalRandom.current().nextInt(MIN_RECORDS, MAX_RECORDS + 1);

        // 第1步：精确匹配
        log.info("第1步：精确匹配 tradeId={}", tradeId);
        List<ThirdpartyLiveRecordVo> records = thirdpartyLiveRecordProducer.randomByTradeIds(
                Collections.singletonList(tradeId), randomCount);
        if (!records.isEmpty()) {
            log.info("第1步成功，返回{}条记录", records.size());
            return R.ok("获取成功", records);
        }

        // 第2步：向下查子级
        log.info("第2步：向下查子级");
        List<Long> childIds = tradeProducer.getChildById(tradeId);
        // 去掉自身（第1步已查过）
        childIds.remove(tradeId);
        if (!childIds.isEmpty()) {
            records = thirdpartyLiveRecordProducer.randomByTradeIds(childIds, randomCount);
            if (!records.isEmpty()) {
                log.info("第2步成功，返回{}条记录", records.size());
                return R.ok("获取成功", records);
            }
        }

        // 第3步：向上查父级
        log.info("第3步：向上查父级");
        var parentTradeInfos = tradeProducer.listParentsByTradeId(tradeId, 0);
        List<Long> parentIds = parentTradeInfos == null ? Collections.emptyList() :
                parentTradeInfos.stream()
                .map(vo -> vo.getId())
                .filter(id -> !id.equals(tradeId)) // 去掉自身
                .collect(Collectors.toList());
        if (!parentIds.isEmpty()) {
            records = thirdpartyLiveRecordProducer.randomByTradeIds(parentIds, randomCount);
            if (!records.isEmpty()) {
                log.info("第3步成功，返回{}条记录", records.size());
                return R.ok("获取成功", records);
            }
        }

        // 第4步：兜底随机
        log.info("第4步：兜底随机");
        records = thirdpartyLiveRecordProducer.randomAll(randomCount);
        log.info("第4步完成，返回{}条记录", records.size());
        return R.ok("获取成功", records);
    }
}
