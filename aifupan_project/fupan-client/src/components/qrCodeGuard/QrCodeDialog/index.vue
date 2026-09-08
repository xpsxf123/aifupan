<template>
    <customerServiceQrCode ref="qrCode" width="414px" :custom-class="customClass">
        <div v-if="isEnterpriseOrAbove">
            <div v-if="dialogHtml" v-html="dialogHtml"></div>
        </div>
        <template v-else>
            <div v-if="$store.state?.userInfo?.packageLevel === -1" class="font-s16 text-center">
                您当前的版本是<span class="font-s20 text-colorErr">激活</span>版本，<span class="font-s20 text-colorErr">需要激活后</span>才能正常使用。请添加产品顾问，帮您<span class="font-s20 text-colorErr">免费激活软件</span>并<span class="font-s20 text-colorErr">领取</span>更高的版本的试用套餐。
            </div>
            <div v-else-if="$store.state?.userInfo?.packageLevel === 0" class="font-s16 text-center">
                您当前版本是<span class="font-s20 text-colorErr">免费</span>版本，请联系产品顾问，升级您的套餐版本！
            </div>
        </template>
        <template #footer>
            <div class="text-color1 font-s12 mg-t8">微信扫码添加</div>
        </template>
    </customerServiceQrCode>
</template>

<script>
/**
 * @description 二次封装二维码弹窗：企业版以下走默认版本提示；企业版及以上按 type 渲染文案（从 JSON 读取）。
 */
import customerServiceQrCode from '@/views/commonComponent/customerServiceQrCode.vue'
import dialogCopywriting from '../dialogCopywriting.json'

export default {
    components: {
        customerServiceQrCode
    },
    props: {
        type: {
            type: String,
            default: ''
        },
        html: {
            type: String,
            default: ''
        },
        customClass: {
            type: String,
            default: 'version-qr-code'
        },
        check: {
            type: Function,
            default: null
        }
    },
    data() {
        return {
            dialogData: {
                type: '',
                html: ''
            }
        }
    },
    computed: {
        isEnterpriseOrAbove() {
            const getterFlag = this.$store.getters?.largeEnterprises
            if (getterFlag !== undefined) return !!getterFlag
            const level = Number(this.$store.state?.userInfo?.packageLevel)
            return Number.isFinite(level) ? level >= 20 : false
        },
        dialogHtml() {
            return this.dialogData.html || dialogCopywriting[this.dialogData.type] || ''
        }
    },
    methods: {
        async show(data = {}) {
            const checkFn = typeof data.check === 'function' ? data.check : this.check
            if (checkFn) {
                try {
                    const result = await checkFn(data)
                    if (result === false || result === undefined || result === null) {
                        this.$emit('skipped', { data })
                        return false
                    }
                    if (result !== true) {
                        const payload = typeof result === 'object' ? result : { type: result }
                        data = { ...data, ...payload }
                    }
                } catch (e) {
                    this.$emit('skipped', { data, error: e })
                    return false
                }
            }

            const nextType = data.type || this.type || ''
            const nextHtml = data.html || this.html || ''
            this.dialogData = {
                type: nextType,
                html: nextHtml
            }
            this.$refs.qrCode?.init?.()
            this.$nextTick(() => {
                const el = document.querySelectorAll(`.${this.customClass}`)?.[0]
                const wrapper = el?.parentNode
                if (wrapper?.classList) {
                    wrapper.classList.add('zIndex999999')
                }
            })
            this.$emit('shown', { type: nextType, html: nextHtml })
            return true
        }
    }
}
</script>

<style lang="scss">
.zIndex999999{
    z-index: 99999999 !important;
}
</style>

