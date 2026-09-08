<template>
    <dialog-box class="dialog-box" :visible.sync="dialogVisible" :close-on-click-modal="false" :show-close="false" width="340px">
        <div class="warm-hint-box">
            <div class="warm-hint-content">
                <slot></slot>
            </div>
            <div class="dialog-wxts-btn flex-jc-sb">
                <afp-button round class="left-btn" @click="leftBtn">{{ leftBt }}</afp-button>
                <afp-button round class="right-btn" @click="rightBtn" type="primary">{{ rightBt }}</afp-button>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog'
import dialogMixin from '@/mixins/dialog.js'
export default {
    components: {DialogBox},
    mixins: [dialogMixin],
    props:{
        leftBt: {
            type:String,
            default: '取消'
        },
        rightBt: {
            type:String,
            default: '切换模型'
        }
    },
    data() {
        return {
        
        };
    },
    computed: {},
    watch: {},
    methods: {
        leftBtn(){
            this.$emit('left-click')
        },
        rightBtn(){
            this.$emit('right-click')
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
.dialog-box{
    ::v-deep(.el-dialog) {
        padding: 0;
        background-image: url('~@/assets/imgs/aiDialog.png');
        background-size: cover;
        background-color: transparent;
        box-shadow: none;
        .el-dialog__header{
            display: none;
        }
        .warm-hint-box{
            padding-top: 80px;
            padding-left: 35px;
            padding-right: 35px;
            height: 234px;
        }
        .warm-hint-content{
            height: 80px;
        }
        .dialog-wxts-btn{
            margin-top: 20px;
            padding-bottom: 19.5px;
        }
    }
}
</style>