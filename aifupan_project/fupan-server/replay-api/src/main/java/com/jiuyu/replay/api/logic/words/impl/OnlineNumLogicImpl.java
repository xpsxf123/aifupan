package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.api.logic.words.OnlineNumLogic;
import com.jiuyu.replay.common.tencent.TencentCosUtils;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.ReplayFileUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.words.bll.OnlineNumBll;
import com.jiuyu.replay.words.bll.SocketCollectMessageBll;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.vo.OnlineNumInfoVo;
import com.jiuyu.replay.words.vo.OnlineNumListVo;
import com.jiuyu.replay.words.vo.SocketCollectMessageInfoVo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


/**
 * 直播实时在线人数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@Service
public class OnlineNumLogicImpl implements OnlineNumLogic {

    private static final Logger log = LoggerFactory.getLogger(OnlineNumLogicImpl.class);
    @Resource
    private OnlineNumBll onlineNumBll;
    @Resource
    private SocketCollectMessageBll socketCollectMessageBll;


    @Override
    public R<PageUtils<OnlineNumListVo>> queryPage(OnlineNumListBo onlineNumListBo) {

        return onlineNumBll.queryPage(onlineNumListBo);
    }

    @Override
    public R<OnlineNumInfoVo> info(Long id) {

        return onlineNumBll.info(id);
    }

    @Override
    public R<String> save(OnlineNumBo onlineNumBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        onlineNumBo.setUserId(user.getId());

        return onlineNumBll.save(onlineNumBo);
    }

    @Override
    public R<String> update(OnlineNumBo onlineNumBo) {

        return onlineNumBll.update(onlineNumBo);
    }

    @Override
    public R<String> delete(Long id) {

        return onlineNumBll.delete(id);
    }

    @Override
    public R<String> saveOrUpdate(OnlineNumBo onlineNumBo) throws Exception {

        UserCacheVo user = GlobalObject.getLocalUser();
        onlineNumBo.setUserId(user.getId());
        onlineNumBo.setTenantId(user.getActiveTenantId());

        RRException.isNotEmpty(onlineNumBo.getBatchNumber(), "批次号不能为空");
        RRException.isNotEmpty(onlineNumBo.getUserId(), "用户不能为空");
        RRException.isNotEmpty(onlineNumBo.getVideoId(), "视频号不能为空");

        UploadSocketMessageBo bo = new UploadSocketMessageBo();
        BeanUtil.copyProperties(onlineNumBo, bo);
        if (ObjectUtil.isNotEmpty(onlineNumBo.getPeopleNumData())){
            ArrayList<SocketProcessDataBo> datas = new ArrayList<>();
            // setdatas
            for (String data : onlineNumBo.getPeopleNumData().split("_")) {
                String[] split = data.split("@");
                String date = split[0];
                if (split.length == 1) continue;
                String num = CommonUtils.removeWanAdd(split[1]);
                SocketProcessDataBo socketProcessDataBo = new SocketProcessDataBo();
                socketProcessDataBo.setTime(date);
                socketProcessDataBo.setRenshu(num);
                datas.add(socketProcessDataBo);
            }
            bo.setStartDate(DateUtil.parse(datas.get(0).getTime()));
            bo.setEndDate(DateUtil.parse(datas.get(datas.size() - 1).getTime()));
            SocketDataBo socketDataBo = new SocketDataBo(bo);
            socketDataBo.setVersion("1.0");
            socketDataBo.setDatas(datas);
            bo.setWebSocketData(JSONUtil.toJsonStr(socketDataBo));
        }
        if (ObjectUtil.isNotEmpty(bo.getVideoId())){
            String[] split = bo.getVideoId().split("_");
            for (String s : split) {
                if (ObjectUtil.isNotEmpty(s)){
                    bo.setVideoId(s);
                    R<SocketCollectMessageInfoVo> byBatch = socketCollectMessageBll.getByBatch(bo.getBatchNumber(), bo.getUserId(), bo.getVideoId());
                    String cosKey = "socketMessageFile/" + new SimpleDateFormat("yyyy/MM/dd/").format(new Date())+ UUID.randomUUID() + ".zip";
                    if (ObjectUtil.isNotEmpty(bo.getWebSocketData())){
                        if (byBatch.getCode() == 0 && ObjectUtil.isNotEmpty(byBatch.getData())){
                            SocketCollectMessageInfoVo data = byBatch.getData();
                            if (ObjectUtil.isNotEmpty(data.getCosKey())){
                                cosKey = data.getCosKey();
                            }
                        }
                    }
                    // 上传到cos
                    ByteArrayOutputStream zipStream = ReplayFileUtils.createZipStream(bo.getWebSocketData(), bo.getVideoId()+".txt");
                    if (TencentCosUtils.putStreamObject(TencentCosUtils.getPrivateCosBucketName(), new ByteArrayInputStream(zipStream.toByteArray()), cosKey)) {
                        // 添加缓存
                        socketCollectMessageBll.setWebsocketRedisData(bo.getVideoId(), bo.getWebSocketData());

                        UploadSocketDataBo uploadSocketDataBo = BeanUtil.copyProperties(bo, UploadSocketDataBo.class);
                        uploadSocketDataBo.setCosKey(cosKey);
                        socketCollectMessageBll.uploadSocketData(uploadSocketDataBo);
                    }else{
                        log.error("在线人数上传cos失败");
                        return R.error(30001,"上传失败");
                    }
                }
            }
        }

        return R.ok();
    }


}

