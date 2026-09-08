package com.jiuyu.replay.api.controller.common;

import com.jiuyu.replay.common.bll.ClientLogBll;
import com.jiuyu.replay.common.bo.clientlog.RequestBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.ClientLogVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("replay/clientlog")
@Tag(name = "客户端日志")
public class ClientLogController {

    @Autowired
    private ClientLogBll clientLogBll;



    @Operation(summary = "分页获取客户端日志")
    @PostMapping("/pagelist/{pageIndex}/{pageSize}")
    public R<PageUtils<ClientLogVo>> pageList(
                                               @RequestBody RequestBo requestBo,
                                               @Parameter(required = false) @PathVariable("pageIndex") Integer pageIndex,
                                               @Parameter(required = false) @PathVariable("pageSize") Integer pageSize
                                           ) {
        return clientLogBll.queryPage(requestBo,pageIndex,pageSize);
    }

    @Operation(summary = "根据查询条件删除客户端日志")
    @PostMapping("/delete")
    public R<String> delete(@RequestBody RequestBo requestBo)
    {
        return clientLogBll.deleteBySearch(requestBo);
    }

    @Operation(summary = "根据选中的数据删除客户端日志")
    @PostMapping("/deleteByIds")
    public R<String> deleteByIds(@RequestBody List<Long> ids)
    {
        return clientLogBll.deleteByIds(ids);
    }
}
