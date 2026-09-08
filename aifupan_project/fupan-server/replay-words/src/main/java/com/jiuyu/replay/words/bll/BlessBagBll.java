package com.jiuyu.replay.words.bll;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.BlessBagInfoVo;
import com.jiuyu.replay.generic.vo.words.BlessBagListVo;
import com.jiuyu.replay.words.bo.BlessBagBo;
import com.jiuyu.replay.words.bo.BlessBagListBo;
import com.jiuyu.replay.words.producer.BlessBagProducer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;


/**
 * 福袋信息
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 20:02:21
 */
@Component
public class BlessBagBll {

    @Resource
    private BlessBagProducer blessBagProducer;
    @Resource
    private UserFeign userFeign;


    /**
     * 福袋信息列表
     * @param blessBagListBo 福袋信息列表查询参数
     * @return
     */
    public R<PageUtils<BlessBagListVo>> queryPage(BlessBagListBo blessBagListBo) {

        return R.ok("获取成功", blessBagProducer.queryPage(blessBagListBo));
    }

    /**
    * 福袋信息信息
    * @param id 福袋信息id
    * @return
    */
    public R<BlessBagInfoVo> info(Long id) {

        BlessBagInfoVo blessBagInfoVo = blessBagProducer.info(id);
        return R.ok("获取成功", blessBagInfoVo);
    }


    public R<List<BlessBagInfoVo>> infoByVideo(String videoId) {

        List<BlessBagInfoVo> blessBagInfoVos = blessBagProducer.infoByVideo(videoId);
        return R.ok("获取成功", blessBagInfoVos);
    }


    /**
     * 新增福袋信息
     * @param blessBagBo 福袋信息对象
     * @return
     */
    public R<String> save(BlessBagBo blessBagBo) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");
        if (ObjectUtil.isEmpty(blessBagBo.getUserId())){
            blessBagBo.setUserId(user.getId());
        }
        if (ObjectUtil.isEmpty(blessBagBo.getTenantId())){
            blessBagBo.setTenantId(user.getActiveTenantId());
        }
        // 去掉lotteryInfo中无用的参数
        if (ObjectUtil.isNotEmpty(blessBagBo.getLotteryInfo())) {
            JSONObject jsonObject = JSONObject.parseObject(blessBagBo.getLotteryInfo());
            JSONObject jsonObjectNew = new JSONObject();

            Stream.of("lottery_id", "owner_user_id", "anchor_id", "owner_type", "room_id",
                            "status", "prize_info", "conditions", "prize_count", "lucky_count", "count_down", "start_time", "draw_time",
                            "real_lucky_count", "total_lucky_record_count", "total_grant_count", "withdraw_count", "real_draw_time",
                            "current_time", "candidate_num", "lottery_id_str", "room_id_str", "use_new_draw_interaction", "launch_task_id",
                            "index", "dilatation_now")
                    .filter(jsonObject::containsKey)
                    .forEach(fieldName -> jsonObjectNew.put(fieldName, jsonObject.get(fieldName)));
            blessBagBo.setLotteryInfo(JSONObject.toJSONString(jsonObjectNew));
        }

        BlessBagInfoVo blessBagInfoVo = blessBagProducer.save(blessBagBo);
        return R.ok("添加成功");
    }

    /**
     * 修改福袋信息
     * @param blessBagBo 福袋信息对象
     * @return
     */
    public R<String> update(BlessBagBo blessBagBo) {

        blessBagProducer.update(blessBagBo);
        return R.ok("修改成功");
    }

    /**
     * 删除福袋信息
     * @param id 福袋信息id
     * @return
     */
    public R<String> delete(Long id) {

        blessBagProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据视频id查询福袋信息
     * @param videoId
     * @return
     */
    public List<BlessBagInfoVo> listByVideoId(String videoId) {
        return blessBagProducer.listByVideoId(videoId);
    }
}

