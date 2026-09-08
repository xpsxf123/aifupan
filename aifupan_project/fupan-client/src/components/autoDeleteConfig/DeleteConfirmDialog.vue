<!--
@description 自动删除二次确认弹窗：勾选删除内容时弹出，需输入“确认删除”四个字才能确认。
注意：弹窗仅负责确认交互与输入校验，确认/取消结果通过事件交由父组件处理。
-->
<template>
    <el-dialog
        :visible.sync="dialogVisible"
        :show-close="false"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        width="480px"
        custom-class="auto-delete-confirm-dialog"
        append-to-body>
        <div class="confirm-content">
            <div class="confirm-title">
                <span class="confirm-icon">
                    <i class="icon font_family icon-shanchu"></i>
                </span>
                <span class="confirm-title-text">确认删除视频？</span>
            </div>
            <p class="confirm-desc">删除后，视频无法恢复</p>
            <div class="confirm-label">
                请输入<span class="confirm-highlight">“确认删除”</span>四个字，以完成配置
            </div>
            <el-input
                v-model="inputValue"
                class="confirm-input"
                placeholder="请输入“确认删除”"
                autocomplete="off"
                spellcheck="false" />
        </div>
        <div slot="footer" class="confirm-footer">
            <afp-button size="default" @click="handleCancel">取消</afp-button>
            <afp-button size="default" type="primary" :plain="false" :disabled="!isConfirmed" @click="handleConfirm">
                确定
            </afp-button>
        </div>
    </el-dialog>
</template>

<script>
/**
 * @description 自动删除二次确认弹窗脚本：控制弹窗显隐、输入校验与确认/取消事件分发。
 */
export default {
    name: 'DeleteConfirmDialog',
    props: {
        /**
         * @description 是否显示弹窗。
         * @type {boolean}
         */
        visible: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            inputValue: ''
        }
    },
    computed: {
        /**
         * @description 双向绑定弹窗显隐，通过 update:visible 同步回父组件。
         * @returns {boolean}
         */
        dialogVisible: {
            get() {
                return this.visible
            },
            set(val) {
                this.$emit('update:visible', val)
            }
        },
        /**
         * @description 输入内容是否为“确认删除”，用于控制确定按钮可用态。
         * @returns {boolean}
         */
        isConfirmed() {
            return this.inputValue.trim() === '确认删除'
        }
    },
    watch: {
        visible(val) {
            // 每次打开弹窗时清空输入，避免复用上次内容
            if (val) {
                this.inputValue = ''
            }
        }
    },
    methods: {
        /**
         * @description 取消操作：关闭弹窗并通知父组件。
         * @returns {void}
         */
        handleCancel() {
            this.dialogVisible = false
            this.$emit('cancel')
        },
        /**
         * @description 确认操作：输入正确后关闭弹窗并通知父组件。
         * @returns {void}
         */
        handleConfirm() {
            if (!this.isConfirmed) return
            this.dialogVisible = false
            this.$emit('confirm')
        }
    }
}
</script>

<style lang="scss" scoped>
.confirm-content {
    .confirm-title {
        display: flex;
        align-items: center;
        margin-bottom: 12px;

        .confirm-icon {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 40px;
            height: 40px;
            background: #FEE4E2;
            border-radius: 50%;
            margin-right: 12px;

            .icon {
                color: #F04438;
                font-size: 20px;
            }
        }

        .confirm-title-text {
            font-size: 16px;
            font-weight: 600;
            color: #1d2129;
            line-height: 1.4;
        }
    }

    .confirm-desc {
        font-size: 14px;
        color: #4e5a6b;
        line-height: 1.6;
        margin: 0 0 16px 52px;
    }

    .confirm-label {
        font-size: 14px;
        color: #4e5a6b;
        margin-bottom: 8px;

        .confirm-highlight {
            color: #F04438;
            font-weight: 500;
        }
    }

    .confirm-input {
        width: 416px;

        ::v-deep(.el-input__inner) {
            height: 34px;
            line-height: 34px;
            background: #FFFFFF;
            border-radius: 4px;
            border: 1px solid #DCDCDC;
            padding: 0 12px;
        }
    }
}

.confirm-footer {
    text-align: right;
}

::v-deep(.auto-delete-confirm-dialog) {
    border-radius: 16px;

    .el-dialog__header {
        display: none;
    }

    .el-dialog__body {
        padding: 32px 32px 0;
    }

    .el-dialog__footer {
        padding: 20px 32px 28px;
    }
}
</style>
