package com.jiuyu.replay.api.controller.openapi;

import com.jiuyu.replay.api.logic.third.AiModelLogic;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/4/18 下午8:17
 */
@RestController
@CrossOrigin
    @RequestMapping("replay/openapi/v2500")
@Tag(name = "客户端openAPI-2.5.00")
@AllArgsConstructor
public class ClientOpenApi2500 {

    private final AiModelLogic aiModelLogic;

}
