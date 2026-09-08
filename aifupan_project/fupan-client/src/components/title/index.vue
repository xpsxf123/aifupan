<template>
    <h4 class="title-box">
        <slot></slot>
    </h4>
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
.title-box{
    position: relative;
    padding-left: 8px;
    font-weight: 500;
    font-size: 14px;
    color: #151719;
    margin: 0;
    &::after{
        content: ' ';
        position: absolute;
        left: 0;
        top: 2px;
        width: 4px;
        height: 16px;
        background: #05D0FF;
        border-radius: 42px 42px 42px 42px;
    }
}
</style>