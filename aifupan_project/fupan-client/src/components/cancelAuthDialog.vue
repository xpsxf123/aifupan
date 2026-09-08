<template>
    <div class="cancelAuthDialog">
        <el-dialog
            title="取消授权"
            :close-on-press-escape="false"
            :close-on-click-modal="false"
            :show-close="false"
            :visible.sync="dialogVisible"
            width="360px"
            :custom-class="['cancelAuth-box']"
        >
            <div v-loading="statusLoading">
                <div v-if="!statusLoading && noPlatformCanCancel" class="no-auth-tip">
                    当前账号没有已授权的平台
                </div>
                <el-radio-group v-else v-model="selectedPlatform" class="radio-group-left">
                    <el-radio
                        v-for="p in platformList"
                        :key="p.key"
                        :label="p.key"
                        :disabled="!p.canCancel"
                        class="radio-as-checkbox"
                    >
                        {{ p.label }}
                        <span v-if="p.canCancel" class="auth-tag authed">已授权</span>
                        <span v-else class="auth-tag unauthed">未授权</span>
                    </el-radio>
                </el-radio-group>
            </div>
            <div slot="footer" class="text-center">
                <afp-button size="default" @click="handleClose">取 消</afp-button>
                <afp-button size="default" type="primary" :plain="false" :disabled="noPlatformCanCancel || !selectedCanCancel" @click="handleOk">确 定</afp-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import { getLiveRoomAuthStatus } from '@/utils/liveRoomAuthStatus'

/** 平台能力配置：取消授权 API + 确认文案 */
const CANCEL_CAPABILITIES = {
    juliang: {
        label: '巨量百应',
        confirmText: '取消授权后将停止该主播的巨量百应授权状态与相关数据采集，是否继续？',
        confirmTitle: '取消巨量百应授权',
        /** @param {Object} ctx 组件实例 this */
        async cancel(ctx) {
            const res = await ctx.$httpClient.buyIn.cancelAuthorizeJuliang({ secUid: ctx.currentSecUid })
            if (res?.code === 0) {
                ctx.$message.success('巨量百应授权已解约')
                return true
            }
            ctx.$message.error(res?.msg || '取消巨量百应授权失败')
            return false
        }
    },
    life: {
        label: '来客',
        confirmText: '取消授权后将停止该主播的来客授权状态与相关数据采集，是否继续？',
        confirmTitle: '取消来客授权',
        /** @param {Object} ctx 组件实例 this */
        async cancel(ctx) {
            const res = await ctx.$httpClient.life.cancelAuthorizeLife({ secUid: ctx.currentSecUid })
            if (res?.code === 0) {
                ctx.$message.success('来客授权已解约')
                return true
            }
            ctx.$message.error(res?.msg || '取消来客授权失败')
            return false
        }
    },
    qianchuan: {
        label: '千川',
        confirmText: '取消授权后将停止该主播的千川授权状态与相关数据采集，是否继续？',
        confirmTitle: '取消千川授权',
        /** @param {Object} ctx 组件实例 this */
        async cancel(ctx) {
            const res = await ctx.$httpClient.qianchuan.cancelAuthorizeQianchuan({ secUid: ctx.currentSecUid })
            if (res?.code === 0) {
                ctx.$message.success('千川授权已解约')
                return true
            }
            ctx.$message.error(res?.msg || '取消千川授权失败')
            return false
        }
    }
}

export default {
    name: 'CancelAuthDialog',
    data() {
        return {
            dialogVisible: false,
            selectedPlatform: '',
            currentSecUid: '',
            /** @type {Object.<string, number>} 各平台授权状态 */
            authStatusMap: {},
            statusLoading: false
        }
    },
    computed: {
        platformList() {
            return Object.keys(CANCEL_CAPABILITIES).map(key => ({
                key,
                label: CANCEL_CAPABILITIES[key].label,
                canCancel: Number(this.authStatusMap[key]) === 1
            }))
        },
        noPlatformCanCancel() {
            return this.platformList.length > 0 && this.platformList.every(p => !p.canCancel)
        },
        selectedCanCancel() {
            const p = this.platformList.find(p => p.key === this.selectedPlatform)
            return p?.canCancel ?? false
        }
    },
    methods: {
        async open(secUid) {
            this.currentSecUid = secUid || ''
            this.selectedPlatform = ''
            this.authStatusMap = {}
            this.statusLoading = false
            this.dialogVisible = true

            if (!secUid) return
            await this.fetchAuthStatus()
            // 默认选中第一个可取消的平台
            this.$nextTick(() => {
                const first = this.platformList.find(p => p.canCancel)
                if (first) this.selectedPlatform = first.key
            })
        },

        async fetchAuthStatus() {
            if (!this.currentSecUid) return
            this.statusLoading = true
            try {
                const res = await getLiveRoomAuthStatus(this.currentSecUid)
                if (res?.authStatus) {
                    this.authStatusMap = {
                        juliang: res.authStatus.juliangAuthStatus,
                        life: res.authStatus.lifeAuthStatus,
                        qianchuan: res.authStatus.qianchuanAuthStatus
                    }
                }
            } catch (e) {
                // 查询失败不阻断流程
            }
            this.statusLoading = false
        },

        handleClose() {
            this.dialogVisible = false
            this.currentSecUid = ''
            this.selectedPlatform = ''
            this.authStatusMap = {}
        },

        async handleOk() {
            const secUid = this.currentSecUid
            const platformKey = this.selectedPlatform
            const cap = CANCEL_CAPABILITIES[platformKey]
            if (!cap || !secUid || !this.selectedCanCancel) return

            // 二次确认
            try {
                await this.$confirm(cap.confirmText, cap.confirmTitle, {
                    confirmButtonText: '确定取消',
                    cancelButtonText: '暂不取消',
                    type: 'warning'
                })
            } catch (e) {
                return // 用户取消
            }

            this.dialogVisible = false
            const success = await cap.cancel(this)
            this.currentSecUid = ''
            this.selectedPlatform = ''
            this.authStatusMap = {}

            if (success) {
                this.$emit('canceled', { platform: platformKey, secUid })
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.radio-group-left {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
}
.radio-as-checkbox {
    width: calc(50% - 7px);
    flex-shrink: 0;
    margin-right: 0 !important;
    margin-top: 10px;
}
::v-deep(.cancelAuth-box .el-dialog__body){
    padding-top: 0;
    padding-bottom: 10px;
}

::v-deep(.radio-group-left .el-radio) {
    display: block;
    margin-right: 0 !important;
    margin-left: 0 !important;
}

::v-deep(.radio-group-left .el-radio:nth-last-child(1):nth-child(odd)) {
    margin-bottom: 0;
}

.auth-tag {
    margin-left: 6px;
    font-size: 12px;
}

.auth-tag.authed {
    color: #61B593;
}

.auth-tag.unauthed {
    color: #C0C4CC;
}

.no-auth-tip {
    padding: 12px 0;
    font-size: 14px;
    color: #909399;
}

::v-deep(.el-radio.is-disabled .el-radio__label) {
    color: #C0C4CC;
}
</style>
