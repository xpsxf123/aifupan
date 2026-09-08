package com.jiuyu.replay.api.controller.order;

import com.jiuyu.replay.api.logic.order.OrderLogic;
import com.jiuyu.replay.api.task.OrderScheduledTasks;
import com.jiuyu.replay.common.alibaba.OssUtils;
import com.jiuyu.replay.generic.dto.activity.InviteUserRewardDetailDto;
import com.jiuyu.replay.generic.feign.order.OrderFeign;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("replay/test")
@Tag(name = "test")
@RequiredArgsConstructor
public class TestController {

    private final OrderScheduledTasks orderScheduledTasks;
    private final OssUtils ossUtils;
    private final OrderLogic orderLogic;
    private final OrderFeign orderFeign;
    private final UserPropertyFeign userPropertyFeign;
    private final AnchorUrlBll anchorUrlBll;

    @PostMapping("/recalculateOrder")
    @Operation(summary = "重新计算订单")
    public R<String> recalculateOrder(){
        orderScheduledTasks.batchSaveAudioLog();
        orderScheduledTasks.changeParentAndChild();
        return R.ok("ok");
    }

    @PostMapping("/brushBoardOrder")
    @Operation(summary = "刷年订单用户的数据看板数据")
    public R<String> brushBoardOrder(@RequestBody Map<Integer, Long> map){
        return  orderLogic.brushBoardOrder(map);
    }

    @PostMapping("/addActivityOrder")
    @Operation(summary = "addActivityOrder")
    public R<String> addActivityOrder(@RequestBody List<InviteUserRewardDetailDto> dtoList){
        orderFeign.addActivityOrder(dtoList);
        return R.ok("ok");
    }

    @GetMapping("/use")
    @Operation(summary = "use")
    public R<Boolean> use(Long userId, String code, long quantity) {
        return R.ok(userPropertyFeign.useShortVideoProperty(userId, code, quantity));
    }

    @GetMapping("/check")
    @Operation(summary = "check")
    public R<Boolean> check(Long userId, String code, long quantity) {
        return R.ok(userPropertyFeign.checkUseProperty(userId, code, quantity));
    }
}
