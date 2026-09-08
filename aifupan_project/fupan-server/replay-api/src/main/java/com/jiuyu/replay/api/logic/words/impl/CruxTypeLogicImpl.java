package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.CruxTypeLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.CruxTypeBll;
import com.jiuyu.replay.words.bo.CruxTypeBo;
import com.jiuyu.replay.words.bo.CruxTypeListBo;
import com.jiuyu.replay.generic.vo.words.CruxTypeInfoVo;
import com.jiuyu.replay.words.vo.CruxTypeListVo;
import com.jiuyu.replay.words.vo.CruxTypeTreeVo;
import com.jiuyu.replay.generic.vo.words.CruxTypeVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 关键词类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Service
public class CruxTypeLogicImpl implements CruxTypeLogic {

    @Resource
    private CruxTypeBll cruxTypeBll;


    @Override
    public R<PageUtils<CruxTypeListVo>> queryPage(CruxTypeListBo cruxTypeListBo) {

        return cruxTypeBll.queryPage(cruxTypeListBo);
    }

    @Override
    public R<CruxTypeInfoVo> info(Long id) {

        return cruxTypeBll.info(id);
    }

    @Override
    public R<String> save(CruxTypeBo cruxTypeBo) {

        return cruxTypeBll.save(cruxTypeBo);
    }

    @Override
    public R<String> update(CruxTypeBo cruxTypeBo) {

        return cruxTypeBll.update(cruxTypeBo);
    }

    @Override
    public R<String> delete(Long id) {

        return cruxTypeBll.delete(id);
    }

    @Override
    public R<List<CruxTypeVo>> getShowCompassList() {

        return cruxTypeBll.getShowCompassList();
    }

    @Override
    public R<List<CruxTypeTreeVo>> listTree(Integer childrenNotNull) {

        return cruxTypeBll.listTree(childrenNotNull);
    }


}

