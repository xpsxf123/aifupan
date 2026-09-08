<template>
    <div class="not-found">
        <img src="@/assets/imgs/not_found.png" style="width: 354px;height: 266px;">
        <span>页面丢失,前往<a href="https://www.ifupan.com">爱复盘官网</a></span>
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        
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
.not-found {
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: column;
    color: #151719;
    font-size: 16px;
    height: 100vh;
}
</style>