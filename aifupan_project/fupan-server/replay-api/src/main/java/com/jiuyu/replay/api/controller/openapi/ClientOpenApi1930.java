package com.jiuyu.replay.api.controller.openapi;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.api.annotation.UserLock;
import com.jiuyu.replay.api.logic.system.DictDataLogic;
import com.jiuyu.replay.api.logic.words.SocketCollectMessageLogic;
import com.jiuyu.replay.common.bo.SendSocketErrorData;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.tencent.TencentCosUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.TencentCosTokenVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.system.bo.DictDataListBo;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.words.bo.UploadSocketDataBo;
import com.jiuyu.replay.words.vo.OnlineNumInfoVo;
import com.jiuyu.replay.words.vo.SocketCollectMessageInfoVo;
import com.jiuyu.replay.words.vo.SynchronizeTwoDayVideoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("replay/openapi/v1930")
@Tag(name = "客户端openAPI-1.9.30")
public class ClientOpenApi1930 {

    @Autowired
    private SocketCollectMessageLogic socketCollectMessageLogic;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private DictDataLogic dictDataLogic;


    @Operation(summary = "上传websocket采集的数据")
    @PostMapping("/uploadSocketData")
    @UserLock(prefixKey = "user_lock:onlineNum")
    public R<String> uploadSocketData(@RequestBody UploadSocketDataBo bo){
        return socketCollectMessageLogic.uploadSocketData(bo);
    }

    @Operation(summary = "获取当前系统时间")
    @GetMapping("/currentTime")
    public R<Long> currentTime(){
        return R.ok(System.currentTimeMillis());
    }

    @Operation(summary = "查询websocket采集数据是否存在")
    @GetMapping("/socketDataExist")
    public R<Boolean> socketDataExist(@Parameter(description = "直播批次号") String batchNumber,
                                        @Parameter(description = "用户Id") Long userId,
                                        @Parameter(description = "视频id") String videoId){
        if (userId == null) {
            UserCacheVo user = GlobalObject.getLocalUser();
            userId = user.getId();
        }
        return socketCollectMessageLogic.socketDataExist(batchNumber, userId, videoId);
    }

    @Operation(summary = "查询websocket采集数据-不带json")
    @GetMapping("/socketMessageInfoNotJson")
    public R<SocketCollectMessageInfoVo> socketMessageInfoNotJson(@Parameter(description = "用户Id") @RequestParam(required = false)  Long userId,
                                                           @Parameter(description = "视频id") String videoId,
                                                           @Parameter(description = "批次id") String batchNumber
    ){
        return socketCollectMessageLogic.socketMessageInfoNotJson(userId, videoId, batchNumber);
    }

    @Operation(summary = "获取在线人数列表")
    @GetMapping("/getOnlineNumList")
    public R<List<OnlineNumInfoVo>> getOnlineNumList(@Parameter(description = "直播批次号") String batchNumber,
                                                     @Parameter(description = "用户Id") @RequestParam(required = false)  Long userId,
                                                     @Parameter(description = "视频id") String videoId
    ) throws Exception {
        return socketCollectMessageLogic.getOnlineNumList(batchNumber, userId, videoId);
    }

    @Operation(summary = "获取websocket地址的方式 0：js获取，1：浏览器获取")
    @GetMapping("/getWebsocketWay")
    public R<Integer> getWebsocketWay() {
        DictDataListBo dictDataListBo = new DictDataListBo();
        dictDataListBo.setTypeLogo("websocket_address_type");
        dictDataListBo.setLimit(-1);
        R<PageUtils<DictDataListVo>> pageUtilsR = dictDataLogic.queryPage(dictDataListBo);
        if (pageUtilsR.getCode() == 0 && ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())  && !pageUtilsR.getData().getList().isEmpty()){
            List<DictDataListVo> list = pageUtilsR.getData().getList();
            return R.ok(Integer.parseInt(list.get(0).getValue()));
        }
        return R.ok(0);
    }

    @Operation(summary = "发送错误消息 ")
    @PostMapping("/sendErrorMessage")
    public R<String> sendMessage(@RequestBody SendSocketErrorData data){
        // 向列表的右边添加元素
        redisTemplate.opsForList().rightPush(RedisCacheKey.getRedisKey(RedisCacheKey.websocketErrorCacheKey, data.getType()), JSONUtil.toJsonStr(data));
        return R.ok();
    }

    @Operation(summary = "批量发送错误消息")
    @PostMapping("/sendErrorMessageList")
    public R<String> sendMessageList(@RequestBody List<SendSocketErrorData> datas){
        // 向列表的右边添加元素
        if (ObjectUtil.isNotEmpty(datas)){
            datas.forEach(data -> redisTemplate.opsForList().rightPush(RedisCacheKey.getRedisKey(RedisCacheKey.websocketErrorCacheKey, data.getType()), JSONUtil.toJsonStr(data)));
        }
        return R.ok();
    }

    @Operation(summary = "获取cos临时上传凭证 ")
    @GetMapping("/cosUploadTempToken")
    public R<TencentCosTokenVo> getCosUploadTempToken() throws Exception {
        return R.ok(TencentCosUtils.privateCosUploadTempToken());
    }

    @Operation(summary = "获取前两天的累计观看人数和场观")
    @GetMapping("/synchronizeTwoDayVideo")
    public R<SynchronizeTwoDayVideoVo> synchronizeTwoDayVideo(@Parameter(description = "用户Id") @RequestParam(required = false) Long userId) {
        return socketCollectMessageLogic.synchronizeTwoDayVideo(userId);
    }
}
