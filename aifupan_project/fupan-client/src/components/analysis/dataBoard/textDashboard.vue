<template>
    <div class="text-data-container" v-loading.lock="liveDataLoading">
        <div class="flex items-center data-type">
            <afp-button v-if="!$isWeb&&!notBtn&&isSelectTrade&&isShowBuyIn&&!isAnyAuthorized"
                        size="medium" class="buyIn_btn"
                        @click="openAuthorizeDialog">
                平台授权
            </afp-button>
            <span v-if="!$isWeb&&!notBtn&&isSelectTrade&&isShowBuyIn&&juliangAuthStatus===1"
                  class="auth-status-text">
                巨量已授权
            </span>
            <span v-if="!$isWeb&&!notBtn&&isSelectTrade&&isShowBuyIn&&lifeAuthStatus===1"
                  class="auth-status-text">
                来客已授权
            </span>
        </div>

        <template v-if="renderType==='f'">
            <Dashboard v-if="!liveDataLoading&&statusSuccess" :resData="resData" :span="span"
                       :sentenceMarkData="sentenceMarkData"/>
        </template>

        <template v-if="renderType==='g'">
            <Crowd v-if="!liveDataLoading&&statusSuccess" :resData="resData" :span="span"
                   :sentenceMarkData="sentenceMarkData"/>
        </template>

        <template v-if="renderType==='h'">
            <Flow v-if="!liveDataLoading&&statusSuccess" :resData="resData" :span="span"
                  :sentenceMarkData="sentenceMarkData"/>
        </template>

        <div class="tips-child" v-if="isDouYin||isShipinhao">
            <template v-if="!$isWeb&&!notBtn&&isSelectTrade&&isShowTips&&isEmptyType!==-1">
                <div class="text-xs tips-buyIn" style="color: red;font-weight: 400"
                     v-if="!isAnyAuthorized&&!dataAuthType&&isDouYin">
                    重要提示：自有账号，为了数据分析的准确性，推荐使用AI数据识图或巨量百应授权功能！
                    <span class="cursor-pointer" style="color: var(--color-main)"
                          @click="openAuthorizeDialog()">点我授权</span></div>
                <div class="text-xs tips-buyIn" style="color: red;font-weight: 400"
                     v-if="dataAuthType&&Object.keys(resData)?.length">
                    截至数据时间：{{ resData?.dataStartTime }} 至 {{ resData?.dataEndTime }}
                    <span class="data-time-tip">（总数据会有30秒误差，属于正常现象）</span>
                </div>
            </template>
        </div>

        <div class="last-child" v-if="!notBtn">
            <el-radio-group
                v-if="dataAuthType"
                v-model="dataType"
                size="mini"
                class="data-type-switch"
                :disabled="liveDataLoading"
                @input="dataTypeChange">
                <el-radio-button label="all">
                    <el-tooltip
                        effect="dark"
                        :content="getReplayType === 'replayAll' ? '截止到视频结束时间的汇总数据' : '原视频的数据汇总'"
                        placement="top">
                        <span>
                            {{ getReplayType === 'replayAll' ? '截止数据' : '原视频数据' }}
                        </span>
                    </el-tooltip>
                </el-radio-button>
                <el-radio-button label="section">
                    <el-tooltip
                        effect="dark"
                        :content="getReplayType === 'replayAll' ? '本视频开始到结束时间内的数据，可按主播分段' : '本切片开始到结束时间内的数据'"
                        placement="top">
                        <span>
                            {{ getReplayType === 'replayAll' ? '本段数据' : '本切片数据' }}
                        </span>
                    </el-tooltip>
                </el-radio-button>
            </el-radio-group>
            <div v-if="isRefreshShow && !isExample&&getReplayType === 'replayAll'"
                 class="refresh"
                 @click="refreshClick">
                <span v-if="getDataStatus">获取</span>
                <span v-else>刷新</span>
            </div>
            <!--            <div :class="disabledBtn?['nimble-btn-dis','nimble-btn']:['nimble-btn-use','nimble-btn']"-->
            <!--                 @click="aiAnalysisData">-->
            <!--                <span :style="{color:disabledBtn?'#ABAEB3':'#0077FF'}">AI分析数据</span>-->
            <!--            </div>-->
            <!-- <div
                :class="disabledBtn?['nimble-btn-dis','nimble-btn-dis-long','nimble-btn']:['radio-nimble-btn-use','nimble-btn']"
                v-if="!isWebOnline && !isExample && getReplayType === 'replayAll'"
                @click="selectVideo">
                <span :style="{color:disabledBtn?'#ABAEB3':'#05833A'}">对比上一场数据</span>
            </div> -->
        </div>
        <Empty :isEmptyType="isEmptyType" :buyInStatus="buyInStatus" v-if="!liveDataLoading&&!statusSuccess"/>
        <ContrastLastTime ref="lastTime" :sentenceMarkData="sentenceMarkData"
                          aiType="dataBoard" :targetType="targetType"/>
        <LiveRoomAuthorizeDialog ref="liveRoomAuthorizeDialog" @authorized="handleAuthorized" />
    </div>
</template>

<script>

import {isEmpty} from 'lodash'
import Empty from './empty.vue'
import Crowd from './text/crowd.vue'
import Flow from './text/flow.vue'
import Dashboard from './text/dashboard.vue'
import ContrastLastTime from './../contrastLastTime.vue'
import buyIn from '@/mixins/buyIn'
import exampleMixin from '@/mixins/exampleMixin'
import {PLATFORM_TYPE_ENUM} from "@/enum";
import myUtils from "@/utils/utils";
import LiveRoomAuthorizeDialog from '@/components/liveRoomAuthorizeDialog.vue'
import {getLiveRoomAuthStatus} from '@/utils/liveRoomAuthStatus'

export default {
    components: {Empty, ContrastLastTime, Crowd, Flow, Dashboard, LiveRoomAuthorizeDialog},
    mixins: [buyIn, exampleMixin],
    props: {
        requestId: {
            type: String,
            default: ''
        },
        renderType: {
            type: String,
            default: 'f'
        },
        sentenceMarkData: {
            type: Object,
            default: () => {
            }
        },
        span: {
            type: Number,
            default: 12
        },
        isWebOnline: {
            type: Boolean,
            default: false
        },
        notBtn: {
            type: Boolean,
            default: false
        },
        targetType: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            liveDataLoading: false,
            isEmptyType: '',
            resData: {},
            buyInStatus: 0,
            juliangAuthStatus: 0,
            lifeAuthStatus: 0,
            buyInStatusText: new Map([
                [0, '平台授权'],
                [1, '已授权'],
                [2, '授权过期'],
                [3, '授权失败'],
                [4, '授权中'],
                [5, '授权账号不匹配'],
                [6, '子账号无权限']
            ]),
            dataType: 'section',
            loadEnd: false,
            dataRType: null //只有巨量百应的才展示
        }
    },
    computed: {
        disabledBtn() {
            return (!this.statusSuccess) || this.liveDataLoading || !this.isTenantIdAuthenticated
        },
        getPlatform() {
            return this.sentenceMarkData?.videoInfo?.PlatformType || this.sentenceMarkData?.fileInfo?.platformType
        },
        isDouYin() {
            return this.getPlatform == PLATFORM_TYPE_ENUM.douyin
        },
        isShipinhao() {
            return this.getPlatform == PLATFORM_TYPE_ENUM.shipinhao
        },
        getDataStatus() {
            return [-1, 2, 3, 4, 5, 6, 7].includes(this.isEmptyType) || (isEmpty(this.resData) && this.isEmptyType !== -2)
        },
        isTenantIdAuthenticated() {
            const {videoInfo = {}} = this.sentenceMarkData
            return videoInfo?.TenantId ? this.$auth([videoInfo?.TenantId], 'every') : true
        },
        isAuthenticated() {
            const {videoInfo = {}} = this.sentenceMarkData
            return videoInfo?.UserId ? this.$auth([videoInfo?.UserId, videoInfo?.TenantId], 'every') : true
        },
        isRefreshShow() {
            return this.isTenantIdAuthenticated && !this.liveDataLoading
        },
        isSelectTrade() {
            return this.targetType !== 'online' && this.targetType !== 'webOnline';
        },
        isMoreThan7Days() {
            const {videoInfo} = this.sentenceMarkData
            const {StartTime} = videoInfo || {}
            if (!StartTime) return false;
            const now = new Date();
            const target = new Date(StartTime.replace(/-/g, '/'));

            const diffMs = Math.abs(now - target); // 毫秒差
            const sevenDaysMs = 7 * 24 * 60 * 60 * 1000; // 7天毫秒数
            return diffMs > sevenDaysMs;
        },
        statusSuccess() {
            return [1, 8].includes(this.isEmptyType)
        },
        isShowBuyIn() {
            return this.sentenceMarkData?.anchorInfo?.AccountType === 0 && this.isDouYin
        },
        isAnyAuthorized() {
            return this.juliangAuthStatus === 1 || this.lifeAuthStatus === 1
        },
        isShowTips() {
            return this.sentenceMarkData?.anchorInfo?.AccountType === 0 && (this.isDouYin || this.isShipinhao)
        },
        dataAuthType() {
            return this.dataRType === 1 && this.renderType === 'f'
        },
        getReplayType() {
            return myUtils.getReplayType(this.sentenceMarkData)
        },
    },
    watch: {
        requestId: {
            async handler(newVal) {
                if (newVal) {
                    if (this.isSelectTrade) await this.getBuyInStatus()
                    await this.getDataBoard()
                }
            },
            immediate: true
        }
    },
    methods: {
        async buyInAuthorize() {
            await this.buyInFront(this.sentenceMarkData?.anchorInfo?.SecUid)
        },
        openAuthorizeDialog() {
            this.$refs.liveRoomAuthorizeDialog?.open?.(this.sentenceMarkData?.anchorInfo?.SecUid)
        },
        async handleAuthorized() {
            await this.getBuyInStatus()
        },
        async getDataByBuyIn() {
            try {
                const response = await this.$httpClient.buyIn.pullJuliang({videoId: this.requestId})
                if (response.code !== 0) {
                    this.liveDataLoading = false
                    return this.$message.error(response.msg)
                }
            } catch (e) {
                this.liveDataLoading = false
            }
        },
        async getDataByThird() {
            if (this.getDataStatus) {
                const result = await this.getUserResources()
                if (result?.dataBoardNum > 0) {
                    return this.getCreateDataViewing(this.requestId)
                } else {
                    this.isEmptyType = 5
                    return this.$message.error('数据看板资源不足')
                }
            } else {
                await this.getDataBoard()
            }
        },
        async refreshClick() {
            if (!this.isTenantIdAuthenticated) return
            if (this.liveDataLoading) return
            this.liveDataLoading = true
            if (this.isSelectTrade) {
                await this.getBuyInStatus();
                if (!this.isMoreThan7Days && this.juliangAuthStatus === 1) {
                    await this.getDataByBuyIn();
                    return;
                }
            }

            await this.getDataByThird();
        },
        async getUserResources() {
            return await this.$httpBack.userProperty.getUserProperty()
        },
        aiAnalysisData() {
            if (this.disabledBtn) return
            this.$emit('aiAnalysisData', {type: 'dataBoard'})
        },
        selectVideo() {
            if (this.disabledBtn) return
            this.$refs.lastTime?.openDialog()
        },
        getCreateDataViewing(requestId) {
            this.liveDataLoading = true
            this.$httpBack.v2400.createDataViewing({
                videoId: requestId
            }).then(res => {
            }).finally(() => {
                this.liveDataLoading = false
                // this.getDataBoard(requestId)
                this.getInfoByVideoId(requestId)
            })
        },
        isNil(status) {
            return status === null || status === undefined || status === ''
        },
        async getBuyInStatus() {
            if (this.notBtn) return
            if (!this.isSelectTrade) return;
            if (!this.sentenceMarkData?.anchorInfo?.SecUid) {
                this.liveDataLoading = false
                this.$message.error('获取到主播信息失败，请稍后再试')
            }
            try {
                const result = await getLiveRoomAuthStatus(this.sentenceMarkData?.anchorInfo?.SecUid)
                if (result.code === 0 && result?.authStatus) {
                    this.juliangAuthStatus = result.authStatus.juliangAuthStatus || 0
                    this.lifeAuthStatus = result.authStatus.lifeAuthStatus || 0
                    this.buyInStatus = this.juliangAuthStatus
                } else {
                    this.liveDataLoading = false
                }
            } catch (e) {
                this.liveDataLoading = false
            }
        },
        dataTypeChange() {
            this.getDataBoard(this.dataType)
        },
        async getParagraphDataBoard(requestId) {
            this.liveDataLoading = true
            try {
                const result = await this.$httpBack.words.paragraphInfoByVideoId({videoId: requestId})
                this.liveDataLoading = false
                if (result.code === 0) {
                    const resData = result.data || {}
                    this.dataRType = resData?.dataSourceType
                    const status = resData?.dataStatus
                    this.isEmptyType = !this.isNil(status) ? status : -1
                    if (!result.data) {
                        return {resData, status: 'success'}
                    }
                    this.resData = resData
                    return {resData, status: 'success'}
                }
                return {resData: null, status: 'error'}
            } catch (e) {
                this.liveDataLoading = false
                this.isEmptyType = -2
                return {resData: null, status: 'error'}
            }
        },
        async getDataBoard(dataType) {
            this.dataType = dataType || 'section'
            const videoSliceType = this.sentenceMarkData?.videoInfo?.videoSliceType
            if (videoSliceType === 1) { //切片数据
                let result1 = {
                    status: 'success'
                }
                if (dataType === 'section' || !dataType) {
                    result1 = await this.getParagraphDataBoard(this.requestId)
                }

                if (result1.status === 'success' && isEmpty(result1.resData)) {
                    this.dataType = 'all'
                    const parentVideoId = this.sentenceMarkData?.videoInfo?.parentVideoInfo?.videoId
                    if (!parentVideoId) return this.$message.error('获取原视频信息失败，无法查看原视频数据')
                    let result2 = await this.getParagraphDataBoard(parentVideoId)
                    if (result2.status === 'success' && isEmpty(result2.resData)) {
                        await this.getInfoByVideoId(this.requestId)
                    }
                    if (dataType === 'section' && this.dataRType === 1) this.$message.warning('当前数据暂无，已自动切换数据查看')
                }
            } else {
                if (this.dataType === 'all') {
                    await this.getInfoByVideoId(this.requestId)
                } else {
                    let result1 = await this.getParagraphDataBoard(this.requestId)
                    if (result1.status === 'success' && isEmpty(result1.resData)) {
                        this.dataType = 'all'
                        await this.getInfoByVideoId(this.requestId)
                        if (dataType === 'section' && this.dataRType === 1) this.$message.warning('当前数据暂无，已自动切换数据查看')
                    }
                }
            }
        },
        async getInfoByVideoId(videoId) {
            this.liveDataLoading = true
            try {
                const result = await this.$httpBack.v2400.infoByVideoId({videoId: videoId})
                this.liveDataLoading = false
                if (result.code === 0) {
                    const resData = result.data || {}
                    this.dataRType = resData?.dataSourceType
                    const status = resData?.dataStatus
                    this.isEmptyType = !this.isNil(status) ? status : -1
                    if (!result.data) {
                        return {resData, status: 'success'}
                    }
                    this.resData = resData
                    return {resData, status: 'success'}
                }
                return {resData: null, status: 'error'}
            } catch (e) {
                this.isEmptyType = -2
                this.liveDataLoading = false
                return {resData: null, status: 'error'}
            }
        },
        /** buyIn 弹窗关闭时停止外层 loading */
        onBuyInDialogClosed() {
            this.liveDataLoading = false
        }
    },
    created() {

    },
    mounted() {
        this.watchAuthorizedBuyInSuccess(() => {
            this.juliangAuthStatus = 1
            this.buyInStatus = 1
        })
        this.watchAuthorizedBuyInError()
        this.watchAuthorizedBuyInPullDataSuccess(() => {
            this.getDataBoard()
        })
        /** 监听 buyIn 弹窗关闭事件，关闭时停止外层 loading */
        this.$root.$on('buyIn-dialog-closed', this.onBuyInDialogClosed)
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
        this.$root.$off('buyIn-dialog-closed', this.onBuyInDialogClosed)
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss'>
.text-data-container {
    min-height: 186px;
    margin: 6px 20px;
    position: relative;

    .data-type {
        position: absolute;
        right: 38px;
        top: -42px;
        z-index: 1000;

        .el-icon-warning {
            color: #E6A23C;
            font-size: 14px;
            margin-left: 6px;
        }
    }

    .el-loading-mask {
        background-color: transparent;
    }

    .buyIn_btn {
        margin-left: 24px;
        padding-inline: 18px !important;
        background: transparent !important;
    }

    .auth-status-text {
        margin-left: 24px;
        color: #28BD6C;
        font-size: 14px;
        font-weight: 500;
    }

    .buy_in_selected {
        color: #28BD6C !important;
        border-color: #28BD6C !important;
        background-color: rgba(40, 189, 108, 0.05);
    }

    .text-data {
        display: flex;
        flex-flow: row wrap;
        align-content: flex-start;

        .flows {
            position: relative;
            //height: 90px;
            .after {
                font-weight: 400;
                color: #7D7F82;
            }

            .child-data {
                display: flex;
                align-items: flex-start;
                justify-content: flex-start;
                font-weight: 500;
                min-height: 50px;
                font-size: 14px;
                margin: 8px 0;

                .child {
                    padding: 0 18px;

                    .title {
                        color: #7A7C80;
                        white-space: nowrap;
                        padding: 0 5px 5px 5px;
                    }

                    .value {
                        white-space: nowrap;
                        font-weight: 800;
                        padding: 5px;
                    }
                }
            }

            .header-title {
                padding-left: 8px;

                &::after {
                    content: ' ';
                    position: absolute;
                    left: 0;
                    top: 2px;
                    width: 4px;
                    height: 16px;
                    background: #05D0FF;
                    border-radius: 42px;
                }
            }
        }
    }
}

.tips-child {
    display: flex;
    align-items: flex-end;
    justify-content: flex-end;
    width: 100%; //50%;
    position: absolute;
    left: 0;
    bottom: 8px;
    z-index: 99;

    .tips-buyIn {
        position: absolute;
        // width: 540px;
        left: 0;
        top: -2px;

        .data-time-tip {
            color: #909399;
            font-size: 12px;
            font-weight: 400;
        }
    }
}

.last-child {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    width: 340px; //50%;
    position: absolute;
    right: 38px;
    bottom: 14px;
    z-index: 99;

    .data-type-switch {
        margin-right: 12px;
    }

    .refresh {
        background: transparent;
        color: var(--color-main);
        margin-inline: 5px;
        cursor: pointer;
        border: 0.5px var(--color-main) solid;
        padding: 4px 15px;
        font-weight: 500;
        font-size: 13px;
        border-radius: 28px;
    }

    .disBtn {
        color: #B5AEBE;
        border-color: #B5AEBE;

        &:hover {
            border-color: #B5AEBE;
            color: #B5AEBE;
        }
    }
}
</style>
