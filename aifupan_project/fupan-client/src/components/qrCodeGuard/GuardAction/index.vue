<template>
    <div class="guardActionContainer">
        <QrCodeDialog ref="qrDialog" />
        <slot :guard="guard" :loading="loading">
            <el-button
                :type="buttonType"
                :disabled="disabled || loading"
                @click="guard">
                {{ text }}
            </el-button>
        </slot>
    </div>
</template>

<script>
/**
 * @description 资源拦截按钮/动作容器：执行 check，返回 false 放行；返回 type/对象则弹二维码弹窗。
 */
import QrCodeDialog from '../QrCodeDialog/index.vue'

export default {
    components: {
        QrCodeDialog
    },
    props: {
        text: {
            type: String,
            default: '点击'
        },
        buttonType: {
            type: String,
            default: 'primary'
        },
        disabled: {
            type: Boolean,
            default: false
        },
        check: {
            type: Function,
            default: null
        }
    },
    data() {
        return {
            loading: false
        }
    },
    methods: {
        async run(check, onPass) {
            if (this.disabled || this.loading) return
            this.loading = true
            let result = false
            try {
                const fn = typeof check === 'function' ? check : this.check
                result = await (fn ? fn() : false)
            } catch (e) {
                result = false
            } finally {
                this.loading = false
            }

            if (result === false || result === undefined || result === null) {
                if (typeof onPass === 'function') {
                    onPass()
                }
                this.$emit('success')
                return
            }

            const payload = typeof result === 'object' ? result : { type: result }
            this.$refs.qrDialog?.show?.(payload)
            this.$emit('blocked', payload)
        },
        guard(onPass) {
            return this.run(this.check, onPass)
        },
        openByType(type, html) {
            this.$refs.qrDialog?.show?.({ type, html })
        }
    }
}
</script>

<style scoped lang="scss">
.guardActionContainer{
    display: inline-block;
}
</style>

