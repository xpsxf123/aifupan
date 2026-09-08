<template>
    <span class="font-s12 text-color3" style="cursor: pointer;"  @click="onClick">
        {{ flod ? '收起':'展开' }}<i :class="flod?'el-icon-arrow-down': 'el-icon-arrow-up'"></i>
    </span>
</template>

<script>

export default {
    components: {},
    props:{
        
    },
    data() {
        return {
            flod: false
        };
    },
    computed: {},
    watch: {},
    methods: {
        onClick(){
            this.flod = !this.flod;
            this.$emit('click',this.flod)
        }
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

</style>