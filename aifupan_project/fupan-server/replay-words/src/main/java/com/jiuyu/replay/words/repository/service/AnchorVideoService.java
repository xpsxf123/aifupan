package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.generic.vo.words.AnchorVideoVO;
import com.jiuyu.replay.words.bo.AnchorVideoBo;
import com.jiuyu.replay.words.bo.video.HistoryBatchNumberListBo;
import com.jiuyu.replay.words.entity.AnchorVideoEntity;

import java.util.List;

public interface AnchorVideoService extends IService <AnchorVideoEntity>{

    /**
     * 获取历史批次列表
     *
     * @param bo 参数
     * @return 返回对应的列表
     */
    List<AnchorVideoEntity> historyBatchNumberList(HistoryBatchNumberListBo bo);

    /**
     * 分页接口
     *
     * @param anchorVideoVO
     */
    IPage<AnchorVideoVO> pageList(Page page, AnchorVideoBo anchorVideoVO);
}
