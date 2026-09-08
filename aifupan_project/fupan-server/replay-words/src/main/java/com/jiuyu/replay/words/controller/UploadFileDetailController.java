package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.UploadFileDetailBll;
import com.jiuyu.replay.words.vo.file.UploadFileDetailInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：liwj
 * &#064;description：文件上传详情的控制器
 * @date ：2025/9/18 18:45
 */
@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("replay/words/uploadFileDetail")
@Tag(name = "文件上传详情的控制器")
public class UploadFileDetailController {

    private final UploadFileDetailBll uploadFileDetailBll;


    @GetMapping("infoByFileId")
    @Schema(description = "获取上传文件详情")
    public R<UploadFileDetailInfoVo> infoByFileId(String fileId) {
        return R.ok(uploadFileDetailBll.infoByFileId(fileId));
    }

}
