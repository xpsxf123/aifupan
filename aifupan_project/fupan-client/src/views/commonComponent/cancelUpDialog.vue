<template>
    <div>
        <el-dialog :visible.sync="visible"  width="324px" :close-on-click-modal="false">
            <div class="cancelUpClose" @click="visible = false">
                <img src="@/assets/imgs/close.png" style="width: 18px;height: 18px;">
            </div>
            <div class="cancelUpWarningLogo">
                <img src="@/assets/imgs/Frame.png" style="width: 60px;height: 60px;">
            </div>
            <div class="warningInfo">
                <span class="warningText">目前正在上传云空间，关闭后无法继续上传，是否关闭窗口？</span>
            </div>
            <div class="cancelUpButton">
                <afp-button @click="cancelClick">确认离开</afp-button>
                <afp-button type="primary" @click="visible = false">继续上传</afp-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
    export default{
        data(){
            return {
                visible:false,
            }
        },
        methods:{
            init(){
                this.visible = true;
            },
            cancelClick(){
                this.visible = false;
                this.$emit('child-event');
            }
        }
    }
</script>

<style>
    .cancelUpClose{
        display: flex;
        justify-content: end;
        margin-right: 16px;
        padding-top: 16px;
        cursor: pointer;
    }
    .cancelUpWarningLogo{
        display: flex;
        justify-content: center;
    }
    .warningInfo{
        display: flex;
        justify-content: center;
        margin: 28px 0px 40px 0px;
    }
    .warningText{
        width: 244px;
        text-align: center;
        color: #151917;
        font-size: 14px;
    }
    .cancelUpButton{
        display: flex;
        justify-content: center;
        padding-bottom: 50px;
    }
</style>