package com.jiuyu.replay.generic.feign.common;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.FileShowVo;

import java.util.Collection;
import java.util.List;

public interface FileFeign {

    /**
     * 根绝文件id集合获取文件列表
     * @param fileIds 文件id集合
     * @return
     */
    R<List<FileShowVo>> listByFileIds(Collection<Long> fileIds);

    /**
     * 根绝文件id获取文件信息
     * @param fileId 文件id
     * @return
     */
    R<FileShowVo> infoByFileId(Long fileId);
}
