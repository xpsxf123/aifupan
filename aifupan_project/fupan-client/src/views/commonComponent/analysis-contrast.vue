<!--
@description 对比详情容器：承载双场复盘对比布局、AI 精灵入口以及多场诊断跳转逻辑。
-->
<template>
  <div class="contrastContainer" v-if="loading" v-loading="webLoading" style="height: 100%;" @contextmenu="preventRightClick">
    <div class="contrast-window">
      <!-- 内容区域 -->
      <div class="analysis-flex-column common-bg">
        <analysisCompareLayout :isWebOnline="isWebOnline" :syncScene="syncScene" :contrastId="getContrastId" :targetType="targetType"
          :sentenceMarkData="sentenceMarkData" @brushChange="brushChange" @isAiElfChange="isAiElfChange" @aIContrastData="aIContrastData">
          <template #page-right>
            <slot name="page-right"></slot>
          </template>
        </analysisCompareLayout>
      </div>
      <online-not-analysis-dialog v-if="onlineNotAnalysisDialogVisible"
        ref="onlineNotAnalysisDialog"></online-not-analysis-dialog>
    </div>

    <cancelUpDialog v-if="cancelUpDialogVisible" ref="cancelUpDialog" @child-event="cancelClick"></cancelUpDialog>
    <!-- Ai精灵 -->
    <aiElf v-if="!webLoading&&isAiElf" isCompare @click="elfClick" ref="aiElfRef"></aiElf>
  </div>
</template>

<script lang="jsx">
import myUtils from '/src/utils/utils';
// import analysisContrastItem from "/src/views/commonComponent/analysisLayout/contrast-only.vue"
import OnlineNotAnalysisDialog from '/src/views/commonComponent/onlineNotAnalysisDialog.vue';
import cancelUpDialog from './cancelUpDialog.vue';
import analysisMixin from '/src/mixins/analysisMixin';
import analysisCompareLayout from '/src/views/commonComponent/analysisLayout/analysis-compare-layout.vue';
import aiElf from '/src/components/analysis/aiElf/index.vue';
// // src\views\commonComponent\analysisLayout\mixin\publicMixin.js
import publicMixin from "./analysisLayout/mixin/publicMixin";
import commonUtils from "@/utils/common";
/**
 * @description 对比详情页脚本入口：负责对比分析页的数据初始化、AI 精灵点击分流与分享能力联动。
 */
export default {
  components: {
    // analysisContrastItem,
    // OnlineAnalysisDialog,
    OnlineNotAnalysisDialog,
    // WordTable,
    cancelUpDialog,
    analysisCompareLayout,
    aiElf
  },
  props: {
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
  inject: ['appVnode','APP'],
  mixins: [analysisMixin, publicMixin],
  data() {
    return {
      contrastId: '',
      syncScene:'',
      cancelUpDialogVisible: false,
      shareUrl: "",
      // 关键词/敏感词列表
      showVideo: false,
      sentenceMarkData: {
        data1: {},
        data2: {}
      },
      onlineFileInfo: {},
      contrastInfo: {},

      onlineNotAnalysisDialogVisible: false,
      webLoading: false,
      loading: false,
      toAi:false,
      isAiElf:true,
      getInifDebounce: myUtils.debounce(100,true),
    };
  },
  watch: {
  },
  created() {

  },
  activated() {
    this.saveSuccess();
    this.addShareDom();
    this.getContrastInfo(this.getContrastId,' activated')
  },
  mounted() {
    // 加载数据
    this.getContrastInfo(this.getContrastId,'mounted')
    this.saveSuccess();
    this.addShareDom();
  },
  destroyed() {
    this.delShareDom();
    if(this.toAi || this.$isWeb){return};
    this.APP.refresh();
  },
  beforeRouteLeave(to, from, next) {
    this.delShareDom()
    next()
  },
  computed: {
    isWebOnline() {
      return this.targetType === 'webOnline'
    },
    // 对比ID
    getContrastId() {
      return this.$route.query?.contrastId || this.id || this.contrastInfo?.ContrastId;
    },
    getHttpRequest() {
      if (this.httpRequest) {
        return this.httpRequest
      }
    },
    isAiUrl() {
        const path = this.$route.path.split('/').slice(0, -1).join('/');
      if (this.targetType === 'online') {
        return `${path}/aiContrast`
      } else {
        const path = '/' + this.$route.path.split('/')[1]
        return `${path}/aiAnalysis`
      }
    }
  },
  methods: {
    // 设置分析数据
    setSentenceMarkData(key, data) {
            let o = {
                ...data
            };
            // 段落数据
            o.sentenceMarkList = data.sentenceMarkList;
            // 视频播放地址
            o.playUrl = data.playUrl;
            // 主播信息
            o.anchorInfo = data.anchorInfo;
            if (data.fileInfo) {
                o.fileInfo = data.fileInfo
            } else if (data.videoInfo) {
                o.videoInfo = data.videoInfo
            }
            this.$set(this.sentenceMarkData, key, o);
        },
        // 获取对比信息
        getContrastInfo(contrastId, type) {
            this.getInifDebounce(()=>{
                this.sentenceMarkData.data1 = {};
                this.sentenceMarkData.data2 = {};
                this.loading = false;
                let httpRequest = this.httpRequest;
                let params = { contrastId };
                this.onLoading(this.isWebOnline);
                this.appVnode?.setAnalyserShowLoading?.(true)
                httpRequest(params).then(res => {
                    if (res.code == 0 && res.data) {
                      this.sentenceMarkData.data1.sentenceMarkList = [];
                      this.sentenceMarkData.data2.sentenceMarkList = [];
                      this.initPayerIndexMap();
                      this.initAudioaAlyses(res.data.SentenceMark1, (data) => {
                          this.setSentenceMarkData('data1', {...data, index: 1});
                          this.setPayerIndexMap(data.sentenceMarkList, 'A1');
                      });
                      this.initAudioaAlyses(res.data.SentenceMark2, (data) => {
                          this.setSentenceMarkData('data2', {...data, index: 2});
                          this.setPayerIndexMap(data.sentenceMarkList, 'A2');
                      });
                      this.successData(res.data, this.sentenceMarkData)
                      this.loading = true;
                      this.$emit('request',{
                        type: 'success',
                        data: res
                      })
                    } else {
                      this.$emit('request',{
                        type: 'lose',
                        data: res
                      });
                      this.$message.error("对比数据不存在");
                    }
                }).finally(()=>{
                    this.onLoading(false);
                    this.appVnode?.setAnalyserShowLoading?.(false);
                }).catch((err) => {
                  this.$emit('request',{
                    type: 'error',
                    data: err
                  })
                });
            })
        },
      aIContrastData(type){
          this.elfClick(type)
      },

      brushChange(data){
         this.elfClick('assistant',{ data1: data.data1?.join(','), data2: data.data2?.join(',') })
      },
      isAiElfChange () {
          this.$refs.aiElfRef?.toBorder()
      },
    onLoading(val) {
      this.webLoading = val;
    },
    elfClick(type,o) {
      if (type === 'multiDiagnose') {
        this.openAiAgentWorkbench()
        return
      }
      if (this.isWebOnline) {
        this.$emit('webElfClick', type,o)
        return
      };
      this.toAi = true;
      this.$router.push({
        path: this.isAiUrl,
        query: {
          contrastId: this.getContrastId,
          type: type,
            ...o
        }
      });
    },
    /**
     * @description 打开 AI 智能体工作台，多场诊断场景只锁定直播间，不锁定具体场次。
     * @returns {void}
     */
    openAiAgentWorkbench() {
      const primaryData = this.sentenceMarkData?.data1 || {}
      const fallbackData = this.sentenceMarkData?.data2 || {}
      const secUid = String(
        primaryData?.anchorInfo?.SecUid
        || primaryData?.anchorInfo?.secUid
        || fallbackData?.anchorInfo?.SecUid
        || fallbackData?.anchorInfo?.secUid
        || ''
      ).trim()
      if (this.$httpClient?.system?.openAIAgentWeb) {
        this.$httpClient.system.openAIAgentWeb({
          secUid,
          openInBrowserWindow: this.isWebOnline
        })
        return
      }
      this.$router.push({ path: '/aiAssistant' })
    },
    saveSuccess() {
      if (this.isWebOnline) { return }
      this.appVnode.addUploadVodSuccess((vList,cloudRemarks) => {
        this.confirmShareAnalysis(vList,cloudRemarks);
      });
    },
    addShareDom() {
      this.$store.commit("setTitleRightRender", (h) => {
        if (this.showVideo && this.sentenceMarkData.data1.videoInfo && this.sentenceMarkData.data1.videoInfo.VideoId) {
          return (<div style="height: 100%;" class="flex-ai-c mg-r20">
            {this.contrastInfo.IsShard === 0 ?
              <div class="shardText" onClick={this.shareContrastAnalysis}>分享复盘</div>
              : <div class="shardText" onClick={this.copyShareAnalysisLink}>{this.shareUrl ? '复制分享链接' : '分享复盘'}</div>}
          </div>)
        } else {
          return '';
        }
      });
    },
    delShareDom() {
      this.$store.commit("setTitleRightRender", null)
    },
    // 成功加载数据
    successData(data, sentenceMarkData) {
      this.showVideo = true;
      this.contrastInfo = data.VideoContrast || {};
      this.contrastId = data?.VideoContrast?.ContrastId;
      this.sentenceMarkData = sentenceMarkData;
      this.syncScene = data.VideoContrast?.syncScene;
      this.shareUrl = this.contrastInfo.ShareUrl;
      if (this.isWebOnline) { return }
      if (this.contrastInfo?.IsShard === 1) {
          if (!this.shareUrl) {
              this.shareUrl = commonUtils.assemblyShareUrl(`contrastOnlineAnalysis/${this.contrastInfo?.ContrastId}`)
          }
      }
      this.appVnode.setShareUrl(this.shareUrl);
      //
    },
    // 复制分享链接
    async copyShareAnalysisLink() {
        if (this.contrastInfo?.IsShard === 1) {
            if (!this.shareUrl) {
                this.shareUrl = commonUtils.assemblyShareUrl(`contrastOnlineAnalysis/${this.contrastInfo?.ContrastId}`)
            }
            this.appVnode.copyShareUrl(this.shareUrl);
            return;
        }
      this.onlineFileInfo = {
        duration: 0,
        fileSize: 0,
        videoList: []
      }
      this.appVnode.showContrastAnalysis();
      // this.onlineAnalysisDialogVisible = true;
      this.$nextTick(async () => {
        let userPropertyRes = await this.$httpBack.userProperty.info({});
        if (userPropertyRes.code == 0 && userPropertyRes.data) {
          await this.countVideo(this.sentenceMarkData.data1);
          await this.countVideo(this.sentenceMarkData.data2);
          this.shareUrl = this.contrastInfo.ShareUrl;

          this.appVnode.showContrastAnalysis(userPropertyRes.data, this.onlineFileInfo)
        }
      });
    },
    // 分享对比复盘
    async shareContrastAnalysis() {
      this.onlineFileInfo = {
        duration: 0,
        fileSize: 0,
        videoList: []
      }
      if (this.sentenceMarkData.data1.videoInfo && !this.sentenceMarkData.data1.videoInfo.ShareUrl) {
        // 对比的第一个文件没有分享上传
        await this.countVideo(this.sentenceMarkData.data1);
      }
      if (this.sentenceMarkData.data2.videoInfo && !this.sentenceMarkData.data2.videoInfo.ShareUrl) {
        // 对比的第二个文件没有分享上传，
        await this.countVideo(this.sentenceMarkData.data2);
      }
      if (this.onlineFileInfo.duration > 0) {
        // 获取用户资产，判断资源是否足够
        this.checkOnlineProperty();
      } else {
        // console.log('上传视频成功，进入分享修改')
        // 没有需要上传的视频，直接修改分享状态
        this.confirmShareAnalysis();
      }
    },
    // 计算视频的大小时长
    async countVideo(data) {
      if (data.videoInfo.ShareUrl) {
        // 视频已经上传过
        this.onlineFileInfo.videoList.push({
          VideoId: data.videoInfo.VideoId
        });
        this.onlineFileInfo.fileSize += parseInt(data.videoInfo.VedioSizie / 1024 / 1024);
        this.onlineFileInfo.duration += Math.floor(myUtils.toSecond(data.videoInfo.Duration) / 60 <= 1 ? 1 : myUtils.toSecond(data.videoInfo.Duration) / 60);
      } else {
        let res = await this.$httpClient.video.compress({ videoId: data.videoInfo.VideoId });
        if (res.code == 0) {
          this.onlineFileInfo.videoList.push({
            VideoId: data.videoInfo.VideoId,
            filePath: res.data.filePath
          });
          this.onlineFileInfo.fileSize += res.data.fileSize;
          this.onlineFileInfo.duration += Math.floor((res.data.duration / 60) <= 1 ? 1 : res.data.duration / 60);
        }
      }

    },

    // 判断分享复盘资源是否足够
    checkOnlineProperty() {
      // 获取用户资产，判断资源是否足够
      this.$httpBack.userProperty.info({}).then(userPropertyRes => {
        this.userProperty = userPropertyRes.data;
        if (this.userProperty.storageNum / 1024 >= this.onlineFileInfo.fileSize) {
          // 存储空间足够
          this.appVnode.showContrastAnalysis(this.userProperty, this.onlineFileInfo)
        } else {
          // 存储空间不足
          this.onlineNotAnalysisDialogVisible = true;
          this.$nextTick(() => {
            this.$refs.onlineNotAnalysisDialog.init(this.userProperty, this.onlineFileInfo);
          })
        }
      });
    },

    // 修改分享状态
    async confirmShareAnalysis(videoList, cloudRemarks, viewCrowdType) {
      if (videoList) {
        for (const video of videoList) {
          await this.$httpBack.video.shareVideoToCloud({ videoId: video.VideoId, onlineFileUrl: video.playUrl, viewCrowdType }).then(res => {
            if (res.code == 0 && res.data) {
              if (this.sentenceMarkData.data1.videoInfo.VideoId == video.VideoId) {
                this.sentenceMarkData.data1.videoInfo.UploadStatus = 1;
                this.sentenceMarkData.data1.videoInfo.ShareUrl = res.data;
              } else {
                this.sentenceMarkData.data2.videoInfo.UploadStatus = 1;
                this.sentenceMarkData.data2.videoInfo.ShareUrl = res.data;
              }
            }
          });
        }
      }
      this.$httpBack.contrast.shareContrastToCloud({ contrastId: this.contrastInfo?.ContrastId, cloudRemarks, viewCrowdType }).then(res => {
        if (res.code == 0 && res.data) {
          this.contrastInfo.IsShard = 1;
          this.contrastInfo.ShareUrl = res.data;
          this.shareUrl = res.data;
          this.appVnode.setShareUrl(this.shareUrl);
          this.$message.success("上传成功，可以复制分享链接啦");
        }
      });
    },

    // 禁止鼠标右键
    preventRightClick(event) {
      event.preventDefault();
    },



    close() {
      // 调用关闭检查
      if (this.isWebOnline) { return }
      this.appVnode.close()
    },

    cancelClick() {
      this.$refs.onlineAnalysisDialog.cancelUpload();
      this.$router.back();
    },
    // 返回上一页
    back() {
      this.$router.back();
    }
  },
};
</script>

<style scoped lang="scss">
.contrastContainer {
  // padding: 20px 32px 10px 32px;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
  position: relative;

  overflow: hidden;
  box-sizing: border-box;
  // *{
  //   overflow: auto;
  // }
}

.contrast-window {
  height: 100%;
  box-sizing: border-box;
  justify-content: center;
}

.analysisNavContainer {
  border-bottom: 1px solid #DCE0E7;
  padding: 10px;
}
</style>


<style scoped lang="less">
/deep/ .el-dialog__body {
  padding: 0;
}

/deep/ .el-dialog__header {
  display: none;
}

/deep/ .el-table--mini .el-table__cell {
  padding: 0;
}

/deep/ .el-table .el-table__cell {
  padding: 0;
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



.windowCtrlImg {
  width: 16px;
  height: 16px;
  margin-left: 21px;
  cursor: pointer;
}

.windowCtrlContainer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex: 0 !important;
}

.analysis-box {
  height: calc(100% - 43px);
}
</style>
