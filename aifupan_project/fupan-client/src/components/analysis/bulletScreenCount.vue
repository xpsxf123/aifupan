<template>
    <div class="wordsItemContainer flex-ai-c justify-between">
        <div class="wordsItemColorContainer flex-ai-c">
            <div class="wordsItemText wordsItemBox font-s12 text-color2">
                弹幕总数：{{ bulletScreen?countNum:'**' }}条
            </div>
        </div>
        <Toggle @change="markClick" :default="bulletScreen" active="标记弹幕" inactive='隐藏弹幕'></Toggle>
    </div>
</template>

<script>
import Toggle from './toggle.vue'
export default {
    components: {Toggle},
    props:{
        countNum: {
            type: [String,Number],
            default: 0
        }
    },
    data() {
        return {
            bulletScreen: true
        };
    },
    mounted() {

    },
    created() {

    },
    methods: {
        markClick() {
            this.bulletScreen = !this.bulletScreen
            this.$emit('click', this.bulletScreen)
        }
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.wordsBodyContentText2 {
    font-weight: 400;
    font-size: 12px;
}
</style>