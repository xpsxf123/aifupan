package com.jiuyu.replay.api.controller.test;

import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/replay/time")
public class TimeTestController {

    @Operation(summary = "获取当前系统时间")
    @GetMapping("/currentTime")
    public R<Long> currentTime(){

        return R.ok(System.currentTimeMillis());
    }

}
