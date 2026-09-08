<!--
@description 详情页投放数据 Tab：读取单场 ROI 汇总接口，支持空态展示与千川授权入口。
-->
<template>
    <div class="roi-data-box" v-loading.lock="dataLoading">
        <div class="roi-header">
            <afp-button
                v-if="showQianchuanAuthButton"
                size="medium"
                class="qianchuan-btn"
                @click="authorizeQianchuanHandler">
                千川授权
            </afp-button>
            <span v-else-if="showQianchuanAuthorizedText" class="qianchuan-authorized-text">
                千川已授权
            </span>
        </div>
        <div v-if="!dataLoading && canShowRoiMetrics" class="roi-strip-panel">
            <div class="roi-strip-row">
                <div class="roi-strip-col roi-strip-col-first">
                    <div class="roi-strip-lab">整体消耗</div>
                    <div class="roi-strip-amount">
                        <span class="roi-strip-amount-value">{{ formatAmountValue(roiData.launchRoiAmount) }}</span>
                        <span v-if="shouldShowUnit(roiData.launchRoiAmount)" class="roi-strip-amount-unit">元</span>
                    </div>
                </div>
                <div class="roi-strip-col roi-strip-col-mid">
                    <div class="roi-strip-lab">成交单量</div>
                    <div class="roi-strip-amount">
                        <span class="roi-strip-amount-value">{{ formatIntValue(roiData.payCount) }}</span>
                        <span v-if="shouldShowUnit(roiData.payCount)" class="roi-strip-amount-unit">单</span>
                    </div>
                </div>
                <div class="roi-strip-col roi-strip-col-mid">
                    <div class="roi-strip-lab">整体成交金额</div>
                    <div class="roi-strip-amount">
                        <span class="roi-strip-amount-value">{{ formatAmountValue(roiData.salesAmount) }}</span>
                        <span v-if="shouldShowUnit(roiData.salesAmount)" class="roi-strip-amount-unit">元</span>
                    </div>
                </div>
                <div class="roi-strip-col roi-strip-col-mid">
                    <div class="roi-strip-lab">净成交金额</div>
                    <div class="roi-strip-amount">
                        <span class="roi-strip-amount-value">{{ formatAmountValue(roiData.netTransactionAmount) }}</span>
                        <span v-if="shouldShowUnit(roiData.netTransactionAmount)" class="roi-strip-amount-unit">元</span>
                    </div>
                </div>
                <div class="roi-strip-col roi-strip-col-mid">
                    <div class="roi-strip-lab">千次成交GPM</div>
                    <div class="roi-strip-amount">
                        <span class="roi-strip-amount-value">{{ formatAmountValue(roiData.gpm) }}</span>
                        <span v-if="shouldShowUnit(roiData.gpm)" class="roi-strip-amount-unit">元</span>
                    </div>
                </div>
                <div class="roi-strip-col roi-strip-col-mid">
                    <div class="roi-strip-lab">整体支付ROI</div>
                    <div class="roi-strip-roi">
                        <span class="roi-strip-roi-value">{{ formatRoiValue(roiData.overallCostRoi) }}</span>
                    </div>
                </div>
                <div class="roi-strip-col roi-strip-col-last">
                    <div class="roi-strip-lab roi-strip-lab-with-tip">
                        <span>净成交ROI</span>
                        <el-tooltip effect="dark" placement="top">
                            <div slot="content" class="roi-tooltip-content">
                                <div>净成交ROI=本段净成交÷本段投放消耗</div>
                                <div>本段净成交指的是本段开始到最后一分钟的成交减去退款，下一段或下播后的退款不计入本数据</div>
                            </div>
                            <i class="el-icon-question roi-strip-tip-icon"></i>
                        </el-tooltip>
                    </div>
                    <div class="roi-strip-roi">
                        <span class="roi-strip-roi-value">{{ formatRoiValue(roiData.netTransactionRoi) }}</span>
                        <span v-if="shouldShowCoreTag()" class="roi-strip-roi-tag">核心</span>
                    </div>
                </div>
            </div>
        </div>
        <div v-else-if="!dataLoading" class="empty-box">
            <img src="@/assets/imgs/chartEmpty.png" alt="">
            <div class="empty-text">暂无数据</div>
        </div>
        <div class="delivery-tip" style="color:#999;font-size:12px;margin-top:16px;padding:0 16px;">
            为避免数据口径混乱，<span class="text-colorErr">目前仅获取录制（下播）最后一刻的数据</span>，数据大屏的数据会变动是因为部分用户在下播后支付或退款。
        </div>
    </div>
</template>

<script>
/**
 * @description 详情页投放数据面板：按视频维度读取 ROI 汇总数据，除纯净版外由外层 Tab 控制显示。
 */
import { PLATFORM_TYPE_ENUM } from '@/enum'

export default {
    name: 'RoiData',
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => ({})
        },
        targetType: {
            type: String,
            default: ''
        },
        isWebOnline: {
            type: Boolean,
            default: false
        },
        roiResponse: {
            type: Object,
            default: null
        },
        roiLoading: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
        }
    },
    computed: {
        dataLoading() {
            return !!this.roiLoading
        },
        rawRoi() {
            return this.roiResponse || null
        },
        roiData() {
            return this.normalizeRoiData(this.rawRoi || {})
        },
        qcAuthStatus() {
            const val = this.rawRoi?.qcAuthStatus
            if (val === undefined || val === null || val === '') return null
            const num = Number(val)
            return Number.isFinite(num) ? num : null
        },
        jlbyAuthStatus() {
            const val = this.rawRoi?.jlbyAuthStatus
            if (val === undefined || val === null || val === '') return null
            const num = Number(val)
            return Number.isFinite(num) ? num : null
        },
        hasData() {
            if (!this.rawRoi) return false
            if (this.rawRoi?.hasData === true) return true
            if (this.rawRoi?.hasData === false) return false
            return this.metricList.some(item => {
                const value = this.roiData?.[item.key]
                return value !== undefined && value !== null && value !== ''
            })
        },
        videoId() {
            return this.sentenceMarkData?.videoInfo?.VideoId || this.sentenceMarkData?.videoInfo?.videoId || ''
        },
        secUid() {
            return this.sentenceMarkData?.anchorInfo?.SecUid || this.sentenceMarkData?.anchorInfo?.secUid || ''
        },
        platformType() {
            return this.sentenceMarkData?.videoInfo?.PlatformType
                || this.sentenceMarkData?.fileInfo?.platformType
                || this.sentenceMarkData?.anchorInfo?.platform
                || ''
        },
        isDouyin() {
            return String(this.platformType) === String(PLATFORM_TYPE_ENUM.douyin)
        },
        isSelfAnchor() {
            return Number(this.sentenceMarkData?.anchorInfo?.AccountType) === 0
        },
        canOpenQianchuanAuth() {
            if (this.isWebOnline) return false
            if (this.$isWeb) return false
            return !!this.$httpClient?.qianchuan?.authorizeQianchuan
        },
        showQianchuanAuthButton() {
            return this.isDouyin
                && this.isSelfAnchor
                && !!this.secUid
                && !this.dataLoading
                && this.qcAuthStatus !== 1
                && this.canOpenQianchuanAuth
        },
        showQianchuanAuthorizedText() {
            return this.isDouyin
                && this.isSelfAnchor
                && !!this.secUid
                && !this.dataLoading
                && this.qcAuthStatus === 1
        },
        metricList() {
            return [
                { key: 'launchRoiAmount', label: '整体消耗', unit: '元' },
                { key: 'payCount', label: '成交单量', unit: '单' },
                { key: 'salesAmount', label: '整体成交金额', unit: '元' },
                { key: 'overallCostRoi', label: '整体支付ROI', unit: '' },
                { key: 'netTransactionAmount', label: '净成交金额', unit: '元' },
                { key: 'gpm', label: '千次成交GPM', unit: '元' },
                { key: 'netTransactionRoi', label: '净成交ROI', unit: '' }
            ]
        },
        canShowRoiMetrics() {
            return this.hasData
        }
    },
    methods: {
        /**
         * @description 兼容 ROI 汇总接口的主字段与可能的别名字段，避免字段调整后页面整体空白。
         * @param {Object} rawData 原始响应数据
         * @returns {Object}
         */
        normalizeRoiData(rawData = {}) {
            return {
                launchRoiAmount: rawData.launchRoiAmount ?? rawData.totalQianchuanCost ?? null,
                payCount: rawData.payCount ?? rawData.transactionCount ?? rawData.totalTransactionCount ?? null,
                salesAmount: rawData.salesAmount ?? rawData.totalSalesAmount ?? null,
                overallCostRoi: rawData.overallCostRoi ?? rawData.totalOverallCostRoi ?? null,
                netTransactionAmount: rawData.netTransactionAmount ?? rawData.totalNetTransactionAmount ?? null,
                gpm: rawData.gpm ?? rawData.totalGpm ?? null,
                netTransactionRoi: rawData.netTransactionRoi ?? rawData.totalNetTransactionRoi ?? null,
            }
        },
        /**
         * @description 打开千川授权流程，复用主播列表中的授权二维码链路。
         * @returns {Promise<void>}
         */
        async authorizeQianchuanHandler() {
            if (this.isWebOnline || this.$isWeb) {
                this.$message?.warning?.('云空间暂不支持千川授权，请前往客户端操作')
                return
            }
            if (!this.secUid) {
                this.$message?.error?.('获取主播信息失败，请稍后再试')
                return
            }
            if (!this.$httpClient?.qianchuan?.authorizeQianchuan) {
                this.$message?.error?.('当前环境暂不支持千川授权')
                return
            }
            const res = await this.$httpClient.qianchuan.authorizeQianchuan({ secUid: this.secUid })
            if (res?.code !== 0) {
                this.$message?.error?.(res?.msg || '千川授权失败')
            }
        },
        formatAmountValue(value) {
            if (value === undefined || value === null || value === '') return '-'
            const num = Number(value)
            if (Number.isNaN(num)) return String(value)
            return num.toLocaleString('zh-CN', {
                minimumFractionDigits: 0,
                maximumFractionDigits: 2
            })
        },
        formatIntValue(value) {
            if (value === undefined || value === null || value === '') return '-'
            const num = Number(value)
            if (Number.isNaN(num)) return String(value)
            return num.toLocaleString('zh-CN', {
                minimumFractionDigits: 0,
                maximumFractionDigits: 0
            })
        },
        formatRoiValue(value) {
            if (value === undefined || value === null || value === '') return '-'
            const num = Number(value)
            if (Number.isNaN(num)) return String(value)
            return num.toLocaleString('zh-CN', {
                minimumFractionDigits: 0,
                maximumFractionDigits: 2
            })
        },
        shouldShowUnit(value) {
            return value !== undefined && value !== null && value !== ''
        },
        shouldShowCoreTag() {
            return this.roiData?.netTransactionRoi !== undefined
                && this.roiData?.netTransactionRoi !== null
                && this.roiData?.netTransactionRoi !== ''
        }
    }
}
</script>

<style lang="scss" scoped>
.roi-data-box {
    min-height: 160px;
    margin: 4px 20px 0;
    position: relative;
}

.roi-header {
    position: absolute;
    top: -40px;
    right: 0;
    display: flex;
    align-items: center;
}

.qianchuan-btn {
    flex-shrink: 0;
    height: 30px;
    padding: 0 14px;
    border-radius: 16px;
    border: 1px solid #5B6CFF;
    color: #5B6CFF;
    background: #FFFFFF;
    font-size: 12px;
    line-height: 18px;
}

.qianchuan-authorized-text {
    flex-shrink: 0;
    color: #61B593;
    font-size: 14px;
    line-height: 22px;
    font-weight: 500;
}

.roi-strip-panel {
    border-radius: 14px;
    background: #e7f5ff;
    border: 1px solid #d3e8f8;
    padding: 22px 26px;
}

.roi-strip-row {
    display: flex;
    align-items: center;
}

.roi-strip-col {
    flex: 1;
    display: flex;
    flex-direction: column;
}

.roi-strip-col-first {
    padding: 0 22px 0 0;
}

.roi-strip-col-mid {
    padding: 0 22px;
    border-left: 1px solid #cfe4f6;
}

.roi-strip-col-last {
    padding: 0 0 0 22px;
    border-left: 1px solid #cfe4f6;
}

.roi-strip-lab {
    font-size: 12px;
    color: #7a808e;
}

.roi-strip-lab-with-tip {
    display: inline-flex;
    align-items: center;
}

.roi-strip-tip-icon {
    margin-left: 4px;
    font-size: 13px;
    color: #a6acb9;
    cursor: pointer;
}

.roi-strip-amount {
    display: flex;
    align-items: baseline;
    gap: 3px;
    margin-top: 6px;
}

.roi-strip-amount-value {
    font-size: 28px;
    font-weight: 800;
    color: #2b2b3a;
}

.roi-strip-amount-unit {
    font-size: 13px;
    color: #9aa0ac;
    font-weight: 600;
}

.roi-strip-roi {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 6px;
}

.roi-strip-roi-value {
    font-size: 28px;
    font-weight: 800;
    color: #e08a1e;
}

.roi-strip-roi-tag {
    font-size: 11px;
    font-weight: 700;
    color: #e08a1e;
    background: #fff4e0;
    padding: 2px 7px;
    border-radius: 20px;
}

.roi-tooltip-content {
    max-width: 320px;
    line-height: 20px;
    white-space: normal;
}

.empty-box {
    min-height: 170px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #95A1AF;
    border-radius: 4px;
}

.empty-box img {
    max-height: 160px;
}

.empty-text {
    margin-top: 12px;
}
</style>
