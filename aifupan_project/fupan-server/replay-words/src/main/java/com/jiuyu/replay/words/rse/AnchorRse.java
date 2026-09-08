package com.jiuyu.replay.words.rse;

import com.jiuyu.replay.generic.bo.words.anchor.SetAnchorTradeBo;
import com.jiuyu.replay.generic.bo.words.anchor.SubAnchorListBo;
import com.jiuyu.replay.generic.bo.words.anchor.UpdateAnchorBaseInfoBo;
import com.jiuyu.replay.generic.bo.words.anchor.UpdateAnchorUserBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;

import java.util.Collection;
import java.util.List;

/**
 * 主播相关的RSE接口
 *
 * @author AI Assistant
 */
public interface AnchorRse {

    /**
     * 获取子账号主播列表
     *
     * @param subAnchorListBo 查询参数
     * @return
     */
    PageUtils<AnchorUrlUserVo> getSubUserAnchorList(SubAnchorListBo subAnchorListBo);

    /**
     * 根据主播secUid集合获取主播列表
     * @param secUidList 主播secUid集合
     * @return
     */
    List<AnchorUrlInfoVo> listBySecUids(Collection<String> secUidList);

    /**
     * 更新主播基础信息
     *
     * @param updateAnchorBaseInfoBo 主播基础信息
     * @return
     */
    void updateAnchorBaseInfo(UpdateAnchorBaseInfoBo updateAnchorBaseInfoBo);

    /**
     * 更新用户主播信息
     *
     * @param updateAnchorUserBo 用户主播基础信息
     * @return
     */
    void updateUserAnchorInfo(UpdateAnchorUserBo updateAnchorUserBo);

    /**
     * 分页获取最近添加的主播secUid列表（按创建时间倒序）
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 主播secUid列表
     */
    List<String> listRecentAnchorSecUids(int pageNum, int pageSize);

    /**
     * 设置主播行业
     *
     * @param setAnchorTradeBo 设置主播行业参数
     */
    void setAnchorTrade(SetAnchorTradeBo setAnchorTradeBo);
}
