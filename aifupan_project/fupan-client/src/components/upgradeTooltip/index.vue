<template>
    <span :class="['upgrade-tooltip-wrap', { 'upgrade-tooltip-wrap-block': block }]">
        <el-tooltip
            :disabled="resolvedDisabled"
            :placement="placement"
            :effect="effect"
            width="400"
            :popper-class="popperClass">
            <div slot="content">
                <slot name="content" :openUpgrade="openUpgrade" :largeEnterprises="innerLargeEnterprises" :disabled="resolvedDisabled">
                    <div class="upgrade-tooltip-content">
                        <span>{{ prefixText }}</span>
                        <span class="upgrade-tooltip-tag">
                            <slot name="tag">
                                <img :src="tagImgSrc" alt="">
                            </slot>
                        </span>
                        <span>{{ suffixText }}</span>
                        <span class="upgrade-tooltip-link" @click.stop="openUpgrade()">{{ linkText }}</span>
                    </div>
                </slot>
            </div>
            <span class="upgrade-tooltip-reference">
                <slot :largeEnterprises="innerLargeEnterprises" :disabled="resolvedDisabled"></slot>
            </span>
        </el-tooltip>
        <versionQrCode ref="versionQrCode"></versionQrCode>
    </span>
</template>

<script>
import versionQrCode from '@/views/commonComponent/versionQrCode.vue'

export default {
    name: 'UpgradeTooltip',
    inject: {
        appVnode: {default: null}
    },
    components: {
        versionQrCode
    },
    props: {
        block: {
            type: Boolean,
            default: false
        },
        disabled: {
            type: Boolean,
            default: null
        },
        disabledFn: {
            type: Function,
            default: null
        },
        largeEnterprises: {
            type: Boolean,
            default: null
        },
        placement: {
            type: String,
            default: 'top-start'
        },
        effect: {
            type: String,
            default: 'dark'
        },
        popperClass: {
            type: String,
            default: 'upgrade-tooltip-popper'
        },
        tagImgSrc: {
            type: String,
            default: require('@/assets/imgs/version-professional.png')
        },
        prefixText: {
            type: String,
            default: '开通'
        },
        suffixText: {
            type: String,
            default: '及以上版本即可使用此功能，'
        },
        linkText: {
            type: String,
            default: '去升级会员'
        }
    },
    computed: {
        innerLargeEnterprises() {
            if (this.largeEnterprises !== null) return this.largeEnterprises
            return this.$store?.getters?.largeEnterprises
        },
        resolvedDisabled() {
            if (typeof this.disabledFn === 'function') {
                return !!this.disabledFn({
                    largeEnterprises: this.innerLargeEnterprises
                })
            }
            if (this.disabled !== null) return this.disabled
            return !!this.innerLargeEnterprises
        }
    },
    methods: {
        openUpgrade(data) {
            this.$emit('upgrade-click')
            if (this.appVnode?.versionQrCodeShow) {
                this.appVnode.versionQrCodeShow(data)
                return
            }
            this.$refs.versionQrCode?.show?.(data)
        }
    }
}
</script>

<style lang="scss">
.upgrade-tooltip-wrap {
    display: inline-block;
}

.upgrade-tooltip-wrap-block {
    display: block;
}

.upgrade-tooltip-reference {
    display: inline-block;
}

.upgrade-tooltip-wrap-block .upgrade-tooltip-reference {
    display: block;
}

.upgrade-tooltip-popper {
    background: rgba(51, 51, 51, 0.96) !important;
    border: none !important;
    padding: 10px 14px !important;
    border-radius: 6px !important;
}

.upgrade-tooltip-popper[x-placement^='top'] .popper__arrow::after {
    border-top-color: rgba(51, 51, 51, 0.96) !important;
}

.upgrade-tooltip-popper[x-placement^='bottom'] .popper__arrow::after {
    border-bottom-color: rgba(51, 51, 51, 0.96) !important;
}

.upgrade-tooltip-content {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
    color: #fff;
    font-size: 12px;
    line-height: 18px;
}

.upgrade-tooltip-tag {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    max-width: 60px;
    height: 22px;

    img {
        max-width: 100%;
    }
}

.upgrade-tooltip-link {
    color: var(--color-main);
    cursor: pointer;
}
</style>
