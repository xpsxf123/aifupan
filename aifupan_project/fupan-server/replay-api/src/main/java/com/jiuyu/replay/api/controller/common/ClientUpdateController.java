package com.jiuyu.replay.api.controller.common;

import com.jiuyu.replay.common.bll.ClientUpdateBll;
import com.jiuyu.replay.common.bo.ClientUpdateBo;
import com.jiuyu.replay.common.bo.ClientUpdateListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.ClientUpdateVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("replay/clientupdate")
@Tag(name = "客户端版本更新")
public class ClientUpdateController {

    @Autowired
    private ClientUpdateBll clientUpdateBll;


    @Operation(summary = "新增客户端版本更新信息")
    @PostMapping("/save")
    public R<String> save(@RequestBody ClientUpdateBo clientUpdateBo) {

        return clientUpdateBll.save(clientUpdateBo);
    }


    @Operation(summary = "修改客户端版本更新信息")
    @PostMapping("/update")
    public R<String> update(@RequestBody ClientUpdateBo clientUpdateBo) {
       return clientUpdateBll.update(clientUpdateBo);
    }


    @Operation(summary = "修改客户端版本更新状态")
    @GetMapping("/updateStatus")
    public R<String> updateStatus(@Parameter(description = "客户端版本更新信息id", required = true) Long id,
                                  @Parameter(description = "状态", required = true) Integer status){
        return clientUpdateBll.updateStatus(id,status);
    }

    @Operation(summary = "根据id删除客户端版本更新信息")
    @PostMapping("/delete")
    public R<String> delete(@RequestBody List<Long> ids) {
       return clientUpdateBll.delete(ids);
    }

    @Operation(summary = "根据id获取客户端版本更新信息")
    @GetMapping("/info")
    public R<ClientUpdateVo> info(@Parameter(description = "客户端版本更新信息id", required = true) @RequestParam("id") Long id) {
        return clientUpdateBll.info(id);
    }

    @Operation(summary = "分页获取客户端版本更新信息")
    @GetMapping("/pagelist")
    public R<PageUtils<ClientUpdateVo>> pageList(ClientUpdateListBo listBo) {
       return clientUpdateBll.pageList(listBo);
    }

    @Operation(summary = "上传文件")
    @PostMapping("/uploadFile")
    public R<Map<String,String>> uploadFile(MultipartFile file, String version){
       return clientUpdateBll.upLoadFile(file,version);
    }
}
