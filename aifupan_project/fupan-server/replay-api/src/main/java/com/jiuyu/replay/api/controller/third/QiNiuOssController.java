package com.jiuyu.replay.api.controller.third;

import com.jiuyu.replay.api.logic.third.QiNiuLogic;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("/replay/qiniu")
public class QiNiuOssController {

    @Resource
    private QiNiuLogic qiNiuLogic;

    /**
     * 获取上传文件的临时凭证
     * @return
     */
    @Operation(summary = "获取上传文件的临时凭证")
    @GetMapping("/getUploadToken")
    public R<String> getUploadToken() {

        return qiNiuLogic.getUploadToken();

    }
}
