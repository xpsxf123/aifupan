package com.jiuyu.replay.api.logic.common;

import com.jiuyu.replay.common.bo.ClientLogBo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CommonLogic {

    /**
     * 上传图片
     * @param file 图片文件
     * @param flag 压缩类型 0: 正常压缩  1：超级压缩 2：不压缩
     * @param sort 排序
     * @param resourceType 来源类型
     * @param isCheckSecurity 是否需要进行安全检测 0：否 1：是
     * @return
     */
    R<FileShowVo> uploadImg(MultipartFile file, Integer flag, Integer sort, Integer resourceType, Integer isCheckSecurity) throws IOException;

    /**
     * 显示图片
     * @param fileName 图片文件名
     * @return
     */
    byte[] showImg(String fileName);

    /**
     * 添加客户端日志
     * @param clientLogBo
     * @return
     */
    R<String> addClientLog(ClientLogBo clientLogBo);
}
