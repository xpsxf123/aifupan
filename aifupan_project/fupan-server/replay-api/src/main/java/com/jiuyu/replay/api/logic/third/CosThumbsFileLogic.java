package com.jiuyu.replay.api.logic.third;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.bo.CosThumbsFileBo;
import com.jiuyu.replay.third.bo.CosThumbsFileListBo;
import com.jiuyu.replay.third.vo.CosThumbsFileInfoVo;
import com.jiuyu.replay.third.vo.CosThumbsFileListVo;


/**
 * 点赞问答文件上传cos记录表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-21 16:50:31
 */
public interface CosThumbsFileLogic {


    /**
     * 点赞问答文件上传cos记录表列表
     * @param cosThumbsFileListBo 点赞问答文件上传cos记录表列表查询参数
     * @return
     */
    R<PageUtils<CosThumbsFileListVo>> queryPage(CosThumbsFileListBo cosThumbsFileListBo);

    /**
    * 点赞问答文件上传cos记录表信息
    * @param id 点赞问答文件上传cos记录表id
    * @return
    */
    R<CosThumbsFileInfoVo> info(Long id);

    /**
     * 新增点赞问答文件上传cos记录表
     * @param cosThumbsFileBo 点赞问答文件上传cos记录表对象
     * @return
     */
    R<String> save(CosThumbsFileBo cosThumbsFileBo);

    /**
     * 修改点赞问答文件上传cos记录表
     * @param cosThumbsFileBo 点赞问答文件上传cos记录表对象
     * @return
     */
    R<String> update(CosThumbsFileBo cosThumbsFileBo);

    /**
     * 删除点赞问答文件上传cos记录表
     * @param id 点赞问答文件上传cos记录表id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 保存或更新点赞问答文件上传cos记录表
     * @param cosThumbsFileListBo
     * @return
     */
    R<String> saveOrUpdateCosThumbsFile(CosThumbsFileBo cosThumbsFileListBo) throws InterruptedException, Throwable;

    /**
     * 根据contextId获取点赞问答文件上传cos记录表
     * @param contextId
     * @return
     */
    R<CosThumbsFileInfoVo> getCosThumbsFileByContextId(String contextId);
}

