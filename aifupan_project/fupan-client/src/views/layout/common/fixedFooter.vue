<template>
    <div class="fixed-footer-box">
        <div class="fixed-footer-windons">
            <slot></slot>
        </div>
        <div class="fixed-footer-content">
            <slot name="footer"></slot>
        </div>
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
.fixed-footer-box{
    height: 100%;
    overflow: hidden;
}
.fixed-footer-windons{
    height: calc(100vh - 46px - 70px);
    overflow: hidden;
    overflow-y: auto;
}
.fixed-footer-content{
    height: 70px;
}
</style>