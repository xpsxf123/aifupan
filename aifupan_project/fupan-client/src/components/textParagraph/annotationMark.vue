<template>
    <span v-if="mark[item.index]" class="annotation-item-mark"
    :class="`annotation_${mark[item.index]?.markStartIndex}_${mark[item.index]?.markEndIndex}_${opt?.paragraphIndex}`">批{{ mark[item.index]?.markNo }}</span>
</template>

<script>

export default {
    components: {},
    props:{
        mark:{
            type: Object,
            default: () => {}
        },
        item:{
            type: Object,
            default: () => {}
        },
        opt:{
            type: Object,
            default: () => {}
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

</style>