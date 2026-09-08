<template>
    <el-dialog
        :visible.sync="dialogVisible"
        class="ai-diagnosis-status-dialog"
        width="394px"
        :append-to-body="true"
        :close-on-click-modal="false"
        :show-close="false"
        top="20vh">
        <button class="status-close" type="button" @click="handleClose">
            <i class="el-icon-close"></i>
        </button>
        <div
            class="status-panel"
            :class="`is-${status}`"
            :style="{ backgroundImage: `url(${currentConfig.bg})` }">
            <div class="status-content">
                <template v-if="isSuccess">
                    <div class="status-text">AI数据诊断报告</div>
                    <div class="status-subtext">前往AI工作台查看详情</div>
                    <afp-button class="view-button" @click="nextStep">
                        前往AI工作台
                    </afp-button>
                </template>
                <template v-else>
                    <div class="status-text">爱复盘 <span style="color: #0E79F7;font-size: 16px">直播运营智能体</span></div>
                    <div class="status-subtext" style="font-size: 18px">3分钟快速生成 <span style="color: #0E79F7;">数据诊断报告</span></div>
                    <afp-button class="view-button" @click="nextStep">
                        前往AI工作台
                    </afp-button>
                </template>
            </div>
        </div>
    </el-dialog>
</template>

<script>
import createBg from '@/assets/imgs/2_6_0/crate.png'
import viewBg from '@/assets/imgs/2_6_0/view.png'

export default {
    name: 'DiagnosisPop',
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        status: {
            type: [String,Number],
            default: ''
        }
    },
    computed: {
        userId() {
            return this.$store?.state?.userInfo?.id
        },
        dialogVisible: {
            get() {
                return this.visible
            },
            set(value) {
                this.$emit('update:visible', value)
            }
        },
        isSuccess() {
            return this.status == 2
        },
        currentConfig() {
            if (this.isSuccess) {
                return {
                    bg: viewBg
                }
            }
            return {
                bg: createBg
            }
        }
    },
    watch: {
        visible: {
            immediate: true,
            handler(val) {
                if (!val) return
                const userId = this.userId
                if (!userId) return
                if (this.hasShown(userId)) {
                    this.$emit('update:visible', false)
                    this.$emit('close')
                    return
                }
                this.markShown(userId)
            }
        }
    },
    methods: {
        getShownKey() {
            return 'aiDiagnosisPopShown'
        },
        hasShown(userId) {
            const shown = localStorage.getItem(this.getShownKey())?.split(',') || []
            return shown.includes(String(userId))
        },
        markShown(userId) {
            const shown = localStorage.getItem(this.getShownKey())?.split(',') || []
            const id = String(userId)
            if (shown.includes(id)) return
            shown.push(id)
            localStorage.setItem(this.getShownKey(), shown.join(','))
        },
        handleClose() {
            if (this.userId) {
                this.markShown(this.userId)
            }
            this.$emit('update:visible', false)
            this.$emit('close')
        },
        nextStep(){
            if (this.userId) {
                this.markShown(this.userId)
            }
            this.$emit('nextStep')
        }
    }
}
</script>

<style lang="scss" scoped>
.ai-diagnosis-status-dialog {
    ::v-deep .el-dialog {
        background: transparent;
        box-shadow: none;
    }

    ::v-deep .el-dialog__header {
        display: none;
    }

    ::v-deep .el-dialog__body {
        position: relative;
        padding: 0;
        background: transparent;
    }
}

.status-close {
    position: absolute;
    right: 35px;
    top: -42px;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    padding: 0;
    border: 0;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.56);
    color: #fff;
    font-size: 16px;
    cursor: pointer;
}

.status-panel {
    width: 394px;
    height: 309px;
    margin-top: 50px;
    overflow: hidden;
    background-repeat: no-repeat;
    background-size: 394px 309px;
}

.status-content {
    padding-top: 158px;
    text-align: center;
}

.status-text,
.status-subtext {
    color: #1f2d3d;
    font-size: 14px;
    line-height: 20px;
}

.status-subtext {
    margin-top: 18px;
}

.view-button {
    min-width: 125px;
    height: 32px;
    margin-top: 18px;
    padding: 0 14px;
    border-color: #11bb8d;
    border-radius: 16px;
    color: #11bb8d;
}

.view-button:hover,
.view-button:focus {
    border-color: #11bb8d;
    background: rgba(17, 187, 141, 0.08);
    color: #11bb8d;
}
</style>
