package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.words.vo.ClientAiFavListVo;
import com.jiuyu.replay.words.vo.ClientAiFavInfoVo;
import com.jiuyu.replay.words.bo.ClientAiFavBo;
import com.jiuyu.replay.words.bo.ClientAiFavListBo;

import java.util.List;


/**
 * 运营/违规收藏列表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-03-12 16:42:18
 */
public interface ClientAiFavProducer {


    /**
     * 运营/违规收藏列表列表
     * @param clientAiFavListBo 运营/违规收藏列表列表查询参数
     * @return
     */
    PageUtils<ClientAiFavListVo> queryPage(ClientAiFavListBo clientAiFavListBo);

    /**
    * 运营/违规收藏列表信息
    * @param id 运营/违规收藏列表id
    * @return
    */
    ClientAiFavInfoVo info(Long id);

    /**
     * 新增运营/违规收藏列表
     * @param clientAiFavBo 运营/违规收藏列表对象
     * @return
     */
     ClientAiFavInfoVo save(ClientAiFavBo clientAiFavBo);

    /**
     * 修改运营/违规收藏列表
     * @param clientAiFavBo 运营/违规收藏列表对象
     * @return
     */
    void update(ClientAiFavBo clientAiFavBo);

    /**
     * 删除运营/违规收藏列表
     * @param id 运营/违规收藏列表id
     * @return
     */
    void deleteById(Long id);


    /**
     * 批量删除运营/违规收藏列表
     * @param ids 运营/违规收藏列表id集合
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    void batchDelete(List<String> ids, Long userId, Long tenantId);

    /**
     * 根据收藏类型和来源id获取信息
     * @param favType 收藏类型 0：运营助手 1：违规助手
     * @param dataResourceUuid 来源id
     * @return
     */
    ClientAiFavInfoVo infoByFavTypeAndResourceId(Integer favType, String dataResourceUuid);

    /**
     * 根据来源id集合删除记录
     * @param resourceIds 来源id集合
     */
    void removeByResourceIds(List<String> resourceIds);

    /**
     * 判断是否存在需要删除的记录
     * @param clientAiFavBo
     * @return
     */
    boolean hasDelete(ClientAiFavBo clientAiFavBo);
}

