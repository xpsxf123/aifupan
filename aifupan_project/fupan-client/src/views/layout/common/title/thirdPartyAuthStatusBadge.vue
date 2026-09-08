<template>
    <ThirdPartyAuthAction
        v-if="currentPlatformLabel && currentStatusText"
        :platform="platform"
        :disabled="!canAuthorize"
        @click="handleAuthorizeClick">
        <div
            class="third-party-auth-badge"
            :class="[
                `third-party-auth-badge--${currentAuthTone}`,
                {'third-party-auth-badge--clickable': canAuthorize}
            ]">
            <span class="third-party-auth-badge__icon-wrap">
                <img
                    v-if="currentPlatformConfig?.icon"
                    :src="currentPlatformConfig.icon"
                    class="third-party-auth-badge__icon"
                    alt="">
            </span>
            <span class="third-party-auth-badge__platform font-s12">{{ currentPlatformLabel }}</span>
            <span class="third-party-auth-badge__status font-s12" :class="`third-party-auth-badge__status--${currentAuthTone}`">
                {{ currentStatusText }}
            </span>
        </div>
    </ThirdPartyAuthAction>
</template>

<script>
/**
 * @description 第三方授权状态角标。
 * 负责监听客户端 thirdPartyAuthStatus 推送，并按平台展示授权状态。
 */
import ThirdPartyAuthAction from './thirdPartyAuthAction.vue'
import {
    THIRD_PARTY_AUTH_STATUS_MAP,
    THIRD_PARTY_PLATFORM_MAP,
    getThirdPartyAuthStorageKey
} from './titleStatusConfig'

export default {
    name: 'ThirdPartyAuthStatusBadge',
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
            authStatus: ''
        }
    },
    computed: {
        currentPlatformConfig() {
            return THIRD_PARTY_PLATFORM_MAP[this.platform] || null
        },
        currentAuthStatus() {
            return Object.prototype.hasOwnProperty.call(THIRD_PARTY_AUTH_STATUS_MAP, this.authStatus) ? this.authStatus : ''
        },
        currentAuthTone() {
            return THIRD_PARTY_AUTH_STATUS_MAP[this.currentAuthStatus]?.tone || ''
        },
        currentPlatformLabel() {
            return this.currentPlatformConfig?.label || ''
        },
        currentStatusText() {
            return THIRD_PARTY_AUTH_STATUS_MAP[this.currentAuthStatus]?.suffix || ''
        },
        canAuthorize() {
            return !!THIRD_PARTY_AUTH_STATUS_MAP[this.currentAuthStatus]?.canAuthorize
        }
    },
    methods: {
        /**
         * @description 处理第三方授权状态推送数据
         * @param {{status?: string, platform?: number}} payload 推送数据
         * @returns {boolean}
         */
        applyThirdPartyAuthPayload(payload = {}) {
            const currentPlatform = Number(payload?.platform)
            const currentStatus = payload?.status || ''
            if (currentPlatform !== this.platform) {
                return false
            }
            if (!Object.prototype.hasOwnProperty.call(THIRD_PARTY_AUTH_STATUS_MAP, currentStatus)) {
                return false
            }
            this.authStatus = currentStatus
            return true
        },
        /**
         * @description 注册第三方授权状态监听
         */
        registerThirdPartyAuthNotify() {
            this.$CSharpNotify.addTask('thirdPartyAuthStatus', (res) => {
                this.applyThirdPartyAuthPayload(res)
            })
        },
        /**
         * @description 测试能力：手动更新第三方授权状态
         * @param {string} status 授权状态
         * @param {number} platform 平台
         * @returns {boolean}
         */
        previewThirdPartyAuthStatus(status = 'wait', platform = this.platform) {
            return this.applyThirdPartyAuthPayload({status, platform})
        },
        /**
         * @description 透出授权点击事件
         * @param {Object} payload 点击参数
         */
        handleAuthorizeClick(payload) {
            this.$emit('authorize-click', payload)
        }
    },
    mounted() {
        this.registerThirdPartyAuthNotify()
    }
}
</script>

<style lang="scss" scoped>
.third-party-auth-badge{
    display: inline-flex;
    align-items: center;
    height: 32px;
    padding: 0 14px 0 10px;
    margin-right: 12px;
    border-radius: 18px;
    border: 1px solid #D8DDE6;
    background: #FFFFFF;
    line-height: 1;
    box-sizing: border-box;
    flex-shrink: 0;
}

.third-party-auth-badge--clickable{
    cursor: pointer;
}

.third-party-auth-badge__icon-wrap{
    width: 18px;
    height: 18px;
    margin-right: 6px;
    border-radius: 50%;
    overflow: hidden;
    flex-shrink: 0;
    background: #000;
}

.third-party-auth-badge__icon{
    width: 100%;
    height: 100%;
    display: block;
}

.third-party-auth-badge__text{
    white-space: nowrap;
}

.third-party-auth-badge__platform{
    color: #303133;
    margin-right: 2px;
}

.third-party-auth-badge__status{
    white-space: nowrap;
}

.third-party-auth-badge__status--wait{
    color: #444DFF;
}

.third-party-auth-badge__status--fail{
    color: #F05B5B;
}

.third-party-auth-badge__status--success{
    color: #32C36C;
}
</style>
