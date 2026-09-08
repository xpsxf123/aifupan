package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.ClientAiFavLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.ClientAiFavBo;
import com.jiuyu.replay.words.bo.ClientAiFavListBo;
import com.jiuyu.replay.words.vo.ClientAiFavInfoVo;
import com.jiuyu.replay.words.vo.ClientAiFavListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 运营/违规收藏列表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-03-12 16:42:18
 */
@RestController
@CrossOrigin
@RequestMapping("replay/clientaifav")
@Tag(name = "运营-违规收藏列表")
public class ClientAiFavController {

    @Resource
    private ClientAiFavLogic clientAiFavLogic;

    /**
     * 运营/违规收藏列表列表
     * @param clientAiFavListBo 运营/违规收藏列表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "运营/违规收藏列表列表")
    public R<PageUtils<ClientAiFavListVo>> list(@Parameter(description = "运营/违规收藏列表列表查询参数", required = true) @RequestBody ClientAiFavListBo clientAiFavListBo){

        return clientAiFavLogic.queryPage(clientAiFavListBo);
    }


    /**
     * 运营/违规收藏列表信息
     * @param id 运营/违规收藏列表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "运营/违规收藏列表信息")
    public R<ClientAiFavInfoVo> info(@Parameter(description = "运营/违规收藏列表id", required = true) @RequestParam("id") Long id){

        return clientAiFavLogic.info(id);
    }

    /**
     * 新增或修改运营/违规收藏列表
     * @param clientAiFavBo 运营/违规收藏列表对象
     * @return
     */
    @PostMapping("/saveOrUpdate")
    @Operation(summary = "新增或修改运营/违规收藏列表")
    public R<String> saveOrUpdate(@Parameter(description = "运营/违规收藏列表对象", required = true) @RequestBody ClientAiFavBo clientAiFavBo){

        return clientAiFavLogic.saveOrUpdate(clientAiFavBo);
    }

    /**
     * 新增或修改运营/违规收藏列表-如果有已经删除则不新增
     * @param clientAiFavBo 运营/违规收藏列表对象
     * @return
     */
    @PostMapping("/saveOrUpdateByNotExist")
    @Operation(summary = "新增或修改运营/违规收藏列表-如果有已经删除则不新增")
    public R<String> saveOrUpdateByNotExist(@RequestBody ClientAiFavBo clientAiFavBo){
        return clientAiFavLogic.saveOrUpdateByNotExist(clientAiFavBo);
    }

    /**
     * 新增运营/违规收藏列表
     * @param clientAiFavBo 运营/违规收藏列表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增运营/违规收藏列表")
    public R<String> save(@Parameter(description = "运营/违规收藏列表对象", required = true) @RequestBody ClientAiFavBo clientAiFavBo){

        return clientAiFavLogic.save(clientAiFavBo);
    }

    /**
     * 修改运营/违规收藏列表
     * @param clientAiFavBo 运营/违规收藏列表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改运营/违规收藏列表")
    public R<String> update(@Parameter(description = "运营/违规收藏列表对象", required = true) @RequestBody ClientAiFavBo clientAiFavBo){

        return clientAiFavLogic.update(clientAiFavBo);
    }

    /**
     * 删除运营/违规收藏列表
     * @param id 运营/违规收藏列表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除运营/违规收藏列表")
    public R<String> delete(@Parameter(description = "运营/违规收藏列表id", required = true) @RequestParam("id") Long id){

        return clientAiFavLogic.delete(id);
    }

    /**
     * 批量删除运营/违规收藏列表
     * @param ids 运营/违规收藏列表id集合
     * @return
     */
    @PostMapping("/batchDelete")
    @Operation(summary = "删除运营/违规收藏列表")
    public R<String> batchDelete(@RequestBody List<String> ids){

        return clientAiFavLogic.batchDelete(ids);
    }

}
