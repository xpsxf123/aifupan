<template>
    <!-- 标准稿预览/编辑面板：展示标准稿时间轴内容，并按场景提供“确认标准稿/生成报告”主操作 -->
    <div class="standard-draft-panel">
        <!-- 可滚动区域：标题、摘要信息、时间轴表格 -->
        <div class="standard-scroll">
            <!-- 区块标题 -->
            <div class="section-title">
                <span class="title-mark"></span>
                <span>标准直播稿件</span>
            </div>

            <!-- 摘要信息：话术模式/循环时长/语速 -->
            <div class="summary-line">
                <span>类型：{{ formModel.scriptType === 1 ? '循环话术' : '非循环话术' }}</span>
                <span v-if="formModel.scriptType === 1">预估循环时长：{{ formModel.loopDuration || 0 }}分钟</span>
                <span>语速：{{ formModel.wordsPerMinute || 0 }}字/分钟</span>
            </div>

            <!-- 回到表单步骤：重新录入参考脚本 -->
            <el-button class="reinput-btn" type="text" icon="el-icon-refresh-right" @click="$emit('reinput')">
                重新录入参考直播脚本
            </el-button>

            <!-- 标准稿时间轴表格：时间段/框架标题/直播话术（可编辑） -->
            <el-table
                class="draft-table"
                :data="draftRows"
                border
                :header-cell-style="headerCellStyle"
                :cell-style="cellStyle">
                <el-table-column prop="timeRange" label="时间段" width="105"></el-table-column>
                <el-table-column prop="title" label="框架标题" width="146">
                    <template slot-scope="{ row }">
                        <el-input
                            v-if="editable"
                            v-model="row.title"
                            class="draft-edit-textarea"
                            type="textarea"
                            resize="none"
                            :autosize="{ minRows: 2 }"
                        />
                        <div v-else>{{ row.title }}</div>
                    </template>
                </el-table-column>
                <el-table-column prop="content" label="直播话术">
                    <template slot-scope="{ row }">
                        <el-input
                            v-if="editable"
                            v-model="row.content"
                            class="draft-edit-textarea"
                            type="textarea"
                            resize="none"
                            :autosize="{ minRows: 5 }">
                        </el-input>
                        <div v-else class="draft-content">{{ row.content }}</div>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 底部操作区：按场景显示主操作按钮 + 编辑/保存按钮 -->
        <div class="standard-footer">
            <el-button
                v-if="scene === 'anchorConfig'"
                class="primary-action"
                type="primary"
                @click="$emit('confirm-standard-script')">
                确认标准直播稿
            </el-button>
            <el-button
                v-else
                class="primary-action"
                type="primary"
                @click="$emit('generate-report')">
                点我生成还原度报告
            </el-button>
            <el-button class="edit-action" plain @click="handleEditClick">
                {{editable?'保存标准稿':'编辑标准稿'}}
            </el-button>
        </div>
    </div>
</template>

<script>
/**
 * @description 话术还原度 - 标准直播稿预览与本地编辑组件。
 * - 本组件只负责“本地编辑/本地保存（更新到父组件）”，不直接调用后端接口；
 * - 标准稿的落库确认由父组件根据场景（preConfig/report）统一触发 confirmStandardScript。
 */

export default {
    name: 'ScriptRestorationDraft',
    props: {
        /**
         * @description 弹窗使用场景
         * - preConfig：添加主播/直播间预设配置，只做标准稿确认（不生成报告）
         * - report：已明确资源，确认后可生成还原度报告
         */
        scene: {
            type: String,
            default: 'report'
        },
        /**
         * @description 表单模型（用于顶部摘要展示）
         */
        formModel: {
            type: Object,
            required: true
        },
        /**
         * @description 标准稿时间轴行数据（父组件传入）
         */
        rows: {
            type: Array,
            default: () => []
        }
    },
    data() {
        return {
            // 是否处于可编辑状态（编辑框架标题 + 直播话术）
            editable: false,
            // 可编辑的时间轴行（避免直接修改父组件引用）
            draftRows: this.rows.map(item => ({...item})),
            headerCellStyle: {
                height: '42px',
                padding: '0 12px',
                background: '#f2f2f2',
                color: '#151719',
                fontSize: '14px',
                fontWeight: 500,
                lineHeight: '22px',
                borderColor: '#d0d7de'
            },
            cellStyle: {
                padding: '10px 12px',
                color: '#151719',
                fontSize: '14px',
                lineHeight: '22px',
                borderColor: '#d0d7de',
                verticalAlign: 'middle'
            }
        }
    },
    watch: {
        rows: {
            handler(val) {
                this.draftRows = val.map(item => ({...item}))
            },
            deep: true
        }
    },
    methods: {
        /**
         * @description 切换编辑/保存。
         * - 进入编辑：仅切换 editable
         * - 保存：把本地编辑结果回传给父组件（不落库；落库由“确认标准直播稿/生成报告”触发）
         * @returns {void}
         */
        handleEditClick() {
            if (!this.editable) {
                this.editable = true
                return
            }
            this.$emit('update-rows', this.draftRows.map(item => ({...item})))
            this.editable = false
        }
    }
}
</script>

<style scoped lang="scss">
.standard-draft-panel {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
}

.standard-scroll {
    position: relative;
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 17px 30px 24px;
    box-sizing: border-box;
}

.section-title {
    display: flex;
    align-items: center;
    gap: 4px;
    height: 22px;
    color: #484a4c;
    font-size: 14px;
    font-weight: 500;
    line-height: 22px;
}

.title-mark {
    width: 3px;
    height: 14px;
    border-radius: 50px;
    background: #444dff;
}

.summary-line {
    display: flex;
    align-items: center;
    gap: 50px;
    margin: 8px 0 18px 7px;
    color: #151719;
    font-size: 14px;
    line-height: 22px;
    white-space: nowrap;
}

.reinput-btn {
    position: absolute;
    top: 47px;
    right: 30px;
    padding: 0;
    color: #444dff;
    font-size: 14px;
    font-weight: 500;

::v-deep(.draft-edit-textarea .el-textarea__inner) {
    padding: 10px;
}
    font-weight: 500;
    line-height: 22px;
}

.draft-table {
    width: 722px;
    border-radius: 8px;
    overflow: hidden;
    color: #151719;
    font-size: 14px;
}

::v-deep(.draft-table .el-table__row) {
    min-height: 137px;
}

.draft-content {
    white-space: pre-line;
    word-break: break-all;
}

.draft-edit-textarea {
    width: 100%;
}

::v-deep(.draft-edit-textarea .el-textarea__inner) {
    padding: 0;
    border: 0;
    color: #151719;
    font-size: 14px;
    line-height: 22px;
    box-shadow: none;
}

.standard-footer {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 30px 15px;
}

.primary-action,
.edit-action {
    height: 34px;
    padding: 4px 12px;
    border-radius: 54px;
    font-size: 14px;
    font-weight: 500;
    line-height: 22px;
}

.primary-action {
    border-color: #444dff;
    background: #444dff;
}

.edit-action {
    border-color: #444dff;
    color: #444dff;
}
</style>

