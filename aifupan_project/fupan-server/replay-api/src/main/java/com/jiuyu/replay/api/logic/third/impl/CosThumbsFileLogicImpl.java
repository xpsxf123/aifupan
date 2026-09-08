package com.jiuyu.replay.api.logic.third.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.api.logic.third.CosThumbsFileLogic;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.system.bll.DictDataBll;
import com.jiuyu.replay.third.bll.CosThumbsFileBll;
import com.jiuyu.replay.third.bo.CosThumbsFileBo;
import com.jiuyu.replay.third.bo.CosThumbsFileListBo;
import com.jiuyu.replay.third.constant.VolcengineProperties;
import com.jiuyu.replay.third.vo.CosThumbsFileInfoVo;
import com.jiuyu.replay.third.vo.CosThumbsFileListVo;
import jakarta.annotation.Resource;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


/**
 * 点赞问答文件上传cos记录表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-21 16:50:31
 */
@Service
public class CosThumbsFileLogicImpl implements CosThumbsFileLogic {

    @Resource
    private CosThumbsFileBll cosThumbsFileBll;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private DictDataBll dictDataBll;
    @Resource
    private VolcengineProperties volcengineProperties;
    @Resource
    private RedissonClient redissonClient;


    @Override
    public R<PageUtils<CosThumbsFileListVo>> queryPage(CosThumbsFileListBo cosThumbsFileListBo) {
        return cosThumbsFileBll.queryPage(cosThumbsFileListBo);
    }

    @Override
    public R<CosThumbsFileInfoVo> info(Long id) {

        return cosThumbsFileBll.info(id);
    }

    @Override
    public R<String> save(CosThumbsFileBo cosThumbsFileBo) {

        return cosThumbsFileBll.save(cosThumbsFileBo);
    }

    @Override
    public R<String> update(CosThumbsFileBo cosThumbsFileBo) {

        return cosThumbsFileBll.update(cosThumbsFileBo);
    }

    @Override
    public R<String> delete(Long id) {

        return cosThumbsFileBll.delete(id);
    }

    @Override
    public R<CosThumbsFileInfoVo> getCosThumbsFileByContextId(String contextId) {
        return cosThumbsFileBll.getCosThumbsFileByContextId(contextId);
    }

    @Override
    public R<String> saveOrUpdateCosThumbsFile(CosThumbsFileBo cosThumbsFileListBo) {
        UserCacheVo localUser = GlobalObject.getLocalUser();

        if (ObjectUtil.isEmpty(cosThumbsFileListBo.getCoskey())){
            int contextSaveTime = volcengineProperties.getContextSaveTime();
            redisTemplate.opsForValue().set(
                    RedisCacheKey.getRedisKey(RedisCacheKey.aiVideoContextIdCacheKey, localUser.getId(),cosThumbsFileListBo.getCosType(), StrUtil.format("{}#{}", cosThumbsFileListBo.getSourceType(), cosThumbsFileListBo.getSourceId())),
                    cosThumbsFileListBo.getContextId(),
                    contextSaveTime - (60 * 5),
                    TimeUnit.SECONDS);
        }

        //获取用户缓存信息
        if (ObjectUtil.isNotEmpty(cosThumbsFileListBo.getUserId())){
            if (ObjectUtil.isNotEmpty(localUser) && ObjectUtil.isEmpty(cosThumbsFileListBo.getUserId())){
                cosThumbsFileListBo.setUserId(localUser.getId());
            }
        }
        saveOrUpdateAsync(cosThumbsFileListBo);
        return R.ok();
    }

    /**
     * 保存上下文缓存数据
     * @param cosThumbsFileListBo
     */
    public void saveOrUpdateAsync(CosThumbsFileBo cosThumbsFileListBo){
        cosThumbsFileBll.saveOrUpdateAsync(cosThumbsFileListBo);
    }
}

