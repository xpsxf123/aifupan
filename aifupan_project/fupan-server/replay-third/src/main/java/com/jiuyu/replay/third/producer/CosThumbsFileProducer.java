package com.jiuyu.replay.third.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
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
public interface CosThumbsFileProducer {


    /**
     * 点赞问答文件上传cos记录表列表
     * @param cosThumbsFileListBo 点赞问答文件上传cos记录表列表查询参数
     * @return
     */
    PageUtils<CosThumbsFileListVo> queryPage(CosThumbsFileListBo cosThumbsFileListBo);

    /**
    * 点赞问答文件上传cos记录表信息
    * @param id 点赞问答文件上传cos记录表id
    * @return
    */
    CosThumbsFileInfoVo info(Long id);

    /**
     * 新增点赞问答文件上传cos记录表
     * @param cosThumbsFileBo 点赞问答文件上传cos记录表对象
     * @return
     */
     CosThumbsFileInfoVo save(CosThumbsFileBo cosThumbsFileBo);

    /**
     * 修改点赞问答文件上传cos记录表
     * @param cosThumbsFileBo 点赞问答文件上传cos记录表对象
     * @return
     */
    void update(CosThumbsFileBo cosThumbsFileBo);

    /**
     * 删除点赞问答文件上传cos记录表
     * @param id 点赞问答文件上传cos记录表id
     * @return
     */
    void deleteById(Long id);


    /**
     * 点赞问答文件上传cos记录表列表
     * @param cosThumbsFileListBo
     */
    void saveOrUpdate(CosThumbsFileBo cosThumbsFileListBo);

    /**
     * 点赞问答文件上传cos记录表列表
     * @param contextId
     * @return
     */
    CosThumbsFileInfoVo getCosThumbsFileByContextId(String contextId);
}

