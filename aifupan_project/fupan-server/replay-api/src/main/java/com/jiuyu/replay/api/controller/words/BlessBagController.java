package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.BlessBagLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.BlessBagBo;
import com.jiuyu.replay.words.bo.BlessBagListBo;
import com.jiuyu.replay.generic.vo.words.BlessBagInfoVo;
import com.jiuyu.replay.generic.vo.words.BlessBagListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;



/**
 * 福袋信息
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 20:02:21
 */
@RestController
@CrossOrigin
@RequestMapping("replay/blessbag")
@Tag(name = "福袋信息")
public class BlessBagController {

    @Resource
    private BlessBagLogic blessBagLogic;

    /**
     * 福袋信息列表
     * @param blessBagListBo 福袋信息列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "福袋信息列表")
    public R<PageUtils<BlessBagListVo>> list(@Parameter(description = "福袋信息列表查询参数", required = true) @RequestBody BlessBagListBo blessBagListBo){

        return blessBagLogic.queryPage(blessBagListBo);
    }


    /**
     * 福袋信息信息
     * @param id 福袋信息id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "福袋信息信息")
    public R<BlessBagInfoVo> info(@Parameter(description = "福袋信息id", required = true) @RequestParam("id") Long id){

        return blessBagLogic.info(id);
    }


    /**
     * 通过video获取福袋信息信息
     * @param videoId 福袋信息id
     * @return
     */
    @GetMapping("/infoByVideo")
    @Operation(summary = "通过video获取福袋信息信息")
    public R<List<BlessBagInfoVo>> infoByVideo(@Parameter(description = "福袋信息id", required = true) @RequestParam("videoId") String videoId){
        return blessBagLogic.infoByVideo(videoId);
    }


    /**
     * 新增福袋信息
     * @param blessBagBo 福袋信息对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增福袋信息")
    public R<String> save(@Parameter(description = "福袋信息对象", required = true) @RequestBody BlessBagBo blessBagBo){

        return blessBagLogic.save(blessBagBo);
    }

    /**
     * 修改福袋信息
     * @param blessBagBo 福袋信息对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改福袋信息")
    public R<String> update(@Parameter(description = "福袋信息对象", required = true) @RequestBody BlessBagBo blessBagBo){

        return blessBagLogic.update(blessBagBo);
    }

    /**
     * 删除福袋信息
     * @param id 福袋信息id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除福袋信息")
    public R<String> delete(@Parameter(description = "福袋信息id", required = true) @RequestParam("id") Long id){

        return blessBagLogic.delete(id);
    }

}
