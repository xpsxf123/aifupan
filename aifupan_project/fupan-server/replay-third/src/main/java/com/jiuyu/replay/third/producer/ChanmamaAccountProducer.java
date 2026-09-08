package com.jiuyu.replay.third.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.third.vo.chanmama.ChanmamaAccountListVo;
import com.jiuyu.replay.third.vo.chanmama.ChanmamaAccountInfoVo;
import com.jiuyu.replay.third.bo.chanmama.ChanmamaAccountBo;
import com.jiuyu.replay.third.bo.chanmama.ChanmamaAccountListBo;


/**
 * 第三方数据平台账号
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-14 15:59:03
 */
public interface ChanmamaAccountProducer {


    /**
     * 第三方数据平台账号列表
     * @param chanmamaAccountListBo 第三方数据平台账号列表查询参数
     * @return
     */
    PageUtils<ChanmamaAccountListVo> queryPage(ChanmamaAccountListBo chanmamaAccountListBo);

    /**
    * 第三方数据平台账号信息
    * @param id 第三方数据平台账号id
    * @return
    */
    ChanmamaAccountInfoVo info(Long id);

    /**
     * 新增第三方数据平台账号
     * @param chanmamaAccountBo 第三方数据平台账号对象
     * @return
     */
     ChanmamaAccountInfoVo save(ChanmamaAccountBo chanmamaAccountBo);

    /**
     * 修改第三方数据平台账号
     * @param chanmamaAccountBo 第三方数据平台账号对象
     * @return
     */
    void update(ChanmamaAccountBo chanmamaAccountBo);

    /**
     * 删除第三方数据平台账号
     * @param id 第三方数据平台账号id
     * @return
     */
    void deleteById(Long id);


}

