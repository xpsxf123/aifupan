<template>
    <div class="polish">
        <slot></slot>
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
@keyframes shark-txt {
  from{
    left: -100px;
  }
  50%{
    left: 100%;
  }
  to {
    left: -100px;
  }
}
.polish{
    position: relative;
    overflow: hidden;
    display: flex;
    align-items: center;
    &::after{
        content: ' ';
        height: 200%;
        width: 100px;
        position: absolute;
        top: -50%;
        left: -100px;
        -webkit-text-fill-color: transparent;
        animation: shark-txt 6s infinite;
        background: linear-gradient(110deg, rgba(255,255,255,0) 40%, rgba(255, 255, 255, 0.7), rgba(255,255,255,0) 60%);
    }
}
</style>