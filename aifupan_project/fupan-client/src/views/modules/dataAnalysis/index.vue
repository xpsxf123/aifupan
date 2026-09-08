<template>
    <el-row>
        <el-col :span="24">

        </el-col>
        <el-col v-if="getTabsName === 'first'" :span="24">
            <AllAnchors :isAll="isAll" ref="allAnchors">
                <!-- 搜索栏 -->
                <template #search-header>
                    <div class="search-box flex-ai-c">
                        <el-form :inline="true">
                            <el-form-item  label="搜索">
                                <el-input class="input-gray input-border-none" placeholder="请输入直播间名称" size="default" v-model="anchorName" clearable></el-input>
                            </el-form-item>
                            <el-form-item>
                                <afp-button @click="keywordChange" size="default" type="primary" plain>查询</afp-button>
                            </el-form-item>
                            <el-form-item>
                                <el-button @click="recordHistory" type="text">录制历史</el-button>
                            </el-form-item>

                        </el-form>
<!--                        <polish class="mg-l24">-->
<!--                            <img v-if="$store.getters.getPackageLevel === 0" src="@/assets/imgs/mflz.png"-->
<!--                                 style="max-height: 24px;">-->
<!--                        </polish>-->
                    </div>
                </template>
            </AllAnchors>
        </el-col>
        <el-col v-else-if="getTabsName !== 'first'" :span="24">
            <Record ref="Record" :isAll="isAll" @tabsName="(val)=>{setTabsName(val,'toList')}">
                <!-- 搜索栏 -->
                <template #search>
                    <div class="search-box flex-ai-c">
                        <el-form :inline="true">
                            <el-form-item label="搜索">
                                <el-input class="input-gray input-border-none" placeholder="请输入直播间名称" size="default" v-model="anchorName" clearable></el-input>
                            </el-form-item>
                            <el-form-item>
                                <afp-button @click="keywordChange" size="default" type="primary" plain>查询</afp-button>
                            </el-form-item>
                            <el-form-item>
                                <el-button @click="recordHistory" type="text">录制历史</el-button>
                            </el-form-item>
                        </el-form>
<!--                        <polish class="mg-l24">-->
<!--                            <img v-if="$store.getters.getPackageLevel === 0" src="@/assets/imgs/mflz.png"-->
<!--                                 style="max-height: 24px;">-->
<!--                        </polish>-->
                    </div>
                </template>
            </Record>
        </el-col>
        <el-col :span="24">
            <!-- 行业弹窗 -->
            <!--            <el-dialog title="行业选择" :visible.sync="tardeDialogVisible" width="600px" :close-on-click-modal="false">-->
            <!--                <el-cascader v-model="selectTradeId" :options="tradeTreeList" style="width: 100%;" node-key="id"-->
            <!--                    :props="{ checkStrictly: true, expandTrigger: 'click', value: 'id', label: 'name', emitPath: false }"-->
            <!--                    filterable placeholder="选择一个行业，提高关键词匹配准确性" @change="changeTradeHandle" ref="tradeCascader">-->
            <!--                </el-cascader>-->
            <!--                <span slot="footer" class="dialog-footer">-->
            <!--                    <el-button @click="tardeDialogVisible = false">取消</el-button>-->
            <!--                    <el-button type="primary" @click="tradeConfirm()">确定</el-button>-->
            <!--                </span>-->
            <!--            </el-dialog>-->

            <BasicSettings :tradeTreeList="tradeTreeList" ref="basicSettings" @setDiagnosisParams="setDiagnosisParams"
                           @tradeConfirm="tradeConfirm"/>


            <!-- 客服弹窗 -->
            <customer-service-qr-code ref="customerServiceQrCode"></customer-service-qr-code>
            <!-- 自定义加载转圈 -->
            <loading :dialogText="loadingText" ref="loadingStop"></loading>

            <RecordHistory :visible.sync="recordHistoryDialogVisible"/>

            <KnowledgeBaseConfigDrawer
                :visible.sync="knowledgeBaseDrawerVisible"
                :anchorName="currentKnowledgeBaseRow.AnchorName || currentKnowledgeBaseRow.RemarksName || ''"
                :value="currentKnowledgeBaseForm"
                :showIntroPopover="true"
                @save="handleKnowledgeBaseSave"
            />

            <!-- 每天第一次打开提醒弹窗 -->
            <!-- <dayFirstDialog v-if="dayFirstVisible" ref="dayFirst" @beginRecord="startDetection"></dayFirstDialog> -->
        </el-col>
    </el-row>
</template>

<script>
import myUtils from '/src/utils/utils'
import tabs from './../../../mixins/tabs'
import AllAnchors from './component/allAnchors.vue'
import Record from './component/record.vue'
import customerServiceQrCode from './../../commonComponent/customerServiceQrCode.vue'
import loading from './loading.vue'
// import dayFirstDialog from '../../commonComponent/dayFirstDialog.vue';
import BasicSettings from './dialog/basicSettings.vue'
import polish from '@/components/polish/index.vue'
import RecordHistory from './component/recordHistory.vue'
import KnowledgeBaseConfigDrawer from '@/components/knowledgeBaseConfigDrawer/index.vue'
import {pick} from 'lodash'

export default {
    components: { AllAnchors, Record, customerServiceQrCode, loading, polish, BasicSettings, RecordHistory, KnowledgeBaseConfigDrawer },
    mixins: [tabs],
    provide () {
        return {
            parent: this,
        }
    },
    inject: ['appVnode','APP'],
    props: {},
    data () {
        return {
            // 加载文本
            loadingText: '正在停止录制和分析',
            // tabs配置
            tabs: [{ label: '所有直播间', name: 'first' ,showTextKey:'AllTotal'}, { label: '录制中', name: 'second',showTextKey:'CurrentLiveNum' }],
            // 查询直播间名称
            anchorName: '',
            // 定时器
            listTimer: null,
            // 检测时间相关
            detectionTimeTimer: false,
            detectionTime: '0',
            isAllTime: 5,
            startDetectionTime: 0,
            //
            loadingVisible: true,
            // 客服对话框可见性
            kefuDialogVisible: false,
            // 是否是当天第一次可见
            dayFirstVisible: false,
            //主播信息
            compereInfo: {},
            // 录制中列表
            compereList: [],
            // 主播列表
            compereMapList: {},
            // 行业分类
            tradeMap: {},
            // 行业加载状态
            tradeLoading: false,
            // 行业列表
            tradeList: [],
            // 行业树形列表
            tradeTreeList: [],
            // 是否已开启检测
            detection: false,
            // 行业选择对话框可见性
            tardeDialogVisible: false,
            selectTradeId: '1',
            currentAnchorSecUid: '',
            // 主播列表总数
            compereListLen: 0,
            // 获取设备信息定时器计时器
            getBasinSetupInfoTime: 0,
            diagnosisParams:{},//以前是内容诊断字段，现在是数据诊断字段，接口调整了，先沿用这个对象
            recordHistoryDialogVisible:false,
            addOrUpdateAnchorPending: false,
            knowledgeBaseDrawerVisible: false,
            knowledgeBaseDraftMap: {},
            currentKnowledgeBaseRow: {},
            // 节流相关变量
            initDataThrottleTimer: null,
            isInitDataExecuting: false
        }
    },
    computed: {
        /**
         * 判断是否为第一个标签页
         *
         * 此方法用于确定当前标签页的索引是否为0（即第一个标签页）
         * 它通过调用`ifTabsIndex`方法并传入0作为参数来实现
         *
         * @returns {boolean} 如果当前标签页索引为0，则返回true；否则返回false
         */
        isAll () {
            return this.getTabsName === 'first'
            // return this.ifTabsIndex(0);
        },
        getTabDom () {
            if (this.getTabsName === 'first') {
                return this.$refs.allAnchors
            } else {
                return this.$refs.Record
            }
        },
        currentKnowledgeBaseForm() {
            return this.getKnowledgeBaseFormData(this.currentKnowledgeBaseRow)
        }
    },
    watch: {
        // getTabsName:{
        //     handler(v){
        //         if(this.isAll){
        //             this.$nextTick(() => {
        //                 // 获取列表数据
        //                 this.getDataList();
        //             })
        //         }
        //     },
        // }
        // "$route.path":{
        //     handler(val,old){
        //         if(val === '/dataAnalysis'){
        //
        //         }
        //     },
        //     deep:true,
        //     immediate:true
        // }
    },
    methods: {
        /**
         * @description 获取主播知识库本地缓存 key，方便当前阶段用假数据完成录入/编辑/回显。
         * @returns {string}
         */
        getKnowledgeBaseStorageKey() {
            return 'dataAnalysisAnchorKnowledgeBaseDraftMap'
        },
        /**
         * @description 从本地缓存读取知识库草稿映射，按主播 SecUid 维度维护。
         * @returns {void}
         */
        loadKnowledgeBaseDraftMap() {
            try {
                const cache = localStorage.getItem(this.getKnowledgeBaseStorageKey())
                this.knowledgeBaseDraftMap = cache ? JSON.parse(cache) : {}
            } catch (e) {
                this.knowledgeBaseDraftMap = {}
            }
        },
        /**
         * @description 持久化当前知识库草稿映射，保证刷新后仍可用于“编辑”态查看。
         * @returns {void}
         */
        saveKnowledgeBaseDraftMap() {
            localStorage.setItem(this.getKnowledgeBaseStorageKey(), JSON.stringify(this.knowledgeBaseDraftMap || {}))
        },
        /**
         * @description 获取指定主播的知识库表单数据。
         * @param {Object} row 主播行数据
         * @returns {Object}
         */
        getKnowledgeBaseFormData(row = {}) {
            const secUid = row?.SecUid
            if (!secUid) return {}
            return this.knowledgeBaseDraftMap?.[secUid] || {}
        },
        /**
         * @description 判断主播是否已有本地知识库数据，用于列表按钮“录入/编辑”文案切换。
         * @param {Object} row 主播行数据
         * @returns {boolean}
         */
        hasKnowledgeBaseData(row = {}) {
            const current = this.getKnowledgeBaseFormData(row)
            return ['operationKnowledge', 'sensitiveKnowledge', 'healthKnowledge'].some((key) => String(current?.[key] || '').trim())
        },
        /**
         * @description 获取主播列表中知识库列的按钮文案。
         * @param {Object} row 主播行数据
         * @returns {string}
         */
        getKnowledgeBaseButtonText(row = {}) {
            return this.hasKnowledgeBaseData(row) ? '编辑' : '录入'
        },
        /**
         * @description 打开知识库配置抽屉，当前阶段使用本地假数据做查看与编辑。
         * @param {Object} row 主播行数据
         * @returns {void}
         */
        openKnowledgeBaseDrawer(row = {}) {
            this.currentKnowledgeBaseRow = { ...row }
            const secUid = row?.SecUid
            this.fetchKnowledgeBaseInfo(secUid).finally(() => {
                this.knowledgeBaseDrawerVisible = true
            })
        },
        /**
         * @description 保存主播知识库假数据，并更新列表“录入/编辑”状态。
         * @param {Object} formData 知识库表单数据
         * @returns {void}
         */
        async handleKnowledgeBaseSave(formData = {}) {
            const secUid = this.currentKnowledgeBaseRow?.SecUid
            if (!secUid) {
                this.$message.warning('缺少主播标识，暂无法保存知识库')
                return
            }
            const current = this.knowledgeBaseDraftMap?.[secUid] || {}
            const payload = {
                secUid,
                operationContent: String(formData?.operationKnowledge || '').trim(),
                sensitiveContent: String(formData?.sensitiveKnowledge || '').trim(),
                healthScore: String(formData?.healthKnowledge || '').trim(),
            }
            try {
                if (current?.id) {
                    await this.$httpBack.anchorKnowledge?.update({
                        ...payload,
                        id: current.id
                    })
                } else {
                    await this.$httpBack.anchorKnowledge?.save(payload)
                }
                await this.fetchKnowledgeBaseInfo(secUid)
                this.$message.success('知识库已保存')
            } catch (e) {}
        },
        async fetchKnowledgeBaseInfo(secUid) {
            if (!secUid) return
            try {
                const res = await this.$httpBack.anchorKnowledge?.info({secUid})
                if (res?.code !== 0 || !res?.data) return
                const info = res.data || {}
                const current = this.knowledgeBaseDraftMap?.[secUid] || {}
                this.knowledgeBaseDraftMap = {
                    ...this.knowledgeBaseDraftMap,
                    [secUid]: {
                        ...current,
                        id: info?.id ?? current?.id,
                        operationKnowledge: info?.operationContent ?? info?.operationKnowledge ?? current?.operationKnowledge ?? '',
                        sensitiveKnowledge: info?.sensitiveContent ?? info?.sensitiveKnowledge ?? current?.sensitiveKnowledge ?? '',
                        healthKnowledge: info?.healthScore ?? info?.healthKnowledge ?? current?.healthKnowledge ?? '',
                        updatedAt: Date.now()
                    }
                }
                this.saveKnowledgeBaseDraftMap()
            } catch (e) {}
        },
        _cNotification () {
            this.$CSharpNotify.addTask('addAnchorEnd', (res) => {
                // 成功回调：res 为 data（AnchorInfo），仅 code=0&&status=200 时才触发
                if (!this.addOrUpdateAnchorPending) return
                this.addOrUpdateAnchorPending = false
                this.tardeDialogVisible = false
                this.getDataList()
                this.diagnosisParams = {}
                this.$confirmSuccess({
                    title: '修改成功',
                    message: '基础设置已保存',
                    showCancelButton: false,
                    confirmButtonText: '确定'
                })
            }, (resultData = {}) => {
                if (!this.addOrUpdateAnchorPending) return
                this.addOrUpdateAnchorPending = false
                if (resultData.status === 500) {
                    const codeMsgMap = {
                        70007: '请先关闭AI话术监控功能后，再修改账号归属类型',
                        70002: '授权量不足，请联系产品顾问',
                        70005: '请先确认标准直播稿'
                    }
                    const message = codeMsgMap[resultData.code] || resultData.msg || '修改失败'
                    this.$message.error(message)
                }
            })
        },
        getSessionList (list = []) {
            if (list.length > 1) {
                return list.sort((a, b) => {
                    let aTime = new Date(a.StartRecordDate || 0).getTime()
                    let bTime = new Date(b.StartRecordDate || 0).getTime()
                    return aTime - bTime
                })
            } else {
                return list
            }
        },
        getRatio (rote) {
            return (parseInt((rote || 0) * 1000) / 10).toFixed(1)
        },

        // 每天第一次打开弹窗
        dayFirstOpen () {
            this.appVnode.openDayFirst(() => {
                this.startDetection()
            })
        },
        // 开始录制
        startDetection () {
            this.$nextTick(() => {
                this.$refs.allAnchors.startClick()
            })
        },
        setDiagnosisParams(data){
            this.diagnosisParams = data
        },
        // 基础设置
        tradeConfirm (data) {
            let recordTime = ''
            if (data.recordTime?.length > 0) {
                recordTime = data?.recordTime?.join('-') || '';
            }

            if(data.recordTimes?.length>0){
                recordTime= data?.recordTimes?.map(d=>d.join('-'))?.join(',')
            }
            let requestData = {
                secUid: this.currentAnchorSecUid,
                tradeId: data.tradeId || '1',
                recordTime: recordTime || '',
                anchorSituation: data.anchorSituation,
                accountType: data.accountType,
                smsTip: data.smsTip,
                remarksName: data.remarksName,
                // diagnosisParams:this.diagnosisParams,
                accountStage:data.accountStage,
                accountWaterLevel:data.accountWaterLevel,
                accountFlow:data.accountFlow,
                roiAccuracy: data.roiAccuracy === undefined || data.roiAccuracy === null || data.roiAccuracy === ''
                    ? ''
                    : String(data.roiAccuracy),
                recordDefinition:data.recordDefinition,
                recordLimitValue:data.recordLimitValue === 0 ? -1 : data.recordDuration,
                recordLimitType:data.recordLimitType,
                isAutoAnalysis:data.isAutoAnalysis,
                // 自动删除：仅在 CompereForm 透传（autoDeleteTime !== '-1'）时回写，避免跟随全局/不删除场景丢失或误传
                ...(data.autoDeleteTime === undefined ? {} : {
                    autoDeleteTime: data.autoDeleteTime,
                    deleteContent: data.deleteContent
                }),
                platform:data.platform,
                livingMode: data.livingMode,
                ...pick({...data,...this.diagnosisParams},['livingTarget','marketing','optimizeDirection','premiereDate','engSerViceType','pureRecordOnlineNum']),
                isScheduleRecord:data.isScheduleRecord,
                isStatisticsPerformance:data.isStatisticsPerformance,
                segmentTimePoints:data.segmentTimePoints,
                ...(data.isScriptQualityInspection === undefined ? {} : { isScriptQualityInspection: data.isScriptQualityInspection }),
                ...(data.isScriptFidelityMonitor === undefined ? {} : { isScriptFidelityMonitor: data.isScriptFidelityMonitor }),
                ...(data.isInteractionPatrol === undefined ? {} : { isInteractionPatrol: data.isInteractionPatrol }),
                ...(data.standardScriptId === undefined ? {} : { standardScriptId: data.standardScriptId }),
                ...(data.speechMode === undefined || data.speechMode === null ? {} : { speechMode: data.speechMode }),
                ...(data.speechSpeed === undefined || data.speechSpeed === null ? {} : { speechSpeed: data.speechSpeed }),
                ...(data.referenceScript === undefined ? {} : { referenceScript: data.referenceScript }),
                ...(data.cycleDurationMinutes === undefined || data.cycleDurationMinutes === null ? {} : { cycleDurationMinutes: data.cycleDurationMinutes }),
                ...(Array.isArray(data.timeAxisScript) && data.timeAxisScript.length ? { timeAxisScript: data.timeAxisScript } : {})
            }
            this.addOrUpdateAnchorPending = true
            this.$httpClient.compere.addOrUpdateAnchor(requestData).then((res) => {
                if (res?.code !== 0) {
                    this.addOrUpdateAnchorPending = false
                    this.$message.error(res?.msg || '修改失败')
                    return
                }
                setTimeout(() => {
                    if (!this.addOrUpdateAnchorPending) return
                    this.addOrUpdateAnchorPending = false
                    this.tardeDialogVisible = false
                    this.getDataList()
                    this.diagnosisParams = {}
                    this.$message.success('配置保存成功')
                }, 800)
            }).catch(() => {
                this.addOrUpdateAnchorPending = false
            })
        },
        // 停止计时
        stopHandler () {
            this.detection = this.$store.state.detectionStatus
            // 清除检测时间定时器
            // clearInterval(this.detectionTimeTimer);
            this.detectionTimeTimer = false
        },
        // 打开计时
        startHanlder () {
            this.detection = this.$store.state.detectionStatus
            this.startDetectionTime = this.$store.state.startDetectionTime
            // 创建检测时间定时器
            this.detectionTimeTimer = true
        },
        // 清除列表定时器
        stopListTimer () {
            if (this.listTimer) {
                clearInterval(this.listTimer)
                this.listTimer = null
            }
        },
        // 选择行业回调
        changeTradeHandle () {
            // 关闭级联列表下拉
            this.$refs.tradeCascader.dropDownVisible = false
        },
        // 开启列表定时器
        startListTimer () {
            if (!this.listTimer) {
                clearInterval(this.listTimer)
                this.listTimer = setInterval(() => {
                    // 更新设备信息 30s更新一次，累计超过或等于30s将会获取一次设备信息，并且重置为0；
                    this.getBasinSetupInfoTime++
                    if (this.getBasinSetupInfoTime >= 30) {
                        this.getBasinSetupInfo()
                        this.getBasinSetupInfoTime = 0
                    }
                    // 记录时长计时
                    if (this.detectionTimeTimer && this.$store.state.startDetectionTime) {
                        this.detectionTime = myUtils.toformatTime(Date.now() - this.startDetectionTime)
                    } else {
                        this.detectionTime = 0
                    }
                    //持续检查点击时间锁
                    if (this.$refs.Record?.runClickTimerLock) {
                        this.$refs.Record?.runClickTimerLock()
                    }
                    if (this.isAllTime > 0) {
                        // if (this.isAll && this.isAllTime > 0 && !this.detectionTimeTimer) {
                        this.isAllTime -= 1
                        return
                    }
                    this.isAllTime = 5
                    this.$nextTick(() => {
                        // 获取列表数据
                        this.getDataList()
                    })
                }, 1000)
            }
        },
        tabsClick () {
            this.anchorName = ''
            this.appVnode.getUserproperty()
            this.isAllTime = 5
            this.$nextTick(() => {
                // 获取列表数据
                this.getDataList()
            })
        },

        // 查询主播列表
        keywordChange () {
            // 判断是全部主播还是录制主播
            this.getDataList()
        },
        recordHistory(){
            this.recordHistoryDialogVisible=true
        },
        // 停止录制回调
        stopHandler (bl) {
            if (typeof bl !== 'undefined') {
                this.detection = bl
                this.$refs.loadingStop.show()
                return
            }
            this.$refs.loadingStop.hide()
            this.appVnode.getDisk()
            this.detection = false // this.$store.getters.getDetectionStatus;
            // 清除检测时间定时器
            // clearInterval(this.detectionTimeTimer);
            this.detectionTimeTimer = false

        },
        // 开始录制回调
        startHanlder (bl) {
            if (typeof bl !== 'undefined') {
                this.detection = bl
                return
            }
            this.appVnode.getDisk()
            this.detection = true // this.$store.getters.getDetectionStatus;
            this.startDetectionTime = this.$store.state.startDetectionTime
            // 创建检测时间定时器
            this.detectionTimeTimer = true
        },
        // 获取基本设置信息
        getBasinSetupInfo () {
            this.$httpClient.setup.getmodel({}).then((res) => {
                if (res.code == 0) {
                    this.configInfo = res.data
                    if (this.configInfo.IsRocord == 1) {
                        // 正在检测中
                        this.$store.commit('saveDetectionStatus', true)
                        if (!this.$store.state.startDetectionTime) {
                            this.$store.commit('saveDetectionTime', Date.now())
                        }

                    } else {
                        // 没有在检测
                        this.$store.commit('saveDetectionStatus', false)
                        this.$store.commit('saveDetectionTime', null)

                    }
                    // 设置检测状态
                    this.detection = this.$store.state.detectionStatus
                    this.startDetectionTime = this.$store.state.startDetectionTime
                    if (this.detection && !this.detectionTimeTimer) {
                        this.detectionTimeTimer = true
                    }
                }
            })
        },
        // 开启、关闭自动录制
        autoRecordChange (value, SecUid) {
            let requestData = {
                SecUid,
                isAuto: value ? 1 : 0
            }
            // localStorage.setItem("notCloseLoading", "1");
            this.$httpClient.compere.openorcloseautorecord(requestData, { load: true }).then((res) => {
                if (res.code == 0) {
                    this.$message.success('修改成功')
                }
            })
        },
        // 获取行业列表
        async getTradeList () {
            this.tradeLoading = true
            await this.$httpBack.trade.list({ limit: 99999 }).then((res) => {
                if (res && res.code == 0) {
                    this.tradeList = res.data.list
                    this.tradeList.forEach(item => {
                        this.tradeMap[item.id] = item
                    })
                }
                this.tradeLoading = false
            }).catch((e)=>{}).finally(() => {
                this.tradeLoading = false
            })
        },
        // 获取主播列表
        getDataList () {
            return this.getTabDom?.getCompereList({
                anchorName: this.anchorName
            })
        },
        // 显示基础设置弹窗
        showTrade (data) {
            const {
                SecUid,
                TradeId,
                RecordTime,
                SmsTip,
                accountStage,
                accountWaterLevel,
                AnchorSituation,
                AnchorName,
                RemarksName,
                AccountType,
                isDataDiagnosis,
                juliangAuthStatus,
                dataDiagnosisParams,
                accountFlow,
                platform
            } = data
            if (!SecUid) {
                this.$message?.warning?.('缺少主播标识（secUid），无法从服务端获取基础配置')
            }
            this.currentAnchorSecUid = SecUid
            this.selectTradeId = TradeId || '1'
            this.diagnosisParams = {
                ...dataDiagnosisParams,
                accountType: AccountType,
            };
            // 抖音快手开播监控时间
            let recordTime = '';
            let recordTimes = [];
            if(platform === 2){
                RecordTime.split(',').filter(item => item).forEach(item => {
                    if(item.length){
                        recordTimes.push(item.split('-'))
                    }
                })
            }else{
               recordTime = RecordTime ? RecordTime?.split('-') : ['',''];
            }


            const formData = {
                tradeId: TradeId || '1',
                recordTime: recordTime,
                recordTimes: recordTimes,
                smsTip: SmsTip,
                secUid: SecUid,
                id: SecUid,
                anchorSituation: AnchorSituation,
                accountType: AccountType,
                accountStage: accountStage,
                accountWaterLevel: accountWaterLevel,
                remarksName: RemarksName || AnchorName,
                juliangAuthStatus:juliangAuthStatus,
                accountFlow: accountFlow || 0,
                platform: platform,
                // ...pick(data,['recordDefinition','recordLimitType','recordLimitValue','isAutoAnalysis']),
                ...data
            }

            if (!this.tradeTreeList || this.tradeTreeList.length < 1) {
                this.$httpBack.trade.listTree({}).then((res) => {
                    if (res && res.code === 0) {
                        this.tradeTreeList = res.data
                        // this.tardeDialogVisible = true;
                        this.$refs.basicSettings?.showDialog(formData)
                    }
                })
            } else {
                // this.tardeDialogVisible = true;
                this.$refs.basicSettings?.showDialog(formData)
            }
        },
        openAiReport () {
            this.$refs.ai_content_report.changeDrawerStatus(true)
        },
        /**
         * 初始化数据方法（带节流机制）
         * @param {string} type - 调用类型（mounted/activated）
         * @description 防止mounted和activated同时执行导致的重复初始化
         */
        async initData (type) {
            // 如果正在执行，则忽略后续调用
            if (this.isInitDataExecuting) {
                console.log(`initData已在执行中，忽略${type}调用`)
                return
            }

            // 清除之前的定时器
            if (this.initDataThrottleTimer) {
                clearTimeout(this.initDataThrottleTimer)
            }

            // 设置节流定时器，200ms内只执行一次
            this.initDataThrottleTimer = setTimeout(async () => {
                try {

                    // 获取配置信息
                    await this.getBasinSetupInfo();
                    this.isInitDataExecuting = true
                    // console.log(`开始执行initData，调用类型：${type}`)
                    await this.getTradeList()
                    //刷新用户资产
                    // this.appVnode.getUserproperty();
                    this.setTabsClickCallback(() => {
                        this.tabsClick()
                    })
                    // 开启列表定时器
                    this.startListTimer()
                    // this.APP.buyIn()
                    // 停止录制
                    if (this.$store.state.stopRecord) {
                        this.$store.commit('saveStopRecord', false)
                        this.$httpClient.compere.stopdecector({}).then((res) => {
                            if (res.code == 0) {
                                this.$store.commit('saveDetectionStatus', false)
                                this.$store.commit('saveDetectionTime', null)
                                this.detection = this.$store.state.detectionStatus
                                // 清除检测时间定时器
                                // clearInterval(this.detectionTimeTimer);
                                this.detectionTimeTimer = false
                                this.$message.success('已停止录制')
                            }
                        })
                    }
                    this.$nextTick(async () => {
                        // 获取列表数据
                        await this.getDataList().then(() => {
                            const compereAllList = Object.values(this.compereMapList).flatMap(item => item.list)
                            const unauthorizedList = compereAllList.filter(_item => [2,3,5,6].includes(_item.juliangAuthStatus)&& _item.AccountType === 0)
                            if(unauthorizedList.length !== 0) {
                                if(!myUtils.dailySession.get()){
                                    this.APP.buyIn(unauthorizedList)
                                }
                            }
                            // 有主播列表才会进行录制弹窗，没有则判断是否位新手
                            if (this.compereListLen > 0) {
                                // 每天第一次打开弹窗(登录成功后,设为true;当点击关闭后设为false)
                                if (localStorage.getItem('currentPrompt') === 'true') {
                                    this.dayFirstOpen()
                                }
                            }
                            // 执行新手引导
                            this.appVnode.onTour(this.compereListLen)
                        })
                    })
                } catch (error) {
                    console.error('initData执行出错：', error)
                } finally {
                    this.isInitDataExecuting = false
                }
            }, 200)
        },
        /**
         * 清理初始化相关资源
         * @description 清除定时器和重置状态
         */
        clearInit () {
            // 清除列表定时器
            this.stopListTimer()
            // 清除检测时间定时器
            this.detectionTimeTimer = false
            // 清除节流定时器
            if (this.initDataThrottleTimer) {
                clearTimeout(this.initDataThrottleTimer)
                this.initDataThrottleTimer = null
            }
            // 重置执行状态
            this.isInitDataExecuting = false
            sessionStorage.setItem('notLoading', '')
        }
    },
    created () {
        sessionStorage.setItem('notLoading', '')
        this.loadKnowledgeBaseDraftMap()
        this._cNotification()
    },
    async mounted () {
        await this.initData('mounted')
    },
    beforeCreate () { }, //生命周期 - 创建之前
    beforeMount () { }, //生命周期 - 挂载之前
    beforeUpdate () { }, //生命周期 - 更新之前
    updated () { }, //生命周期 - 更新之后
    beforeDestroy () {
        this.clearInit()
    }, //生命周期 - 销毁之前
    destroyed () { }, //生命周期 - 销毁完成
    async activated () {
        await this.initData('activated')
    }, //如果页面有keep-alive缓存功能，这个函数会触发
    beforeRouteLeave (to, from, next) {
        this.clearInit()
        next()
    }
}
</script>
<style lang='scss' scoped>
.search-box {
    padding-right: 12px;

    ::v-deep(.el-form-item) {
        margin-bottom: 0;
    }
}

::v-deep(.data-analysis-tab-box) {
    .emptyTipText {
        font-weight: 400;
        font-size: 14px;
        color: #677583;
    }

    .emptyContainer {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        height: calc(100vh - 240px);
    }

}
</style>
