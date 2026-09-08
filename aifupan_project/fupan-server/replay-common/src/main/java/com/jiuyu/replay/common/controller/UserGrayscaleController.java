package com.jiuyu.replay.common.controller;

import com.jiuyu.replay.common.bll.UserGrayscaleBll;
import com.jiuyu.replay.common.bo.userGrayscale.UserGrayscaleAddsBo;
import com.jiuyu.replay.common.bo.userGrayscale.UserGrayscaleListBo;
import com.jiuyu.replay.common.producer.UserGrayscaleProduct;
import com.jiuyu.replay.common.vo.userGrayscale.UserGrayscaleVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/5 16:42
 */
@Slf4j
@RestController
@RequestMapping("replay/common/userGrayscale")
@Tag(name = "用户灰度相关的接口")
@AllArgsConstructor
public class UserGrayscaleController {

    private final UserGrayscaleProduct userGrayscaleProduct;
    private final UserGrayscaleBll userGrayscaleBll;

    @PostMapping("/queryPage")
    @Operation(summary = "用户灰度分页查询")
    public R<PageUtils<UserGrayscaleVo>> queryPage(@RequestBody UserGrayscaleListBo listBo) {
        return R.ok(userGrayscaleProduct.queryPage(listBo));
    }

    @PostMapping("/adds")
    @Operation(summary = "添加多个用户")
    public R<String> adds(@RequestBody @Validated UserGrayscaleAddsBo addsBo) {
        return R.ok(userGrayscaleBll.adds(addsBo));
    }

    @PostMapping("/deleteByIds")
    @Operation(summary = "根据id集合删除用户灰度记录")
    public R<String> deleteByIds(@RequestBody @NotEmpty(message = "id集合不能为空") @Schema(description = "用户灰度id") List<Long> ids) {
        userGrayscaleBll.deleteByIds(ids);
        return R.ok();
    }

}
