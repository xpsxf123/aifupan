package com.jiuyu.replay.third.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.bo.chanmama.ChanmamaAccountBo;
import com.jiuyu.replay.third.bo.chanmama.ChanmamaAccountListBo;
import com.jiuyu.replay.third.producer.ChanmamaAccountProducer;
import com.jiuyu.replay.third.vo.chanmama.ChanmamaAccountInfoVo;
import com.jiuyu.replay.third.vo.chanmama.ChanmamaAccountListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 第三方数据平台账号
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-14 15:59:03
 */
@Component
public class ChanmamaAccountBll {

    @Resource
    private ChanmamaAccountProducer chanmamaAccountProducer;


    /**
     * 第三方数据平台账号列表
     * @param chanmamaAccountListBo 第三方数据平台账号列表查询参数
     * @return
     */
    public R<PageUtils<ChanmamaAccountListVo>> queryPage(ChanmamaAccountListBo chanmamaAccountListBo) {

        return R.ok("获取成功", chanmamaAccountProducer.queryPage(chanmamaAccountListBo));
    }

    /**
    * 第三方数据平台账号信息
    * @param id 第三方数据平台账号id
    * @return
    */
    public R<ChanmamaAccountInfoVo> info(Long id) {

        ChanmamaAccountInfoVo chanmamaAccountInfoVo = chanmamaAccountProducer.info(id);
        return R.ok("获取成功", chanmamaAccountInfoVo);
    }

    /**
     * 新增第三方数据平台账号
     * @param chanmamaAccountBo 第三方数据平台账号对象
     * @return
     */
    public R<String> save(ChanmamaAccountBo chanmamaAccountBo) {

        ChanmamaAccountInfoVo chanmamaAccountInfoVo = chanmamaAccountProducer.save(chanmamaAccountBo);
        return R.ok("添加成功");
    }

    /**
     * 修改第三方数据平台账号
     * @param chanmamaAccountBo 第三方数据平台账号对象
     * @return
     */
    public R<String> update(ChanmamaAccountBo chanmamaAccountBo) {

        chanmamaAccountProducer.update(chanmamaAccountBo);
        return R.ok("修改成功");
    }

    /**
     * 删除第三方数据平台账号
     * @param id 第三方数据平台账号id
     * @return
     */
    public R<String> delete(Long id) {

        chanmamaAccountProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

