package com.jiuyu.replay.api.logic.ai.impl;


import com.jiuyu.replay.ai.bll.ShareLinkRecordBll;
import com.jiuyu.replay.ai.bo.ShareLinkRecordBo;
import com.jiuyu.replay.ai.bo.ShareLinkRecordListBo;
import com.jiuyu.replay.ai.vo.ShareLinkRecordInfoVo;
import com.jiuyu.replay.ai.vo.ShareLinkRecordListVo;
import com.jiuyu.replay.api.logic.ai.ShareLinkRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;



import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

import java.io.IOException;


/**
 * 分享链接记录
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-21 18:39:53
 */
@Service
public class ShareLinkRecordLogicImpl implements ShareLinkRecordLogic {

    @Resource
    private ShareLinkRecordBll shareLinkRecordBll;


    @Override
    public R<PageUtils<ShareLinkRecordListVo>> queryPage(ShareLinkRecordListBo shareLinkRecordListBo) {

        return shareLinkRecordBll.queryPage(shareLinkRecordListBo);
    }

    @Override
    public R<ShareLinkRecordInfoVo> info(Long id) throws IOException {

        return shareLinkRecordBll.info(id);
    }



    @Override
    public R<ShareLinkRecordInfoVo> save(ShareLinkRecordBo shareLinkRecordBo) {

        return shareLinkRecordBll.save(shareLinkRecordBo);
    }

    @Override
    public R<String> update(ShareLinkRecordBo shareLinkRecordBo) {

        return shareLinkRecordBll.update(shareLinkRecordBo);
    }

    @Override
    public R<String> delete(Long id) {

        return shareLinkRecordBll.delete(id);
    }


}

