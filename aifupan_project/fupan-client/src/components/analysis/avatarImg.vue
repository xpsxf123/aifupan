<template>
    <div class="avatar-img">
        <img v-if="avatar" :src="avatar" :class="{wordsBodyAvatar:true,sliceAvatar:replayType==='replaySection'}">
        <img v-else src="@/assets/imgs/avatar.png" :class="{wordsBodyAvatar:true,sliceAvatar:replayType==='replaySection'}">
        <img v-if="replayType==='replaySection'" src="@/assets/imgs/slice.png" class="slice">
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        avatar: {
            type:String,
            default:''
        },
        replayType:{
            type:String,
            default:''
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
.wordsBodyAvatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    border: 0.5px #ccc solid;
}
.avatar-img{
    position: relative;
    .slice{
        position: absolute;
        top: 20px;
        right: -2px;
    }
    .sliceAvatar{
        border: 0.5px #F4BE34 solid;
    }
}
</style>