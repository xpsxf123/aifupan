<template>
    <div class="wordsItemContainer flex-ai-c justify-between">
        <div class="wordsItemColorContainer flex-ai-c">
            <div class="wordsItemText wordsItemBox font-s12 text-color2">
                互动率：{{ interactionRate ? getInteractionPercent : '-' }}
            </div>
        </div>
        <Toggle @change="markClick" :default="interactionRate" active="点击分析" inactive='隐藏互动率'></Toggle>
    </div>
</template>

<script>
import Toggle from './toggle.vue'

export default {
    components: {Toggle},
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            interactionRate: false
        };
    },
    computed: {
        getInteractionPercent() {
            const interactionPercent = this.sentenceMarkData.interactionPercent || 0
            if (!interactionPercent) {
                return '0.00%'
            } else {
                return `${interactionPercent.toFixed(2)} %`
            }
        }
    },
    mounted() {

    },
    created() {

    },
    methods: {
        markClick() {
            this.interactionRate = !this.interactionRate
            this.$emit('click', this.interactionRate)
        }
    },
    beforeCreate() {
    }, //生命周期 - 创建之前
    beforeMount() {
    }, //生命周期 - 挂载之前
    beforeUpdate() {
    }, //生命周期 - 更新之前
    updated() {
    }, //生命周期 - 更新之后
    beforeDestroy() {
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.wordsBodyContentText2 {
    font-weight: 400;
    font-size: 12px;
}
</style>