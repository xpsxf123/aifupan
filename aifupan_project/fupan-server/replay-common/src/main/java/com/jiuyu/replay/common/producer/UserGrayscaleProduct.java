package com.jiuyu.replay.common.producer;

import com.jiuyu.replay.common.bo.userGrayscale.UserGrayscaleBo;
import com.jiuyu.replay.common.bo.userGrayscale.UserGrayscaleListBo;
import com.jiuyu.replay.common.vo.userGrayscale.UserGrayscaleVo;
import com.jiuyu.replay.generic.utils.PageUtils;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/5 17:02
 */
public interface UserGrayscaleProduct {


    /**
     * 分页查询
     *
     * @param listBo 参数
     * @return 数据
     */
    PageUtils<UserGrayscaleVo> queryPage(UserGrayscaleListBo listBo);

    /**
     * 新增
     *
     * @param bos 参数
     */
    void adds(List<UserGrayscaleBo> bos);

    /**
     * 根据id集合删除
     *
     * @param ids id集合
     */
    void deleteByIds(List<Long> ids);
}
