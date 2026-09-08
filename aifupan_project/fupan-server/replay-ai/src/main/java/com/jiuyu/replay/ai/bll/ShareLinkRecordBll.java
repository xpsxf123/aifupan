package com.jiuyu.replay.ai.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.ai.bo.ShareLinkRecordBo;
import com.jiuyu.replay.ai.bo.ShareLinkRecordListBo;
import com.jiuyu.replay.ai.bo.ShareLinkRecordSaveBo;
import com.jiuyu.replay.ai.rse.ConversationRse;
import com.jiuyu.replay.ai.rse.ShareLinkRecordRse;
import com.jiuyu.replay.ai.vo.ShareLinkRecordInfoVo;
import com.jiuyu.replay.ai.vo.ShareLinkRecordListVo;
import com.jiuyu.replay.ai.vo.ShareSaveVo;
import com.jiuyu.replay.common.alibaba.AiOssUtils;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.ConversationVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoSocketReadTimeoutException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * 分享链接记录
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-21 18:39:53
 */
@Component
@Slf4j
public class ShareLinkRecordBll {

    @Resource
    private ShareLinkRecordRse shareLinkRecordProducer;
    @Resource
    private ConversationRse conversationProducer;
    @Resource
    private AiOssUtils aiOssUtils;
    @Resource
    private UserFeign userFeign;
    @Resource
    private SystemKvProducer systemKvProducer;
    @Autowired
    private ImgOssUtils imgOssUtils;


    /**
     * 分享链接记录列表
     * @param shareLinkRecordListBo 分享链接记录列表查询参数
     * @return
     */
    public R<PageUtils<ShareLinkRecordListVo>> queryPage(ShareLinkRecordListBo shareLinkRecordListBo) {

        return R.ok("获取成功", shareLinkRecordProducer.queryPage(shareLinkRecordListBo));
    }

    /**
    * 分享链接记录信息
    * @param id 分享链接记录id
    * @return
    */
    public R<ShareLinkRecordInfoVo> info(Long id) throws IOException {

        ShareLinkRecordInfoVo shareLinkRecordInfoVo = shareLinkRecordProducer.info(id);
        if (Objects.nonNull(shareLinkRecordInfoVo)&&shareLinkRecordInfoVo.getCodes()!=null){
            List<String> list = JSONUtil.toList(shareLinkRecordInfoVo.getCodes(), String.class);
            List<ConversationVo> conversationVos = new ArrayList<>();
            try {
                conversationVos = conversationProducer.listByIds(list);
                conversationVos.forEach(item -> {
                    if (ObjectUtil.isNotEmpty(item.getHtmlSavePath())) {
                        item.setHtmlDomainName(imgOssUtils.getAccessUrl());
                    }
                });
            } catch (MongoSocketOpenException | MongoSocketReadTimeoutException e) {
                log.info("MongoDB连接超时，检查服务是否启动: {}", e.getMessage());
                throw new RRException("MongoDB连接 Timed out");
            } catch (Exception e) {
                log.error("获取聊天记录失败-接口-conversationProducer.listByIds(list): {}", e.getMessage());
                throw new RRException("获取聊天记录失败");
            }
            shareLinkRecordInfoVo.setConversationVoList(conversationVos);
        }
        return R.ok("获取成功", shareLinkRecordInfoVo);
    }


    /**
     * 新增分享链接记录
     *
     * @param shareLinkRecordBo 分享链接记录对象
     * @return
     */
    public R<ShareLinkRecordInfoVo> save(ShareLinkRecordBo shareLinkRecordBo) {
        ShareLinkRecordInfoVo shareLinkRecordInfoVo = shareLinkRecordProducer.save(shareLinkRecordBo);
        return R.ok("添加成功",shareLinkRecordInfoVo);
    }

    /**
     * 修改分享链接记录
     * @param shareLinkRecordBo 分享链接记录对象
     * @return
     */
    public R<String> update(ShareLinkRecordBo shareLinkRecordBo) {

        shareLinkRecordProducer.update(shareLinkRecordBo);
        return R.ok("修改成功");
    }

    /**
     * 删除分享链接记录
     * @param id 分享链接记录id
     * @return
     */
    public R<String> delete(Long id) {

        shareLinkRecordProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 到期失效 改状态
     * @param value
     */
    public void updateUrlExpire(String value) {
        shareLinkRecordProducer.updateUrlExpire(value);
    }

    /**
     * 新增链接分享记录
     * @param shareLinkRecordBo
     * @return
     */
    public ShareSaveVo addShare(ShareLinkRecordSaveBo shareLinkRecordBo) {

        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");

        ShareLinkRecordBo linkRecordBo = new ShareLinkRecordBo();
        linkRecordBo.setUserId(user.getId());
        linkRecordBo.setTenantId(user.getActiveTenantId());
        linkRecordBo.setCodes(JSONUtil.toJsonStr(shareLinkRecordBo.getIds()));
        linkRecordBo.setSourceId(shareLinkRecordBo.getSourceId());
        linkRecordBo.setSourceType(shareLinkRecordBo.getSourceType());
        ShareLinkRecordInfoVo save = shareLinkRecordProducer.save(linkRecordBo);


        ShareSaveVo shareSaveVo = BeanUtil.copyProperties(save, ShareSaveVo.class);
        shareSaveVo.setHost("");
        SystemKvInfoVo clientFrontDomainName = systemKvProducer.getByKey("client_front_domain_name");
        if (ObjectUtil.isNotEmpty(clientFrontDomainName)){
            shareSaveVo.setHost(ObjectUtil.defaultIfEmpty(clientFrontDomainName.getKvValue(), ""));
        }
        return shareSaveVo;
    }
}

