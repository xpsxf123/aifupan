package com.jiuyu.replay.common.api;

import com.jiuyu.replay.common.bll.FileBll;
import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.generic.feign.common.FileFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileApi implements FileFeign {

    @Resource
    private FileBll fileBll;

    @Override
    public R<List<FileShowVo>> listByFileIds(Collection<Long> fileIds) {

        List<FileShowVo> fileShowVos = this.fileBll.listByFileIds(fileIds);
        if(fileShowVos != null && fileShowVos.size() > 0) {
            List<FileShowVo> fileShowVoList = fileShowVos.stream().map(item -> {
                FileShowVo fileShowVo = new FileShowVo();
                BeanUtils.copyProperties(item, fileShowVo);
                return fileShowVo;
            }).collect(Collectors.toList());

            return R.ok(fileShowVoList);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "文件列表信息为空");
    }

    @Override
    public R<FileShowVo> infoByFileId(Long fileId) {

        FileShowVo fileShowVo = this.fileBll.infoByFileId(fileId);
        if(fileShowVo != null) {
            FileShowVo fileShowInfo = new FileShowVo();
            BeanUtils.copyProperties(fileShowVo, fileShowInfo);
            return R.ok(fileShowInfo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "文件信息为空");
    }
}
