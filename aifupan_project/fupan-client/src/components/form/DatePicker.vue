<template>
    <el-date-picker v-model="getValues" size="default"
        v-bind="getBind">
    </el-date-picker>
</template>

<script>
import commonMixin from './mixin'
export default {
    components: {},
    mixins: [commonMixin],
    props: {

    },
    data() {
        return {};
    },
    computed: {
        getBind(){
            return {
                'range-separator':'~',
                'start-placeholder':'开始时间',
                'end-placeholder':'结束时间',
                ...this.$attrs
            }
        }
    },
    watch: {},
    methods: {

    },
    created() {
    },
    mounted() {

    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped></style>