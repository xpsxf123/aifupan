package com.jiuyu.replay.api.logic.common.impl;

import com.jiuyu.replay.api.constant.Constant;
import com.jiuyu.replay.api.logic.common.FileLogic;
import com.jiuyu.replay.common.bll.FileBll;
import com.jiuyu.replay.common.bo.FileListBo;
import com.jiuyu.replay.common.bo.UpdateFileBo;
import com.jiuyu.replay.common.vo.FileInfoVo;
import com.jiuyu.replay.common.vo.FileListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


/**
 * 文件
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-15 18:18:36
 */
@Service
public class FileLogicImpl implements FileLogic {

    @Resource
    private FileBll fileBll;


    @Override
    public R<PageUtils<FileListVo>> queryPage(FileListBo fileListBo) {

        return fileBll.queryPage(fileListBo);
    }

    @Override
    public R<FileInfoVo> info(Long id) {

        return fileBll.info(id);
    }


    @Override
    public R<String> update(UpdateFileBo updateFileBo) {

        fileBll.updateOne(updateFileBo.getResourceId(), updateFileBo.getResourceType(), updateFileBo.getRemarks(), updateFileBo.getId());
        return R.ok("修改成功");
    }

    @Override
    public R<String> delete(Long id) {

        return fileBll.delete(id);
    }

    @Override
    public R<FileShowVo> showOne(Long resourceId, Integer resourceType) {
        FileShowVo fileShowVo = fileBll.getByResourceIdAndType(resourceId, resourceType);
        if(fileShowVo != null) {
            return R.ok(fileShowVo);
        }
        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "文件不存在");
    }

    /**
     * 上传压缩文件
     * @param file
     * @return
     */
    @Override
    public R<String> uploadAifuPa(MultipartFile file) throws IOException {
        return fileBll.uploadAifuPa(file);
    }


}

