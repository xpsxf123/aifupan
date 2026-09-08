<template>
    <div class="wordsBodyContentText2 not-word">
        <span>
            自然时间：{{ naturalTime }}
        </span>
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        naturalTime: {
            type: String,
            default: ''
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
</style>