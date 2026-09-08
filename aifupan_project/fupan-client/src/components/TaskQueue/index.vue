<template>
    <div class="uploadProgressPopContainer" @click="clickQueue">
        <div class="uploadProgressPopContainer-text" style="color: var(--color-main);font-size: 10px;">
            <span v-if="uploading">正在上传</span>
            <span v-else style="color: #07AF54;">上传成功</span>
        </div>
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        uploading: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
        
        };
    },
    computed: {},
    watch: {},
    methods: {
        clickQueue(){
            this.$emit('click')
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
.uploadProgressPopContainer {
    /* display: flex;
    align-items: center;
    justify-content: center; */
    position: fixed;
    right: 25px;
    bottom: 32px;
    color: #FFF;
    z-index: 999999;
    cursor: pointer;
    width: 92px;
    height: 50px;
    line-height: 1.7;
    /* line-height: 50px; */
    background-size: cover;
    /* background-color: #fff; */
    background-image: url('~@/assets/imgs/Fileup.png');
    &-text{
        width: 92px;
        text-align: right;
        padding-right: 12px;
        padding-top: 8px;
    }
}
</style>