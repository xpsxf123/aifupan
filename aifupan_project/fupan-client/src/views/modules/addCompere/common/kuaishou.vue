<template>
    <div>
        <div>在下方粘贴要检测录制的直播间账号ID，支持以下格式：</div>
        <br />
        <div>
            <span>快手ID：xxxxx(一般是一串数字、字母、下划线或减号组成,注意不是快手昵称)</span>
            <el-button type="text" @click="showDyHelp">（查看如何获取快手直播ID）</el-button>
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
            this.$emit('showDyHelp', 1)
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