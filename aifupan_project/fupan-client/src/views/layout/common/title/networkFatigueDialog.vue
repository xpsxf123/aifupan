<template>
    <el-dialog
        :visible.sync="dialogVisible"
        width="392px"
        class="network-fatigue-dialog"
        :show-close="false"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :destroy-on-close="false">
        <div class="network-fatigue-dialog__main">
            <div class="network-fatigue-dialog__bg"></div>
            <div class="network-fatigue-dialog__panel">
                <div class="network-fatigue-dialog__content">
                    <div class="network-fatigue-dialog__tip">
                        当前电脑的网络IP疲劳，
                        <span class="network-fatigue-dialog__highlight">有可能无法正确获取抖音在线数据和弹幕数据</span>，
                        请定时重启光猫，可以更换新的网络IP。
                    </div>
                    <div class="network-fatigue-dialog__desc">
                        直播每天重启光猫和路由器，核心是通过“断电-上电”的物理重置，清除设备长期运行累积的缓存、采收内存、修复进程异常，同步网络重置并降低硬件热负荷，从而快速恢复网络稳定性，避免录制或直播中出现卡顿、延迟、断流等问题。
                    </div>
                    <div class="network-fatigue-dialog__desc">
                        授权抖音账号（非直播账号）后，能有效降低网络IP疲劳度，是否马上授权。
                    </div>
                </div>
                <div class="network-fatigue-dialog__footer">
                    <div class="network-fatigue-dialog__btn network-fatigue-dialog__btn--ghost" @click="ignoreRisk">
                        无视风险
                    </div>
                    <ThirdPartyAuthAction :platform="platform" tag="div" @click="authorizeNow">
                        <div class="network-fatigue-dialog__btn network-fatigue-dialog__btn--primary">点我授权</div>
                    </ThirdPartyAuthAction>
                </div>
            </div>
        </div>
    </el-dialog>
</template>

<script>
/**
 * @description 网络疲惫提示弹窗。
 * 负责监听 openNetworkFatiguePage 推送，并提供忽略风险、点击授权两种处理动作。
 */
import ThirdPartyAuthAction from './thirdPartyAuthAction.vue'

export default {
    name: 'NetworkFatigueDialog',
    components: {
        ThirdPartyAuthAction
    },
    props: {
        platform: {
            type: Number,
            default: 0
        }
    },
    data() {
        return {
            dialogVisible: false
        }
    },
    methods: {
        /**
         * @description 处理网络疲惫弹窗推送数据
         * @param {{open?: number}} payload 推送数据
         * @returns {boolean}
         */
        applyNetworkFatiguePayload(payload = {}) {
            const openValue = Number(payload?.open)
            if (openValue !== 1) {
                return false
            }
            this.dialogVisible = true
            return true
        },
        /**
         * @description 注册网络疲惫弹窗监听
         */
        registerNetworkFatigueNotify() {
            this.$CSharpNotify.addTask('openNetworkFatiguePage', (res) => {
                this.applyNetworkFatiguePayload(res)
            })
        },
        /**
         * @description 测试能力：手动触发网络疲惫弹窗
         * @param {number} open 是否开启弹窗
         * @returns {boolean}
         */
        previewNetworkFatigueDialog(open = 1) {
            return this.applyNetworkFatiguePayload({open})
        },
        /**
         * @description 无视风险
         */
        ignoreRisk() {
            this.dialogVisible = false
            this.$emit('ignore-risk')
        },
        /**
         * @description 点我授权
         * @param {Object} payload 授权点击参数
         */
        authorizeNow(payload) {
            this.dialogVisible = false
            this.$emit('authorize-click', payload)
        }
    },
    mounted() {
        this.registerNetworkFatigueNotify()
    }
}
</script>

<style lang="scss" scoped>
.network-fatigue-dialog{
    ::v-deep(.el-dialog){
        border-radius: 20px;
        overflow: hidden;
    }

    ::v-deep(.el-dialog__header){
        display: none;
    }

    ::v-deep(.el-dialog__body){
        padding: 0;
    }
}

.network-fatigue-dialog__main{
    position: relative;
    border-radius: 20px;
    overflow: hidden;
}

.network-fatigue-dialog__bg{
    position: absolute;
    inset: 0;
    background: url("~@/assets/imgs/wlpb.png") no-repeat top center;
    background-size: cover;
}

.network-fatigue-dialog__panel{
    position: relative;
    z-index: 1;
    margin: 146px 12px 0;
    border-radius: 20px;
    padding: 12px 14px 16px;
}

.network-fatigue-dialog__content{
    padding: 0;
    padding-top: 32px;
    font-size: 14px;
    line-height: 22px;
    letter-spacing: 0.7px;
}

.network-fatigue-dialog__tip{
    font-size: 14px;
    line-height: 22px;
    color: #243241;
}

.network-fatigue-dialog__highlight{
    color: #FF4949;
    font-weight: 500;
}

.network-fatigue-dialog__desc{
    margin-top: 10px;
    font-size: 14px;
    line-height: 22px;
    color: #243241;
}

.network-fatigue-dialog__footer{
    display: flex;
    justify-content: center;
    gap: 16px;
    padding-top: 22px;
}

.network-fatigue-dialog__btn{
    width: 126px;
    height: 40px;
    border-radius: 24px;
    font-size: 20px;
    line-height: 40px;
    text-align: center;
    box-sizing: border-box;
    cursor: pointer;
    user-select: none;
}

.network-fatigue-dialog__btn--ghost{
    border: 1px solid #FF7B2D;
    color: #FF7B2D;
    background: #FFFFFF;
}

.network-fatigue-dialog__btn--primary{
    background: linear-gradient( 180deg, #FFA14E 0%, #FF7B2D 100%);
    color: #FFFFFF;
}
</style>
