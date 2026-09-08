<template>
    <div class="shipinghao-authorization">
        <div class="title flex-ji-c" style="height: 400px;">视频号授权说明</div>
        <div class="flex-ji-c">
            <el-button type="primary" size="large" @click="toIfupanWebsite">去授权</el-button>
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
        toIfupanWebsite(){
            location.href = location.origin + location.pathname + '#/shipinhao-status';
            // window.open('https://www.ifupan.com/');
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