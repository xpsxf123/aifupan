package com.jiuyu.replay.api.controller.common;

import com.jiuyu.replay.api.logic.common.CommonLogic;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
@RequestMapping("replay/common")
@Tag(name = "公共")
public class CommonController {

    @Resource
    private CommonLogic commonLogic;
    @Resource
    private NonStaticResourceHttpRequestHandler nonStaticResourceHttpRequestHandler;

    /**
     * 显示视频
     * @return
     */
    @GetMapping(value = "/video")
    public void getVideo(HttpServletRequest request, HttpServletResponse response) {

        try {
            String path = "C:\\Users\\Administrator\\Desktop\\video\\test.ts";
            request.setAttribute(NonStaticResourceHttpRequestHandler.ATTR_FILE, path);
            nonStaticResourceHttpRequestHandler.handleRequest(request, response);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 上传图片
     *
     * @param file            图片文件
     * @param flag            压缩类型 0: 正常压缩  1：超级压缩 2：不压缩 默认0
     * @param sort            排序
     * @param resourceType    来源类型
     * @param isCheckSecurity 是否需要进行安全检测 0：否 1：是
     * @return
     */
    @PostMapping("/uploadImg")
    public R<FileShowVo> uploadImg(@RequestParam("file") MultipartFile file, @RequestParam(value = "flag", required = false) Integer flag, @RequestParam(value = "sort", required = false) Integer sort, @RequestParam(value = "resourceType", required = false) Integer resourceType, @RequestParam(value = "isCheckSecurity", required = false) Integer isCheckSecurity) throws IOException {

        return this.commonLogic.uploadImg(file, flag, sort, resourceType, isCheckSecurity);
    }

    /**
     * 显示图片
     *
     * @param fileName 图片文件名
     * @return
     */
    @GetMapping(value = "/img/{fileName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] showImg(@PathVariable("fileName") String fileName) {

        return commonLogic.showImg(fileName);
    }

    /**
     * 获取服务器当前时间
     *
     * @return 当前服务器时间毫秒值
     */
    @GetMapping("/serverCurrentTime")
    public R<String> getServerCurrentTimeMilliseconds() {
        return R.ok(String.valueOf(System.currentTimeMillis()));
    }
}
