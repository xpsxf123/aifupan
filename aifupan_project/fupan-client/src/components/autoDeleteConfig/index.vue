<!--
@description 自动删除配置表单项：统一承载「自动删除时间」与「删除内容」选择逻辑，供基础设置与主播录制配置复用。
字段模型（与后端文档一致）：
- autoDeleteTime：String，-1 不删除 / 0 马上删除 / 1、3、7、30、90 天后删除
- deleteContent：String，ts=删除源视频 / mp4=删除mp4成品 / all=全部删除
交互约束：
1) 删除内容不再提供「跟随全局」选项，选择自动删除时间后由用户明确选择删除内容；
2) 勾选具体删除内容时需经过二次确认弹窗；
3) 删除时间与删除内容选项优先使用父组件传入，未传入时从数据字典接口自动获取。
-->
<template>
    <div class="auto-delete-config">
        <div class="auto-delete-form" :class="isInlineLayout ? 'auto-delete-form-inline' : 'auto-delete-form-block'">
            <el-form-item
                :label="label"
                :prop="prop"
                :label-width="labelWidth"
                :class="formItemClass">
                <el-radio-group
                    :value="localValue.autoDeleteTime"
                    class="auto-delete-time-group"
                    @input="onTimeChange">
                    <el-radio
                        v-for="item in resolvedTimeOptions"
                        :key="item.value"
                        :label="String(item.value)"
                        :style="itemStyle">
                        {{ item.label }}
                    </el-radio>
                </el-radio-group>
            </el-form-item>
            <el-form-item
                v-if="showDeleteContent"
                class="auto-delete-content-item"
                :label-width="isInlineLayout ? '0px' : labelWidth"
                :class="formItemClass">
                <template #label v-if="!isInlineLayout">
                    <span v-if="contentRequired" class="required-mark">*</span>
                    <span>{{ contentLabel }}</span>
                </template>
                <span v-if="isInlineLayout" class="content-inline-label">
                    <span v-if="contentRequired" class="required-mark">*</span>
                    <span>{{ contentLabel }}</span>
                </span>
                <el-radio-group
                    :value="localValue.deleteContent"
                    class="auto-delete-content-group"
                    @input="onContentChange">
                    <el-radio
                        v-for="item in resolvedContentOptions"
                        :key="item.value"
                        :label="String(item.value)"
                        :style="itemStyle">
                        {{ item.label }}
                    </el-radio>
                </el-radio-group>
            </el-form-item>
        </div>
        <div v-if="tips" class="tips">{{ tips }}</div>

        <DeleteConfirmDialog
            :visible.sync="confirmDialogVisible"
            @confirm="onDeleteConfirm"
            @cancel="onDeleteCancel" />
    </div>
</template>

<script>
import DeleteConfirmDialog from './DeleteConfirmDialog.vue'

/**
 * @description 自动删除配置表单项组件：自动删除时间单选 + 删除内容单选。
 */
export default {
    name: 'AutoDeleteConfig',
    components: {
        DeleteConfirmDialog
    },
    props: {
        value: {
            type: Object,
            default: () => ({})
        },
        label: {
            type: String,
            default: '自动删除'
        },
        prop: {
            type: String,
            default: ''
        },
        labelWidth: {
            type: String,
            default: '100px'
        },
        formItemClass: {
            type: [String, Array, Object],
            default: ''
        },
        /**
         * @description 自动删除时间选项（可选，缺省从字典 auto_delete_time 获取）。
         * @type {Array<{label: string, value: string|number}>}
         */
        timeOptions: {
            type: Array,
            default: () => []
        },
        /**
         * @description 自动删除时间字典编码：系统设置传 global_auto_delete_time（全局），
         *              主播/基础设置使用默认 auto_delete_time（主播）。
         * @type {string}
         */
        timeDictCode: {
            type: String,
            default: 'auto_delete_time'
        },
        /**
         * @description 删除内容选项（可选，缺省从字典 delete_content 获取）。
         * @type {Array<{label: string, value: string}>}
         */
        contentOptions: {
            type: Array,
            default: () => []
        },
        contentRequired: {
            type: Boolean,
            default: false
        },
        contentLayout: {
            type: String,
            default: 'inline'
        },
        contentLabel: {
            type: String,
            default: '删除内容'
        },
        tips: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            confirmDialogVisible: false,
            pendingContentValue: '',
            innerTimeOptions: [],
            innerContentOptions: []
        }
    },
    computed: {
        /**
         * @description 归一化自动删除配置对象，避免外部未传完整字段时渲染异常。
         * @returns {{autoDeleteTime: string, deleteContent: string}}
         */
        localValue() {
            return {
                autoDeleteTime: String(this.value?.autoDeleteTime ?? '-1'),
                deleteContent: String(this.value?.deleteContent ?? '')
            }
        },
        /**
         * @description 仅在选择了「删除时间」后展示删除内容区域（不删除 -1 时不展示）。
         * @returns {boolean}
         */
        showDeleteContent() {
            return String(this.localValue.autoDeleteTime) !== '-1'
        },
        /**
         * @description 优先使用父组件传入的删除时间选项，否则使用字典获取结果。
         * @returns {Array}
         */
        resolvedTimeOptions() {
            return this.timeOptions && this.timeOptions.length ? this.timeOptions : this.innerTimeOptions
        },
        /**
         * @description 优先使用父组件传入的删除内容选项，否则使用字典获取结果；过滤掉「跟随全局」（null/空值）选项。
         * @returns {Array}
         */
        resolvedContentOptions() {
            const options = this.contentOptions && this.contentOptions.length ? this.contentOptions : this.innerContentOptions
            // 删除内容不再提供「跟随全局」，过滤掉 null/undefined/空字符串，避免渲染无效选项
            return options.filter(item => {
                const value = item?.value
                return value !== null && value !== undefined && String(value) !== ''
            })
        },
        itemStyle() {
            return {
                marginLeft: '16px'
            }
        },
        isInlineLayout() {
            return this.contentLayout === 'inline'
        }
    },
    mounted() {
        this.fetchDictOptions()
    },
    methods: {
        /**
         * @description 从数据字典接口获取自动删除时间与删除内容选项。
         * @returns {Promise<void>}
         */
        async fetchDictOptions() {
            try {
                const dictdata = this.$httpBack?.dictdata
                const [timeRes, contentRes] = await Promise.all([
                    dictdata?.dictDataListByCode ? dictdata.dictDataListByCode({ code: this.timeDictCode }) : Promise.resolve(null),
                    dictdata?.dictDataListByCode ? dictdata.dictDataListByCode({ code: 'delete_content' }) : Promise.resolve(null)
                ])
                if (timeRes?.code === 0 && Array.isArray(timeRes?.data)) {
                    this.innerTimeOptions = timeRes.data.map(d => ({ label: d.label, value: String(d.value) }))
                }
                if (contentRes?.code === 0 && Array.isArray(contentRes?.data)) {
                    this.innerContentOptions = contentRes.data.map(d => ({ label: d.label, value: String(d.value) }))
                }
            } catch (e) {
                // 字典获取失败时保持空选项，不阻断表单渲染
            }
        },
        /**
         * @description 对外同步自动删除配置变更。
         * @param {{autoDeleteTime: string, deleteContent: string}} nextValue
         * @returns {void}
         */
        emitChange(nextValue) {
            this.$emit('input', nextValue)
            this.$emit('change', nextValue)
        },
        /**
         * @description 处理删除时间变更：切回「不删除」时清空删除内容。
         * @param {string|number} val 删除时间值
         * @returns {void}
         */
        onTimeChange(val) {
            const autoDeleteTime = String(val)
            const nextValue = {
                autoDeleteTime,
                deleteContent: autoDeleteTime === '-1' ? '' : this.localValue.deleteContent
            }
            this.emitChange(nextValue)
        },
        /**
         * @description 处理删除内容变更：选择具体内容时二次确认。
         * @param {string} val 删除内容值（ts / mp4 / all）
         * @returns {void}
         */
        onContentChange(val) {
            const nextVal = String(val ?? '')
            // 过滤 null/空值：删除内容不再提供「跟随全局」，空值不触发二次确认
            if (!nextVal) return
            this.pendingContentValue = nextVal
            this.confirmDialogVisible = true
        },
        /**
         * @description 二次确认弹窗确定后写入待确认的删除内容。
         * @returns {void}
         */
        onDeleteConfirm() {
            const value = this.pendingContentValue
            this.pendingContentValue = ''
            if (value) {
                this.emitChange({ ...this.localValue, deleteContent: value })
            }
        },
        /**
         * @description 二次确认弹窗取消后清空待确认值，保持原状态不变。
         * @returns {void}
         */
        onDeleteCancel() {
            this.pendingContentValue = ''
        }
    }
}
</script>

<style scoped lang="scss">
.auto-delete-config {
    .auto-delete-form-inline {
        display: flex;
        align-items: flex-start;
        flex-wrap: wrap;

        .auto-delete-content-item {
            margin-left: 36px;
        }

        ::v-deep(.el-form-item) {
            margin-bottom: 0;
        }

        ::v-deep(.el-form-item__content) {
            line-height: 32px;
        }
    }

    // 非 inline 布局（block/stack）：删除时间与删除内容分两行，label 与选项垂直居中
    .auto-delete-form-block {
        ::v-deep(.el-form-item) {
            display: flex;
            align-items: center;
            margin-bottom: 16px;
        }

        ::v-deep(.el-form-item__label) {
            float: none;
            flex-shrink: 0;
            line-height: 32px;
            padding-top: 0;
        }

        ::v-deep(.el-form-item__content) {
            margin-left: 0 !important;
            flex: 1;
            line-height: normal;
        }
    }

    .auto-delete-time-group,
    .auto-delete-content-group {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        row-gap: 12px;
        .el-radio{
            margin-left: 0 !important;
            // margin-right: 0 !important;
            // width: 130px;
        }
    }

    .required-mark {
        color: #F56C6C;
        margin-right: 6px;
    }

    .content-inline-label {
        margin-right: 12px;
    }

    .tips {
        margin-top: 6px;
        color: #F4BE34;
        font-size: 12px;
    }
}
</style>
