<template>
    <div class="ai-setting-content">
        <div class="accountInfo">
            <div class="ai-setting-item" style="padding-left:0" v-if="!isFile">
                <div class="pd-b6">首播日期</div>
                <el-date-picker
                    size="default"
                    style="width: 100%"
                    v-model="aiReportModel.premiereDate"
                    @change="changeDate"
                    type="date"
                    :pickerOptions="{
                        disabledDate(time) {
                            return time.getTime() > Date.now();
                        }
                    }"
                    format="yyyy-MM-dd"
                    value-format="yyyy-MM-dd HH:mm:ss"
                    placeholder="选择日期时间">
                </el-date-picker>
            </div>
            <div class="ai-setting-item" style="padding-left:0">
                <el-checkbox class="w100 flex-jc-sb flex-ai-c"
                             :disabled="config.hasDataScreenshot === 0" v-model="getUploadScreenshot"
                             :true-label="1" :false-label="0">AI数据识图
                </el-checkbox>
            </div>
            <div class="ai-setting-item" style="padding-left:0">
                <el-checkbox class="w100 flex-jc-sb flex-ai-c" :disabled="config.hasBoard === 0"
                             v-model="getUploadBoard" :true-label="1" :false-label="0">数据看板
                </el-checkbox>
            </div>
            <el-form ref="ruleForm" :model="aiReportModel" label-width="0" size="mini">
                <el-collapse v-model="activeNames">
                    <el-collapse-item v-for="item in currentReportActionConfig"
                                      :title="item.label" :name="item.value" :key="item.value">
                        <template slot="title">
                            <div class="collapse-title">
                                <span
                                    :style="{color:isValueEmpty(aiReportModel[item.value])?'#303133':'var(--color-main)'}">
                                    {{ item.label }}
                                </span>
                                <span
                                    :style="{color:activeNames.includes(item.value)?'var(--color-main)':'#303133'}">
                                    <span>{{ activeNames.includes(item.value) ? '收起' : '展开' }}</span>
                                    <i class="el-icon-arrow-right custom-icon font-bold"
                                       :class="{ rotate: activeNames.includes(item.value) }"></i>
                                </span>
                            </div>
                        </template>

                        <el-form-item :prop="item.value" label-width="0" style="margin-bottom: 0"
                                      v-if="item.selectType==='single'">
                            <el-radio-group
                                v-model="aiReportModel[item.value]">
                                <el-radio
                                    v-for="_item in item.config"
                                    :label="_item.value"
                                    style="margin-bottom: 8px" class="radio-as-checkbox">
                                    {{ _item.label }}
                                </el-radio>
                            </el-radio-group>
                        </el-form-item>

                        <el-form-item :prop="item.value" label-width="0" style="margin-bottom: 0" v-else>
                            <el-checkbox-group v-model="aiReportModel[item.value]">
                                <el-checkbox v-for="_item in item.config" :label="_item.value">
                                    <span style="padding-left: 5px">{{ _item.label }}</span>
                                </el-checkbox>
                            </el-checkbox-group>
                        </el-form-item>
                    </el-collapse-item>
                    <el-collapse-item :title="getAccountType===1?'重点疑问':'账号问题'" name="anchor_situation"
                                      key="anchor_situation">
                        <template slot="title">
                            <div class="collapse-title">
                                <span
                                    :style="{color:[undefined,null,''].includes(aiReportModel.anchorSituation)?'#303133':'var(--color-main)'}">
                                        {{ getAccountType === 1 ? '重点疑问' : '账号问题' }}
                                </span>
                                <span
                                    :style="{color:activeNames.includes('anchor_situation')?'var(--color-main)':'#303133'}">
                                    <span>{{ activeNames.includes('anchor_situation') ? '收起' : '展开' }}</span>
                                    <i class="el-icon-arrow-right custom-icon font-bold"
                                       :class="{ rotate: activeNames.includes('anchor_situation') }"></i>
                                </span>
                            </div>
                        </template>

                        <el-form-item prop="anchorSituation" label-width="0" style="margin-bottom: 0">
                            <el-input v-model="aiReportModel.anchorSituation" type="textarea" :rows="4"
                                      resize="none"
                                      maxlength="500"
                                      show-word-limit
                                      placeholder="例如：最近在线一直在往下掉，曝光量越来越低，投放ROI不高，怎么提升在线，怎么提升投放ROI"/>
                        </el-form-item>
                    </el-collapse-item>
                </el-collapse>
            </el-form>
        </div>
        <div class="text-right">
            <afp-button size="small" :plain="false" @click="$emit('popoverStatus',false)"
                        style="margin: 6px;padding-inline: 12px">取消
            </afp-button>
            <afp-button size="small" :plain="false" type="primary" @click="onSubmit"
                        style="margin: 6px;padding-inline: 12px">确认
            </afp-button>
        </div>
    </div>
</template>

<script>
import {actionConfig, ENUM_OBJ} from '@/utils/actionConfig.js';
import myUtils from "@/utils/utils";
import {cloneDeep, omit} from "lodash";

export default {
    components: {},
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => {
                return {}
            },
            required: () => {
                return {}
            }
        },
        targetType: {
            type: String,
            default: ''
        },
        backgroundConfig: {
            type: Object,
            default: () => {
                return {}
            }
        },
        config: {
            type: Object,
            default: () => {
                return {}
            }
        },
        uploadScreenshot: {
            type: Number,
            default: 0
        },
        uploadBoard: {
            type: Number,
            default: 0
        },
        item: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {
            activeNames: ['anchor_situation'],
            aiReportModel: {
                optimizeDirection: [],//优化方向
                learning: [],//学习方向
            },
            currentReportActionConfig: {},
        };
    },
    computed: {
        getSecUid() {
            return this.sentenceMarkData?.anchorInfo?.SecUid
        },
        isOnline() {
            return ['webOnline', 'online'].includes(this.targetType)
        },
        isAuthenticated() {
            const {videoInfo = {}} = this.sentenceMarkData
            return videoInfo?.UserId ? this.$auth([videoInfo?.UserId, videoInfo?.TenantId], 'every') : true
        },
        getAccountType() {
            return this.aiReportModel?.accountType
        },
        getUploadScreenshot: {
            get() {
                return this.uploadScreenshot
            },
            set(val) {
                this.$emit('update:uploadScreenshot', val)
            }
        },
        getUploadBoard: {
            get() {
                return this.uploadBoard
            },
            set(val) {
                this.$emit('update:uploadBoard', val)
            }
        },
        getVideoId() {
            return this.sentenceMarkData?.videoInfo?.VideoId
        },
        getFileId() {
            return this.sentenceMarkData?.fileInfo?.fileId
        },
        isFile() {
            return this.sentenceMarkData?.fileInfo?.fileId
        },
        isValueEmpty(){
            return (value)=>{
                if (value == null) return true;
                if (typeof value === 'string' && value.trim() === '') return true;
                if (Array.isArray(value) && value.length === 0) return true;
                return false;
            }
        },
        reportActionConfig() {
            return actionConfig
        },
    },
    watch: {
        backgroundConfig: {
            handler(val) {
                this.aiReportModel = {
                    ...val,
                    optimizeDirection: myUtils.normalizeVal(val.optimizeDirection),
                    learning: myUtils.normalizeVal(val.learning)
                }
                if (val.accountType !== undefined) this.getActionConfig()
            },
            deep: true,
            immediate: true
        }
    },
    methods: {
        toNumberOrOriginal(val) {
            const num = Number(val);
            return Number.isFinite(num) ? num : val;
        },
        changeDate(value){
            if (!myUtils.isMoreThanOtherDays(15,value)) {
                if([null,undefined,''].includes(this.aiReportModel?.accountStage)){
                    this.$set(this.aiReportModel, 'accountStage', 0)
                }
            }
        },
        async getDictDataListByCodes(codes) {
            try {
                const result = await this.$httpBack.dictdata.dictDataListByCodes({codes})
                if (result.code === 0) {
                    const list = cloneDeep(this.currentReportActionConfig)
                    const accountEnum = myUtils.createEnumHelper(ENUM_OBJ)
                    list.forEach(item => {
                        const _list = result.data[accountEnum.getValue(item.value)]
                        _list.forEach(item => {
                            item.value = this.toNumberOrOriginal(item.value)
                        })
                        item.config = _list
                    })
                    this.currentReportActionConfig = list
                }
            } catch (e) {
            }
        },
        getActionConfig() {
            let config = []
            const accountEnum = myUtils.createEnumHelper(ENUM_OBJ)
            const isVideoFile = this.sentenceMarkData?.fileInfo?.fileType === 0;
            const analysisConfig = cloneDeep(isVideoFile ? this.reportActionConfig.fileConfig : this.reportActionConfig.analysisConfig)
            config = isVideoFile ? analysisConfig : analysisConfig.filter(v => v.accountType.includes(this.getAccountType))
            const codes = config.map(item => accountEnum.getValue(item.value))
            this.currentReportActionConfig = config
            this.getDictDataListByCodes(codes.toString())
        },
        onSubmit() {
            this.$refs.ruleForm?.validate(async valid => {
                if (valid) {
                    try {
                        const target = this.getAccountType === 0 ? {
                            optimizeDirection: this.aiReportModel?.optimizeDirection?.join(','),
                        } : {
                            learning: this.aiReportModel?.learning?.join(','),
                        }
                        const params = {
                            secUid: this.getSecUid,
                            sourceId: this.getVideoId || this.getFileId,
                            sourceType: this.getVideoId ? 1 : 2,
                            ...omit(this.aiReportModel, ['optimizeDirection', 'learning']),
                            accountType: this.aiReportModel.accountType || this.getAccountType,
                            ...target
                        }
                        if (this.isAuthenticated) {
                            const httpServer = this.isOnline ? this.$httpBack.compere.updateAiPartial : this.$httpClient.compere.updateAiPartial
                            const result = await httpServer(params)
                            if (result.code !== 0) return
                            this.$message.success(`${this.getVideoId ? '主播' : '文件'}基础信息更新成功`)
                        }

                        this.$emit('popoverStatus', false)
                        this.$emit('backgroundConfigChange', omit(params, ['secUid', 'sourceId', 'sourceType']))
                    } catch (e) {
                    }
                } else {

                }
            })
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

::v-deep(.accountInfo) {
    max-height: 68vh;
    overflow: auto;
    padding: 2px 10px;

    .ai-setting-item {
        padding: 10px;

        .el-checkbox__label {
            padding: 0;
        }
    }

    .el-checkbox {
        flex-flow: row-reverse !important;

        //.el-checkbox__input {
        //    display: flex;
        //    align-items: center;
        //}
    }

    .el-form-item {
        margin-bottom: 2px !important;
    }

    .el-checkbox__label {
        padding: 0;
    }

    .el-collapse, .el-collapse-item__header, .el-collapse-item__wrap {
        border: none;
    }

    .el-collapse-item__header {
        height: 39px;
        line-height: 39px;
    }

    .el-collapse-item__content {
        background: #F9FAFB;
        padding: 6px;
        border-radius: 8px;
    }

    .el-collapse-item__arrow {
        display: none;
    }

    .collapse-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        width: 100%;
    }

    .custom-icon {
        transition: 0.3s;
    }

    .custom-icon.rotate {
        transform: rotate(90deg);
    }

}
</style>