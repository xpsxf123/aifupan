<template>
    <!-- 续费页面 -->
    <div class="renewal">
        <!-- 续费顶部 -->
        <div class="renewal-top">
            <i class="el-icon-arrow-left" style="cursor: pointer;" @click="back"></i>
            <div class="xufei">购买版本</div>
        </div>

        <!-- 续费中间具体内容 -->
        <div class="renewal-center">
            <div class="renewal-info-top">

                <div class="renewal-account">
                    <div style="margin-right:9px; color: #2E3742; font-size: 14px;">当前版本：</div>
                    <div class="version-gratis">
                        <div style="margin-right: 8px;">{{ packageInfo.packageName }}</div>
                        <!-- <img class="version-gratis-logo" src="@/assets/imgs/version-gratis.png" alt=""> -->
                    </div>
                </div>

                <div class="purchase-version">
                    <div style="font-size: 14px; margin-right: 6px; color: #2E3742;">购买版本：</div>
                    <!-- <div class="version-professional">
                        <img class="version-professional-logo" src="@/assets/imgs/version-professional.png" alt="">
                    </div>
                    <div style="font-size: 14px;">包含12个直播号，支持xxx</div> -->
                    <div v-for="item in packageInfo.packageList" :key="item.id"
                        :class="item.id == currentPackageInfo.id ? 'actionPackageName' : 'packageName'"
                        style="margin-right: 16px;" @click="clickPackage(item)">{{ item.name
                        }}</div>
                </div>

                <div class="time">
                    <div class="time-text">购买时长：</div>

                    <div v-for="item in currentPackageInfo.commodityPriceList" :key="item.id"
                        :class="{ 'one-year': true, 'all-test': item.id == currentCommodityPriceInfo.id }"
                        @click="clickPrice(item)">
                        <div class="six-fold" v-if="item.discount != 1">
                            <div class="six-fold-text">{{ '限时' + item.discount * 10 + '折' }}</div>
                        </div>
                        <div class="one-year-text">{{ item.validityNum + ['小时', '天', '月', '季度', '半年',
                            '年'][item.validityUnit] }}</div>
                        <div class="one-year-money">￥{{ retainDecimals(item.realPrice / 100) }}</div>
                        <div class="one-year-Nomoney">￥{{ retainDecimals(item.originalPrice / 100) }}</div>
                        <div :class="{ 'six-month-block1': item.id == currentCommodityPriceInfo.id }"></div>
                        <div :class="{ 'six-month-block2': item.id == currentCommodityPriceInfo.id }"></div>
                    </div>

                </div>
            </div>

            <div v-if="currentPackageInfo.typeConsumptionList && currentPackageInfo.typeConsumptionList.length > 0"
                style="margin-top: 10px;">
                <div class="account-amount" v-for="item in currentPackageInfo.typeConsumptionList" :key="item.id">
                    <div style="margin-right: 8px;">{{ item.commodityTypeName + "：" }}</div>

                    <div v-if="item.commodityTypeCode == 'storageNum'">{{ retainDecimals(item.number / 1024 / 1024) +
                        'GB' }}</div>
                    <div v-else>{{ item.number + item.commodityTypeUnit }}</div>
                </div>
            </div>


            <!-- 中间内容下部内容 -->
            <!-- <div class="renewal-info-bottom">
                <div class="analyse-text" style="margin-bottom: 24px;">
                    <div style="margin-right: 8px;">AI话术分析：</div>
                    <div>100小时</div>
                </div>
                <div class="analyse-text">
                    <div style="margin-right: 8px;">关键词分析：</div>
                    <div>20小时</div>
                </div>
                <div class="payment">
                    <div class="payment-text" style="margin-right: 4px;">支付方式：</div>
                    <div :class="{ 'weixin-payment': true, 'payment-change': wxPaymentActive }" @click="wxPaymentClick">
                        <img class="wx-pay-img" src="@/assets/imgs/wx_pay.png" alt="" />
                        <div class="weixin-payment-text">微信支付</div>
                        <img v-if="wxPaymentActive" :class="{ 'pay-tick': wxPaymentActive }"
                            src="@/assets/imgs/tick.png" />
                    </div>

                    <div :class="{ 'ali-payment': true, 'payment-change': aliPaymentActive }" @click="aliPaymentClick">
                        <img class="ali-pay-img" src="@/assets/imgs/ali_pay.png" alt="">
                        <div class="ali-payment-text">支付宝支付</div>
                        <img v-if="aliPaymentActive" :class="{ 'pay-tick': aliPaymentActive }"
                            src="@/assets/imgs/tick.png" />
                    </div>

                </div>
            </div> -->
        </div>

        <!-- 续费底部内容 -->
        <div class="renewal-bottom">
            <div style="display: flex; align-items: center;justify-content: space-between; width: 100%;">
                <div style="display: flex; align-items: center;margin-left: 40px">
                    <el-checkbox class="renewal-checkbox"></el-checkbox>
                    <div class="agreement-text">输入我已阅读并同意《服务协议》</div>
                </div>
                <div style="display: flex; align-items: center; margin-right: 28px;">
                    <div class="payment-money-text">实际支付：</div>
                    <div class="payment-money">{{ paymentMoney.money }}</div>
                    <afp-button type="primary" @click="submitPay">确定支付</afp-button>
                </div>
            </div>
        </div>

        <CustomerServiceQrCode v-if="qrCodeVisible" ref="customerServiceQrCode"></CustomerServiceQrCode>
    </div>
</template>

<script>

import CustomerServiceQrCode from '../../commonComponent/customerServiceQrCode.vue';
import myUtils from "../../../utils/utils";

export default {
    components: { CustomerServiceQrCode },
    data() {
        return {
            qrCodeVisible: false,
            packageInfo: {
                packageList: [],
            },
            currentPackageInfo: {
                commodityPriceList: [],
                typeConsumptionList: [],
            },
            currentCommodityPriceInfo: {},
            paymentMoney: {
                money: 0
            }
        }
    },
    created() {
        this.getPackageList();
        this.paymentMoney.money = this.purchaseYear.money;
    },
    methods: {
        clickPackage(packageItem) {
            this.currentPackageInfo = packageItem;
            this.currentCommodityPriceInfo = this.currentPackageInfo.commodityPriceList[0];
            this.clickPrice(this.currentCommodityPriceInfo);
        },
        retainDecimals(val) {
            return myUtils.retainDecimals(val);
        },
        clickPrice(priceItem) {
            this.paymentMoney.money = this.retainDecimals(priceItem.realPrice / 100);
            this.currentCommodityPriceInfo = priceItem;
        },
        getPackageList() {
            this.$httpBack.userPackage.packageListAll().then(res => {
                if (res.code == 0) {
                    this.packageInfo = res.data;
                    this.currentPackageInfo = this.packageInfo.packageList[0];
                    this.clickPackage(this.currentPackageInfo);
                }
            })
        },
        // 返回设置页
        back() {
            this.$store.commit("saveSetupMenu", "accountSetup");
            this.$emit("updateMenuIndex", 4);
        },
        submitPay() {
            this.qrCodeVisible = true;
            this.$nextTick(() => {
                this.$refs.customerServiceQrCode.init();
            })
        },
        purchaseYearClick() {
            this.paymentMoney.money = this.purchaseYear.money;
            this.sixMonthActive = false;
            this.oneMonthActive = false;
            this.yearActive = true;
        },
        purchaseSixMonthClick() {
            this.paymentMoney.money = this.purchaseSixMonth.money;
            this.yearActive = false;
            this.oneMonthActive = false;
            this.sixMonthActive = true;
        },
        purchaseOneMonthClick() {
            this.paymentMoney.money = this.purchaseOneMonth.money;
            this.yearActive = false;
            this.sixMonthActive = false;
            this.oneMonthActive = true;
        },

        wxPaymentClick() {
            this.aliPaymentActive = false;
            this.wxPaymentActive = true
        },
        aliPaymentClick() {
            this.wxPaymentActive = false;
            this.aliPaymentActive = true;
        }
    }
}
</script>

<style scoped>
.packageName {
    cursor: pointer;
}

.actionPackageName {
    color: var(--color-main);
    cursor: pointer;
    border-bottom: 2px var(--color-main) solid;
}

/* #region 续费顶部 */
.renewal-top {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #DCE0E7;
    width: calc(100% - 28px);
    padding-bottom: 20px;
    font-size: 20px;
    margin-left: 14px;
}

/* 返回图标 */
.fanhui {
    /* font-size: 14.14px; */
}

/* 续费 */
.xufei {
    color: #2E3742;
    font-size: 16px;
    margin-left: 8px;
}

/* #endregion */

/* #region 续费中间顶部内容 */
.renewal-center {
    display: flex;
    flex-direction: column;
    margin-top: 27px;
}

/* 内容上部文本 */
.renewal-info-top {
    display: flex;
    flex-direction: column;
    font-size: 14px;
    margin-left: 16px;
}

/* 内容账号样式 */
.renewal-account {
    display: flex;
    align-items: center;
    margin-bottom: 24px;
}

/* 免费版本样式 */
.version-gratis {
    display: flex;
    align-items: center;
    overflow: hidden;
}

/* 免费版本logo */
.version-gratis-logo {
    width: 86px;
    height: 27px;
}

/* 购买版本 */
.purchase-version {
    display: flex;
    align-items: center;
    margin-bottom: 80px;
}

/* 专业版本样式 */
.version-professional {
    display: flex;
    margin-right: 10px;
}

.version-professional-logo {
    width: 86px;
    height: 27px;
}

/* 购买时长 */
.time {
    display: flex;
}

.time-text {
    margin-right: 9px;
    color: #2E3742;
    font-size: 14px;
}

/* 购买时长一年样式 */
.one-year {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 166px;
    height: 113.29px;
    margin-top: -45px;
    background-color: #FFFBF5;
    border: 1px solid #F3B272;
    border-radius: 8px;
    margin-right: 32px;
    transition: all 0.3s;
}

.six-fold {
    display: flex;
    justify-content: center;
    position: absolute;
    right: -1px;
    bottom: 103px;
    padding: 0 10px;
    height: 20px;
    border-radius: 0px 8px 0px 8px;
    background-image: linear-gradient(137deg, #F23600 0%, #FF683C 100%);
}

.six-fold-text {
    color: #FFFFFF;
    font-size: 14px;
}

.one-year-text {
    font-size: 18px;
    color: #2E3742;
    margin-bottom: 8px;
}

.one-year-money {
    font-size: 22px;
    color: #2E3742;
}

.one-year-Nomoney {
    text-decoration-line: line-through;
    font-size: 14px;
    color: #2E3742;
}

/* 购买6个月时长样式 */
.six-month {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 166px;
    height: 113.29px;
    margin-top: -47px;
    background-color: #FFFBF5;
    border: 1px solid #F3B272;
    border-radius: 8px;
    margin-right: 32px;
    transition: all 0.3s;
}

.seven-flod {
    display: flex;
    justify-content: center;
    position: absolute;
    right: -1px;
    bottom: 101px;
    width: 62px;
    height: 20px;
    background-image: linear-gradient(137deg, #F23600, #FF683C);
    border-radius: 0px 8px 0px 8px;
}

.seven-flod-text {
    font-size: 14px;
    color: #FFFFFF;
}

.six-month-text {
    font-size: 18px;
    color: #2E3742;
    margin-bottom: 8px;
}

.six-month-money {
    font-size: 22px;
    color: #2E3742;
}

.six-month-Nomoney {
    text-decoration-line: line-through;
    font-size: 14px;
    color: #2E3742;
}

/* 购买1个月时长样式 */
.one-month {
    position: relative;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    width: 166px;
    height: 113.29px;
    margin-top: -45px;
    background-color: #FFFBF5;
    border: 1px solid #F3B272;
    border-radius: 8px;
    transition: all 0.3s;
}

.one-month-text {
    font-size: 18px;
    color: #2E3742;
    margin-bottom: 8px;
}

.one-month-money {
    font-size: 22px;
    color: #2E3742;
}

/* #endregion */

/* 直播号数量 */
.account-amount {
    display: flex;
    margin: 12px 0px 0px 4px;
    font-size: 14px;
    color: #2E3742;
}

/* #region 续费中间下部内容 */
.renewal-info-bottom {
    margin: 24px 0px 0px 4px;
    width: 746.5px;
    height: 250px;
}

.analyse-text {
    display: flex;
    font-size: 14px;
    color: #2E3742;
}

/* 选择支付方式 */
.payment {
    display: flex;
    margin: 40px 0px 0px 18px;
    font-size: 14px;
    color: #2E3742;
}

.weixin-payment {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-top: -16px;
    margin-right: 32px;
    width: 166px;
    height: 52px;
    background-color: #FFFFFF;
    border: 1px solid #DCE0E7;
    border-radius: 8px;
    cursor: pointer;
}

.wx-pay-img {
    width: 24px;
    height: 24px;
    margin-right: 8px;
}

.ali-payment {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-top: -16px;
    width: 166px;
    height: 52px;
    background-color: #FFFFFF;
    border: 1px solid #DCE0E7;
    border-radius: 8px;
    cursor: pointer;
}

.ali-pay-img {
    width: 24px;
    height: 24px;
    margin-right: 11px;
}

/* #endregion */


/* #region 续费底部内容 */
.renewal-bottom {
    display: flex;
    align-items: center;
    width: calc(100% + 36px);
    height: 88px;
    border-top: 1px solid #DCE0E7;
    margin-left: -18px;
    position: absolute;
    bottom: -18px;
}

.el-checkbox .el-checkbox__inner {
    width: 20px;
    height: 20px;
    border-radius: 3px;
}

.el-checkbox__inner::after {
    width: 6px;
    height: 13px;
    left: 5px;
}

.agreement-text {
    font-size: 13px;
    color: #677583;
    margin-left: 10px;
}

.payment-money-text {
    font-size: 14px;
    color: #2E3742;
}

.payment-money {
    font-size: 22px;
    color: #2E3742;
    margin-right: 21px;
}

.payment-sure {
    background-color: var(--color-main);
    color: #FFFFFF;
}

/* #endregion */

/* 鼠标悬浮时改变样式 */
.one-year:hover {
    cursor: pointer;
    transform: scale(1.08);
    box-shadow: 0px 0px 5px 0 rgba(0, 0, 0, 0.5);
}

.six-month:hover {
    cursor: pointer;
    transform: scale(1.08);
    box-shadow: 0px 0px 5px 0 rgba(0, 0, 0, 0.5);
}

.one-month:hover {
    cursor: pointer;
    transform: scale(1.08);
    box-shadow: 0px 0px 5px 0 rgba(0, 0, 0, 0.5);
}

/* 鼠标点击时改变样式 */
.six-month-block1 {
    position: absolute;
    width: 31.5px;
    height: 63px;
    background-color: #FFD28D;
    border-top-left-radius: 63px;
    border-bottom-left-radius: 63px;
    opacity: 0.57;
    right: -1px;
    top: 41px;
}

.six-month-block2 {
    position: absolute;
    width: 73px;
    height: 36px;
    background-image: linear-gradient(34deg, #FFE6AD, #FFC875);
    opacity: 0.42;
    border-top-left-radius: 36px;
    border-top-right-radius: 36px;
    border-bottom-right-radius: 8px;
    right: 0px;
    bottom: -1px;
}

.all-test {
    background-image: linear-gradient(146deg, #FFEED9, #FFD8B4);
    transform: scale(1.08);
    box-shadow: 0px 0px 5px 0 rgba(0, 0, 0, 0.5);
}

.payment-change {
    border: 1px solid var(--color-main);
}

.pay-tick {
    position: absolute;
    right: 0;
    bottom: 0;
    width: 32px;
    height: 32px;
}

.renewal {
    position: relative;
    height: calc(100vh - 52px);
}
</style>