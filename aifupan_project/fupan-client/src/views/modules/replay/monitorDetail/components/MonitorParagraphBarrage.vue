<template>
    <div class="monitorParagraphBarrage">
        <div v-if="loading" class="font-s12 text-color3 pd-t6 pd-b6">弹幕加载中...</div>
        <div v-else-if="list.length" class="monitorParagraphBarrageList">
            <scrolling
                v-for="(row, i) in list"
                :key="row.id || `${row.recordDate || ''}-${i}`"
                :item="row"
                :isShowTime="true"
                :dataDisplay="dataDisplay"
                :deductionTime="videoStartTimestamp"
            />
        </div>
        <div v-else class="font-s12 text-color3 pd-t6 pd-b6">直播时间段暂无弹幕...</div>
    </div>
</template>

<script>
import scrolling from '/src/components/analysis/scrolling/index.vue'
import myUtils from '@/utils/utils'

const barrageResponseCache = new Map()
const barragePendingMap = new Map()
const barrageTimerMap = new Map()

/**
 * @description 将请求时间统一转为直播绝对时间戳；旧段落数据里的 start/endTime 为相对开播时长毫秒。
 * @param {number|string} time 原始时间
 * @param {number} baseTimestamp 直播开始绝对时间戳
 * @returns {number}
 */
function normalizeRequestTime(time, baseTimestamp) {
    const value = Number(time || 0)
    if (!value) return 0
    if (!baseTimestamp) return value < 1000000000000 ? 0 : value
    return value < 1000000000000 ? baseTimestamp + value : value
}

function parseLiveStartTimestamp(input) {
    if (input === null || input === undefined || input === '') return 0
    const raw = String(input).trim()
    if (!raw) return 0
    const num = Number(raw)
    if (Number.isFinite(num) && num > 0) {
        const len = raw.length
        const ms = len === 10 ? num * 1000 : len === 13 ? num : (num < 1000000000000 ? num * 1000 : num)
        return ms >= 1000000000000 ? ms : 0
    }
    const sec = myUtils?.toSecondByDate?.(raw)
    if (Number.isFinite(sec) && sec > 0) {
        const ms = sec * 1000
        return ms >= 1000000000000 ? ms : 0
    }
    const timestamp = new Date(raw).getTime()
    return Number.isFinite(timestamp) && timestamp >= 1000000000000 ? timestamp : 0
}

/**
/**
 * @description 复用同一时间范围的请求，并在真正发起接口前做一次短防抖，避免重复渲染造成连发。
 * @param {string} key 请求唯一键
 * @param {Function} fetcher 实际请求函数
 * @returns {Promise<Array>}
 */
function requestBarrageList(key, fetcher) {
    if (barrageResponseCache.has(key)) {
        return Promise.resolve(barrageResponseCache.get(key))
    }
    if (barragePendingMap.has(key)) {
        return barragePendingMap.get(key)
    }

    const requestPromise = new Promise((resolve) => {
        const oldTimer = barrageTimerMap.get(key)
        if (oldTimer) clearTimeout(oldTimer)

        const timer = setTimeout(() => {
            Promise.resolve(fetcher())
                .then((list) => {
                    const safeList = Array.isArray(list) ? list : []
                    barrageResponseCache.set(key, safeList)
                    resolve(safeList)
                })
                .catch(() => {
                    resolve([])
                })
                .finally(() => {
                    barragePendingMap.delete(key)
                    barrageTimerMap.delete(key)
                })
        }, 120)

        barrageTimerMap.set(key, timer)
    })

    barragePendingMap.set(key, requestPromise)
    return requestPromise
}

export default {
    components: {
        scrolling
    },
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => ({})
        },
        paragraph: {
            type: Object,
            default: null
        },
        startTime: {
            type: [Number, String],
            default: ''
        },
        endTime: {
            type: [Number, String],
            default: ''
        }
    },
    data() {
        return {
            loading: false,
            list: [],
            dataDisplay: {
                dateTime: true,
                nickName: true,
                level: true,
                fansLevel: true,
                isNew: true
            }
        }
    },
    computed: {
        videoInfo() {
            return this.sentenceMarkData?.videoInfo || {}
        },
        parentVideoInfo() {
            return this.videoInfo?.parentVideoInfo || {}
        },
        paragraphStartTimeSecond() {
            const v = this.paragraph?.startTimeSecond
            const n = Number(v)
            return Number.isFinite(n) && n > 0 ? n : 0
        },
        paragraphEndTimeSecond() {
            const v = this.paragraph?.endTimeSecond
            const n = Number(v)
            return Number.isFinite(n) && n > 0 ? n : 0
        },
        videoStartTimestamp() {
            const candidates = [
                this.videoInfo?.StartTime,
                this.videoInfo?.startTime,
                this.parentVideoInfo?.StartTime,
                this.parentVideoInfo?.startTime
            ]
            for (const item of candidates) {
                const ts = parseLiveStartTimestamp(item)
                if (ts) return ts
            }
            return 0
        },
        requestStartTime() {
            if (this.paragraphStartTimeSecond) return this.paragraphStartTimeSecond * 1000
            return this.absoluteStartTime
        },
        requestEndTime() {
            if (this.paragraphEndTimeSecond) return this.paragraphEndTimeSecond * 1000
            return this.absoluteEndTime
        },
        absoluteStartTime() {
            return normalizeRequestTime(this.startTime, this.videoStartTimestamp)
        },
        absoluteEndTime() {
            return normalizeRequestTime(this.endTime, this.videoStartTimestamp)
        },
        requestVideoId() {
            return (
                this.paragraph?.videoId
                || this.parentVideoInfo?.videoId
                || this.videoInfo?.VideoId
                || ''
            )
        },
        requestBatchNumber() {
            return this.paragraph?.batchNumber || this.videoInfo?.BatchNumber || ''
        },
        requestKey() {
            return [this.requestVideoId, this.requestBatchNumber, this.requestStartTime, this.requestEndTime].join('_')
        }
    },
    watch: {
        requestKey: {
            handler() {
                this.fetchList()
            },
            immediate: true
        }
    },
    methods: {
        fetchList() {
            const startTime = Number(this.requestStartTime || 0)
            const endTime = Number(this.requestEndTime || 0)
            if (!this.videoStartTimestamp || !startTime || !endTime || endTime <= startTime) {
                this.loading = false
                this.list = []
                return
            }

            const parentVideoInfo = this.parentVideoInfo
            const videoId = this.requestVideoId
            const batchNumber = this.requestBatchNumber
            if (!videoId || !batchNumber) {
                this.loading = false
                this.list = []
                return
            }

            const currentRequestKey = this.requestKey

            const params = {
                limit: 9999,
                page: 1,
                queryType: 2,
                recordDate: startTime,
                startTime,
                endTime,
                videoId,
                batchNumber,
                parentVideoInfo
            }

            if (barrageResponseCache.has(currentRequestKey)) {
                this.loading = false
                this.list = barrageResponseCache.get(currentRequestKey) || []
                return
            }

            this.loading = true
            requestBarrageList(currentRequestKey, () => {
                return Promise.resolve(this.$httpBack?.v2100?.queryDanMuData?.(params, { load: false }))
                    .then((res) => {
                        if (res?.code === 0) {
                            return res?.data?.list || []
                        }
                        return []
                    })
            }).then((list) => {
                if (this.requestKey === currentRequestKey) {
                    this.list = list
                }
            }).finally(() => {
                if (this.requestKey === currentRequestKey) {
                    this.loading = false
                }
            })
        }
    }
}
</script>

<style scoped lang="scss">
.monitorParagraphBarrage {
    margin-top: 8px;
    padding: 6px 8px;
    background: #F7F7F7;
    border-radius: 6px;
}

.monitorParagraphBarrageList {
    display: flex;
    flex-direction: column;
    gap: 2px;
    max-height: 225px;
    overflow-y: auto;
}
</style>
