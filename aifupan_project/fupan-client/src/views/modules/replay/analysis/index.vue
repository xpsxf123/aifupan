<template>
    <div class="analysisItemContainer">
        <!-- 视频分析 -->
        <div class="videoAnalysisContainer">
            <div class="analysisItemContainer">
                <analysis-item v-if="showAnalysis"  ref="analysis" :sentenceMarkData="sentenceMarkData"
                 @showUploadPop="showUploadPop" :targetType="getTargetType" @repairVideo="setHasUnsavedChanges" @notesEditer="setNotesEditerChanges"
                 @monitorDetailJump="handleMonitorDetailJump"></analysis-item>
            </div>
        </div>
    </div>
</template>
<script>
import analysisMixin from '@/mixins/analysisMixin';
import routerRepairStop from "@/mixins/routerRepairStop";
import AnalysisItem from "./../../../commonComponent/analysis-item.vue";
export default {
    components: {
        AnalysisItem
    },
    props:{
        targetType: {
            type: String,
            default: ''
        },
        id: {
            type: String,
            default: ''
        },
        httpRequest: {
            type: Function,
            default: null
        }
    },
    inject: ["appVnode"],
    mixins: [analysisMixin,routerRepairStop],
    data() {
        return {
            // 文字段落信息
            sentenceMarkData: {
                sentenceMarkList: [],
                videoInfo: {},
                anchorInfo: {},
                playUrl: ""
            },
            videoId: "",
            showAnalysis: false,
            example: false
        };
    },
    watch: {
        '$route'(to, from) {
            if (to.path !== from.path) {
                this.initGetData();
            }
        }
    },
    computed:{
        getTargetType(){
            if(this.example){
                return 'webOnline'
            }
            return this.targetType;
        }
    },
    create(){
        
    },
    mounted() {
        
        this.initGetData();
    },
    activated() {
        this.initGetData();
    },
    beforeDestroy() {
        this.showAnalysis = false;
    },
    
    methods: {
        handleMonitorDetailJump({ id, targetType, reportType, sourceKind, sceneType } = {}) {
            if (!id) return
            this.$router.push({
                path: '/replayMonitorDetail',
                query: {
                    id,
                    targetType,
                    reportType,
                    sourceKind,
                    sceneType
                }
            })
        },
        initGetData(){
            // 获取视频id
            this.videoId = this.$route.query.id || this.id || this.$store.state.videoAnalysisId;
            this.example = this.$route.query.type  === 'example';
            if (this.videoId) {
                this.getVideoAnalysis();
            }
        },
        showUploadPop(userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo) {
            if(shareUrl && this.targetType === 'online'){
                this.appVnode?.copyShareUrl(shareUrl)
                return
            }
            this.appVnode?.showUploadPop(userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo)
            // this.$emit("showUploadPop", userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo)
        },
        // 获取视频分析信息
        getVideoAnalysis() {
            this.sentenceMarkData.sentenceMarkList = [];
            this.showAnalysis = false;
            this.appVnode.setAnalyserShowLoading(true);
            
            let http = null;
            let httpApi = this.targetType === 'online'? this.$httpClient.video.lockCloudAnalysis : this.$httpClient.video.lockanalysis;
            if(this.example){
                http = this.$httpBack.v2000.getOnlineAnalysis({ uuid: this.videoId,type: 0 });
            }else{
                http = httpApi({ videoId: this.videoId });
            }
            http.then((res) => { 
                if (res.code == 0) {
                    this.initAudioaAlyses(res.data);
                    this.showAnalysis = true;
                }
            }).then(() => {
                this.appVnode.setAnalyserShowLoading(false);
            }).catch((err) => {
                this.appVnode.setAnalyserShowLoading(false);
            });
        }
    },
};
</script>
<style scoped>

.analysisItemContainer {
    width: 100%;
    height: calc(100vh - 66px);
}

</style>
