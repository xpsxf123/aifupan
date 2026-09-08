package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.words.BlessBagListVo;
import com.jiuyu.replay.generic.vo.words.BlessBagInfoVo;
import com.jiuyu.replay.words.bo.BlessBagBo;
import com.jiuyu.replay.words.bo.BlessBagListBo;

import java.util.List;


/**
 * 福袋信息
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 20:02:21
 */
public interface BlessBagLogic {


    /**
     * 福袋信息列表
     * @param blessBagListBo 福袋信息列表查询参数
     * @return
     */
    R<PageUtils<BlessBagListVo>> queryPage(BlessBagListBo blessBagListBo);

    /**
    * 福袋信息信息
    * @param id 福袋信息id
    * @return
    */
    R<BlessBagInfoVo> info(Long id);

    /**
     * 新增福袋信息
     * @param blessBagBo 福袋信息对象
     * @return
     */
    R<String> save(BlessBagBo blessBagBo);

    /**
     * 修改福袋信息
     * @param blessBagBo 福袋信息对象
     * @return
     */
    R<String> update(BlessBagBo blessBagBo);

    /**
     * 删除福袋信息
     * @param id 福袋信息id
     * @return
     */
    R<String> delete(Long id);


    R<List<BlessBagInfoVo>> infoByVideo(String videoId);

    /**
     * 按租户 + 视频id列表查询全部福袋信息（仅租户隔离，不做用户级过滤，内部分批查询）
     * @param tenantId 租户id
     * @param videoIds 视频id列表
     * @return 福袋信息列表
     */
    R<List<BlessBagListVo>> listByVideoIds(Long tenantId, List<String> videoIds);
}

