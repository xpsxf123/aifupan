package com.jiuyu.replay.power.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.power.vo.BindingAccountListVo;
import com.jiuyu.replay.power.vo.BindingAccountInfoVo;
import com.jiuyu.replay.power.bo.BindingAccountBo;
import com.jiuyu.replay.power.bo.BindingAccountListBo;


/**
 * 父子绑定记录
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-31 10:18:16
 */
public interface BindingAccountProducer {


    /**
     * 父子绑定记录列表
     * @param bindingAccountListBo 父子绑定记录列表查询参数
     * @return
     */
    PageUtils<BindingAccountListVo> queryPage(BindingAccountListBo bindingAccountListBo);

    /**
    * 父子绑定记录信息
    * @param id 父子绑定记录id
    * @return
    */
    BindingAccountInfoVo info(Long id);

    /**
     * 新增父子绑定记录
     * @param bindingAccountBo 父子绑定记录对象
     * @return
     */
     BindingAccountInfoVo save(BindingAccountBo bindingAccountBo);

    /**
     * 修改父子绑定记录
     * @param bindingAccountBo 父子绑定记录对象
     * @return
     */
    void update(BindingAccountBo bindingAccountBo);

    /**
     * 删除父子绑定记录
     * @param id 父子绑定记录id
     * @return
     */
    void deleteById(Long id);


    /**
     * 解绑
     * @param currentUserId
     * @param subUserId
     */
    void unbindingSubAccount(Long currentUserId, Long subUserId, String unbindReason);

    /**
     * Data Hub：按父账号批量查全部绑定/解绑记录
     *
     * @param parentUserIds 父账号id列表
     *
     * @return {@link java.util.List }<{@link com.jiuyu.replay.power.vo.BindingAccountVo }>
     */
    java.util.List<com.jiuyu.replay.power.vo.BindingAccountVo> listByParentUserIds(java.util.Collection<Long> parentUserIds);
}

