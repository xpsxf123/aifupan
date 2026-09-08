<template>
    <div class="liveRoomAuthorizeDialog">
        <!-- ========== 弹窗模式 ========== -->
        <template v-if="mode === 'dialog'">
            <el-dialog
                :title="'选择授权方式'"
                :close-on-press-escape="false"
                :close-on-click-modal="false"
                :show-close="false"
                :visible.sync="dialogVisible"
                width="360px"
            >
                <div v-loading="statusLoading" class="text-center">
                    <div v-if="!statusLoading && allAuthed" class="no-auth-tip">
                        当前账号已完成所有授权
                    </div>
                    <el-radio-group v-else v-model="dialogPlatform" class="radio-group-left">
                        <el-radio
                            v-for="p in enabledPlatforms"
                            :key="p.key"
                            :label="p.key"
                            :disabled="p.authed"
                            class="radio-as-checkbox"
                        >
                            {{ p.label }}
                            <span v-if="p.authed" class="auth-tag">已授权</span>
                        </el-radio>
                    </el-radio-group>
                </div>
                <div slot="footer" class="text-center">
                    <afp-button size="default" @click="handleDialogClose">取 消</afp-button>
                    <afp-button size="default" type="primary" :plain="false" :disabled="allAuthed" @click="handleDialogOk">确 定</afp-button>
                </div>
            </el-dialog>
        </template>

        <!-- ========== 按钮模式 ========== -->
        <div v-else-if="mode === 'button'" class="auth-buttons" :class="`auth-buttons--${layout}`">
            <template v-for="p in enabledPlatforms">
                <span v-if="p.authed" :key="p.key" class="auth-done-tag">{{ p.label }}:已授权</span>
                <afp-button v-else size="default" type="text" @click="handleButtonClick(p.key)">
                    {{ p.label }}授权
                </afp-button>
            </template>
        </div>

        <!-- LifeAuthDialog 始终渲染，弹窗和按钮模式均需使用 -->
        <LifeAuthDialog ref="lifeAuthDialog" @authorized="handleLifeAuthorized" />
    </div>
</template>

<script>
import buyIn from '@/mixins/buyIn'
import LifeAuthDialog from '@/components/lifeAuthDialog.vue'
import { getLiveRoomAuthStatus } from '@/utils/liveRoomAuthStatus'

/** 平台能力配置：定义各平台的授权动作 */
const PLATFORM_CAPABILITIES = {
    juliang: {
        label: '巨量百应',
        /** @param {Object} ctx 组件实例 this @param {string} secUid */
        authorize(ctx, secUid) { ctx.buyInFront(secUid) }
    },
    life: {
        label: '来客',
        /** @param {Object} ctx 组件实例 this @param {string} secUid */
        authorize(ctx, secUid) { ctx.$refs.lifeAuthDialog?.open?.(secUid) }
    },
    qianchuan: {
        label: '千川',
        /** @param {Object} ctx 组件实例 this @param {string} secUid */
        authorize(ctx, secUid) { ctx.$httpClient?.qianchuan?.authorizeQianchuan?.({ secUid }) }
    }
}

export default {
    name: 'LiveRoomAuthorizeDialog',
    components: { LifeAuthDialog },
    mixins: [buyIn],
    props: {
        /** 模式：dialog=弹窗选择, button=内联按钮 */
        mode: { type: String, default: 'dialog' },
        /** 按钮模式下传入的 secUid，会自动触发查询（替代手动调用 query） */
        secUid: { type: String, default: '' },
        /** 按钮模式下的排列方式 */
        layout: { type: String, default: 'horizontal' },
        /** 需要展示的授权平台列表 */
        platforms: { type: Array, default: () => ['juliang', 'life'] }
    },
    data() {
        return {
            dialogVisible: false,
            dialogPlatform: 'juliang',
            currentSecUid: '',
            /** @type {Object.<string, number>} 各平台授权状态 */
            authStatusMap: {},
            statusLoading: false
        }
    },
    computed: {
        /** 启用的平台列表（含授权状态） */
        enabledPlatforms() {
            return this.platforms
                .filter(key => PLATFORM_CAPABILITIES[key])
                .map(key => ({
                    key,
                    label: PLATFORM_CAPABILITIES[key].label,
                    authed: Number(this.authStatusMap[key]) === 1
                }))
        },
        /** 是否所有平台都已授权 */
        allAuthed() {
            return this.enabledPlatforms.length > 0 && this.enabledPlatforms.every(p => p.authed)
        }
    },
    watch: {
        /** 按钮模式下，secUid prop 变化时自动查询授权状态 */
        secUid: {
            immediate: true,
            handler(val) {
                if (val && this.mode === 'button') {
                    this.currentSecUid = val
                    this.fetchAuthStatus()
                }
            }
        }
    },
    methods: {
        // ==================== 公开方法 ====================

        /** 弹窗模式：打开授权选择弹窗 */
        async open(secUid) {
            this.currentSecUid = secUid || ''
            this.dialogPlatform = 'juliang'
            this.dialogVisible = true
            if (secUid) await this.fetchAuthStatus()
        },

        /** 按钮模式：传入 secUid 并查询授权状态 */
        async query(secUid) {
            this.currentSecUid = secUid || ''
            if (secUid) await this.fetchAuthStatus()
        },

        // ==================== 授权状态查询 ====================

        /** 调用云端接口查询授权状态 */
        async fetchAuthStatus() {
            if (!this.currentSecUid) return
            this.statusLoading = true
            try {
                const res = await getLiveRoomAuthStatus(this.currentSecUid)
                if (res?.authStatus) {
                    // 映射 authStatus 中的字段到组件内部 key
                    const map = {}
                    if (this.platforms.includes('juliang')) {
                        map.juliang = res.authStatus.juliangAuthStatus
                    }
                    if (this.platforms.includes('life')) {
                        map.life = res.authStatus.lifeAuthStatus
                    }
                    if (this.platforms.includes('qianchuan')) {
                        map.qianchuan = res.authStatus.qianchuanAuthStatus
                    }
                    this.authStatusMap = map

                    // 弹窗模式：自动切换选中未授权平台
                    if (this.mode === 'dialog') {
                        const cur = this.enabledPlatforms.find(p => p.key === this.dialogPlatform)
                        if (cur?.authed) {
                            const unauthed = this.enabledPlatforms.find(p => !p.authed)
                            if (unauthed) this.dialogPlatform = unauthed.key
                        }
                    }
                }
            } catch (e) {
                // 查询失败不阻断流程
            }
            this.statusLoading = false
        },

        // ==================== 弹窗模式 ====================

        handleDialogClose() {
            this.dialogVisible = false
            this.currentSecUid = ''
            this.dialogPlatform = 'juliang'
            this.authStatusMap = {}
        },

        async handleDialogOk() {
            if (this.allAuthed) return
            const secUid = this.currentSecUid
            const platformKey = this.dialogPlatform
            if (!secUid) {
                this.dialogVisible = false
                this.currentSecUid = ''
                this.dialogPlatform = 'juliang'
                this.authStatusMap = {}
                return
            }

            // 先关闭弹窗避免视觉残留，但保留 currentSecUid / authStatusMap
            // 防止 doAuthorize 内部链路引用 this.currentSecUid 时读到空值
            this.dialogVisible = false
            this.doAuthorize(platformKey, secUid)

            // 授权执行完毕后再清理状态
            this.currentSecUid = ''
            this.dialogPlatform = 'juliang'
            this.authStatusMap = {}
        },

        // ==================== 按钮模式 ====================

        handleButtonClick(platformKey) {
            this.doAuthorize(platformKey, this.currentSecUid)
        },

        // ==================== 通用授权执行 ====================

        /** 执行指定平台的授权动作 */
        doAuthorize(platformKey, secUid) {
            const cap = PLATFORM_CAPABILITIES[platformKey]
            if (!cap || !secUid) return
            cap.authorize(this, secUid)
        },

        // ==================== 事件转发 ====================

        handleLifeAuthorized(payload) {
            this.$emit('authorized', { ...(payload || {}), platform: 'life' })
        }
    }
}
</script>

<style lang="scss" scoped>
/* ===== 弹窗模式 ===== */
.radio-group-left {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
}

.radio-as-checkbox {
    width: calc(50% - 7px);
    flex-shrink: 0;
}

.auth-tag {
    margin-left: 6px;
    font-size: 12px;
    color: #61B593;
}

.no-auth-tip {
    padding: 12px 0;
    font-size: 14px;
    color: #909399;
}

::v-deep(.radio-group-left .el-radio) {
    display: block;
    margin-right: 0 !important;
    margin-left: 0 !important;
}

::v-deep(.radio-group-left .el-radio:nth-last-child(1):nth-child(odd)) {
    margin-bottom: 0;
}

::v-deep(.el-radio.is-disabled .el-radio__label) {
    color: #C0C4CC;
}

/* ===== 按钮模式 ===== */
.auth-buttons {
    display: flex;
    align-items: center;
    gap: 8px;
}

.auth-buttons--vertical {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
}

.auth-buttons--horizontal {
    flex-direction: row;
    flex-wrap: wrap;
}

.auth-done-tag {
    display: inline-flex;
    align-items: center;
    height: 32px;
    padding: 0 12px;
    font-size: 14px;
    color: #61B593;
    border-radius: 4px;
    white-space: nowrap;
}
</style>
