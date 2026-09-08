<template>
    <component
        :is="tag"
        class="third-party-auth-action"
        :class="{'third-party-auth-action--disabled': disabled}"
        @click="handleClick">
        <slot></slot>
    </component>
</template>

<script>
/**
 * @description 第三方授权点击动作组件。
 * 用于统一处理授权点击行为，默认按平台配置打开对应授权地址。
 */
import {THIRD_PARTY_PLATFORM_MAP} from './titleStatusConfig'

export default {
    name: 'ThirdPartyAuthAction',
    props: {
        platform: {
            type: Number,
            default: 0
        },
        disabled: {
            type: Boolean,
            default: false
        },
        autoOpen: {
            type: Boolean,
            default: true
        },
        tag: {
            type: String,
            default: 'div'
        },
        url: {
            type: String,
            default: ''
        },
        requestData: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    computed: {
        currentPlatformConfig() {
            return THIRD_PARTY_PLATFORM_MAP[this.platform] || null
        },
        targetUrl() {
            return this.url || this.currentPlatformConfig?.authorizeUrl || ''
        }
    },
    methods: {
        /**
         * @description 处理授权点击
         * @param {MouseEvent} event 点击事件
         */
        handleClick(event) {
            if (this.disabled) {
                return
            }
            const payload = {
                platform: this.platform,
                url: this.targetUrl,
                event
            }
            this.$emit('click', payload)

            if (this.platform === 0 && this.$httpClient?.douyin?.authorizeDouyin) {
                this.$httpClient.douyin.authorizeDouyin(this.requestData).then((res) => {
                    if (res?.code !== 0) {
                        this.$message?.error?.(res?.msg || '抖音授权失败')
                    }
                })
                return
            }

            if (!this.autoOpen || !this.targetUrl) {
                return
            }
            if (this.$httpClient?.system?.openUrl) {
                this.$httpClient.system.openUrl({url: this.targetUrl})
                return
            }
            window.open(this.targetUrl, '_blank')
        }
    }
}
</script>

<style lang="scss" scoped>
.third-party-auth-action{
    display: inline-flex;
}

.third-party-auth-action--disabled{
    cursor: default;
}
</style>
