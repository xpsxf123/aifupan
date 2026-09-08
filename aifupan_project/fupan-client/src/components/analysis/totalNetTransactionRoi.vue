<!--
@description 净成交ROI总计开关组件：用于段落工具栏下拉中展示总净成交ROI，并控制段落净成交ROI显隐。
-->
<template>
    <div class="wordsItemContainer flex-ai-c justify-between">
        <div class="wordsItemColorContainer flex-ai-c">
            <div class="wordsItemText wordsItemBox font-s12 text-color2">
                净成交ROI：{{ totalNetTransactionRoiVisible ? formatRoi : '-' }}
            </div>
        </div>
        <Toggle
            @change="markClick"
            :default="totalNetTransactionRoiVisible"
            active="点击分析"
            inactive="隐藏净成交ROI" />
    </div>
</template>

<script>
/**
 * @description 净成交ROI总计开关组件脚本：统一处理总净成交ROI的格式化与显隐切换。
 */
import Toggle from './toggle.vue'

export default {
    name: 'TotalNetTransactionRoi',
    components: { Toggle },
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            totalNetTransactionRoiVisible: true
        }
    },
    computed: {
        formatRoi() {
            const value = this.sentenceMarkData?.totalNetTransactionRoi
            if (value === undefined || value === null || value === '') {
                return null
            }
            const numberValue = Number(value)
            if (Number.isNaN(numberValue)) {
                return `${value}`
            }
            return numberValue.toLocaleString('zh-CN', {
                minimumFractionDigits: 0,
                maximumFractionDigits: 2
            })
        }
    },
    methods: {
        /**
         * @description 切换段落净成交ROI展示状态。
         * @returns {void}
         */
        markClick() {
            this.totalNetTransactionRoiVisible = !this.totalNetTransactionRoiVisible
            this.$emit('click', this.totalNetTransactionRoiVisible)
        }
    }
}
</script>

<style lang="scss" scoped>
.wordsBodyContentText2 {
    font-weight: 400;
    font-size: 12px;
}
</style>
