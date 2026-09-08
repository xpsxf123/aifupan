<template>
    <div>
        <el-dialog :visible.sync="visible" width="260px">
            <div class="prompt-msg">
                <img src="@/assets/imgs/1_9_30/warning.png" width="60px">
                <span>友情提示</span>
            </div>
            <div class="prompt-text">
                <span>本地电脑时间与北京时间不一致，请校准时间后再使用</span>
                <el-popover
                    placement="right-start"
                    title=""
                    width="400"
                    trigger="hover">
                    <div slot="reference" class="look">
                        查看如何校准
                    </div>
                    <div class="helpHtml-box" v-html="helpHtml"></div>
                </el-popover>
            </div>
            <div class="btn">
                <div class="btn-l" @click="close">退出软件</div>
                <div class="btn-r" @click="getTimeAccurate">我已校准</div>
            </div>
        </el-dialog>
    </div>
</template>

<script>
export default {
    data() {
        return {
            visible: false,
            helpHtml: ''
        }
    },
    created(){

    },
    methods: {
        init() {
            this.visible = true;
            if(!this.helpHtml){
                this.showDyHelp();
            }
        },
        close() {
            this.$emit("closeClient")
        },
        // 获取当前系统时间是否准确
        getTimeAccurate() {
            this.$httpClient.setup.getTimeAccurate().then(res => {
                if (res.data) {
                    this.visible = false;
                } else {
                    this.visible = true;
                    this.$message.error("时间仍未校准，请检查");
                }
            })
        },
        showDyHelp(){
            this.$httpBack.article.list({ limit: -1, type: 1}).then((res)=>{
                if(res.code === 0&& res.data){
                    this.helpHtml = res.data.list[0].content;
                    this.helpHtml = this.helpHtml.replaceAll("<img",'<img style="width: 100%"');
                }
            })
        }
    }
}
</script>

<style scoped>
.prompt-msg {
    display: flex;
    align-items: center;
    flex-direction: column;
    justify-content: center;
    gap: 20px;
    padding-top: 50px;
    color: #151719;
    font-size: 16px;
    font-weight: bold;
}

.prompt-text {
    margin: 16px 40px 24px 45px;
    color: #484A4D;
    font-size: 14px;
}

.look {
    display: flex;
    justify-content: end;
    margin-top: 12px;
    color: var(--color-main);
}

.btn {
    display: flex;
    justify-content: center;
    gap: 20px;
    font-size: 14px;
    color: #151719;
    padding-bottom: 40px;
}

.btn-l {
    border-radius: 4px;
    border: 1px solid #ABAEB3;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 8px 16px;
    cursor: pointer;
}

.btn-r {
    border-radius: 4px;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 8px 16px;
    color: #FFFFFF;
    background-color: var(--color-main);
    cursor: pointer;
}

::v-deep .el-dialog__header {
    padding: 0px;
}

::v-deep .el-dialog__body {
    padding: 0px;
}

::v-deep .el-dialog__headerbtn {
    display: none;
}

::v-deep .el-dialog {
    border-radius: 4px;
}
.helpHtml-box{
    width: 100%;
    max-height: 300px;
    overflow: hidden;
    overflow-y: auto;
}
</style>