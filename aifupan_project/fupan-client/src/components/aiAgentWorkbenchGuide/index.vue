<!--
@description AI智能体工作台引导弹窗：用于在分析详情页首次进入时提示用户体验 AI 智能体工作台。
-->
<template>
    <el-dialog
        :visible.sync="dialogVisible"
        class="ai-agent-workbench-guide"
        width="435px"
        :append-to-body="true"
        :close-on-click-modal="false"
        :show-close="false"
        top="12vh">
        <template #title>
            <button class="guide-close" type="button" @click="handleClose">
                <img :src="closeIcon" alt="关闭">
            </button>
        </template>
        <div class="guide-panel" :style="{ backgroundImage: `url(${guideBg})` }">
            <div class="guide-card">
                <div class="guide-card__title pd-b10">
                    爱复盘*<span>AI智能体工作台</span>
                </div>
                <div class="guide-card__desc">
                    3分钟快速生成<span>诊断报告</span>
                </div>
                <div class="guide-card__sub pd-b10">比<span class="text-colorTheme">Claude/Codex</span>更懂直播</div>
                <afp-button class="guide-card__button" @click="handleExperience">
                    立即体验
                </afp-button>
            </div>
        </div>
    </el-dialog>
</template>

<script>
/**
 * @description AI 智能体工作台引导弹窗。
 * 用于在分析详情页进行轻量提示，引导用户进入 AI 智能体工作台。
 */
import guideBg from '@/assets/imgs/2_6_2/AI_zntgzt.png'
import closeIcon from '@/assets/imgs/2_6_2/cloase.png'

export default {
    name: 'AiAgentWorkbenchGuide',
    props: {
        visible: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            guideBg,
            closeIcon
        }
    },
    computed: {
        dialogVisible: {
            get() {
                return this.visible
            },
            set(value) {
                this.$emit('update:visible', value)
            }
        }
    },
    methods: {
        /**
         * @description 关闭引导弹窗。
         * @returns {void}
         */
        handleClose() {
            this.$emit('close')
            this.$emit('update:visible', false)
        },
        /**
         * @description 点击立即体验。
         * @returns {void}
         */
        handleExperience() {
            this.$emit('experience')
            this.$emit('update:visible', false)
        }
    }
}
</script>

<style scoped lang="scss">
.ai-agent-workbench-guide {
    ::v-deep .el-dialog {
        background: transparent;
        box-shadow: none;
    }

    ::v-deep .el-dialog__header {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        padding-top: 30px !important;
    }

    ::v-deep .el-dialog__body {
        position: relative;
        padding: 0;
        background: transparent;
    }
}

.guide-close {
    position: absolute;
    top: 0px;
    right: -6px;
    z-index: 4;
    width: 44px;
    height: 44px;
    padding: 0;
    border: 0;
    background: transparent;
    cursor: pointer;

    img {
        width: 28px;
        height: 28px;
        object-fit: contain;
    }
}

.guide-panel {
    position: relative;
    width: 430px;
    height: 366px;
    padding-top: 120px;
    overflow: hidden;
    // background-repeat: no-repeat;
    background-position: center;
    background-size:    cover;
    box-sizing: border-box;
}

.guide-title {
    position: absolute;
    top: 38px;
    left: 62px;
    color: #1b1d29;
    font-size: 30px;
    line-height: 42px;
    font-weight: 700;
}

.guide-title__highlight {
    color: #287dff;
}

.guide-card {
    position: relative;
    z-index: 2;
    width: 100%;
    margin: 0 auto;
    padding: 44px 40px 20px;
    border-radius: 28px;
    // background: #ffffff;
    text-align: center;
    box-sizing: border-box;
}

.guide-card__title {
    color: #21242c;
    font-size: 24px;
    line-height: 34px;
    font-weight: 600;

    span {
        color: #5868ff;
    }
}

.guide-card__desc {
    margin-top: 18px;
    color: #3f4452;
    font-size: 16px;
    line-height: 24px;
    font-weight: 500;

    span {
        color: #5868ff;
        font-weight: 700;
    }
}

.guide-card__sub {
    margin-top: 10px;
    color: #6d7280;
    font-size: 14px;
    line-height: 22px;
}

.guide-card__button {
    min-width: 132px;
    height: 40px;
    margin-top: 24px;
    padding: 0 28px;
    border-color: #5868ff;
    border-radius: 22px;
    color: #5868ff;
    font-size: 14px;
    font-weight: 500;
}

.guide-card__button:hover,
.guide-card__button:focus {
    border-color: #5868ff;
    background: rgba(88, 104, 255, 0.08);
    color: #5868ff;
}
</style>
