<template>
    <div>
         <!-- 单场复盘 -->
        <only v-if="ifTabsIndex(0)"></only>
        <!-- 上传文件 -->
        <upload v-if="ifTabsIndex(1)"></upload>
        <!-- 对比分析 -->
        <compare v-if="ifTabsIndex(2)"></compare>
        <!-- 文件对比 -->
        <fileCompare v-if="ifTabsIndex(3)"></fileCompare>
    </div>
</template>

<script>
import Tabs from '@/mixins/tabs.js';
import upload from './upload/index.vue';
import only from './only/index.vue';
import compare from './compare/index.vue';
import fileCompare from './fileCompare/index.vue';
export default {
    mixins: [Tabs],
    components: {
        upload,
        only,
        compare,
        fileCompare
    },
    props:{
        
    },
    data() {
        return {
            tabs: [
                { label: "单场复盘", name: 'only_v' },
                { label: "文件分析", name: 'upload_v' },
                // { label: "对比复盘", name: 'compare_v' },
                // { label: "文件对比", name: 'fileCompare_v' },
            ],
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
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>

</style>