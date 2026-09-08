<template>
    <div>
        <div class="title flex-ji-c" style="height: 400px;">
            <div>
                <div class="title flex-ji-c">授权成功</div>
                <div v-if="isSuccess" class="flex-ji-c text-color3 font-s12 mg-t20">关闭弹窗前往添加视频号</div>
            </div>
        </div>
        <!-- <div class="flex-ji-c">
            关闭弹窗前往添加视频号
        </div> -->
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        
    },
    data() {
        return {
            time: 5,
            isSuccess: true,
        };
    },
    computed: {},
    watch: {},
    methods: {
        saveInfo(){
            localStorage.setItem('shipinhaoInfo', JSON.stringify(this.$route.query) || location.href)
        },
    },
    created() {  
    },
    mounted() {
        this.saveInfo()
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