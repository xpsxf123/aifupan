package com.jiuyu.replay.ai.rse;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.ai.vo.ShareLinkRecordListVo;
import com.jiuyu.replay.ai.vo.ShareLinkRecordInfoVo;
import com.jiuyu.replay.ai.bo.ShareLinkRecordBo;
import com.jiuyu.replay.ai.bo.ShareLinkRecordListBo;


/**
 * 分享链接记录
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-21 18:39:53
 */
public interface ShareLinkRecordRse {


    /**
     * 分享链接记录列表
     * @param shareLinkRecordListBo 分享链接记录列表查询参数
     * @return
     */
    PageUtils<ShareLinkRecordListVo> queryPage(ShareLinkRecordListBo shareLinkRecordListBo);

    /**
    * 分享链接记录信息
    * @param id 分享链接记录id
    * @return
    */
    ShareLinkRecordInfoVo info(Long id);




    /**
     * 新增分享链接记录
     * @param shareLinkRecordBo 分享链接记录对象
     * @return
     */
     ShareLinkRecordInfoVo save(ShareLinkRecordBo shareLinkRecordBo);

    /**
     * 修改分享链接记录
     * @param shareLinkRecordBo 分享链接记录对象
     * @return
     */
    void update(ShareLinkRecordBo shareLinkRecordBo);

    /**
     * 删除分享链接记录
     * @param id 分享链接记录id
     * @return
     */
    void deleteById(Long id);


    void updateUrlExpire (String value);
}

