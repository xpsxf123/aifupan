<template>
    <dialog-box :visible.sync="dialogVisible" class="forced-update-dialog"
    :show-close="false"
    :close-on-press-escape="false"
    :close-on-click-modal="false" width="618px">
        <div class="pd-l20 pd-r20 pd-t16 pd-b0 forced-update-content">
            <div class="flex-jc-sb">
                <div class="title">
                    <h3 class="mg-0 font-s24 text-colorMain">发现新版本<span class="font-s12 text-colorMain mg-l10">({{ dialogData.updateVersion }}版本可更新，您现在为{{dialogData.currentVersion}}版本)</span></h3>
                    <div class="text-colorTheme title-content">2万+头部直播间在用的AI直播复盘工具</div>
                </div>
                <div>
                    <img style="width: 90px;" src="@/assets/imgs/new/logo_forced_update.png" alt="">
                </div>
            </div>
            <!-- <div class="pd-t22">
                <h1 class="pd-l16 pd-t40">
                    <span class="font-s30" style="color: #195EFF;">AI复盘专家</span>
                    <br>
                    <span class="font-s30" style="color: #181D55;">抖音豆包AI+Deepseek</span>
                </h1>
            </div> -->
            <div class="pd-t20 pd-l10 pd-r10">
                <div class="pd-b10" style="height: 240px;">
                    <h4 class="mg-0 text-colorMain pd-b8">{{dialogData.updateVersion}}版本更新内容：</h4>
                    <div class="content font-s12 text-colorMain pd-l10" v-html="dialogData.updateRemarks"></div>
                </div>
                <div class="text-center">
                    <afp-button type="default" :plain="false" v-if="!dialogData.updateType" size="default" @click="closeDialog">暂不更新</afp-button>
                    <afp-button type="primary" size="default" @click="forceUpdate">点我立即更新</afp-button>
                </div>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog'
export default {
    components: {
        DialogBox
    },
    mixins: [dialogMixin],
    props:{
        
    },
    data() {
        return {
        };
    },
    computed: {
    },
    watch: {},
    methods: {
        // updateType 0：可选更新 1：强制更新
        closeDialog(){
            this.hide()
        },
        forceUpdate(){
            this.$httpClient.setup.getmodel({}).then((res) => {
                if (res.data.IsRocord == 1) {
                    // 2025/12/18  中午，11：48。慢总要求，强制更新时，如果在录制，将原有取消逻辑修改为暂不更新，并关闭弹窗.
                    this.$confirm('目前正在录制中，将停止录制进行更新，是否继续？', '提示', {
                        confirmButtonText: '确定',
                        cancelButtonText: '暂不更新',
                        type: 'warning'
                    }).then(() => {
                            this.$httpClient.compere.stopdecector().then((res) => {
                            // 更新
                            this.$httpBack.user.logout({}).then(res => {
                                this.$httpClient.setup.updateProgram({});
                            });
                        });
                    }).catch((err) => {
                        this.hide();
                    });
                } else {
                    // 更新
                    let path = this.$route.path
                    if (path === '/' || path === '/login'){
                        this.$httpClient.setup.updateProgram({});
                    }else {
                        this.$httpBack.user.logout({}).then(res => {
                            this.$httpClient.setup.updateProgram({});
                        });
                    }
                }
            });
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

.forced-update-dialog{
    ::v-deep(.el-dialog) {
        box-shadow: none !important;
        padding: 0;
        min-height: 399px;
        max-height: 426px;
        background-image: url('~@/assets/imgs/new/forcedUpdate.png');
        background-size: cover;
        background-color: transparent;
        border: none !important;
        .el-dialog__header{
            display: none;
        }
        .el-dialog__header{
            padding: 0;
            height: 0;
        }
        .el-dialog__headerbtn{
            font-size: 28px !important;
            z-index: 9999;
        }
        .content{
            width:  100% !important;
            height: 205px;
            overflow: hidden;
            overflow-y: auto;
            *{
                padding: 3px 0;
                margin: 0;
            }
        }
        .title-content{
            padding-top: 8px;
            font-weight:  600;
            font-size: 18px;
        }
    }
}
</style>