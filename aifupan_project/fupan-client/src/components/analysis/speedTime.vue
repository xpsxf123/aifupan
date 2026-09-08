<template>
    <div class="wordsBodyContentText2">
        <span v-if="isAi">{{ label }}</span><span>{{ toformatTime(startTime) }}</span>
    </div>
</template>

<script>
import myUtils from '/src/utils/utils';
export default {
    components: {},
    props:{
        startTime: {
            type:[String,Number],
            default: 0
        },
        isAi: {
            type: Boolean,
            default: false
        },
        label: {
            type: String,
            default: '本段开始时间：'
        }
    },
    data() {
        return {
        
        };
    },
    computed: {},
    watch: {},
    methods: {
        // 毫秒时间戳转成时分秒格式
        toformatTime(val) {
            return myUtils.toformatTime(val);
        },
    },
    created() {
        
    },
    mounted() {
        
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
    color: #95A1AF;
}
</style>