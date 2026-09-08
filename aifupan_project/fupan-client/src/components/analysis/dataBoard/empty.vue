<template>
    <div class="empty">
        <div class="tips" v-if="insufficientResources">注：数据看板不包含在套餐中，请额外购买，
            <span class="buy" @click="showCustomerServiceQrCode">点击购买</span>
        </div>
        <div class="no-data">
            <img src="@/assets/imgs/chartEmpty.png" alt=""
                 v-if="unOpened||notIncludedAnchor||dataFail||lessThanFifty||unPlayed||isEmptyAsValue"
                 class="image-style"/>
            <img src="@/assets/imgs/resources.png" alt="" v-if="insufficientResources" class="image-style"/>
            <img src="@/assets/imgs/data-summary.png" alt="" v-if="dataSummary" class="image-style"/>
            <template v-if="buyInStatus===1">
                <div style="font-weight: 500" v-if="unOpened">巨量数据异常，暂时无法获取数据</div>
            </template>
            <template v-else>
                <div style="font-weight: 500" v-if="unOpened">未开启数据看板</div>
            </template>
            <afp-button v-if="insufficientResources" type="primary"  class="recharge"
                       @click="showCustomerServiceQrCode">立即充值
            </afp-button>
            <div class="font-500" v-if="dataSummary">10分钟后请按刷新按钮，获取数据</div>
            <div class="font-500" v-if="lessThanFifty">低于50分钟的录屏，<br/>需要手动刷新才会获取数据</div>
            <div class="font-500" v-if="notIncludedAnchor">账号流量较低，暂未收录</div>
            <div class="font-500" v-if="dataFail">数据获取失败，请重试</div>
            <div class="font-500" v-if="isEmptyAsValue">数据整理中，请稍后获取</div>
            <div class="font-500" style="margin-bottom: 16px" v-if="unPlayed">直播间还没下播，暂时无法提供数据。<br/>直播中的数据不完整，因此建议直播间下播后，您手动获取
            </div>
            <div style="margin-top: 50px" v-if="networkError">网络问题导致数据抓取错误，请重启网络后，重新获取</div>
        </div>
    </div>
</template>

<script>

export default {
    components: {},
    inject: ['appVnode'],
    props: {
        //-2:网络加载异常 -1：未开启数据看板 0：正在拉取 1：拉取成功 2：拉取失败 3：未收录主播 4：自动生成但视频未达到50分钟 5：资源不足 6：主播未下播 7:数据整理中，请稍后获取 8:销售数据异常
        isEmptyType: {
            type: String | Number,
            default: ''
        },
        buyInStatus: {
            type: String | Number,
            default: ''
        }
    },
    data () {
        return {}
    },
    computed: {
        networkError () {
            return this.isEmptyType === -2
        },
        unOpened () {
            return this.isEmptyType === -1
        },
        dataSummary () {
            return this.isEmptyType === 0
        },
        dataFail () {
            return this.isEmptyType === 2
        },
        notIncludedAnchor () {
            return this.isEmptyType === 3
        },
        lessThanFifty () {
            return this.isEmptyType === 4
        },
        insufficientResources () {
            return this.isEmptyType === 5
        },
        unPlayed () {
            return this.isEmptyType === 6
        },
        isEmptyAsValue () {
            return this.isEmptyType === 7
        }
    },
    watch: {},
    methods: {
        showCustomerServiceQrCode () {
            this.appVnode?.showQrCode()
        },
    },
    created () {

    },
    mounted () {

    },
    beforeCreate () { }, //生命周期 - 创建之前
    beforeMount () { }, //生命周期 - 挂载之前
    beforeUpdate () { }, //生命周期 - 更新之前
    updated () { }, //生命周期 - 更新之后
    beforeDestroy () { }, //生命周期 - 销毁之前
    destroyed () { }, //生命周期 - 销毁完成
    activated () { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.empty {
    display: flex;
    width: 100%;
    justify-content: center;
    align-items: flex-end;
    position: relative;

    .tips {
        font-weight: 500;
        font-size: 12px;
        position: absolute;
        left: 6px;

        .buy {
            color: #1E87FF;
            cursor: pointer
        }
    }

    .no-data {
        text-align: center;
        padding-bottom: 12px;

        .image-style {
            display: block;
            width: 160px;
            margin: 0 auto;
        }

        .recharge {
            border-radius: 42px;
            color: #fff;
            background: linear-gradient(90deg, #E89E73 0%, #D57646 100%);
            font-size: 12px;
            border: none;
            cursor: pointer;
        }

        .font-500 {
            font-weight: 500;
        }
    }
}
</style>
