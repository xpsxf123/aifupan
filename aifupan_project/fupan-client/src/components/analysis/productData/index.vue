<template>
    <div class="productData main-bg brs-10 pd-12">
        <div v-if="loading" class="productData-loading" v-loading="loading"></div>
        <template v-else>
            <div v-if="hasProducts" class="productData-content">
                <div class="productData-toolbar">
                    <div class="productData-toolbar__left">
                        <el-input
                            v-model="keyword"
                            placeholder="请输入商品关键信息搜索"
                            class="productData-search"
                            clearable
                            size="small"
                        />
                        <el-button class="productData-btn productData-btn--query" size="small" type="primary" plain @click="handleSearch">查询</el-button>
                    </div>
                    <div class="productData-toolbar__right">
                        <div class="productData-note">*仅展示成交金额前50的商品</div>
                        <TableColumnSetting v-model="columnVisibleMap" :options="columnOptions" />
                    </div>
                </div>

                <el-table
                    :data="displayRecords"
                    class="productData-table"
                    :header-cell-style="{ background: '#F7F8FA' }"
                    @sort-change="onSortChange"
                >
                    <el-table-column label="商品信息" min-width="220">
                        <template slot-scope="{ row }">
                            <div class="productData-product">
                                <img v-if="row.imageUri" :src="row.imageUri" class="productData-product__img" />
                                <div class="productData-product__title text-clamp2">{{ row.title || '-' }}</div>
                            </div>
                        </template>
                    </el-table-column>

                    <el-table-column v-if="columnVisibleMap.productShowUcnt" prop="productShowUcnt" label="人数" min-width="80" align="center" sortable="custom">
                        <template slot="header" slot-scope="scope">
                            <div class="productData-headerTwoLine">
                                <div v-for="(part, idx) in getHeaderParts('productShowUcnt')" :key="idx">{{ part }}</div>
                            </div>
                        </template>
                    </el-table-column>
                    <el-table-column v-if="columnVisibleMap.productViewShowRatio" prop="productViewShowRatio" label="曝光率" min-width="120" align="center" sortable="custom">
                        <template slot="header" slot-scope="scope">
                            <div class="productData-headerTwoLine">
                                <div v-for="(part, idx) in getHeaderParts('productViewShowRatio')" :key="idx">{{ part }}</div>
                            </div>
                        </template>
                        <template slot-scope="{ row }">{{ formatPercent(row.productViewShowRatio) }}</template>
                    </el-table-column>
                    <el-table-column v-if="columnVisibleMap.explainCnt" prop="explainCnt" label="讲解次数" min-width="80" align="center" sortable="custom">
                        <template slot="header" slot-scope="scope">
                            <div class="productData-headerTwoLine">
                                <div v-for="(part, idx) in getHeaderParts('explainCnt')" :key="idx">{{ part }}</div>
                            </div>
                        </template>
                    </el-table-column>
                    <el-table-column v-if="columnVisibleMap.productClickUcnt" prop="productClickUcnt" label="点击人数" min-width="80" align="center" sortable="custom">
                        <template slot="header" slot-scope="scope">
                            <div class="productData-headerTwoLine">
                                <div v-for="(part, idx) in getHeaderParts('productClickUcnt')" :key="idx">{{ part }}</div>
                            </div>
                        </template>
                    </el-table-column>
                    <el-table-column v-if="columnVisibleMap.productShowClickUcntRatio" prop="productShowClickUcntRatio" label="商品点击率" min-width="90" align="center" sortable="custom">
                        <template slot="header" slot-scope="scope">
                            <div class="productData-headerTwoLine">
                                <div v-for="(part, idx) in getHeaderParts('productShowClickUcntRatio')" :key="idx">{{ part }}</div>
                            </div>
                        </template>
                        <template slot-scope="{ row }">{{ formatPercent(row.productShowClickUcntRatio) }}</template>
                    </el-table-column>
                    <el-table-column v-if="columnVisibleMap.productClickPayUcntRatio" prop="productClickPayUcntRatio" label="点击成交转化率" min-width="120" align="center" sortable="custom">
                        <template slot="header" slot-scope="scope">
                            <div class="productData-headerTwoLine">
                                <div v-for="(part, idx) in getHeaderParts('productClickPayUcntRatio')" :key="idx">{{ part }}</div>
                            </div>
                        </template>
                        <template slot-scope="{ row }">{{ formatPercent(row.productClickPayUcntRatio) }}</template>
                    </el-table-column>
                    <el-table-column v-if="columnVisibleMap.avgPayAmtPerOrder" prop="avgPayAmtPerOrder" label="成交单价" min-width="80" align="center" sortable="custom">
                        <template slot="header" slot-scope="scope">
                            <div class="productData-headerTwoLine">
                                <div v-for="(part, idx) in getHeaderParts('avgPayAmtPerOrder')" :key="idx">{{ part }}</div>
                            </div>
                        </template>
                        <template slot-scope="{ row }">{{ formatNumber(row.avgPayAmtPerOrder) }}</template>
                    </el-table-column>
                    <el-table-column v-if="columnVisibleMap.payCnt" prop="payCnt" label="订单量" min-width="80" align="center" sortable="custom">
                        <template slot="header" slot-scope="scope">
                            <div class="productData-headerTwoLine">
                                <div v-for="(part, idx) in getHeaderParts('payCnt')" :key="idx">{{ part }}</div>
                            </div>
                        </template>
                    </el-table-column>
                    <el-table-column v-if="columnVisibleMap.payAmt" prop="payAmt" label="成交金额" min-width="80" align="center" sortable="custom">
                        <template slot="header" slot-scope="scope">
                            <div class="productData-headerTwoLine">
                                <div v-for="(part, idx) in getHeaderParts('payAmt')" :key="idx">{{ part }}</div>
                            </div>
                        </template>
                        <template slot-scope="{ row }">{{ formatNumber(row.payAmt) }}</template>
                    </el-table-column>
                </el-table>

                <div class="productData-pagination">
                    <el-pagination
                        background
                        layout="total, sizes, prev, pager, next, jumper"
                        :current-page="page"
                        :page-size="limit"
                        :page-sizes="[10, 20, 50]"
                        :total="total"
                        @current-change="onPageChange"
                        @size-change="onSizeChange"
                    />
                </div>
            </div>

            <div v-else class="productData-empty">
                <img :src="emptyImgSrc" class="productData-empty__img" />
                <div v-if="emptyStatus === 'needAuth'" class="productData-empty__text">
                    暂无数据，请授权巨量百应/来客，查看更多商品数据，
                    <span class="productData-empty__link" @click="handleBuyInAuth">点我授权</span>
                </div>
                <div v-else-if="emptyStatus === 'needPull'" class="productData-empty__text">
                    您已完成巨量百应授权，但尚未拉取商品，
                    <span class="productData-empty__link" @click="handlePullProduct">点击获取数据</span>
                </div>
                <div v-else class="productData-empty__text">
                    本场直播未上架商品，暂无商品数据
                </div>
            </div>
        </template>

        <LiveRoomAuthorizeDialog ref="liveRoomAuthorizeDialog" @authorized="handleLifeAuthorized" />
    </div>
</template>

<script>
import buyIn from '@/mixins/buyIn'
import { getLiveRoomAuthStatus } from '@/utils/liveRoomAuthStatus'
import TableColumnSetting from '@/components/tableColumnSetting/index.vue'
import LiveRoomAuthorizeDialog from '@/components/liveRoomAuthorizeDialog.vue'

export default {
    name: 'ProductDataTab',
    components: { TableColumnSetting, LiveRoomAuthorizeDialog },
    mixins: [buyIn],
    props: {
        textData: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            loading: false,
            page: 1,
            limit: 10,
            total: 0,
            pullStatus: false,
            records: [],
            sortBy: 'payAmt',
            sortOrder: 'desc',
            keyword: '',
            /** 列定义（label 用数组存储表头多行文本，与模板 header 插槽保持一致） */
            columnDefs: [
                { prop: 'productShowUcnt', label: ['观看', '人数'] },
                { prop: 'productViewShowRatio', label: ['商品', '曝光率'] },
                { prop: 'explainCnt', label: ['讲解', '次数'] },
                { prop: 'productClickUcnt', label: ['点击', '人数'] },
                { prop: 'productShowClickUcntRatio', label: ['商品', '点击率'] },
                { prop: 'productClickPayUcntRatio', label: ['点击成交', '转化率'] },
                { prop: 'avgPayAmtPerOrder', label: ['成交', '单价'] },
                { prop: 'payCnt', label: ['订单量'] },
                { prop: 'payAmt', label: ['成交', '金额'] }
            ],
            columnVisibleMap: {
                productShowUcnt: true,
                productViewShowRatio: true,
                explainCnt: false,
                productClickUcnt: true,
                productShowClickUcntRatio: true,
                productClickPayUcntRatio: true,
                avgPayAmtPerOrder: true,
                payCnt: true,
                payAmt: true
            },
            juliangAuthStatus: 0,
            lifeAuthStatus: 0,
            emptyImgSrc: require('@/assets/imgs/2_6_2/zwsj.png')
        }
    },
    computed: {
        secUid() {
            return this.textData?.anchorInfo?.SecUid || this.textData?.anchorInfo?.secUid || ''
        },
        batchNumber() {
            return this.textData?.videoInfo?.BatchNumber || this.textData?.videoInfo?.batchNumber || ''
        },
        videoId() {
            return this.textData?.videoInfo?.VideoId || this.textData?.videoInfo?.videoId || ''
        },
        startTime() {
            return this.textData?.videoInfo?.StartTime || this.textData?.videoInfo?.startTime || ''
        },
        endTime() {
            return this.textData?.videoInfo?.EndTime || this.textData?.videoInfo?.endTime || ''
        },
        hasProducts() {
            return Array.isArray(this.records) && this.records.length > 0
        },
        isAuthorized() {
            return Number(this.juliangAuthStatus) === 1 || Number(this.lifeAuthStatus) === 1
        },
        emptyStatus() {
            if (this.hasProducts) return 'hasData'
            if (!this.isAuthorized) return 'needAuth'
            if (!this.pullStatus) return 'needPull'
            return 'noGoods'
        },
        displayRecords() {
            return Array.isArray(this.records) ? this.records : []
        },
        /** 列表配置选项：从 columnDefs 派生，label 数组 join 为单行文本 */
        columnOptions() {
            return this.columnDefs.map(item => ({
                prop: item.prop,
                label: Array.isArray(item.label) ? item.label.join('') : item.label
            }))
        },
        /** 当前可见的可切换列数量 */
        visibleToggleColumnCount() {
            return this.columnDefs.filter(d => this.columnVisibleMap[d.prop]).length
        },
        /** 可见列 >= 7 时使用两行表头，否则合并为一行 */
        shouldUseTwoLine() {
            return this.visibleToggleColumnCount >= 7
        }
    },
    watch: {
        batchNumber: {
            handler() {
                this.resetAndLoad()
            },
            immediate: true
        }
    },
    methods: {
        normalizeImageUri(uri) {
            if (!uri) return ''
            return String(uri).trim().replace(/^`+/, '').replace(/`+$/, '').trim()
        },
        normalizeProductPageData(res) {
            if (Number(res?.code) !== 0) return null
            let data = res?.data
            if (data && typeof data === 'object' && data.data) data = data.data

            if (Array.isArray(data)) {
                return {
                    pullStatus: data.length > 0,
                    total: data.length,
                    records: data
                }
            }

            const records = Array.isArray(data?.records)
                ? data.records
                : Array.isArray(data?.list)
                    ? data.list
                    : []
            const pullStatus = data?.pullStatus === undefined ? records.length > 0 : Boolean(data.pullStatus)
            const total = Number(data?.total ?? data?.totalCount ?? records.length ?? 0)
            return {
                pullStatus,
                total,
                records
            }
        },
        async resetAndLoad() {
            this.page = 1
            this.total = 0
            this.records = []
            this.pullStatus = false
            await this.loadProducts()
            if (!this.hasProducts) {
                await this.loadAuthStatus()
            } else {
                this.juliangAuthStatus = 1
                this.lifeAuthStatus = 0
            }
        },
        async loadAuthStatus() {
            if (!this.secUid) {
                this.juliangAuthStatus = 0
                this.lifeAuthStatus = 0
                return
            }
            const res = await getLiveRoomAuthStatus(this.secUid)
            const juliangStatus = res?.authStatus?.juliangAuthStatus ?? 0
            const lifeStatus = res?.authStatus?.lifeAuthStatus ?? 0
            this.juliangAuthStatus = Number(juliangStatus) || 0
            this.lifeAuthStatus = Number(lifeStatus) || 0
        },
        async loadProducts() {
            if (!this.batchNumber) return
            this.loading = true
            try {
                const title = String(this.keyword || '').trim()
                const payload = {
                    batchNumber: this.batchNumber,
                    videoId: this.videoId,
                    page: this.page,
                    limit: this.limit,
                    sortBy: this.sortBy,
                    sortOrder: this.sortOrder
                }
                if (title) payload.title = title
                if (this.startTime) payload.startTime = this.startTime
                if (this.endTime) payload.endTime = this.endTime

                const res = await this.$httpBack2?.liveRoom?.productPage(payload)
                const normalized = this.normalizeProductPageData(res)
                if (normalized) {
                    this.pullStatus = Boolean(normalized.pullStatus)
                    this.total = Number(normalized.total || 0)
                    this.records = (Array.isArray(normalized.records) ? normalized.records : []).map((item) => ({
                        ...item,
                        imageUri: this.normalizeImageUri(item?.imageUri)
                    }))
                    return
                }
                this.total = 0
                this.records = []
            } catch (e) {
                this.total = 0
                this.records = []
            } finally {
                this.loading = false
            }
        },
        handleSearch() {
            this.page = 1
            this.loadProducts()
        },
        onPageChange(val) {
            this.page = val
            this.loadProducts()
        },
        onSizeChange(val) {
            this.limit = val
            this.page = 1
            this.loadProducts()
        },
        onSortChange({ prop, order }) {
            const sortOrder = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : ''
            if (!sortOrder) return
            this.sortBy = prop || 'payAmt'
            this.sortOrder = sortOrder
            this.page = 1
            this.loadProducts()
        },
        handleBuyInAuth() {
            if (!this.secUid) return
            this.$refs.liveRoomAuthorizeDialog?.open?.(this.secUid)
        },
        async handleLifeAuthorized() {
            await this.resetAndLoad()
        },
        async handlePullProduct() {
            if (!this.secUid || !this.batchNumber) return
            this.loading = true
            try {
                const res = await this.$httpClient?.anchorvideo?.pullProduct({
                    secUid: this.secUid,
                    batchNumber: this.batchNumber
                })
                if (Number(res?.code) === 0) {
                    this.$message.success(res?.msg || '操作成功')
                } else {
                    this.$message.error(res?.msg || '获取数据失败')
                }
            } catch (e) {
                this.$message.error('获取数据失败')
            } finally {
                this.loading = false
            }
            await this.resetAndLoad()
        },
        formatPercent(value) {
            const num = Number(value)
            if (!Number.isFinite(num)) return '-'
            if (num > 1) return `${num.toFixed(2)}%`
            return `${(num * 100).toFixed(2)}%`
        },
        formatNumber(value) {
            const num = Number(value)
            if (!Number.isFinite(num)) return '-'
            return `${num}`
        },
        /** 获取列的表头显示标签数组：双行模式下返回 columnDefs 中的拆分标签，单行时合并为一个元素 */
        getHeaderParts(prop) {
            const def = this.columnDefs.find(d => d.prop === prop)
            if (!def || !Array.isArray(def.label)) return ['']
            if (this.shouldUseTwoLine) return def.label
            return [def.label.join('')]
        }
    }
    ,
    mounted() {
        this.watchAuthorizedBuyInSuccess(async () => {
            await this.resetAndLoad()
        })
        this.watchAuthorizedBuyInError()
    }
}
</script>

<style lang="scss" scoped>

::v-deep(.productData-table){
    .el-table__header{
        .cell{
            display: flex;
            align-items: center;
            justify-content: center;
            white-space: normal;
        }
        .caret-wrapper{
            margin-left: 4px;
        }
    }
}

.productData {
    min-height: 560px;
}

.productData-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    &__left {
        display: flex;
        align-items: center;
        gap: 10px;
    }

    &__right {
        display: flex;
        align-items: center;
        gap: 10px;
    }
}

.productData-search {
    width: 260px;
}

.productData-btn {
    height: 34px !important;
    border-radius: 17px;
    padding: 0 18px;
}

.productData ::v-deep .productData-search .el-input__inner {
    height: 34px;
    line-height: 34px;
    border-radius: 17px;
}

.productData ::v-deep .tableColumnSetting-btn {
    height: 34px;
    border-radius: 17px;
}

.productData-note {
    color: #909399;
    font-size: 12px;
}

.productData-table ::v-deep .el-table__body .cell {
    white-space: pre-line;
}

.productData-headerTwoLine {
    display: inline-flex;
    flex-direction: column;
    align-items: left;
    text-align: left;
    line-height: 16px;
}

.productData-product {
    display: flex;
    align-items: center;
    gap: 10px;

    &__img {
        width: 40px;
        height: 40px;
        border-radius: 4px;
        object-fit: cover;
        background: #f2f3f5;
        flex: 0 0 auto;
    }

    &__title {
        flex: 1;
        min-width: 0;
        color: #303133;
        font-size: 13px;
        line-height: 18px;
    }
}

.productData-pagination {
    display: flex;
    justify-content: flex-end;
    padding-top: 12px;
}

.productData-empty {
    height: 520px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    &__img {
        width: 140px;
        height: 140px;
        opacity: 0.8;
        margin-bottom: 12px;
    }

    &__text {
        font-size: 14px;
        color: #606266;
    }

    &__link {
        color: var(--color-main);
        cursor: pointer;
    }
}
</style>
