<!--
@description AI数据诊断报告抽屉组件。【废弃】2026-08-03 列表入口已移除，组件保留备用但不从列表唤起。
-->
<template>
    <div class="aiReport">
        <el-drawer
            :destroy-on-close="true"
            :visible.sync="drawerStatus"
            :size="800"
            @close="closeDrawer"
            :wrapperClosable="false"
            :close-on-press-escape="false"
            :with-header="false">
            <div class="question-selector">
                <div class="header-content">
                    <!-- AI分析-->
                    <template v-if="!['addFileVideo','fileVideo'].includes(reportType)">
                        <div class="qs-title">
                            <i class="el-icon-close close-icon" @click="closeAiReport"></i>
                            <span>生成数据诊断</span>
                        </div>
                        <div class="add-ai-report" v-if="reportType === 'anchor'">
                            <div class="qs-desc">*请完善背景信息，爱复盘运营智能体，给到数据报告才更有针对性！</div>
                            <div class="qs-desc">1.开启自动AI数据诊断报告后，每一场都会自动生成诊断报告</div>
                            <div class="qs-desc">2.信息和数据越完善，AI思考越充分，给到的解决方案越有针对性</div>
                            <div class="qs-desc">3.有直播数据的场次才会有自动生成诊断报告</div>
                            <div class="qs-desc">4.AI诊断报告<span style="color: red">需要消耗算力</span>，请按需使用
                            </div>
                        </div>
                        <div class="qs-desc" v-else>
                            *请完善背景信息，爱复盘运营智能体，给到数据报告才更有针对性！AI数据诊断报告 <span
                            style="color: red">需要消耗算力</span>，请按需使用！
                        </div>
                    </template>
                    <!--文件分析-->
                    <template v-else>
                        <div class="qs-title">
                            <i class="el-icon-close close-icon" @click="closeAiReport"></i>
                            <span v-if="reportType === 'anchor'">上传视频</span>
                            <span v-else>基础设置</span>
                        </div>
                        <div class="add-ai-report" v-if="reportType === 'anchor'">
                            <div class="qs-desc">*上传文件如果过大，卡顿属于正常现象，请耐心等待</div>
                        </div>
                    </template>
                </div>
                <div class="aiReport_content" :style="{marginTop: contentTop}">
                    <div v-if="drawerStatus&&reportType === 'addFileVideo'" style="width: 60%">
                        <div class="text-sm" style="padding-block: 12px">*上传文件如果过大，卡顿属于正常现象，请耐心等待
                        </div>
                        <UploadContent @getUploadData="getUploadData"/>
                    </div>
                    <el-form ref="ruleForm" :model="aiReportModel" label-width="82px"
                             label-position="right" size="default"
                             :validate-on-rule-change="false">
                        <div style="margin-bottom: 12px"
                             v-if="reportType === 'anchor'">
                            <el-form-item label="自动生成场次" style="margin-bottom: 0" label-width="108px" required>
                                <el-radio-group v-model="aiReportModel.diagnosisGenerateNum">
                                    <el-radio :label="1" class="radio-as-checkbox">1场</el-radio>
                                    <el-radio :label="3" class="radio-as-checkbox pd-l32">3场</el-radio>
                                    <el-radio :label="5" class="radio-as-checkbox pd-l32">5场</el-radio>
                                    <el-radio :label="999999" class="radio-as-checkbox pd-l32">不限</el-radio>
                                </el-radio-group>
                            </el-form-item>
                            <div style="color: #F4BE34;padding-left: 16px" class="text-xs">
                                (接下来的 {{
                                    aiReportModel.diagnosisGenerateNum < 10 ? aiReportModel.diagnosisGenerateNum : '所有'
                                }}
                                场会自动生成AI数据诊断报告{{
                                    [0, 2].includes(anchorPlatform) ? '，需要等到数据看板有数据的时候才会自动生成' : ''
                                }})
                            </div>
                        </div>
                        <div class="flex items-center justify-between">
                            <el-form-item label="行业选择" prop="tradeId" style="width: 50%"
                                          v-if="reportType==='video'">
                                <tradeId v-model="aiReportModel.tradeId" v-removeAriaHidden :options="tradeTreeList"  :disabled="selectedRow.detailPage"
                                         style="width: 100%"></tradeId>
                            </el-form-item>
                            <el-form-item label="选择模型" prop="modelId" style="width: 42%"
                                          v-if="!['addFileVideo','fileVideo'].includes(reportType)">
                                <el-select v-model="aiReportModel.modelId" placeholder="请选择" style="width: 100%">
                                    <el-option
                                        v-for="item in modelOptions"
                                        :key="item.id"
                                        :label="item.modelName"
                                        :value="item.id">
                                    </el-option>
                                </el-select>
                            </el-form-item>
                        </div>
                        <el-form-item label="账号归属" prop="accountType"
                                      v-if="(reportType !== 'anchor'||['addFileVideo','fileVideo'].includes(reportType))&&!selectedRow.detailPage">
                            <el-radio-group v-model="aiReportModel.accountType" @input="radioGroupChange">
                                <el-radio :label="0" class="radio-as-checkbox">自有账号</el-radio>
                                <el-radio :label="1" class="radio-as-checkbox">同行业账号</el-radio>
                            </el-radio-group>
                        </el-form-item>

                        <el-form-item label="首播日期" prop="premiereDate"
                                      v-if="!['addFileVideo','fileVideo'].includes(reportType)&&!selectedRow.detailPage">
                            <el-date-picker
                                v-model="aiReportModel.premiereDate"
                                type="date"
                                @change="changeDate"
                                format="yyyy-MM-dd"
                                :pickerOptions="{
                                    disabledDate(time) {
                                        return time.getTime() > Date.now();
                                    }
                                }"
                                style="width: 42%"
                                value-format="yyyy-MM-dd HH:mm:ss"
                                placeholder="选择日期时间">
                            </el-date-picker>
                        </el-form-item>
                        <template v-if="reportType === 'video'">
                            <div class="flex items-center">
                                <el-form-item label="AI数据识图" prop="selectDataScreenshot">
                                    <el-radio-group v-model="aiReportModel.selectDataScreenshot"
                                                    :disabled="hasDataScreenshot==0">
                                        <el-radio class="radio-as-checkbox" v-if="hasDataScreenshot==0">未获取
                                        </el-radio>
                                        <el-radio :label="1" class="radio-as-checkbox" v-else
                                                  @click.native.prevent="changeRadio('selectDataScreenshot')">已获取
                                        </el-radio>
                                    </el-radio-group>
                                </el-form-item>
                                <div class="cursor-pointer text-xs"
                                     style="color: var(--color-main);margin-bottom: 18px" @click="handDataScreenshot">
                                    {{ hasDataScreenshot == 0 ? '（去添加）' : '（去查看）' }}
                                </div>
                            </div>

                            <div class="flex items-center" v-if="[0,2].includes(anchorPlatform)">
                                <el-form-item label="数据看板" prop="selectBoard" style="margin-bottom: 0">
                                    <el-radio-group v-model="aiReportModel.selectBoard" :disabled="hasBoard==0">
                                        <el-radio class="radio-as-checkbox" v-if="hasBoard==0">未获取
                                        </el-radio>
                                        <el-radio :label="1" class="radio-as-checkbox" v-else
                                                  @click.native.prevent="changeRadio('selectBoard')">已获取
                                        </el-radio>
                                    </el-radio-group>
                                </el-form-item>
                                <div class="cursor-pointer text-xs" style="color: var(--color-main)" @click="handBoard">
                                    {{ hasBoard == 0 ? '（去添加）' : '（去查看）' }}
                                </div>
                            </div>
                            <div class="text-xs pd-l4" style="margin-bottom: 18px;color: red">
                                {{
                                    [0, 2].includes(anchorPlatform) ? ' *AI数据识图、数据看板为二选一必选' : ' *AI数据识图为必选项'
                                }}
                            </div>
                            <el-form-item
                                class="form-item-vertical"
                                :label="aiReportModel.accountType===1?'重点疑问':'账号问题'"
                                prop="anchorSituation">
                                <el-input v-model="aiReportModel.anchorSituation" type="textarea" :rows="4"
                                          resize="none"
                                          maxlength="200"
                                          style="width: 60%;"
                                          show-word-limit
                                          placeholder="例如：最近在线一直在往下掉，曝光量越来越低，投放ROI不高，怎么提升在线，怎么提升投放ROI"/>
                            </el-form-item>
                        </template>

                        <div class="collapse-form">
                            <el-collapse v-model="activeNames">
                                <template v-for="item in currentReportActionConfig">
                                    <el-collapse-item
                                        v-if="item.accountType.includes(aiReportModel.accountType||0)"
                                        :title="item.label" :name="item.value">
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
                                                    v-for="_item in item.config" :key="_item.value"
                                                    :label="_item.value"
                                                    style="margin-bottom: 8px" class="radio-as-checkbox">
                                                    {{ _item.label }}
                                                </el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item :prop="item.value" label-width="0" style="margin-bottom: 0"
                                                      v-else>
                                            <el-checkbox-group v-model="aiReportModel[item.value]">
                                                <el-checkbox v-for="_item in item.config" :label="_item.value"
                                                             :key="_item.value" class="radio-as-checkbox">
                                                    {{ _item.label }}
                                                </el-checkbox>
                                            </el-checkbox-group>
                                        </el-form-item>
                                    </el-collapse-item>
                                </template>
                            </el-collapse>
                        </div>

                        <!--                        <el-form-item label="默认识别语言" prop="engSerViceType" label-width="112px" class="flex-form-item">-->
                        <!--                            <el-select v-model="aiReportModel.engSerViceType" placeholder="请选择识别语言">-->
                        <!--                                <el-option v-for="item in engSerViceTypeList" :key="item.value" :label="item.label" :value="item.value"></el-option>-->
                        <!--                            </el-select>-->
                        <!--                        </el-form-item>-->


                        <el-form-item v-if="['addFileVideo','fileVideo'].includes(reportType)" style="margin-top: 18px"
                                      class="form-item-vertical"
                                      :label="aiReportModel.accountType===1?'重点疑问':'账号问题'"
                                      prop="anchorSituation">
                            <el-input v-model="aiReportModel.anchorSituation" type="textarea" :rows="4"
                                      resize="none"
                                      maxlength="200"
                                      style="width: 60%;"
                                      show-word-limit
                                      placeholder="例如：最近在线一直在往下掉，曝光量越来越低，投放ROI不高，怎么提升在线，怎么提升投放ROI"/>
                        </el-form-item>
                    </el-form>
                </div>

                <div class="qs-footer">
                    <template v-if="reportType === 'anchor'">
                        <afp-button size="medium" @click="handleConfirm">确认配置</afp-button>
                    </template>
                    <template v-if="reportType === 'video'">
                        <template v-if="itemsCompletedStatus===1">
                            <afp-button size="medium" type="primary" :plain="false" :loading="true">
                                AI数据诊断报告生成中
                            </afp-button>
                        </template>
                        <template v-else>
                            <afp-button size="medium" @click="generateAIReport"
                                        :disabled="[0,1,2,3].includes(itemsCompletedStatus)">生成AI数据诊断报告
                            </afp-button>
                            <afp-button type="primary" :plain="false" size="medium" @click="handleView"
                                        :disabled="![2].includes(itemsCompletedStatus)">预览报告
                            </afp-button>
                            <afp-button type="primary" :plain="false" v-if="[2].includes(itemsCompletedStatus)"
                                        size="medium"
                                        @click="mergeReportStatus">下载数据报告
                            </afp-button>
                            <afp-button size="medium" @click="generateAgain"
                                        :disabled="[null,0,1].includes(itemsCompletedStatus)">重新生成诊断报告
                            </afp-button>
                        </template>
                    </template>
                    <template v-if="['addFileVideo','fileVideo'].includes(reportType)">
                        <afp-button size="medium" @click="handleUpload" v-if="reportType === 'addFileVideo'">点击上传
                        </afp-button>
                        <template v-else>
                            <afp-button size="medium" @click="drawerStatus=false" style="padding-inline: 28px">取 消
                            </afp-button>
                            <afp-button type="primary" :plain="false" style="padding-inline: 28px" size="medium"
                                        @click="saveAiPartial">保 存
                            </afp-button>
                        </template>
                    </template>
                </div>
            </div>
        </el-drawer>
        <DialogAiContent
            ref="dialog_ai_report" :tabsList="tabsList"
            :isReport="isReport"
            :uploadType="2"
            :selectedRow="{...selectedRow,outputName}"
            :sponsorship="getModelZh"
            :hideReportModal="hideReportModal"/>
    </div>
</template>

<script>
import aiDataReport from '@/mixins/aiDataReport'
import MergeReport from './mergeReport.vue'
import DialogAiContent from './dialogAIContent.vue'
import tradeId from "@/components/tradeId/index.vue";
import {cloneDeep, isEmpty, omit, pick} from "lodash";
import UploadContent from "@/views/modules/playBckAnalysis/uploadContent.vue";
import myUtils from "@/utils/utils";
import {actionConfig, ENUM_OBJ} from '@/utils/actionConfig.js';
import {VERSION_TYPE} from "@/enum";

export default {
    name: 'AiReport',
    components: {
        UploadContent,
        tradeId,
        MergeReport,
        DialogAiContent
    },
    mixins: [aiDataReport],
    props: {
        diagnosisParams: {
            type: Object,
            default: () => {
                return {}
            }
        },
        reportType: {//AI诊断报告的类型 video/fileVideo/addFileVideo/主播基础信息
            type: String,
            default: 'video'
        }
    },
    data() {
        return {
            modelOptions: [],//模型列表
            outputName: '',
            itemsCompletedStatus: 0,
            aiReportModel: {
                // tradeId: '',//行业id
                // diagnosisGenerateNum: 1,//自动生成场次
                // modelId: '',//选择模型
                // accountType: '',//账号归属
                // premiereDate: '',//首播日期
                // selectDataScreenshot: '',//AI数据识图
                // selectBoard: '',//数据看板
                // anchorSituation: '',//账号问题
                optimizeDirection: [],//优化方向
                learning: [],//学习方向
            },
            hasBoard: 0,
            hasDataScreenshot: 0,
            activeNames: [],
            tradeTreeList: [],
            uploadData: {},//上传文件数据
            currentReportActionConfig: [],
            anchorPlatform: null,
            itemsCompleted: false
        }
    },
    created() {
    },
    mounted() {
    },
    watch: {
        drawerStatus: {
            handler(val) {
                if (!val) {
                    this.stopPolling()
                }
            },
            deep: true,
            immediate: true
        }
    },
    computed: {
        getModelZh() {
            return this.sponsorship || this.modelOptions.find(a => a.id === this.aiReportModel?.modelId)?.modelName
        },
        reportActionConfig() {
            return actionConfig
        },
        isValueEmpty() {
            return (value) => {
                if (value == null) return true;
                if (typeof value === 'string' && value.trim() === '') return true;
                if (Array.isArray(value) && value.length === 0) return true;
                return false;
            }
        },
        contentTop() {
            if (this.reportType === 'anchor') {
                return '210px'
            } else if (['addFileVideo', 'fileVideo'].includes(this.reportType)) {
                return '42px'
            } else {
                return '85px'
            }
        },
        versionType() {
            return this.$store.getters.getVersionType
        },
        isPure() {
            return this.versionType === VERSION_TYPE.PURE
        }
    },
    methods: {
        radioGroupChange(val) {
            const otherInfo = val === 1 ?
                {learning: []} :
                {optimizeDirection: []}

            this.aiReportModel = {
                ...omit(this.aiReportModel, ['optimizeDirection', 'learning']),
                ...otherInfo
            }
        },
        changeDate(value) {
            if (!myUtils.isMoreThanOtherDays(15, value)) {
                if ([null, undefined, ''].includes(this.aiReportModel?.accountStage)) {
                    this.$set(this.aiReportModel, 'accountStage', 0)
                }
            }
        },
        changeRadio(key) {
            this.$set(this.aiReportModel, key, this.aiReportModel[key] ? '' : 1)
        },
        async changeDrawerStatus(status, row) {
            this.selectedRow = row
            this.anchorPlatform = row?.platform
            if (status) {
                await this.getModelList()
                await this.getTradeTreeList()
                await this.getReportActionConfig()
                this.getListDiagnosis(row?.isAddCompere).then(() => {
                    this.drawerStatus = status
                })
                return
            }
            this.drawerStatus = false
        },
        async getDictDataListByCodes(codes, accountInfoData) {
            try {
                const result = await this.$httpBack.dictdata.dictDataListByCodes({codes})
                if (result.code === 0) {
                    const {basicSettingsVo} = accountInfoData || {}
                    const list = cloneDeep(this.currentReportActionConfig)
                    const accountEnum = myUtils.createEnumHelper(ENUM_OBJ)

                    let enumKey = []
                    if (this.reportType === 'anchor') {
                        enumKey = ['diagnosisGenerateNum', 'modelId', 'premiereDate']
                    }
                    if (['addFileVideo', 'fileVideo'].includes(this.reportType)) {//文件分析
                        enumKey = ['anchorSituation']
                    }
                    if (this.reportType === "video") {//视频分析
                        enumKey = ['tradeId', 'modelId', 'accountType', 'premiereDate', 'selectDataScreenshot', 'selectBoard', 'anchorSituation']
                    }

                    list.forEach(item => {
                        enumKey.push(item.value)
                        const _list = result.data[accountEnum.getValue(item.value)]
                        _list.forEach(item => {
                            item.value = this.toNumberOrOriginal(item.value)
                        })
                        item.config = _list
                    })
                    let info = {...basicSettingsVo, ...accountInfoData}
                    if (this.reportType === 'anchor' && !isEmpty(this.diagnosisParams)) {
                        info = this.diagnosisParams
                    }
                    let otherInfo = {}
                    if (!['addFileVideo', 'fileVideo'].includes(this.reportType)) {
                        otherInfo = accountInfoData?.basicSettingsVo?.accountType === 1 ?
                            {learning: myUtils.normalizeVal(info.learning)} :
                            {optimizeDirection: myUtils.normalizeVal(info.optimizeDirection)}
                    }
                    if (!info.modelId) {
                        const defaultModel = await this.getDefaultModel()
                        if(defaultModel.code!==0) return
                        const findTarget = this.modelOptions.find(item => item?.modelCode === defaultModel?.data?.kvValue)
                        info.modelId = findTarget?.id
                    }
                    this.aiReportModel = {
                        ...pick(info, enumKey),
                        ...otherInfo,
                        accountType: info.accountType
                    }
                    this.currentReportActionConfig = list
                }
            } catch (e) {
            }
        },
        toNumberOrOriginal(val) {
            const num = Number(val);
            return Number.isFinite(num) ? num : val;
        },
        async getConfigInfo() {
            if (this.reportType === 'anchor') return {}
            if (['addFileVideo', 'fileVideo'].includes(this.reportType)) {
                try {
                    const result = await this.$httpBack.words.getAiPartial({
                        sourceId: this.selectedRow?.fileId,
                        sourceType: 2,
                    })
                    if (result.code !== 0) return
                    return result.data
                } catch (e) {
                    return {}
                }
            }
            if (this.reportType === 'video') {
                try {
                    const result = await this.$httpBack.video.getDataDiagnosisConfig({
                        sourceId: this.selectedRow?.videoId,
                        sourceType: 0
                    })
                    if (result.code !== 0) return
                    this.selectedCueWord = [result.data?.cueWordsId]
                    this.itemsCompletedStatus = result.data.qaStatus
                    this.anchorPlatform = result?.data?.platform
                    this.itemsCompleted = [null, 2, 3].includes(result.data.qaStatus)
                    this.tabsList = [{
                        tagName: "AI数据诊断",
                        name: 0,
                        ids: [result.data?.cueWordsId], cueWordsList: [{
                            cueWordsId: result.data?.cueWordsId,
                            cueWord: "",
                        }]
                    }]
                    //只有AI复盘才需要轮询
                    this.$nextTick(() => {
                        this.scheduleNextPoll(10000, () => {
                            this.getConfigInfo()
                        })
                    })
                    return result.data
                } catch (e) {
                    return {}
                }
            }
        },
        async getReportActionConfig() {
            let accountInfoData = {}
            if (this.reportType !== 'addFileVideo') accountInfoData = await this.getConfigInfo()
            let config = []
            const accountEnum = myUtils.createEnumHelper(ENUM_OBJ)
            if (this.reportType === 'anchor') {
                config = this.reportActionConfig.compereConfig
            } else if (['addFileVideo', 'fileVideo'].includes(this.reportType)) {
                config = this.reportActionConfig.fileConfig
                if (this.isPure) {
                    const hideFields = ['accountStage', 'accountWaterLevel', 'accountFlow']
                    config = config.filter(item => !hideFields.includes(item.value))
                }
                config = this.reportType === 'fileVideo' ? config : config?.concat([{
                    label: '默认识别语言',
                    value: 'engSerViceType',
                    selectType: 'single',
                    accountType: [0, 1]
                }])
            } else if (this.reportType === 'video') {
                config = this.reportActionConfig.analysisConfig
                this.hasDataScreenshot = accountInfoData?.hasDataScreenshot
                this.hasBoard = accountInfoData?.hasBoard
            }
            const codes = config.map(item => accountEnum.getValue(item.value))
            this.currentReportActionConfig = config
            await this.getDictDataListByCodes(codes.toString(), accountInfoData)
        },
        // 获取行业列表树形
        getTradeTreeList() {
            this.tradeTreeList = []
            this.$httpBack.trade.listTree({}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data
                }
            })
        },
        getModelList() {
            this.$httpBack.v2500.listDiagnosisModel().then(result => {
                if (result.code === 0) {
                    this.modelOptions = result.data
                }
            })
        },
        async getDefaultModel() {
            return this.$httpBack.common.getByKey('diagnosis_ai_model_default')
        },
        closeDrawer() {
            this.selectedCueWord = []
        },
        closeAiReport() {
            if (!this.selectedRow?.action) this.$emit('initAiReport')
            this.drawerStatus = false
        },
        handleConfirm() {
            if(!this.aiReportModel?.diagnosisGenerateNum){
                return this.$message.warning('请选择生成场次')
            }
            this.$emit('setDiagnosisParams', {
                ...this.aiReportModel,
                optimizeDirection: this.aiReportModel?.optimizeDirection?.join(',')
            })
            this.changeDrawerStatus(false, {})
        },
        async updateDataDiagnosisConfig() {
            try {
                const {learning, optimizeDirection} = this.aiReportModel
                const otherInfo = this.aiReportModel?.accountType === 1 ?
                    {learning: Array.isArray(learning) ? learning.join(',') : learning} :
                    {optimizeDirection: Array.isArray(optimizeDirection) ? optimizeDirection.join(',') : optimizeDirection}
                const params = {
                    basicSettingsVo: {
                        ...omit(this.aiReportModel, ['tradeId', 'modelId', 'selectDataScreenshot', 'selectBoard']),
                        ...otherInfo
                    },
                    ...pick(this.aiReportModel, ['tradeId', 'modelId', 'selectDataScreenshot', 'selectBoard']),
                    sourceId: this.selectedRow?.videoId || this.selectedRow?.fileId,
                    sourceType: this.selectedRow?.videoId ? 0 : 1
                }
                const updateResult = await this.$httpClient.video.updateDataDiagnosisConfig(params)
                if (updateResult.code !== 0) return {}
                return updateResult
            } catch (e) {
                return {}
            }
        },
        async generateAIReport() {
            if ([0, 2].includes(this.anchorPlatform)) {
                if ((!this.aiReportModel?.selectDataScreenshot) && (!this.aiReportModel?.selectBoard)) {
                    return this.$message.info('AI数据识图、数据看板二选一必填')
                }
            } else {
                if (!this.aiReportModel?.selectDataScreenshot) {
                    return this.$message.info('AI数据识图为必选项')
                }
            }

            try {
                const data = await this.updateDataDiagnosisConfig()
                if (data?.code !== 0) return this.$message.error(data.msg)
                const {data: updateResult} = data
                const result = await this.$httpClient.aiRelated.saveDiagnosis({
                    videoId: updateResult?.sourceId,
                    cueWordsId: updateResult?.newCueWordsId || updateResult?.cueWordsId
                })
                if (result.code !== 0) return this.$message.error(result.msg)
                this.$message.success('AI诊断报告生成中，请稍后查看')
                this.itemsCompleted = false
                await this.getConfigInfo()
                this.$emit('generateEnd', () => this.closeAiReport())
            } catch (e) {
            }
        },
        getUploadData(data) {
            this.uploadData = data
        },
        handleUpload() {
            this.$emit('uploadFile', {
                platformType: this.uploadData.platformType,
                ...this.uploadData.fileData,
                content: this.uploadData.content,
                ...pick(this.aiReportModel, ['accountType', 'accountStage', 'accountWaterLevel', 'engSerViceType', 'accountFlow', 'anchorSituation'])
            })
        },
        async saveAiPartial() {
            const {learning, optimizeDirection} = this.aiReportModel
            const otherInfo = this.aiReportModel?.accountType === 1 ?
                {learning: Array.isArray(learning) ? learning.join(',') : learning} :
                {optimizeDirection: Array.isArray(optimizeDirection) ? optimizeDirection.join(',') : optimizeDirection}

            const result = await this.$httpClient.compere.updateAiPartial({
                ...this.aiReportModel,
                ...otherInfo,
                sourceId: this.selectedRow?.fileId,
                sourceType: 2,
            })
            if (result.code !== 0) return
            this.drawerStatus = false
            this.$message.success('保存成功')
        },
        handDataScreenshot() {
            this.$router.push({
                path: '/replay/analysis',
                query: {id: this.selectedRow?.videoId, activeName: 'd'}
            })

            this.$emit('jumpCurrentTab')
        },
        handBoard() {
            this.$router.push({
                path: '/replay/analysis',
                query: {id: this.selectedRow?.videoId, activeName: 'f'}
            })

            this.$emit('jumpCurrentTab')
        },
        handleView() {
            this.hideReportModal(false)
            this.$nextTick(() => {
                this.$refs.dialog_ai_report?.changeDialogVisible(this.tabsList, {0: this.selectedCueWord}, true)
            })
        },
        mergeReportStatus() {
            this.outputName = `${this.selectedRow?.videoRename || this.selectedRow?.videoName}_数据诊断报告`
            this.hideReportModal(true)
            this.$nextTick(() => {
                this.$refs.dialog_ai_report?.changeDialogVisible(this.tabsList, {0: this.selectedCueWord}, true)
            })
        },
        generateAgain() {
            this.generateAIReport()
        },

        stopPolling() {
            if (this.pollingTimeout) {
                clearTimeout(this.pollingTimeout)
                this.pollingTimeout = null
            }
        },
        scheduleNextPoll(time, callBack) {
            this.stopPolling()
            if (!this.itemsCompleted) {
                this.pollingTimeout = setTimeout(() => {
                    callBack?.()
                }, time || 1000)
            }
        },
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

<style lang="scss" scoped>
::v-deep(.form-item-vertical) {
    .el-form-item__label {
        float: none;
        width: 100%;
        text-align: left;
        padding-bottom: 6px;
    }
}

::v-deep(.form-item-vertical) {
    .el-form-item__content {
        margin-left: 0 !important;
    }
}

.aiReport {

    .question-selector {
        background: #fff;
        border-radius: 8px;
        margin-bottom: 36px;
        padding: 12px 12px 6px 24px;

        .qs-title {
            font-size: 16px;
            font-weight: 600;
            color: #151719;
            padding: 10px 0;

            .close-icon {
                cursor: pointer;
                padding-right: 12px;
            }
        }

        .add-ai-report {
            background: #F5F7FB;
            border-radius: 8px 8px 8px 8px;
            padding: 6px 16px;
        }

        .qs-desc {
            font-size: 14px;
            margin: 10px 0;
            color: #000;
        }

        .qs-desc-title {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .qs-footer {
            display: flex;
            justify-content: flex-start;
            margin-top: 12px;
        }

        .header-content {
            position: fixed;
            width: 100%;
            background: #fff;
            z-index: 999;
            top: 0;
        }

        .aiReport_content {
            margin-bottom: 36px;

            .collapse-form {
                ::v-deep(.el-collapse) {
                    border: none;

                    .el-collapse-item {
                        &:not(:first-child) {
                            margin-top: 18px;
                        }
                    }

                    .el-collapse-item__header, .el-collapse-item__wrap {
                        border: none;
                    }

                    .el-collapse-item__header {
                        width: 60%;
                        position: relative;
                        height: 34px;
                        background: #F7F7F7;
                        border-radius: 4px 4px 4px 4px;
                        padding-inline: 8px;

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

                    .el-collapse-item__content {
                        border-radius: 4px 4px 4px 4px;
                        padding: 8px 8px 0 8px;
                        border: 1px solid #DFEAF6;
                        margin-block: 6px;
                        width: 60%;
                    }
                }
            }
        }
    }
}
</style>
