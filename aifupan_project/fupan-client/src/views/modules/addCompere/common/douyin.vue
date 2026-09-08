<template>
   <div>
        <template v-if="isRecord">
            <div>在下方粘贴要检测录制的一个直播间账号，支持以下格式：</div>
            <br />
        </template>
        <!-- <div>1、直播账号的电脑网页直播页链接：https://live.douyin.com/xxxxxxxxxxxx</div> -->
        <div>
            <span>{{isRecord?'直播账号':'达人账号'}}的抖音号：xxxxx(一般是一串数字或者英文字母,注意不是抖音昵称)</span>
            <el-button class="mg-l10" type="text" @click="showDyHelp">(查看如何获取抖音号)</el-button>
        </div>
        <!-- <div>3、APP分享链接：https://v.douyin.com/xxxxxx</div> -->
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        isRecord: {
            type: Boolean,
            default: true
        }
    },
    data() {
        return {

        };
    },
    computed: {},
    watch: {},
    methods: {
        showDyHelp(){
            this.$emit('showDyHelp')
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