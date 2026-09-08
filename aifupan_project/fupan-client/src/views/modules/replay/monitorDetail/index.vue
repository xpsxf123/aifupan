<template>
    <div class="monitorDetailPage">
        <MonitorDetailLayout
            v-if="showPage"
            :sentenceMarkData="sentenceMarkData"
            :targetType="getTargetType"
            :defaultReportType="defaultReportType"
        />
    </div>
</template>

<script>
/**
 * @description 监控详情页入口：负责加载监控详情数据，并为当前页面注册独立的返回上一页能力。
 */
import analysisMixin from '@/mixins/analysisMixin'
import MonitorDetailLayout from './components/MonitorDetailLayout.vue'

export default {
    components: {
        MonitorDetailLayout
    },
    inject: ['appVnode'],
    mixins: [analysisMixin],
    data() {
        return {
            sentenceMarkData: {
                sentenceMarkList: [],
                videoInfo: {},
                anchorInfo: {},
                playUrl: ''
            },
            videoId: '',
            showPage: false
        }
    },
    computed: {
        getTargetType() {
            return String(this.$route?.query?.targetType || '')
        },
        sourceKind() {
            return String(this.$route?.query?.sourceKind || '')
        },
        defaultReportType() {
            const t = String(this.$route?.query?.reportType || 'scriptQuality')
            if (['scriptQuality', 'interactionInspection', 'scriptRestoration'].includes(t)) return t
            return 'scriptQuality'
        }
    },
    watch: {
        '$route'(to, from) {
            if (to.path !== from.path) {
                this.initGetData()
            }
        }
    },
    mounted() {
        this.registerPageBackHandler()
        this.initGetData()
    },
    activated() {
        this.registerPageBackHandler()
        this.initGetData()
    },
    beforeDestroy() {
        this.clearPageBackHandler()
        this.showPage = false
    },
    methods: {
        /**
         * @description 为当前监控详情页注册独立返回能力，只执行浏览器上一页，不复用旧详情页的返回拦截逻辑。
         * @returns {void}
         */
        registerPageBackHandler() {
            this.$store.state.routerPathBack = this.handlePageBack
        },
        /**
         * @description 清理当前页面注册的返回处理，避免影响其他页面。
         * @returns {void}
         */
        clearPageBackHandler() {
            if (this.$store.state.routerPathBack === this.handlePageBack) {
                this.$store.state.routerPathBack = null
            }
        },
        /**
         * @description 返回上一页。
         * @returns {void}
         */
        handlePageBack() {
            this.$router.back()
        },
        initGetData() {
            this.videoId = String(this.$route?.query?.id || this.$store.state.videoAnalysisId || '')
            if (!this.videoId) return
            this.getVideoAnalysis()
        },
        getVideoAnalysis() {
            this.sentenceMarkData.sentenceMarkList = []
            this.showPage = false
            this.appVnode?.setAnalyserShowLoading?.(true)

            let requestPromise = null
            if (this.sourceKind === 'uploadFile') {
                requestPromise = this.$httpClient?.uploadFile?.lockanalysis?.({ fileId: this.videoId })
            } else if (this.sourceKind === 'online' || this.getTargetType === 'online') {
                requestPromise = this.$httpClient?.video?.lockCloudAnalysis?.({ videoId: this.videoId })
            } else {
                requestPromise = this.$httpClient?.video?.lockanalysis?.({ videoId: this.videoId })
            }

            Promise.resolve(requestPromise)
                .then((res) => {
                    if (res?.code === 0 && res?.data) {
                        this.initAudioaAlyses(res.data)
                        this.showPage = true
                    }
                })
                .finally(() => {
                    this.appVnode?.setAnalyserShowLoading?.(false)
                })
        }
    },
    beforeRouteLeave(to, from, next) {
        this.clearPageBackHandler()
        next()
    }
}
</script>

<style scoped>
.monitorDetailPage {
    width: 100%;
    height: calc(100vh - 66px);
}
</style>

