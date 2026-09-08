package com.jiuyu.replay.ai.controller;

import com.jiuyu.replay.ai.bll.ShareLinkRecordBll;
import com.jiuyu.replay.ai.bo.ShareLinkRecordBo;
import com.jiuyu.replay.ai.bo.ShareLinkRecordListBo;
import com.jiuyu.replay.ai.bo.ShareLinkRecordSaveBo;
import com.jiuyu.replay.ai.vo.ShareLinkRecordInfoVo;
import com.jiuyu.replay.ai.vo.ShareLinkRecordListVo;
import com.jiuyu.replay.ai.vo.ShareSaveVo;
import com.jiuyu.replay.common.alibaba.AiOssUtils;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @author lyw
 */

@RestController
@CrossOrigin
@RequestMapping("replay/ai/share")
@Tag(name = "链接分享记录")
public class ShareLinkRecordController {


    @Resource
    private AiOssUtils aiOssUtils;
    @Resource
    private ShareLinkRecordBll shareLinkRecordBll;


    /**
     * 分享链接记录列表
     * @param shareLinkRecordListBo 分享链接记录列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "分享链接记录列表")
    public R<PageUtils<ShareLinkRecordListVo>> list(@Parameter(description = "分享链接记录列表查询参数", required = true) @RequestBody ShareLinkRecordListBo shareLinkRecordListBo){

        return shareLinkRecordBll.queryPage(shareLinkRecordListBo);
    }


    /**
     * 分享链接记录信息
     * @param id 分享链接记录id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "分享链接记录信息")
    public R<ShareLinkRecordInfoVo> info(@Parameter(description = "分享链接记录id", required = true) @RequestParam("id") Long id) throws IOException {

        return shareLinkRecordBll.info(id);
    }

    /**
     * 修改分享链接记录
     * @param shareLinkRecordBo 分享链接记录对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改分享链接记录")
    public R<String> update(@Parameter(description = "分享链接记录对象", required = true) @RequestBody ShareLinkRecordBo shareLinkRecordBo){

        return shareLinkRecordBll.update(shareLinkRecordBo);
    }

    /**
     * 删除分享链接记录
     * @param id 分享链接记录id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除分享链接记录")
    public R<String> delete(@Parameter(description = "分享链接记录id", required = true) @RequestParam("id") Long id){

        return shareLinkRecordBll.delete(id);
    }

    /**
     * 新增链接分享记录
     * @param shareLinkRecordBo
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增链接分享记录")
    public R<ShareSaveVo> save(@Parameter(description = "链接分享对象", required = true) @RequestBody ShareLinkRecordSaveBo shareLinkRecordBo){
        return R.ok(shareLinkRecordBll.addShare(shareLinkRecordBo));
    }


    /**
     * 获取数据内容上传的预签名链接
     * @return
     */
    @Operation(summary = "获取数据内容上传的预签名链接")
    @GetMapping("/getDataTxtPutUrl")
    public R<SignUploadUrlVo> getDataTxtPutUrl(){
        return R.ok(aiOssUtils.getSignUploadUrl("dataTxt","txt"));
    }

}
