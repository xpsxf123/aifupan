<template>
    <div class="buyInAccountType">
        <el-dialog
            title="请选择巨量授权账号类型"
            :close-on-press-escape="false"
            :close-on-click-modal="false"
            :show-close="false"
            :visible.sync="dialogVisible"
            width="300px">
            <div class="text-center">
                <el-radio-group v-model="authType">
                    <el-radio :label="0" class="radio-as-checkbox">主账号</el-radio>
                    <el-radio :label="1" class="radio-as-checkbox" style="margin-left: 14px;">子账号</el-radio>
                </el-radio-group>
            </div>
            <div slot="footer" class="text-center">
                <afp-button size="default" @click="handleClose">取 消</afp-button>
                <afp-button size="default" type="primary" :plain="false" @click="handleOk">确 定</afp-button>
            </div>
        </el-dialog>
        <el-dialog
            title="巨量授权"
            :close-on-press-escape="false"
            :close-on-click-modal="false"
            :show-close="false"
            :visible.sync="tipDialogVisible"
            custom-class="buyInAuthTipDialog"
            width="480px">
            <div class="buyInAuthTipDialog-title">{{ tipTitle }}</div>
            <div class="buyInAuthTipDialog-list">
                <div class="buyInAuthTipDialog-item" v-for="(item, index) in tipList" :key="index">
                    <span class="buyInAuthTipDialog-index">{{ index + 1 }}.</span>
                    <span class="buyInAuthTipDialog-text">{{ item }}</span>
                </div>
            </div>
            <div slot="footer" class="text-center">
                <afp-button class="buyInAuthTipDialog-btn buyInAuthTipDialog-btn--cancel" size="default" @click="handleTipCancel">取消</afp-button>
                <afp-button class="buyInAuthTipDialog-btn buyInAuthTipDialog-btn--ok" size="default" type="primary" :plain="false" @click="handleTipOk">点我授权</afp-button>
            </div>
        </el-dialog>
    </div>
</template>
<script>
import buyIn from "@/mixins/buyIn";

export default {
    components: {},
    mixins: [buyIn],
    props: {},
    data() {
        return {
            dialogVisible: false,
            authType: null,
            secUid: '',
            tipDialogVisible: false,
            pendingAuthType: 0,
            pendingSecUid: '',
        };
    },
    computed: {
        tipTitle() {
            return this.pendingAuthType === 1 ? '子账号巨量授权注意以下事项：' : '主账号巨量授权注意以下事项：'
        },
        tipList() {
            return [
                '自己的账号开通了巨量百应才能授权',
                '“扫码账号”必须和“录制账号”是同一个抖音号',
                '抖音账号子账号无法授权',
                '授权二维码1分钟失效，所以先准备好再扫码',
                '授权过程中，请不要关闭二维码弹窗',
                '授权有问题的话，请关闭客户端再授权',
            ]
        },
        tipStorageKey() {
            const id = this.$store?.getters?.getUserInfo?.id
            const uid = id === undefined || id === null || id === '' ? 'anonymous' : String(id)
            return `buyIn_auth_tip_shown_${uid}`
        }
    },
    watch: {},
    methods: {
        open(secUid) {
            this.dialogVisible = true;
            this.secUid = secUid
            this.authType = null
        },
        handleClose() {
            this.dialogVisible = false;
            this.secUid = ''
        },
        async handleOk() {
            if (this.authType === null) {
                this.$message.error('请选择账号类型')
                return
            }
            const secUid = this.secUid
            const authType = this.authType
            this.dialogVisible = false
            this.secUid = ''
            const hasShown = localStorage.getItem(this.tipStorageKey) === '1'
            if (!hasShown) {
                this.pendingAuthType = authType
                this.pendingSecUid = secUid
                this.tipDialogVisible = true
                return
            }
            await this.buyInAuthForLive(secUid, authType)
        },
        handleTipCancel() {
            this.tipDialogVisible = false
            this.pendingAuthType = 0
            this.pendingSecUid = ''
        },
        async handleTipOk() {
            const secUid = this.pendingSecUid
            const authType = this.pendingAuthType
            this.tipDialogVisible = false
            this.pendingAuthType = 0
            this.pendingSecUid = ''
            localStorage.setItem(this.tipStorageKey, '1')
            await this.buyInAuthForLive(secUid, authType)
        }
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() {
    }, //生命周期 - 创建之前
    beforeMount() {
    }, //生命周期 - 挂载之前
    beforeUpdate() {
    }, //生命周期 - 更新之前
    updated() {
    }, //生命周期 - 更新之后
    beforeDestroy() {
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.buyInAccountType {
    .dialog-footer {
        text-align: center;
    }
}
::v-deep(.buyInAuthTipDialog) {
    border-radius: 12px;
    overflow: hidden;
}

::v-deep(.buyInAuthTipDialog .el-dialog__header) {
    background: #F6FAFF;
    padding: 14px 20px;
}

::v-deep(.buyInAuthTipDialog .el-dialog__title) {
    font-size: 16px;
    font-weight: 600;
    color: #1F1F1F;
}

::v-deep(.buyInAuthTipDialog .el-dialog__body) {
    padding: 18px 44px 0;
}

::v-deep(.buyInAuthTipDialog .el-dialog__footer) {
    padding: 18px 0 22px;
}

.buyInAuthTipDialog-title {
    text-align: center;
    color: #3B5CFF;
    font-size: 14px;
    font-weight: 600;
    padding: 0 0 16px;
}

.buyInAuthTipDialog-list {
    padding: 0 0 8px;
}

.buyInAuthTipDialog-item {
    display: flex;
    font-size: 13px;
    color: #303133;
    line-height: 22px;
    padding: 2px 0;
}

.buyInAuthTipDialog-index {
    width: 20px;
    flex: 0 0 auto;
    color: #303133;
}

.buyInAuthTipDialog-text {
    flex: 1;
}

.buyInAuthTipDialog-btn {
    width: 128px;
    height: 36px;
    border-radius: 18px;
}

::v-deep(.buyInAuthTipDialog-btn--cancel .el-button) {
    width: 128px;
    height: 36px;
    border-radius: 18px;
    border: 1px solid #C8CDD8;
    color: #303133;
    background: #fff;
}

::v-deep(.buyInAuthTipDialog-btn--ok .el-button) {
    width: 128px;
    height: 36px;
    border-radius: 18px;
    background: #3B5CFF;
    border-color: #3B5CFF;
    color: #fff;
}
</style>
