package com.jiuyu.replay.power.bll;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.BindingAccountBo;
import com.jiuyu.replay.power.bo.BindingAccountListBo;
import com.jiuyu.replay.power.producer.BindingAccountProducer;
import com.jiuyu.replay.power.vo.BindingAccountInfoVo;
import com.jiuyu.replay.power.vo.BindingAccountListVo;
import com.jiuyu.replay.power.vo.UserVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * 父子绑定记录
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-31 10:18:16
 */
@Component
public class BindingAccountBll {

    @Resource
    private BindingAccountProducer bindingAccountProducer;


    /**
     * 父子绑定记录列表
     * @param bindingAccountListBo 父子绑定记录列表查询参数
     * @return
     */
    public R<PageUtils<BindingAccountListVo>> queryPage(BindingAccountListBo bindingAccountListBo) {

        return R.ok("获取成功", bindingAccountProducer.queryPage(bindingAccountListBo));
    }

    /**
    * 父子绑定记录信息
    * @param id 父子绑定记录id
    * @return
    */
    public R<BindingAccountInfoVo> info(Long id) {

        BindingAccountInfoVo bindingAccountInfoVo = bindingAccountProducer.info(id);
        return R.ok("获取成功", bindingAccountInfoVo);
    }

    /**
     * 新增父子绑定记录
     * @param bindingAccountBo 父子绑定记录对象
     * @return
     */
    public R<String> save(BindingAccountBo bindingAccountBo) {

        BindingAccountInfoVo bindingAccountInfoVo = bindingAccountProducer.save(bindingAccountBo);
        return R.ok("添加成功");
    }

    /**
     * 修改父子绑定记录
     * @param bindingAccountBo 父子绑定记录对象
     * @return
     */
    public R<String> update(BindingAccountBo bindingAccountBo) {

        bindingAccountProducer.update(bindingAccountBo);
        return R.ok("修改成功");
    }

    /**
     * 删除父子绑定记录
     * @param id 父子绑定记录id
     * @return
     */
    public R<String> delete(Long id) {

        bindingAccountProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 获取待解绑的用户列表，时间倒序排序
     * @param parentId
     * @param userList
     * @return
     */
    public R<List<UserVo>> getUnbindUstList(Long parentId, List<UserVo> userList){
        List<UserVo> result = new ArrayList<>();
        result.addAll(userList);
        BindingAccountListBo bo = new BindingAccountListBo();
        bo.setParentUserId(parentId);
        bo.setChildUserIds(userList.stream().map(UserVo::getId).toList());
        bo.setBindingStatus(0);
        bo.setLimit(-1);
        PageUtils<BindingAccountListVo> pageUtils = bindingAccountProducer.queryPage(bo);
        if (ObjectUtil.isNotEmpty(pageUtils.getList())){
            List<BindingAccountListVo> list = pageUtils.getList();
            for (UserVo userVo : result) {
                list.stream()
                        .filter(item -> item.getChildUserId().equals(userVo.getId()))
                        .findFirst()
                        .ifPresent(item -> userVo.setCreateDate(item.getBindingDate()));

            }
        }
        result = result.stream()
                .sorted((a, b) -> Long.compare(b.getCreateDate().getTime(), a.getCreateDate().getTime()))
                .toList();
        return R.ok(result);
    }


    public R<String> unbindingSubAccount(Long currentUserId, Long subUserId, String unbindReason) {
        bindingAccountProducer.unbindingSubAccount(currentUserId, subUserId,unbindReason);
        return R.ok("完成");
    }
}

