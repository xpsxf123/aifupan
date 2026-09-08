package com.jiuyu.replay.third.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.bo.CosThumbsFileBo;
import com.jiuyu.replay.third.bo.CosThumbsFileListBo;
import com.jiuyu.replay.third.producer.CosThumbsFileProducer;
import com.jiuyu.replay.third.vo.CosThumbsFileInfoVo;
import com.jiuyu.replay.third.vo.CosThumbsFileListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 点赞问答文件上传cos记录表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-21 16:50:31
 */
@Component
public class CosThumbsFileBll {

    @Resource
    private CosThumbsFileProducer cosThumbsFileProducer;


    /**
     * 点赞问答文件上传cos记录表列表
     * @param cosThumbsFileListBo 点赞问答文件上传cos记录表列表查询参数
     * @return
     */
    public R<PageUtils<CosThumbsFileListVo>> queryPage(CosThumbsFileListBo cosThumbsFileListBo) {

        return R.ok("获取成功", cosThumbsFileProducer.queryPage(cosThumbsFileListBo));
    }

    /**
    * 点赞问答文件上传cos记录表信息
    * @param id 点赞问答文件上传cos记录表id
    * @return
    */
    public R<CosThumbsFileInfoVo> info(Long id) {

        CosThumbsFileInfoVo cosThumbsFileInfoVo = cosThumbsFileProducer.info(id);
        return R.ok("获取成功", cosThumbsFileInfoVo);
    }

    /**
     * 新增点赞问答文件上传cos记录表
     * @param cosThumbsFileBo 点赞问答文件上传cos记录表对象
     * @return
     */
    public R<String> save(CosThumbsFileBo cosThumbsFileBo) {

        CosThumbsFileInfoVo cosThumbsFileInfoVo = cosThumbsFileProducer.save(cosThumbsFileBo);
        return R.ok("添加成功");
    }

    /**
     * 修改点赞问答文件上传cos记录表
     * @param cosThumbsFileBo 点赞问答文件上传cos记录表对象
     * @return
     */
    public R<String> update(CosThumbsFileBo cosThumbsFileBo) {

        cosThumbsFileProducer.update(cosThumbsFileBo);
        return R.ok("修改成功");
    }

    /**
     * 删除点赞问答文件上传cos记录表
     * @param id 点赞问答文件上传cos记录表id
     * @return
     */
    public R<String> delete(Long id) {

        cosThumbsFileProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 保存上下文缓存数据
     * @param cosThumbsFileListBo
     */
    public void saveOrUpdateAsync(CosThumbsFileBo cosThumbsFileListBo) {
        cosThumbsFileProducer.saveOrUpdate(cosThumbsFileListBo);
    }

    /**
     * 根据contextId查询点赞问答文件上传cos记录表
     * @param contextId
     * @return
     */
    public R<CosThumbsFileInfoVo> getCosThumbsFileByContextId(String contextId) {
        CosThumbsFileInfoVo cosThumbsFileInfoVo = cosThumbsFileProducer.getCosThumbsFileByContextId(contextId);
        return R.ok("获取成功", cosThumbsFileInfoVo);
    }
}

