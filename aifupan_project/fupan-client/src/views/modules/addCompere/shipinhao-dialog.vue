<template>
    <div>
        <dialog-box :visible.sync="dialogVisible" title="前往授权"
            custom-class="shipinhao-dialog"
            :close-on-click-modal="false" width="550px">
            <div>
                <div class="pd-t10 pd-l10 pd-b10 hint-content">
                    <div class="pd-b10">请在<span class="text-colorTheme">授权前开启带货直播数据权限</span>，否则授权后将无法获取直播间视频及相关数据。<br /></div>
                    <div class="flex-jc-sb">
                        开启授权步骤如下：<afp-button class="view-btn" type="text":plain="false" @click="openView">查看详细流程图</afp-button>
                    </div>
                </div>
                <div style="max-height: 390px;" class="overflow_hidden overflow_auto_y pd-b10 pd-l10 pd-r10">
                    <img style="width: 100%;" src="~@/assets/imgs/shipinhao_help.png" alt="" srcset="">
                </div>
                <div class="pd-r10 pd-l10 pd-t20">
                    <div class="flex-jc-sb flex-ai-c items-center">
                        <div>
                            <div class="font-bold" style="color: var(--color-main);font-size: 16px">第一步：先开启视频号助手权限</div>
                            <div class="text-xs mg-t4 text-colorErr">
                                *需要开启视频号助手带货权限，才能有在线数据和销售数据
                            </div>
                        </div>
                        <el-button type="text" size="medium" :plain="false" @click="openUrl">
                            点我开启
                        </el-button>
                    </div>

                    <div class="flex-jc-sb flex-ai-c mg-t20 items-center">
                        <div>
                            <div class="font-bold" style="color: var(--color-main);font-size: 16px">第二步:已开启，点我给爱复盘数据授权</div>
                            <div class="text-xs mg-t4 text-colorErr">*授权时，所有授权项需全部勾选</div>
                        </div>
                        <afp-button type="primary" size="default" class="text-base" :plain="false" @click="openAuth">
                           点我授权
                        </afp-button>
                    </div>
                </div>
            </div>
        </dialog-box>
        <shipinhaoDialogView ref="shipinhaoDialogView"></shipinhaoDialogView>
    </div>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog';
import shipinhaoDialogView from './shipinhao-dialogView.vue'
export default {
    components: {
        DialogBox,
        shipinhaoDialogView,
    },
    mixins: [dialogMixin],
    props: {
    },
    provide() {
        return {
        }
    },
    data() {
        return {
        };
    },
    computed: {
       
    },
    watch: {},
    methods: {
        openUrl() {
            this.$httpClient.system.openUrl({url: 'https://channels.weixin.qq.com/login.html'});
        },
        openAuth(){
            console.log('redirectUrl:',location.origin + location.pathname + '#/shipinhao-status');
            this.$httpClient.compere.authorizeWeChatChannels({
                redirectUrl:  location.origin + location.pathname + '#/shipinhao-status',
            }).then(res => {
                if(res.code == 0){
                    this.$message.success('请在微信视频号助手授权页面确认授权');
                }
            })
        },
        openView(){
            this.$refs.shipinhaoDialogView.show();
        },
        onCancel() {
            this.hide();
        },
        async showCallback() {
        },
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss'>
.shipinhao-dialog{
    .hint-content{
        position: relative;
        .view-btn{
            // position: absolute;
            // bottom: 10;
            // right: 0;
            padding: 0 !important;
            line-height: 16px !important;
            height: 16px !important;
        }
    }
}
</style>