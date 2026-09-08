<template>
    <div
        v-if="currentNetworkStatus"
        class="network-status-badge"
        :class="`network-status-badge--${currentNetworkStatus}`">
        <span class="network-status-badge__icon">
            <img class="network-status-badge__icon-img" :src="currentNetworkIcon" alt="">
        </span>
        <span class="network-status-badge__text font-s12">{{ currentNetworkStatusText }}</span>
    </div>
</template>

<script>
/**
 * @description 标题栏网络状态角标。
 * 负责监听客户端 networkStatus 推送，并在标题栏展示网络状态。
 */
import {NETWORK_STATUS_MAP, NETWORK_STATUS_STORAGE_KEY} from './titleStatusConfig'
import wifiGood from '@/assets/imgs/icon/wifi1.png'
import wifiExhausted from '@/assets/imgs/icon/wifi2.png'
import wifiPoor from '@/assets/imgs/icon/wifi3.png'

export default {
    name: 'NetworkStatusBadge',
    data() {
        return {
            networkStatus: ''
        }
    },
    computed: {
        currentNetworkStatus() {
            return Object.prototype.hasOwnProperty.call(NETWORK_STATUS_MAP, this.networkStatus) ? this.networkStatus : ''
        },
        currentNetworkStatusText() {
            return NETWORK_STATUS_MAP[this.currentNetworkStatus]?.text || ''
        },
        currentNetworkIcon() {
            const iconMap = {
                good: wifiGood,
                exhausted: wifiExhausted,
                poor: wifiPoor
            }
            return iconMap[this.currentNetworkStatus] || wifiGood
        }
    },
    methods: {
        /**
         * @description 处理网络状态推送数据
         * @param {{status?: string}} payload 推送数据
         * @returns {boolean}
         */
        applyNetworkStatusPayload(payload = {}) {
            const currentStatus = payload?.status || ''
            if (!Object.prototype.hasOwnProperty.call(NETWORK_STATUS_MAP, currentStatus)) {
                return false
            }
            this.networkStatus = currentStatus
            return true
        },
        /**
         * @description 注册客户端网络状态监听
         */
        registerNetworkStatusNotify() {
            this.$CSharpNotify.addTask('networkStatus', (res) => {
                this.applyNetworkStatusPayload(res)
            })
        },
        /**
         * @description 测试能力：手动更新网络状态
         * @param {string} status 网络状态值
         * @returns {boolean}
         */
        previewNetworkStatus(status = 'good') {
            return this.applyNetworkStatusPayload({status})
        }
    },
    mounted() {
        this.registerNetworkStatusNotify()
    }
}
</script>

<style lang="scss" scoped>
.network-status-badge{
    display: inline-flex;
    align-items: center;
    height: 32px;
    padding: 0 14px 0 10px;
    margin-right: 12px;
    border-radius: 18px;
    border: 1px solid currentColor;
    background: #FFFFFF;
    font-size: 16px;
    line-height: 1;
    box-sizing: border-box;
    flex-shrink: 0;
}

.network-status-badge__icon{
    position: relative;
    width: 18px;
    height: 18px;
    margin-right: 6px;
    border-radius: 50%;
    box-sizing: border-box;
    flex-shrink: 0;
    overflow: hidden;
}

.network-status-badge__icon-img{
    width: 100%;
    height: 100%;
    display: block;
}

.network-status-badge__text{
    white-space: nowrap;
}

.network-status-badge--good{
    color: #32C36C;
    background: #F3FCF7;
    border-color: #67D68F;
}

.network-status-badge--exhausted{
    color: #E0B13A;
    background: #FFFBF1;
    border-color: #F0CE79;
}

.network-status-badge--poor{
    color: #F05B5B;
    background: #FFF5F5;
    border-color: #F18B8B;
}
</style>
