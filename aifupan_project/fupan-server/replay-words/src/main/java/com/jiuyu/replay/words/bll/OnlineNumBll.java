package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.OnlineNumBo;
import com.jiuyu.replay.words.bo.OnlineNumListBo;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.producer.OnlineNumProducer;
import com.jiuyu.replay.words.producer.SocketCollectMessageProducer;
import com.jiuyu.replay.words.vo.OnlineNumInfoVo;
import com.jiuyu.replay.words.vo.OnlineNumListVo;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * 直播实时在线人数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@Component
public class OnlineNumBll {

    @Resource
    private OnlineNumProducer onlineNumProducer;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private SocketCollectMessageProducer socketCollectMessageProducer;


    /**
     * 直播实时在线人数列表
     * @param onlineNumListBo 直播实时在线人数列表查询参数
     * @return
     */
    public R<PageUtils<OnlineNumListVo>> queryPage(OnlineNumListBo onlineNumListBo) {

        return R.ok("获取成功", onlineNumProducer.queryPage(onlineNumListBo));
    }

    /**
    * 直播实时在线人数信息
    * @param id 直播实时在线人数id
    * @return
    */
    public R<OnlineNumInfoVo> info(Long id) {

        OnlineNumInfoVo onlineNumInfoVo = onlineNumProducer.info(id);
        return R.ok("获取成功", onlineNumInfoVo);
    }

    /**
     * 新增直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
    public R<String> save(OnlineNumBo onlineNumBo) {

//        OnlineNumInfoVo onlineNumInfoVo = onlineNumProducer.save(onlineNumBo);
        onlineNumBo.setPeopleNum(CommonUtils.removeWanAdd(onlineNumBo.getPeopleNum()));
        // 将数据存储到redis，再定时统一批量插入数据库
//        String key = this.wordsProperties.getRedisOnlineNumInfo() + SnowflakeManager.nextValue();
//        this.redisTemplate.opsForValue().set(key, onlineNumBo, Duration.ofHours(1));
        this.redisTemplate.opsForList().rightPush(this.wordsProperties.getRedisOnlineNumInfo(), onlineNumBo);

        return R.ok("添加成功");
    }

    /**
     * 修改直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
    public R<String> update(OnlineNumBo onlineNumBo) {

        onlineNumProducer.update(onlineNumBo);
        return R.ok("修改成功");
    }

    /**
     * 删除直播实时在线人数
     * @param id 直播实时在线人数id
     * @return
     */
    public R<String> delete(Long id) {

        onlineNumProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 新增或修改直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
    public R<String> saveOrUpdate(OnlineNumBo onlineNumBo) throws IOException {
//        RRException.isNotEmpty(onlineNumBo.getBatchNumber(), "批次号不能为空");
//        RRException.isNotEmpty(onlineNumBo.getUserId(), "用户不能为空");
//        RRException.isNotEmpty(onlineNumBo.getVideoId(), "视频号不能为空");
//
//        UploadSocketMessageBo bo = new UploadSocketMessageBo();
//        BeanUtil.copyProperties(onlineNumBo, bo);
//        if (ObjectUtil.isNotEmpty(onlineNumBo.getPeopleNumData())){
//            ArrayList<SocketProcessDataBo> datas = new ArrayList<>();
//            // setdatas
//            for (String data : onlineNumBo.getPeopleNumData().split("_")) {
//                String[] split = data.split("@");
//                String date = split[0];
//                if (split.length == 1) continue;
//                String num = CommonUtils.removeWanAdd(split[1]);
//                SocketProcessDataBo socketProcessDataBo = new SocketProcessDataBo();
//                socketProcessDataBo.setTime(date);
//                socketProcessDataBo.setRenshu(num);
//                datas.add(socketProcessDataBo);
//            }
//            bo.setStartDate(DateUtil.parse(datas.get(0).getTime()));
//            bo.setEndDate(DateUtil.parse(datas.get(datas.size() - 1).getTime()));
//            SocketDataBo socketDataBo = new SocketDataBo(bo);
//            socketDataBo.setVersion("1.0");
//            socketDataBo.setDatas(datas);
//            bo.setWebSocketData(JSONUtil.toJsonStr(socketDataBo));
//        }
//        if (ObjectUtil.isNotEmpty(bo.getVideoId())){
//            String[] split = bo.getVideoId().split("_");
//            for (String s : split) {
//                if (ObjectUtil.isNotEmpty(s)){
//                    bo.setVideoId(s);
//                    socketCollectMessageProducer.uploadSocketMessage(bo);
//                }
//            }
//        }

        return R.ok();
    }

}

