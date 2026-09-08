package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.generic.vo.words.AnchorVideoVO;
import com.jiuyu.replay.words.bo.AnchorVideoBo;
import com.jiuyu.replay.words.bo.video.HistoryBatchNumberListBo;
import com.jiuyu.replay.words.entity.AnchorVideoEntity;
import com.jiuyu.replay.words.repository.dao.AnchorVideoDao;
import com.jiuyu.replay.words.repository.service.AnchorVideoService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("anchorVideoService")
public class AnchorVideoServiceImpl extends ServiceImpl<AnchorVideoDao,AnchorVideoEntity>implements AnchorVideoService {

    @Resource
    private AnchorVideoDao anchorVideoDao;

    @Override
    public List<AnchorVideoEntity> historyBatchNumberList(HistoryBatchNumberListBo bo) {
        return anchorVideoDao.historyBatchNumberList(bo);
    }

    @Override
    public IPage<AnchorVideoVO> pageList(Page page, AnchorVideoBo anchorVideoVO) {
        return anchorVideoDao.pageList(page, anchorVideoVO);
    }
}
