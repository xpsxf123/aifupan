<template>
    <div class="increase">
        <!-- 加量包顶部 -->
        <div class="increase-top">
            <i class="el-icon-arrow-left"></i>
            <div class="icrease-text">加量包</div>
        </div>
        
        <!-- 用户信息 -->
        <div class="increase-user-info">
            <div class="user-account">
                <div style="margin-right: 9px;">当前账号：</div>
                <div>{{ user.phone }}</div>
            </div>
            <div class="recharge-function">
                <div style="margin-right: 9px;">充值功能：</div>
                <div>AI话术分析（当前剩余资源： {{ user.resource }}）</div>
            </div>            
        </div>

        <!-- 选择加量时间 -->
        <div class="recharge-volume">
            <div class="volume-text">充值容量：</div>
            <div :class="{'recharge-volume-time':true,'click-update-style':oneHundredActive}" style="margin-right: 32px;" @click="increaseOneClick">
                <div :class="{'recharge-volume-text':true,'click-update-text-style':oneHundredActive}">{{ oneHundredIncrease.time }}</div>
                <div :class="{'recharge-volume-money':true,'click-update-text-style':oneHundredActive}">￥{{ oneHundredIncrease.money }}</div>
                <div :class="{'recharge-volume-Nomoney':true,'click-update-text-style':oneHundredActive}">￥{{ oneHundredIncrease.Nomoney }}</div>
            </div>
            
            <div :class="{'recharge-volume-time':true,'click-update-style':twoHundredActive}" @click="increaseTwoClick">
                <div :class="{'recharge-volume-text':true,'click-update-text-style':twoHundredActive}">{{ twoHundredIncrease.time }}</div>
                <div :class="{'recharge-volume-money':true,'click-update-text-style':twoHundredActive}">￥{{ twoHundredIncrease.money }}</div>
                <div :class="{'recharge-volume-Nomoney':true,'click-update-text-style':twoHundredActive}">￥{{ twoHundredIncrease.Nomoney }}</div>
            </div>

        </div>

        <!-- 加量小时、金额、支付方式 -->
        <div class="increase-bottom">
            <div>AI话术分析： {{ increaseMoney.aiAnalyse }}</div>
            <div style="margin:24px 0px 0px 14px;">订单金额： {{ increaseMoney.ordersMoney }}</div>
            <div class="increase-pay">
                <div>支付方式：</div>
                <div :class="{'increase-pay-style':true,'click-update-pay':wxPayActive}" @click="wxpayClcik">
                    <img src="../../../assets/imgs/wx_pay.png" class="increase-pay-logo" alt="">
                    <div>微信支付</div>
                </div>

                <div :class="{'increase-pay-style':true,'click-update-pay':aliPayActive}" @click="alipayClcik">
                    <img src="../../../assets/imgs/ali_pay.png" class="increase-pay-logo" alt="">
                    <div>支付宝支付</div>
                </div>
            </div>
        </div>

        <!-- 同意支付 -->
        <div class="increase-agree-pay">
            <div class="agree-pay-text">点击“确定支付”或付款成功，即表示您已同意并接受《服务协议》购买成功后请刷新页面或重新登录账号，即可生效</div>
            <div class="real-money">
                <div style="color: #2E3742; font-size: 14px;">实际支付：</div>
                <div style="color: #2E3742; font-size: 22px;">￥{{ increaseMoney.realMoney }}</div>
            </div>
            <div class="real-pay" @click="realPayClcik">
                <div>确定支付</div>
            </div>
        </div>
    </div>
</template>

<script>
    export default{
        data(){
            return{
                // 用户信息类
                user:{
                    phone:'13512155840',
                    resource:'20小时'
                },

                // 加量类
                oneHundredIncrease:{
                    time:'100小时',
                    money:200,
                    Nomoney:3600
                },
                twoHundredIncrease:{
                    time:'200小时',
                    money:350,
                    Nomoney:400
                },
                // 动态切换加量时间、金额类
                increaseMoney:{
                    aiAnalyse:'',
                    ordersMoney:0,
                    realMoney:0,
                },

                // 切换加量时间改变样式
                oneHundredActive:true,
                twoHundredActive:false,
                // 切换支付方式改变样式
                wxPayActive:true,
                aliPayActive:false
            }
        },
        created(){
            this.increaseMoney.aiAnalyse = this.oneHundredIncrease.time;
            this.increaseMoney.ordersMoney = this.oneHundredIncrease.money;
            this.increaseMoney.realMoney = this.oneHundredIncrease.money;
        },
        methods:{
            increaseOneClick(){
                this.oneHundredActive = true;
                this.twoHundredActive = false;

                this.increaseMoney.aiAnalyse = this.oneHundredIncrease.time;
                this.increaseMoney.ordersMoney = this.oneHundredIncrease.money;
                this.increaseMoney.realMoney = this.oneHundredIncrease.money;
            },
            increaseTwoClick(){
                this.oneHundredActive = false;
                this.twoHundredActive = true;

                this.increaseMoney.aiAnalyse = this.twoHundredIncrease.time;
                this.increaseMoney.ordersMoney = this.twoHundredIncrease.money;
                this.increaseMoney.realMoney = this.twoHundredIncrease.money;
            },
            wxpayClcik(){
                this.wxPayActive = true;
                this.aliPayActive = false;
            },
            alipayClcik(){
                this.wxPayActive = false;
                this.aliPayActive = true;
            },

            realPayClcik(){
                alert('支付成功')
            }
        }
    }
</script>

<style scoped>

    /* 加量包顶部 */
    .increase-top{
        display: flex;
        align-items: center;
        margin-left: 14px;
        margin-right: 14px;
        padding-bottom: 13px;
        border-bottom: 1px solid #DCE0E7;
        font-size: 20px;
    }
    .icrease-text{
        color: #2E3742;
        font-size: 16px;
        margin-left: 8px;
    }

    /* #region 加量包用户信息 */
    .increase-user-info{
        display: flex;
        flex-direction: column;
        margin: 27px 0px 80px 16px;
        font-size: 14px;
        color: #2E3742;
    }
    .user-account{
        display: flex;
        align-items: center;
    }
    .recharge-function{
        display: flex;
        align-items: center;
        margin-top: 24px;
    }
    /* #endregion */


    /* #region 选择加量时间 */
    .recharge-volume{
        display: flex;
        margin:0px 0px 0px 16px;
    }
    .volume-text{
        margin-right: 9px; 
        color: #2E3742; 
        font-size: 14px;
    }

    /* 加量样式 */
    .recharge-volume-time{
        display: flex;
        justify-content: center;
        align-items: center;
        flex-direction: column;
        width: 166px;
        height: 113.29px;
        margin-top: -45px;
        border-radius: 8px;
        background-color: #FFFBF5;
        border: 1px solid #F3B272;
        transition: all 0.3s linear;
    }
    .recharge-volume-text{
        font-size: 18px;
        color: #2E3742;
    }
    .recharge-volume-money{
        font-size: 22px;
        color: #2E3742;
        margin-top: 8px;
    }
    .recharge-volume-Nomoney{
        font-size: 14px;
        color: #2E3742;
        text-decoration-line: line-through;
    }

    /* #endregion */
    
    /* #region 加量小时、金额、支付方式 */
    .increase-bottom{
        display: flex;
        flex-direction: column;
        font-size: 14px;
        color: #2E3742;
        margin: 24px 0px 0px 4px;
    }

    .increase-pay{
        display: flex;
        margin: 40px 0px 0px 14px;
    }

    .increase-pay-style{
        display: flex;
        justify-content: center;
        align-items: center;
        width: 166px;
        height: 52px;
        border-radius: 8px;
        border: 1px solid #DCE0E7;
        margin:-16px 0px 0px 9px;
    }

    .increase-pay-logo {
        margin-right: 9px;
        width: 24px;
        height: 24px;
    }
    /* #endregion */

    /* #region 同意支付 */
    .increase-agree-pay{
        display: flex;
        align-items: center;
        width: calc(100% + 36px);
        margin-left: -18px;
        height: 80px;
        margin-top: 95px;
        border-top: 1px solid #DCE0E7;
    }

    .agree-pay-text{
        width: 419px;
        height: 40px;
        font-size: 13px;
        color: #677583;
        margin-left: 22px;
    }

    .real-money{
        display: flex;
        align-items: center;
        margin-left: 191px;
    }

    .real-pay{
        display: flex;
        justify-content: center;
        align-items: center;
        width: 110px;
        height: 40px;
        background-color: var(--color-main);
        border-radius: 4px;
        color: #FFFFFF;
        font-size: 14px;
        margin-left: 31px;
    }

    /* #endregion */

    /* #region 鼠标悬浮后改变样式 */

    /* 加量 */
    .recharge-volume-time:hover{
        transform: scale(1.08);
        box-shadow: 0px 0px 5px 0 rgba(0, 0, 0, 0.5);
        cursor: pointer;
    }
    /* 支付方式 */
    .increase-pay-style:hover{
        cursor: pointer;
    }
    /* 确定支付 */
    .real-pay:hover{
        cursor: pointer;
        background-color:#b7e1f1;
        color: red;
    }
    /* #endregion */

    /* #region 触发点击事件后改变样式 */

    /* 加量 */
    .click-update-style{
        transform: scale(1.08);
        box-shadow: 0px 0px 5px 0 rgba(0, 0, 0, 0.5);
        cursor: pointer;
        background-image: url(../../../assets/imgs/renewal-rectangle.png);
        border: 0;
    }
    .click-update-text-style{
        color: #5F3A00;
    }

    /* 支付方式 */
    .click-update-pay{
        background-image: url(../../../assets/imgs/tick.png);
        background-size: 32px 32px;
        background-repeat: no-repeat;
        background-position: right bottom;
        border: 1px solid var(--color-main);
    }
    /* #endregion */
</style>