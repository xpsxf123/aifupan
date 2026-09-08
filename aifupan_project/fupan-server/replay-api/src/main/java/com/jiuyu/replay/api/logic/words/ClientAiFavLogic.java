package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

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
public interface ClientAiFavLogic {


    /**
     * 运营/违规收藏列表列表
     * @param clientAiFavListBo 运营/违规收藏列表列表查询参数
     * @return
     */
    R<PageUtils<ClientAiFavListVo>> queryPage(ClientAiFavListBo clientAiFavListBo);

    /**
    * 运营/违规收藏列表信息
    * @param id 运营/违规收藏列表id
    * @return
    */
    R<ClientAiFavInfoVo> info(Long id);

    /**
     * 新增运营/违规收藏列表
     * @param clientAiFavBo 运营/违规收藏列表对象
     * @return
     */
    R<String> save(ClientAiFavBo clientAiFavBo);

    /**
     * 修改运营/违规收藏列表
     * @param clientAiFavBo 运营/违规收藏列表对象
     * @return
     */
    R<String> update(ClientAiFavBo clientAiFavBo);

    /**
     * 删除运营/违规收藏列表
     * @param id 运营/违规收藏列表id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 批量删除运营/违规收藏列表
     * @param ids 运营/违规收藏列表id集合
     * @return
     */
    R<String> batchDelete(List<String> ids);

    /**
     * 新增或修改运营/违规收藏列表
     * @param clientAiFavBo 运营/违规收藏列表对象
     * @return
     */
    R<String> saveOrUpdate(ClientAiFavBo clientAiFavBo);

    /**
     * 新增或修改运营/违规收藏列表-如果有已经删除则不新增
     * @param clientAiFavBo
     * @return
     */
    R<String> saveOrUpdateByNotExist(ClientAiFavBo clientAiFavBo);

}

