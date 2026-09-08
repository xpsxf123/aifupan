<template>
    <div>
        <!-- 文件分析 -->
        <div class="videoAnalysisContainer">
            <div class="analysisItemContainer">
                <analysis-item ref="analysis" :sentenceMarkData="sentenceMarkData" :videoWidth="'18%'" v-if="showAnalysis && loadData"
                    @countWords="countWords"  @repairVideo="setHasUnsavedChanges"  @notesEditer="setNotesEditerChanges"
                    @monitorDetailJump="handleMonitorDetailJump"></analysis-item>
            </div>
        </div>
    </div>
</template>

<script>
import myUtils from '../../../../utils/utils';
import analysisItem from "@/views/commonComponent/analysis-item.vue";
import analysisMixin from '../../../../mixins/analysisMixin';
import routerRepairStop from "@/mixins/routerRepairStop";
export default {
    components: {
        analysisItem
    },
    inject: ['appVnode'],
    mixins: [analysisMixin, routerRepairStop],
    data() {
        return {
            // 文字段落信息
            sentenceMarkData: {
                sentenceMarkList: [],
                fileInfo: {},
                playUrl: ""
            },
            // 关键词/敏感词数据
            wordsInfo: {
                wordsList: [], // 关键词/敏感词列表
                cruxWordsNum: 0, // 关键词个数
                sensitiveWordsNum: 0, // 敏感词个数
                markCrux: true, // 是否标注关键词
                markSensitive: true, // 是否标注敏感词
            },
            fileId: "",
            showAnalysis: false,
            loadData:true,
        };
    },
    watch:{
        '$route'(to, from) {
            if (to.path !== from.path) {
                this.loadData = false;
                this.$nextTick(() => {
                    this.loadData = true;
                })
            }
        }
    },
    activated(){
        this.getInitInfo()
    },
    mounted() {
       this.getInitInfo()
    },
    beforeDestroy() {
        this.showAnalysis = false
    },
    methods: {
        /**
         * @description 处理监控详情跳转：在视频分析/文案预审详情页点击话术质检等 tab 时，进入独立监控详情页。
         * @param {{ id?: string, targetType?: string, reportType?: string }} payload 跳转参数。
         * @returns {void}
         */
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
        getInitInfo(){
             // 获取文件id
                this.fileId =this.$route.query.id;
                const querySceneType = this.$route?.query?.sceneType
                let sceneType = querySceneType !== undefined && querySceneType !== null && querySceneType !== '' ? Number(querySceneType) : NaN
                if (![0, 1, 2].includes(sceneType)) {
                    const path = String(this.$route?.path || '')
                    if (path.includes('/uploadText/')) {
                        sceneType = 2
                    } else if (path.includes('/uploadVideo/')) {
                        sceneType = 1
                    } else {
                        sceneType = 0
                    }
                }
                this.$set(this.sentenceMarkData, 'sceneType', sceneType)
                if (this.fileId) {
                    this.getFileAnalysis();
                }
        },
        // 获取文件分析信息
        getFileAnalysis() {
            this.sentenceMarkData.sentenceMarkList = [];
            this.appVnode.setAnalyserShowLoading(false);
            this.$httpClient.uploadFile.lockanalysis({ fileId: this.fileId }).then((res) => {
                if (res.code == 0) {
                    // 视频/音频播放地址
                    this.initAudioaAlyses(res.data);
                    this.showAnalysis = true;
                }
            }).then(() => {
                this.appVnode.setAnalyserShowLoading(false);
            }).catch((err) => {
                this.appVnode.setAnalyserShowLoading(false);
            });
        },
        // 整理关键词、敏感词数据
        countWords(wordsInfo) {
            this.wordsInfo = wordsInfo;
        },
        // 毫秒时间戳转成时分秒格式
        toformatTime(val) {
            return myUtils.toformatTime(val);
        },
        switchList() {
            this.$emit("switchList");
        }
    },
};
</script>
<style scoped>
.wordsExportContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.analysisItemContainer {
    width: 100%;
    height: calc(100vh - 66px);
}

.border-right {
    border-right: 0.5px solid #ccc;
}

.border-left {
    border-left: 0.5px solid #ccc;
}

.border-bottom {
    border-bottom: 0.5px solid #ccc;
}

.border-top {
    border-top: 0.5px solid #ccc;
}

.wordsSummaryTitleItem {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
    background: #F5F7F9;
    font-size: 13px;
    font-weight: 400;
    color: #2E3742;
    height: 28px;
}

.wordsSummaryContentItem {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 12px;
    font-weight: 400;
    color: #2E3742;
    height: 28px;
}

.wordsSummaryContentItemContainer {
    display: flex;
    align-items: center;
}

.wordsSummaryContentContainer::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 4px;
}

.wordsSummaryContentContainer::-webkit-scrollbar {
    width: 4px;
}

.wordsSummaryContentContainer {
    height: 90px;
    overflow-y: auto;
}

.wordsSummaryTitleContainer {
    display: flex;
    align-items: center;

}

.wordsSummaryBodyContainer {
    margin-top: 10px;
}

.wordsSummaryContainer {
    margin-top: 14px;
    font-weight: 600;
    font-size: 14px;
    color: #2E3742;

}

.videoAnalysisContainer {
    /* margin-top: 8px; */
}


.analysisNavText {
    font-weight: 500;
    font-size: 16px;
    color: #2E3742;
    margin-left: 10px;
}

.analysisNavImg {
    width: 20px;
    height: 20px;
    cursor: pointer;
}

.analysisNavContainer {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #DCE0E7;
    padding-bottom: 10px;
}
</style>
