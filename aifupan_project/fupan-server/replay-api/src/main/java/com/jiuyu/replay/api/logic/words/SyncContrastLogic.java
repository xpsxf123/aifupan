package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.ClientContrastListBo;
import com.jiuyu.replay.words.bo.SyncContrastBo;
import com.jiuyu.replay.words.bo.SyncContrastListBo;
import com.jiuyu.replay.words.vo.OnlineContrastAnalysisInfoVo;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;
import com.jiuyu.replay.words.vo.SyncContrastListVo;

import java.util.List;


/**
 * 客户端对比数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
public interface SyncContrastLogic {


    /**
     * 客户端对比数据列表
     * @param syncContrastListBo 客户端对比数据列表查询参数
     * @return
     */
    R<PageUtils<SyncContrastListVo>> queryPage(SyncContrastListBo syncContrastListBo);

    /**
    * 客户端对比数据信息
    * @param id 客户端对比数据id
    * @return
    */
    R<SyncContrastInfoVo> info(Long id);

    /**
     * 新增客户端对比数据
     * @param syncContrastBo 客户端对比数据对象
     * @return
     */
    R<String> save(SyncContrastBo syncContrastBo);

    /**
     * 修改客户端对比数据
     * @param syncContrastBo 客户端对比数据对象
     * @return
     */
    R<String> update(SyncContrastBo syncContrastBo);

    /**
     * 删除客户端对比数据
     * @param id 客户端对比数据id
     * @return
     */
    R<String> delete(Long id);
    
    /**
     * 获取用户的对比数据列表
     * @return
     */
    R<List<SyncContrastInfoVo>> listByToken();

    /**
     * 根据对比唯一标识获取对比数据信息
     * @param contrastId 对比唯一标识
     * @return
     */
    R<SyncContrastInfoVo> infoByContrastId(String contrastId);

    /**
     * 分析记录页面-对比分析列表分页接口
     *
     * @param syncContrastListBo 查询参数
     * @return 分页数据
     */
    R<PageUtils<SyncContrastListVo>> listAllSyncContrastNew(SyncContrastListBo syncContrastListBo);

    /**
     * PC后端获取对比分析数据
     * @param contrastId 对比分析唯一标识
     * @return
     */
    R<OnlineContrastAnalysisInfoVo> getContrastAnalysisInfo(String contrastId) throws Exception;

    /**
     * 客户端获取对比列表
     * @param clientContrastListBo 查询参数
     * @return
     */
    R<PageUtils<SyncContrastInfoVo>> clientContrastList(ClientContrastListBo clientContrastListBo);

    /**
     * 客户端删除对比
     * @param ids 对比uuid集合
     * @return
     */
    R<String> clientDeleteContrast(List<String> ids);

    /**
     * 客户端添加对比
     * @param syncContrastBo 对比数据
     * @return
     */
    R<String> clientAddContrast(SyncContrastBo syncContrastBo);

    /**
     * 客户端删除云空间对比
     * @param contrastId 对比uuid
     * @return
     */
    R<String> clientDeleteCloudContrast(String contrastId);

    /**
     * 客户端获取云空间对比列表
     * @param clientContrastListBo 查询参数
     * @return
     */
    R<PageUtils<SyncContrastInfoVo>> clientListCloudContrast(ClientContrastListBo clientContrastListBo);

    /**
     * 客户端获取对比记录信息
     * @param contrastId 对比uuid
     * @return
     */
    R<SyncContrastInfoVo> clientGetContrast(String contrastId);

    /**
     * 客户端添加云空间对比
     * @param syncContrastBo 对比数据
     * @return
     */
    R<String> clientAddCloudContrast(SyncContrastBo syncContrastBo);
}

