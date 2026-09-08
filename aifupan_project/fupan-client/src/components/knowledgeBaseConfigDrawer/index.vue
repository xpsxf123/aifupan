<!--
@description 知识库配置抽屉：用于录入/编辑主播知识库的运营知识库、敏感词知识库与直播间健康值，支持是否展示顶部说明气泡。
-->
<template>
    <el-drawer
        :visible.sync="drawerVisible"
        :with-header="false"
        :append-to-body="true"
        :wrapper-closable="false"
        size="680px"
        custom-class="knowledgeBaseConfigDrawer"
    >
        <div class="knowledgeBaseDrawer">
            <div class="knowledgeBaseDrawerHeader">
                <div class="knowledgeBaseDrawerTitle">
                    <i class="el-icon-close closeIcon" @click="drawerVisible = false"></i>
                    <span>直播间&lt;{{ drawerTitleName }}&gt;的知识库</span>
                </div>
            </div>

            <div class="knowledgeBaseDrawerBody">
                <div class="knowledgeBaseTipBar">
                    <span>*什么是知识库?</span>
                    <template v-if="showIntroPopover">
                        <el-popover
                            placement="bottom-start"
                            width="320"
                            trigger="click"
                            popper-class="knowledgeBaseIntroPopover"
                        >
                            <div class="knowledgeBaseIntroContent">
                                <div>运营知识库是运营对直播间的理解</div>
                                <div>1、录入后，每次诊断和分析本直播间，会优先参考您的知识库</div>
                                <div>2、知识库越丰富，则分析越符合您的要求</div>
                                <div>3、知识库内容越多，消耗的AI算力包越多</div>
                                <div>4、请仔细阅读以下提示输入知识库</div>
                            </div>
                            <span slot="reference" class="knowledgeBaseTipLink">点击查看</span>
                        </el-popover>
                    </template>
                </div>

                <el-form ref="knowledgeBaseForm" :model="localForm" label-position="top" class="knowledgeBaseForm">
                    <el-form-item label="运营知识库">
                        <el-input
                            v-model="localForm.operationKnowledge"
                            type="textarea"
                            resize="none"
                            :rows="8"
                            maxlength="6000"
                            show-word-limit
                            :placeholder="operationPlaceholder"
                        />
                    </el-form-item>

                    <el-form-item label="敏感词知识库">
                        <el-input
                            v-model="localForm.sensitiveKnowledge"
                            type="textarea"
                            resize="none"
                            :rows="6"
                            maxlength="2000"
                            show-word-limit
                            :placeholder="sensitivePlaceholder"
                        />
                    </el-form-item>

                    <el-form-item label="直播间健康值">
                        <el-input
                            v-model="localForm.healthKnowledge"
                            type="textarea"
                            resize="none"
                            :rows="5"
                            maxlength="2000"
                            show-word-limit
                            :placeholder="healthPlaceholder"
                        />
                    </el-form-item>
                </el-form>
            </div>

            <div class="knowledgeBaseDrawerFooter">
                <afp-button type="primary" :plain="false" :disabled="isSaveDisabled" @click="handleSave">保存知识库</afp-button>
            </div>
        </div>
    </el-drawer>
</template>

<script>
/**
 * @description 知识库配置抽屉脚本：维护抽屉显隐、表单回显与保存事件派发，供多个业务页面复用。
 */
export default {
    name: 'KnowledgeBaseConfigDrawer',
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        anchorName: {
            type: String,
            default: ''
        },
        value: {
            type: Object,
            default: () => ({})
        },
        showIntroPopover: {
            type: Boolean,
            default: true
        }
    },
    data() {
        return {
            localForm: this.getDefaultForm()
        }
    },
    computed: {
        drawerVisible: {
            get() {
                return this.visible
            },
            set(val) {
                this.$emit('update:visible', val)
            }
        },
        drawerTitleName() {
            return this.anchorName || '未命名主播'
        },
        operationPlaceholder() {
            return '请录入您的本直播间运营的理解、要解决的问题、以及每个主播的优势和劣势'
        },
        sensitivePlaceholder() {
            return '你也可以按照您公司的经验输入所在赛道的违禁词，这样AI分析的时候，优化话术就不会出现指定的违禁词了（建议不超过500个），格式参考：\n以下词汇属于XXX赛道敏感词，在你给到的优化话术或者话术建议或者合规话术建议里，不要出现相关的词汇：\n打已拍、包过、一定有效果、最有效'
        },
        healthPlaceholder() {
            return '设置了直播间行业健康值后，每次分析都会参考您给的数据进行分析和建议，最好将您认为各项基础数据健康值都录入进来\n格式参考：互动率健康值：5%；千次成交健康值：800。'
        },
        isSaveDisabled() {
            const operationKnowledge = String(this.localForm.operationKnowledge || '').trim()
            const sensitiveKnowledge = String(this.localForm.sensitiveKnowledge || '').trim()
            const healthKnowledge = String(this.localForm.healthKnowledge || '').trim()
            return !operationKnowledge && !sensitiveKnowledge && !healthKnowledge
        }
    },
    watch: {
        visible: {
            immediate: true,
            handler(val) {
                if (!val) return
                this.syncLocalForm(this.value)
            }
        },
        value: {
            deep: true,
            handler(val) {
                if (!this.visible) return
                this.syncLocalForm(val)
            }
        }
    },
    methods: {
        /**
         * @description 获取默认知识库表单结构，避免多个页面复用时字段不一致。
         * @returns {{operationKnowledge: string, sensitiveKnowledge: string, healthKnowledge: string}}
         */
        getDefaultForm() {
            return {
                operationKnowledge: '',
                sensitiveKnowledge: '',
                healthKnowledge: ''
            }
        },
        /**
         * @description 将外部传入的知识库数据同步到抽屉本地表单，避免直接修改父级对象。
         * @param {Object} value 知识库值对象
         * @returns {void}
         */
        syncLocalForm(value = {}) {
            this.localForm = {
                ...this.getDefaultForm(),
                operationKnowledge: value?.operationKnowledge ?? value?.operationContent ?? '',
                sensitiveKnowledge: value?.sensitiveKnowledge ?? value?.sensitiveContent ?? '',
                healthKnowledge: value?.healthKnowledge ?? value?.healthScore ?? ''
            }
        },
        /**
         * @description 保存知识库内容，并将整理后的本地假数据回传给父页面。
         * @returns {void}
         */
        handleSave() {
            if (this.isSaveDisabled) {
                this.$message.warning('请至少填写一项知识库内容后再保存')
                return
            }
            const payload = {
                operationKnowledge: String(this.localForm.operationKnowledge || '').trim(),
                sensitiveKnowledge: String(this.localForm.sensitiveKnowledge || '').trim(),
                healthKnowledge: String(this.localForm.healthKnowledge || '').trim()
            }
            this.$emit('save', payload)
            this.drawerVisible = false
        }
    }
}
</script>

<style lang="scss">
.knowledgeBaseConfigDrawer {
    .el-drawer__header {
        display: none;
    }
}

.knowledgeBaseIntroPopover {
    background: rgba(51, 51, 51, 0.92);
    border-color: rgba(51, 51, 51, 0.92);
    color: #fff;

    .popper__arrow::after {
        border-bottom-color: rgba(51, 51, 51, 0.92) !important;
    }
}
</style>

<style scoped lang="scss">
.knowledgeBaseDrawer {
    display: flex;
    flex-direction: column;
    height: 100%;
    background: #fff;
}

.knowledgeBaseDrawerHeader {
    flex: 0 0 auto;
    padding: 18px 24px 0;
}

.knowledgeBaseDrawerTitle {
    display: flex;
    align-items: center;
    color: #303133;
    font-size: 20px;
    font-weight: 600;
    line-height: 28px;
}

.closeIcon {
    margin-right: 10px;
    font-size: 18px;
    color: #909399;
    cursor: pointer;
}

.knowledgeBaseDrawerBody {
    flex: 1;
    min-height: 0;
    padding: 20px 24px 12px;
    overflow-y: auto;
}

.knowledgeBaseTipBar {
    display: flex;
    align-items: center;
    min-height: 48px;
    margin-bottom: 20px;
    padding: 0 16px;
    border-radius: 8px;
    background: #f5f7ff;
    color: #606266;
    font-size: 14px;
}

.knowledgeBaseTipLink {
    margin-left: 4px;
    color: #4c6fff;
    cursor: pointer;
}

.knowledgeBaseIntroContent {
    line-height: 22px;
    font-size: 13px;
}

.knowledgeBaseForm {
    ::v-deep(.el-form-item) {
        margin-bottom: 22px;
    }

    ::v-deep(.el-form-item__label) {
        padding-bottom: 8px;
        color: #303133;
        font-size: 14px;
        line-height: 22px;
    }

    ::v-deep(.el-textarea__inner) {
        min-height: 136px !important;
        padding: 14px 16px 28px;
        border: 1px solid #dfe4ec;
        border-radius: 6px;
        color: #606266;
        font-size: 14px;
        line-height: 22px;
    }

    ::v-deep(.el-input__count) {
        right: 12px;
        bottom: 10px;
        color: #c0c4cc;
        background: transparent;
        line-height: 20px;
    }
}

.knowledgeBaseDrawerFooter {
    flex: 0 0 auto;
    padding: 0 24px 24px;
}
</style>
