<!--
@description 投放消耗总计开关组件：用于段落工具栏下拉中展示总投放消耗，并控制段落投放消耗显隐。
-->
<template>
    <div class="wordsItemContainer flex-ai-c justify-between">
        <div class="wordsItemColorContainer flex-ai-c">
            <div class="wordsItemText wordsItemBox font-s12 text-color2">
                投放消耗：{{ totalQianchuanCostVisible ? formatCost : '-' }}
            </div>
        </div>
        <Toggle
            @change="markClick"
            :default="totalQianchuanCostVisible"
            active="点击分析"
            inactive="隐藏投放消耗" />
    </div>
</template>

<script>
/**
 * @description 投放消耗总计开关组件脚本：统一处理总投放消耗的格式化与显隐切换。
 */
import Toggle from './toggle.vue'

export default {
    name: 'TotalQianchuanCost',
    components: { Toggle },
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            totalQianchuanCostVisible: true
        }
    },
    computed: {
        formatCost() {
            const value = this.sentenceMarkData?.totalQianchuanCost
            if (value === undefined || value === null || value === '') {
                return null
            }
            const numberValue = Number(value)
            if (Number.isNaN(numberValue)) {
                return `${value}元`
            }
            return `${numberValue.toLocaleString('zh-CN', {
                minimumFractionDigits: 0,
                maximumFractionDigits: 2
            })}元`
        }
    },
    methods: {
        /**
         * @description 切换段落投放消耗展示状态。
         * @returns {void}
         */
        markClick() {
            this.totalQianchuanCostVisible = !this.totalQianchuanCostVisible
            this.$emit('click', this.totalQianchuanCostVisible)
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
