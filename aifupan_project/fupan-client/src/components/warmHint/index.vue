<template>
    <dialog-box class="dialog-box" :visible.sync="dialogVisible" :close-on-click-modal="false" :show-close="false" width="436px">
        <div class="warm-hint-box">
            <slot></slot>
            <div class="dialog-wxts-btn flex-jc-sb justify-around">
                <el-button round class="left-btn" @click="leftBtn">{{ leftBt }}</el-button>
                <el-button round class="right-btn" @click="rightBtn">{{ rightBt }}</el-button>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from './../dialog'
import dialogMixin from '@/mixins/dialog.js'
export default {
    components: {DialogBox},
    mixins: [dialogMixin],
    props:{
        leftBt: {
            type:String,
            default: '去删除'
        },
        rightBt: {
            type:String,
            default: '去购买'
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
        background-image: url('~@/assets/imgs/wxts.png');
        background-repeat: no-repeat;
        background-size: contain;
        background-color: transparent;
        box-shadow: none;
        .el-dialog__header{
            display: none;
        }
        .el-dialog__body{
            padding: 30px 20px;
        }
    }
    .dialog-wxts-btn{
        position: absolute;
        width: 100%;
        bottom: 0px;
        text-align: center;
        >.el-button{
            width:118px ;
        }
    }
    .left-btn{
        border-color: #C97A50;
        color: #C97A50;
        background: transparent;
        height: var(--height-default);
        &:hover{
            border-color: #C97A50;
            color: #C97A50;
            background: transparent;
        }
    }
    .right-btn{
        border:none;
        color: #fff;
        background: linear-gradient( 93deg, #E89E73 0%, #D57646 100%);
        height: var(--height-default);
        &:hover{
            color: #fff;
            background: linear-gradient( 93deg, #E89E73 0%, #D57646 100%);
        }
    }
    .warm-hint-box{
        padding: 111px 10px 0;
        //width: 412px;
        height: 300px;
        overflow: hidden;
    }
}
</style>