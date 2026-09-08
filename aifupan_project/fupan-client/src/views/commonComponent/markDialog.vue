<template>
    <dialog-box :visible.sync="dialogVisible" class="mark-dialog-box" :close-on-click-modal="false" width="540px">
        <div class="markDialogContainer">
            <div v-if="dialogData?.sufficient">
                <div class="markDialogBodyContainer">
                    <div class="markDialogBodyText1" v-if="dialogData?.duration">本视频共计{{ dialogData?.duration }}分钟</div>
                    <div class="markDialogBodyText1" v-if="dialogData?.wordNum">本文件共计{{ dialogData?.wordNum }}个字</div>
                    <div class="markDialogBodyText2" v-if="dialogData?.duration">标注敏感词将消耗资源，当前剩余资源：{{
                        dialogData?.userProperty.videoTaggingTime
                    }}分钟，是否继续？
                    </div>
                    <div class="markDialogBodyText2" v-if="dialogData?.wordNum">标注敏感词将消耗资源，当前剩余资源：{{
                       dialogData?.propertyWordNum || dialogData?.userProperty.textTaggingWordCount
                    }}字，是否继续？</div>
                </div>
                <div class="markDialogBtnContainer">
                    <div class="markDialogBtn1" @click="cancalMark">取消</div>
                    <div class="markDialogBtn2" @click="confirmUseMark">继续</div>
                </div>
            </div>
            <div v-else>
                <div class="markDialogBodyContainer">
                    <div class="markDialogBodyText1" v-if="dialogData?.duration">本视频共计{{ dialogData?.duration }}分钟</div>
                    <div class="markDialogBodyText1" v-if="dialogData?.wordNum">本文件共计{{ dialogData?.wordNum }}个字</div>
                    <div class="markDialogBodyText2">当前流量包不足，还请充值后再使用</div>
                </div>
                <div class="markDialogBtnContainer">
                    <div class="markDialogBtn1" @click="cancalMark">知道了</div>
                    <div class="markDialogBtn2" @click="showCustomerServiceQrCode">
                        立即充值
                    </div>
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
    props: {
        sufficient: {
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
        cancalMark(){
            this.$emit('cancal')
        },
        confirmUseMark(){
            this.$emit('useMark', this.dialogData)
        },
        showCustomerServiceQrCode(){
            this.$emit('qrCode')
        }
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
<style lang='scss' scoped>
.mark-dialog-box {
    ::v-deep(.el-dialog) {
        background: linear-gradient(#FFEED9, #FFFCF9);
    }

    .markDialogBodyContainer {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
    }

    .markDialogBodyText2 {
        margin-top: 10px;
    }

    .markDialogBtnContainer {
        display: flex;
        align-items: center;
        justify-content: center;
        margin-top: 24px;
    }

    .markDialogBtn2 {
        width: 110px;
        height: 40px;
        background: #5F3A00;
        border-radius: 4px;
        margin-left: 20px;
        color: #FFF;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
    }

    .markDialogBtn1 {
        width: 110px;
        height: 40px;
        border-radius: 4px;
        border: 1px solid #B26D00;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
    }
}
</style>