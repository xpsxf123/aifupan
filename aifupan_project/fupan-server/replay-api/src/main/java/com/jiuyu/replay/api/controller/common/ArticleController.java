package com.jiuyu.replay.api.controller.common;

import com.jiuyu.replay.api.logic.common.ArticleLogic;
import com.jiuyu.replay.common.bo.ArticleBo;
import com.jiuyu.replay.common.bo.ArticleListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.ArticleInfoVo;
import com.jiuyu.replay.common.vo.ArticleListVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 文章
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-23 20:39:45
 */
@RestController
@CrossOrigin
@RequestMapping("replay/article")
@Tag(name = "文章")
public class ArticleController {

    @Resource
    private ArticleLogic articleLogic;

    /**
     * 文章列表
     * @param articleListBo 文章列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "文章列表")
    public R<PageUtils<ArticleListVo>> list(@Parameter(description = "文章列表查询参数", required = true) @RequestBody ArticleListBo articleListBo){
        UserCacheVo localUser = GlobalObject.getLocalUser();
        return articleLogic.queryPage(articleListBo);
    }


    /**
     * 文章信息
     * @param id 文章id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "文章信息")
    public R<ArticleInfoVo> info(@Parameter(description = "文章id", required = true) @RequestParam("id") Long id){

        return articleLogic.info(id);
    }

    /**
     * 新增文章
     * @param articleBo 文章对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增文章")
    public R<String> save(@Parameter(description = "文章对象", required = true) @RequestBody ArticleBo articleBo){

        return articleLogic.save(articleBo);
    }

    /**
     * 修改文章
     * @param articleBo 文章对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改文章")
    public R<String> update(@Parameter(description = "文章对象", required = true) @RequestBody ArticleBo articleBo){

        return articleLogic.update(articleBo);
    }

    /**
     * 删除文章
     * @param id 文章id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除文章")
    public R<String> delete(@Parameter(description = "文章id", required = true) @RequestParam("id") Long id){

        return articleLogic.delete(id);
    }

}
