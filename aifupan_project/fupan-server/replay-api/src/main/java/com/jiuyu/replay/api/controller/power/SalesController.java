package com.jiuyu.replay.api.controller.power;

import com.jiuyu.replay.api.logic.power.SalesLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.SalesCascaderVo;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.power.SalesListVo;
import com.jiuyu.replay.generic.bo.power.SalesBo;
import com.jiuyu.replay.generic.bo.power.EmployeeStatusBo;
import com.jiuyu.replay.generic.bo.power.SalesListBo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Insert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 用户跟进销售人员表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:13
 */
@RestController
@CrossOrigin
@RequestMapping("replay/sales")
@Tag(name = "用户跟进销售人员表")
public class SalesController {

    @Resource
    private SalesLogic salesLogic;

    /**
     * 获取当前用户的销售人员信息
     * @return
     */
    @GetMapping("/getCurrentUserSale")
    @Operation(summary = "获取当前用户的销售人员信息")
    public R<SalesInfoVo> getCurrentUserSale(){

        return salesLogic.getCurrentUserSale();
    }

    /**
     * 根据手机号获取当用户的销售人员信息 （无token）
     * @return
     */
    @GetMapping("/getByPhoneUserSale")
    @Operation(summary = "根据手机号获取当用户的销售人员信息")
    public R<SalesInfoVo> getByPhoneUserSale(@Parameter(description = "手机号", required = true )@RequestParam("phone")String phone){

        return salesLogic.getCurrentUserSaleByPhone(phone);
    }


    @GetMapping("/getCurrentUserSaleByPhone")
    @Operation(summary = "获取当前用户的手机号获取销售人员信息")
    public R<SalesInfoVo> getCurrentUserSaleByPhone(@Parameter(description = "用户手机号", required = true) @RequestParam("phone") String phone){

        return salesLogic.getCurrentUserSaleByPhone(phone);
    }

    /**
     * 用户跟进销售人员表列表
     * @param salesListBo 用户跟进销售人员表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "用户跟进销售人员表列表")
    public R<PageUtils<SalesListVo>> list(@Parameter(description = "用户跟进销售人员表列表查询参数", required = true) @RequestBody SalesListBo salesListBo){

        return salesLogic.queryPage(salesListBo);
    }


    /**
     * 用户跟进销售人员表信息
     * @param id 用户跟进销售人员表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "用户跟进销售人员表信息")
    public R<SalesInfoVo> info(@Parameter(description = "用户跟进销售人员表id", required = true) @RequestParam("id") Long id){

        return salesLogic.info(id);
    }

    /**
     * 新增用户跟进销售人员表
     * @param salesBo 用户跟进销售人员表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增用户跟进销售人员表")
    public R<String> save(@Parameter(description = "用户跟进销售人员表对象", required = true) @Validated({Insert.class}) @RequestBody SalesBo salesBo) {

        return salesLogic.save(salesBo);
    }

    /**
     * 修改用户跟进销售人员表
     * @param salesBo 用户跟进销售人员表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改用户跟进销售人员表")
    public R<String> update(@Parameter(description = "用户跟进销售人员表对象", required = true) @Validated({Insert.class}) @RequestBody SalesBo salesBo) {

        return salesLogic.update(salesBo);
    }

    @PostMapping("/updateChooseStatus")
    @Operation(summary = "修改用户跟进销售人员表")
    public R<String> updateChooseStatus(@Validated({SalesBo.IsChooseUpdate.class}) @RequestBody SalesBo salesBo) {
        salesLogic.updateChooseStatus(salesBo);
        return R.ok();
    }

    @PostMapping("/updateUserPolling")
    @Operation(summary = "修改销售轮询状态")
    public R<String> updateUserPolling(@Validated({SalesBo.IsUpdateUserPolling.class}) @RequestBody SalesBo salesBo) {
        salesLogic.updateChooseStatus(salesBo);
        return R.ok();
    }

    /**
     * 删除用户跟进销售人员表
     * @param id 用户跟进销售人员表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除用户跟进销售人员表")
    public R<String> delete(@Parameter(description = "用户跟进销售人员表id", required = true) @RequestParam("id") Long id){

        return salesLogic.delete(id);
    }

    @GetMapping("/userListSalesSearch")
    @Operation(summary = "用户列表销售搜索-带权限")
    public R<List<SalesCascaderVo>> userListSalesSearch(Integer employeeStatus) {
        return salesLogic.userListSalesSearch(employeeStatus);
    }

    @GetMapping("/userDetailsSalesSearch")
    @Operation(summary = "用户详情销售搜索-带权限")
    public R<List<SalesInfoVo>> userDetailsSalesSearch(Long userId) {
        return salesLogic.userDetailsSalesSearch(userId);
    }

    /**
     * 修改员工状态
     *
     * @param employeeStatusBo 员工状态参数
     * @return
     */
    @PostMapping("/updateEmployeeStatus")
    @Operation(summary = "修改员工状态")
    public R<String> updateEmployeeStatus(@Parameter(description = "员工状态参数", required = true) @Validated @RequestBody EmployeeStatusBo employeeStatusBo) {
        return salesLogic.updateEmployeeStatus(employeeStatusBo);
    }

}
