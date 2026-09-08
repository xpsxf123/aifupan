<template>
    <div>
        <div>通过以下方式添加视频号授权,目前仅支持录制自有视频号账号。</div>
        <br />
        <div>
            <span>请在授权前开启带货直播数据权限，否则授权后将无法获取直播间视频及相关数据。</span>
<!--            <el-button type="text" @click="showDyHelp">（查看如何授权视频号）</el-button>-->
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
        showDyHelp(){
            this.$emit('showDyHelp', 2)
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