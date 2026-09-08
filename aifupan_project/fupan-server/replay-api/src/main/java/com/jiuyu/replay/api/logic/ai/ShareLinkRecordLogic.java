package com.jiuyu.replay.api.logic.ai;

import com.jiuyu.replay.ai.bo.ShareLinkRecordBo;
import com.jiuyu.replay.ai.bo.ShareLinkRecordListBo;
import com.jiuyu.replay.ai.vo.ShareLinkRecordInfoVo;
import com.jiuyu.replay.ai.vo.ShareLinkRecordListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import java.io.IOException;


/**
 * 分享链接记录
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-21 18:39:53
 */
public interface ShareLinkRecordLogic {


    /**
     * 分享链接记录列表
     * @param shareLinkRecordListBo 分享链接记录列表查询参数
     * @return
     */
    R<PageUtils<ShareLinkRecordListVo>> queryPage(ShareLinkRecordListBo shareLinkRecordListBo);

    /**
    * 分享链接记录信息
    * @param id 分享链接记录id
    * @return
    */
    R<ShareLinkRecordInfoVo> info(Long id) throws IOException;



    /**
     * 新增分享链接记录
     *
     * @param shareLinkRecordBo 分享链接记录对象
     * @return
     */
    R<ShareLinkRecordInfoVo> save(ShareLinkRecordBo shareLinkRecordBo);

    /**
     * 修改分享链接记录
     * @param shareLinkRecordBo 分享链接记录对象
     * @return
     */
    R<String> update(ShareLinkRecordBo shareLinkRecordBo);

    /**
     * 删除分享链接记录
     * @param id 分享链接记录id
     * @return
     */
    R<String> delete(Long id);


}

