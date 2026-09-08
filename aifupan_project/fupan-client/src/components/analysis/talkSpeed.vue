<template>
    <div class="wordsBodyContentText2">
        <span class="wordsBodyContentText1"> <span v-if="!notLabel">语速：</span></span>{{
            parseInt(charNumSecond || 0)
        }}字/分钟
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        charNumSecond: {
            type: [String,Number],
            default: ''
        },
        notLabel: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
        
        };
    },
    computed: {},
    watch: {},
    methods: {
        
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

.wordsBodyContentText1 {
    font-weight: 400;
    font-size: 12px;
    color: #2E3742;
}
</style>