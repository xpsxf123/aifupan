<template>
    <div>
        <el-dialog :visible.sync="visible" width="260px" :close-on-click-modal="false">
            <div class="close" @click="closePrompt">
                <img src="@/assets/imgs/close_new.png">
            </div>
            <div class="content">
                <img src="@/assets/imgs/prompt.png">
                <span class="prompt">温馨提示</span>
                <span>亲爱的用户，别忘了打开</span>
                <span> “点我录制分析”按钮哦！</span>
                <span class="start-msg">点击开启，精彩随手记录！</span>
                <div class="dialog-btn">
                    <div class="not-prompt" @click="notPrompt">不再提醒</div>
                    <div class="begin" @click="beginRecord">点我录制分析</div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script>
    export default{
        data(){
            return{
                visible:false,
                callback: null
            }
        },
        methods:{

            // 每次调用该弹窗时，用一个参数控制是否需要开启弹窗
            // 当点击不再提示时，关闭弹窗并将该参数变为false；在次日时再次将该参数设为true
            // 当点击关闭按钮或点击录制时,关闭弹窗,但不设该参数为false
            checkAndShowPopup(callback){
                this.detection = this.$store.state.detectionStatus;
                if (!this.detection){
                    const lastVisit = localStorage.getItem("lastVisit");
                    // 点击了不在提示
                    if(localStorage.getItem("promptVisible") === 'false'){
                        this.visible = false;
                    }else{
                        if(!lastVisit || (new Date() - new Date(lastVisit)) > 24 * 60 * 60 * 1000){
                            this.visible = true;
                            localStorage.setItem("promptVisible",String(true));
                            localStorage.setItem('lastVisit', new Date().toISOString()); // 更新最后访问时间
                        }
                    }
                }
                this.callback = callback || null;
            },
            notPrompt(){
                this.visible = false;
                // 控制是否打开弹窗
                localStorage.setItem("promptVisible",String(false));
            },
            beginRecord(){
                this.visible = false;
                // 控制是否打开组件
                localStorage.setItem("currentPrompt",String(false));
                this.$emit('beginRecord');
                if(typeof this.callback === 'function'){
                    this.callback();
                }
            },
            closePrompt(){
                this.visible = false;
                localStorage.setItem("currentPrompt",String(false))
            }
        }
    }
</script>

<style scoped>
.close{
    display: flex;
    justify-content: end;
    padding: 15px 16px 0px 0px;
    margin-bottom: 10px;
    cursor: pointer;
}
.content{
    display: flex;
    justify-content: center;
    flex-direction: column;
    align-items: center;
    margin: 0px 20px;
    font-size: 14px;
    color: #151917;
    padding-bottom: 40px;
}
.prompt{
    color: #151917;
    font-size: 16px;
    font-weight: bold;
    margin:18px 0px 20px 0px
}
.start-msg{
    margin-top: 8px;
    color: #4D4D4D;
    font-size: 12px;
}
.dialog-btn{
    display: flex;
    justify-content: center;
    margin-top: 28px;
}
.not-prompt{
    border: 1px solid #ABAEB3;
    margin-right: 12px;
    cursor: pointer;
    padding-inline: 12px;
    line-height: var(--height-default);
    height: var(--height-default);
    border-radius: var(--height-default);
}
.begin{
    background-color: var(--color-main);
    padding-inline: 12px;
    color: #FFFFFF;
    cursor: pointer;
    height: var(--height-default);
    line-height: var(--height-default);
    border-radius: var(--height-default);
}

::v-deep .el-dialog__header{
    padding: 0px;
}

::v-deep .el-dialog__body{
    padding: 0px;
}

::v-deep .el-dialog__headerbtn {
  display: none; 
}

::v-deep .el-dialog{
    border-radius: 4px;
}
</style>