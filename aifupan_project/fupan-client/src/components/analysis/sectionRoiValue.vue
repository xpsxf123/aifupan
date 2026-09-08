<!--
@description 段落 ROI 指标展示组件：用于在详情页段落信息区展示投放消耗、净成交额等简短指标。
-->
<template>
    <div class="section-roi-value" :class="colorClass">
        <span class="section-roi-label" v-if="!notLabel">{{ label }}：</span>
        <span class="section-roi-value-text">{{ formatValue }}</span>
    </div>
</template>

<script>
/**
 * @description 段落 ROI 指标展示组件：统一处理标题、数值与单位拼接，展示风格与销售额/UV/成交等段落指标保持一致。
 */
export default {
    name: 'SectionRoiValue',
    props: {
        label: {
            type: String,
            default: ''
        },
        value: {
            type: [String, Number],
            default: ''
        },
        unit: {
            type: String,
            default: '元'
        },
        notLabel: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        /**
         * @description 按指标类型返回对应颜色类，避免 ROI 指标继续沿用灰色并与现有指标撞色。
         * @returns {string}
         */
        colorClass() {
            if (this.label === '投放消耗') {
                return 'section-roi-value--cost';
            }
            if (this.label === '净成交ROI') {
                return 'section-roi-value--net-roi';
            }
            return 'section-roi-value--default';
        },
        formatValue() {
            if (this.value === undefined || this.value === null || this.value === '') {
                return '-';
            }
            const num = Number(this.value);
            const valueText = Number.isNaN(num)
                ? this.value
                : num.toLocaleString('zh-CN', {
                    minimumFractionDigits: 0,
                    maximumFractionDigits: 2
                });
            return `${valueText}${this.unit}`;
        }
    }
}
</script>

<style lang="scss" scoped>
.section-roi-value {
    font-weight: 400;
    font-size: 12px;
}

.section-roi-value--default {
    color: #5B6CFF;
}

.section-roi-value--cost {
    color: #2F6BFF;
}

.section-roi-value--net-roi {
    color: #F06A2A;
}

.section-roi-label {
    color: inherit;
}

.section-roi-value-text {
    color: inherit;
}
</style>
