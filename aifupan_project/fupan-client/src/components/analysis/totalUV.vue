<template>
    <div class="wordsItemContainer flex-ai-c justify-between">
        <div class="wordsItemColorContainer flex-ai-c">
            <div class="wordsItemText wordsItemBox font-s12 text-color2">
                UV价值：{{ totalUV ? getTotalUV : '-' }}
            </div>
        </div>
        <Toggle @change="markClick" :default="totalUV" active="点击分析" inactive='隐藏UV价值'></Toggle>
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
            totalUV: false
        };
    },
    computed: {
        getTotalUV() {
            const {uvValueStart, uvValueEnd} = this.sentenceMarkData
            if(uvValueStart>=0&&uvValueEnd>=0){
                if (uvValueStart === uvValueEnd) {
                    return `${(this.sentenceMarkData.uvValueEnd || 0).toFixed(2)}`
                } else {
                    return `${(this.sentenceMarkData.uvValueStart || 0).toFixed(2)} - ${(this.sentenceMarkData.uvValueEnd || 0).toFixed(2)}`
                }
            }else {
                return null
            }
        }
    },
    mounted() {

    },
    created() {

    },
    methods: {
        markClick() {
            this.totalUV = !this.totalUV
            this.$emit('click', this.totalUV)
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