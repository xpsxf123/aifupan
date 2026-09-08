<template>
    <div class="wordsItemContainer flex-ai-c justify-between">
        <div class="wordsItemColorContainer flex-ai-c">
            <div class="wordsItemText wordsItemBox font-s12 text-color2">
                销售额：{{ sales ? getSales : '-' }}
            </div>
        </div>
        <Toggle @change="markClick" :default="sales" active="点击分析" inactive='隐藏销售额'></Toggle>
    </div>
</template>

<script>
import Toggle from './toggle.vue'
import myUtils from "@/utils/utils";

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
            sales: true
        };
    },
    computed: {
        getSales() {
            const {volumeStart, volumeEnd} = this.sentenceMarkData
            if (volumeStart >= 0 && volumeEnd >= 0) {
                if (volumeStart === volumeEnd) {
                    return `${myUtils.fnw(this.sentenceMarkData.volumeEnd || 0,2)}`
                } else {
                    return `${myUtils.fnw(this.sentenceMarkData.volumeStart || 0,2)} - ${myUtils.fnw(this.sentenceMarkData.volumeEnd || 0,2)}`
                }
            } else {
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
            this.sales = !this.sales
            this.$emit('click', this.sales)
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