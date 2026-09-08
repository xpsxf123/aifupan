package com.jiuyu.replay.order.bean;

import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;

///**
// * @ClassName : UserPropertyBean
// * @Description : 用于做资产的加减
// */
public interface UserProperty {

    String getCommodityCode();

    boolean isHave(AssetsMinusOrPlusBo assets);

    void use(AssetsMinusOrPlusBo assets);
}
