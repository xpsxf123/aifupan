<template>
  <div>
    <!-- <el-dialog :visible.sync="visible" width="80%"> -->
    <div>
      <div v-if="true" class="onlineAnalysisContainer">
        <template v-if="showVisible">
          <!-- 视频信息 -->
          <div
              v-if="videoInfo && videoInfo.videoId"
              class="analysisFileContainer"
          >
            <div class="analysisFileLeftContainer">
              <img :src="anchorInfo.anchorAvatar" class="analysisCompereImg"/>
              <div class="analysisCompereName">{{ anchorInfo.anchorName }}</div>
            </div>
            <div class="analysisFileRightContainer">
              <div class="analysisFileRightItemContainer rightBorder">
                <div>开始：</div>
                <div>{{ videoInfo.startTime.substring(0, 16) }}</div>
              </div>
              <div class="analysisFileRightItemContainer rightBorder">
                <div>结束：</div>
                <div>{{ videoInfo.endTime.substring(0, 16) }}</div>
              </div>
              <div class="analysisFileRightItemContainer">
                <div>时长：</div>
                <div>{{ durationStr }}</div>
              </div>
            </div>
          </div>

          <!-- 敏感词/关键词/语速汇总 -->
          <div class="wordsContainer">
            <div class="wordsItemContainer">
              <div class="wordsItemColorContainer">
                <div class="wordsItemColor" style="background: pink"></div>
                <div class="wordsItemText">
                  敏感词：{{ wordsInfo.sensitiveWordsNum }}次
                </div>
              </div>
              <div
                  v-if="!wordsInfo.markSensitive"
                  class="indiciaBtn"
                  @click="markClick('sensitive', true)"
              >
                点击标注
              </div>
              <div
                  v-else
                  class="cancelIndiciaBtn"
                  @click="markClick('sensitive', false)"
              >
                取消标注
              </div>
            </div>
            <div class="wordsItemContainer">
              <div class="wordsItemColorContainer">
                <div
                    class="wordsItemColor"
                    style="background: paleturquoise"
                ></div>
                <div class="wordsItemText">
                  关键词：{{ wordsInfo.cruxWordsNum }}次
                </div>
              </div>
              <div
                  v-if="!wordsInfo.markCrux"
                  class="indiciaBtn"
                  @click="markClick('crux', true)"
              >
                点击标注
              </div>
              <div
                  v-else
                  class="cancelIndiciaBtn"
                  @click="markClick('crux', false)"
              >
                取消标注
              </div>
            </div>
            <!-- <div class="wordsItemContainer" v-if="videoInfo && videoInfo.videoId">
                <div class="wordsItemColorContainer">
                    <div class="wordsItemText" v-if="!analysisChar">语速：**字/分钟</div>
                    <div class="wordsItemText" v-else>语速：{{ parseInt(charCountNum / (videoDuration / 60)) }}字/分钟
                    </div>
                </div>
                <div class="indiciaBtn" @click="analysisChar = true" v-if="!analysisChar">点击分析</div>
                <div class="indiciaBtn" @click="analysisChar = false" v-if="analysisChar">隐藏分析</div>
            </div> -->
            <div class="wordsItemContainer">
              <div
                  v-if="sentenceMarkList.length > 0"
                  class="aiAnalysisClick"
                  @click="clickAiAnalysis"
              >
                使用AI分析
              </div>
              <div v-if="sentenceMarkList.length <= 0" class="disableClick">
                暂无文本，无法AI分析
              </div>
            </div>
          </div>

          <!-- 内容 -->
          <div class="videoAndWordContainer">
            <div class="discernContainer">
              <template v-if="videoAndTextActive == 'textActive'">
                <div class="discernSearchContainer">
                  <!-- <div v-if="!playUrl" style="margin-right: 10px;">
                      <el-cascader v-model="tradeIdArr" :options="tradeTreeList" style="width: 220px"
                          :props="{ checkStrictly: true, expandTrigger: 'click', value: 'id', label: 'name' }"
                          filterable size="mini" ref="tradeCascader" disabled>
                      </el-cascader>
                  </div> -->
                  <!-- 行业 -->
                  <div>
                    <el-cascader
                        ref="tradeCascader"
                        v-model="tradeIdArr"
                        :disabled="isMark == 0 ? true : false"
                        :options="tradeTreeList"
                        :props="{
                        checkStrictly: true,
                        expandTrigger: 'click',
                        value: 'id',
                        label: 'name',
                      }"
                        filterable
                        size="small"
                        style="width: 100%"
                        @change="enoughMarkProperty(1)"
                    >
                    </el-cascader>
                  </div>
                  <!-- 搜索内容标注关键字 -->
                  <div
                      v-if="searchWordIndexInfo.name"
                      style="display: flex; align-items: center"
                  >
                    <div
                        v-if="searchWordIndexInfo.count > 0"
                        style="display: flex; align-items: center"
                    >
                      <el-icon
                          style="
                          font-size: 26px;
                          color: dodgerblue;
                          cursor: pointer;
                        "
                          @click="prevSearchWords"
                      >
                        <ArrowLeft/>
                      </el-icon>
                      <div style="font-size: 14px; color: #444; margin: 0 6px">
                        {{
                          searchWordIndexInfo.wordIndex +
                          1 +
                          ' / ' +
                          searchWordIndexInfo.count
                        }}
                      </div>
                      <el-icon
                          style="
                          font-size: 26px;
                          color: dodgerblue;
                          cursor: pointer;
                        "
                          @click="nextSearchWords"
                      >
                        <ArrowRight/>
                      </el-icon>
                    </div>
                    <div
                        v-else
                        style="font-size: 14px; color: #444; margin-right: 10px"
                    >
                      无结果
                    </div>
                  </div>
                  <el-input
                      v-model="searchWordIndexInfo.name"
                      clearable
                      placeholder="输入内容标注关键字"
                      size="small"
                      style="width: 200px"
                      @input="markKeywords"
                  >
                    <template #suffix>
                      <el-icon>
                        <Search/>
                      </el-icon>
                    </template>
                  </el-input>

                  <!-- 导出文字内容 -->
                  <el-button
                      size="small"
                      style="margin-left: 10px"
                      type="primary"
                      @click="exportTxt"
                  >导出文字
                  </el-button>
                </div>
                <!-- 音频/视频文件的文字段落 -->
                <div
                    :style="wordsBodyContainerHeight"
                    class="wordsBodyContainer"
                    @mouseup="handleSelectText"
                    @contextmenu.prevent="rightTickContextMenu"
                >
                  <div
                      v-for="(item, index) in sentenceMarkList"
                      :key="index"
                      ref="dom"
                      class="wordsBodyItemContainer"
                  >
                    <img
                        v-if="anchorInfo && anchorInfo.anchorAvatar"
                        :src="anchorInfo.anchorAvatar"
                        class="wordsBodyAvatar"
                    />
                    <img
                        v-else
                        class="wordsBodyAvatar"
                        src="@/assets/images/avatar.png"
                    />
                    <div class="wordsBodyContentContainer">
                      <div
                          style="
                          display: flex;
                          align-items: center;
                          justify-content: space-between;
                        "
                      >
                        <div style="display: flex; align-items: center">
                          <div class="wordsBodyContentText1">说话人</div>
                          <div
                              v-if="fileInfo.fileType == 0 || videoInfo"
                              class="wordsBodyContentText2"
                          >
                            <span>{{
                                toformatTime(item.items[0].startTime)
                              }}</span>
                          </div>
                          <div
                              v-if="
                              videoInfo &&
                              videoInfo.videoId &&
                              item.onlinePeopleObj.show
                            "
                              class="onlineNumContainer"
                          >
                            <div>
                              在线人数：{{ item.onlinePeopleObj.number }}
                            </div>
                            <div class="onlineNumDifferenceContainer">
                              <div
                                  v-if="item.onlinePeopleObj.difference > 0"
                                  style="
                                  color: #fc4f52;
                                  display: flex;
                                  align-items: center;
                                "
                              >
                                <img
                                    src="@/assets/images/plus.png"
                                    style="
                                    width: 8px;
                                    height: 8px;
                                    margin-right: 1px;
                                  "
                                />
                                <span>{{
                                    item.onlinePeopleObj.difference
                                  }}</span>
                                <img
                                    src="@/assets/images/up.png"
                                    style="width: 16px; height: 16px"
                                />
                              </div>
                              <div
                                  v-else-if="item.onlinePeopleObj.difference < 0"
                                  style="
                                  color: #28bd6c;
                                  display: flex;
                                  align-items: center;
                                "
                              >
                                <img
                                    src="@/assets/images/minus.png"
                                    style="
                                    width: 8px;
                                    height: 8px;
                                    margin-right: 1px;
                                  "
                                />
                                <span>{{
                                    Math.abs(item.onlinePeopleObj.difference)
                                  }}</span>
                                <img
                                    src="@/assets/images/down.png"
                                    style="width: 16px; height: 16px"
                                />
                              </div>
                              <div v-else style="display: flex">
                                <img
                                    src="@/assets/images/flat.png"
                                    style="width: 8px; height: 8px"
                                />
                              </div>
                            </div>
                          </div>
                        </div>
                        <div
                            class="wordsBodyContentText2"
                            style="margin-right: 20px"
                        >
                          <span v-if="item.naturalTime" style="margin-left: 6px"
                          >自然时间：{{ item.naturalTime }}</span
                          >
                        </div>
                      </div>

                      <!-- 段落 -->
                      <div class="paragraphContainer">
                        <!-- 让两个元素重叠 -->
                        <div
                            class="searchMarkContent"
                            style="position: relative"
                            v-html="'<span>' + item.searchMarkContent + '</span>'"
                        ></div>
                        <div
                            class="markContent"
                            style="position: absolute; left: 0; top: 0"
                            v-html="'<span>' + item.markContent + '</span>'"
                        ></div>
                        <div
                            class="restricMarkContent"
                            style="position: absolute; left: 0; top: 0"
                            v-html="
                            '<span>' + item.restricMarkContent + '</span>'
                          "
                        ></div>
                        <div
                            class="words-box"
                            style="position: absolute; left: 0; top: 0"
                        >
                          <span
                              v-for="(wordsItem, index) in item.items"
                              :key="index"
                              :class="
                              wordsItem.startTime < videoCurrentTime &&
                              videoCurrentTime < wordsItem.endTime &&
                              wordsItem.word != '，' &&
                              wordsItem.word != '。' &&
                              wordsItem.word != '！' &&
                              wordsItem.word != '？' &&
                              wordsItem.word != '、'
                                ? 'wordTimeHover'
                                : 'word'
                            "
                              @click="playerReadied(wordsItem.startTime)"
                          >{{ wordsItem.word }}</span
                          >
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- 文本文件的文字段落 -->
                <!-- <div @mouseup="handleSelectText" @contextmenu.prevent="rightTickContextMenu"
                    class="wordsBodyContainer"  :style="wordsBodyContainerHeight">

                    <div v-for="item in sentenceMarkList" :key="item.id || item.index" class="wordsBodyItemContainer" ref="dom">
                        <img :src="anchorInfo.AnchorAvatar" class="wordsBodyAvatar"
                            v-if="anchorInfo && anchorInfo.AnchorAvatar">
                        <img src="@/assets/images/avatar.png" class="wordsBodyAvatar" v-else>
                        <div class="wordsBodyContentContainer">
                            <div class="wordsBodyContentText1">说话人</div>

                            <div class="paragraphContainer-txt">
                                <div class="searchMarkContent"
                                    style="position: relative; white-space: pre-wrap;"
                                    v-html="'<span>' + item.searchMarkContent + '</span>'">
                                </div>
                                <div class="markContent"
                                    style="position: absolute; left: 0; top: 0; white-space: pre-wrap;"
                                    v-html="'<span>' + item.markContent + '</span>'">
                                </div>
                                <div class="restricMarkContent"
                                    style="position: absolute; left: 0; top: 0; white-space: pre-wrap;"
                                    v-html="'<span>' + item.restricMarkContent + '</span>'">
                                </div>
                            </div>

                        </div>
                    </div>
                </div> -->
                <!-- 关键词/敏感词列表 -->
                <div
                    v-if="
                    wordTabelList &&
                    wordTabelList.length > 0 &&
                    (wordsInfo.markCrux || wordsInfo.markSensitive)
                  "
                    ref="wordsSummaryContainer"
                    class="wordsSummaryContainer"
                >
                  <div class="wordsExportContainer">
                    <div
                        v-if="showWordTable"
                        style="
                        display: flex;
                        align-items: center;
                        cursor: pointer;
                      "
                        @click="showWordTableHandle(false)"
                    >
                      <img
                          src="@/assets/images/word_list_down.png"
                          style="width: 16px; height: 16px"
                      />
                      <div>关键词汇总</div>
                    </div>
                    <div
                        v-if="!showWordTable"
                        style="
                        display: flex;
                        align-items: center;
                        cursor: pointer;
                      "
                        @click="showWordTableHandle(true)"
                    >
                      <img
                          src="@/assets/images/word_list_up.png"
                          style="width: 16px; height: 16px"
                      />
                      <div>关键词汇总</div>
                    </div>
                  </div>

                  <div v-if="showWordTable">
                    <el-table
                        :key="wordTabelRefresh"
                        :data="wordTabelList"
                        :header-cell-style="{ background: 'rgb(245,247,249)' }"
                        border
                        max-height="300"
                        size="small"
                        style="width: 100%; margin-top: 6px"
                    >
                      <el-table-column
                          align="center"
                          label="类型"
                          prop="wordsTypeStr"
                          width="110"
                      >
                      </el-table-column>
                      <el-table-column
                          align="center"
                          label="分类"
                          prop="typeStr"
                          width="110"
                      >
                      </el-table-column>
                      <el-table-column
                          align="center"
                          label="词语"
                          prop="name"
                          width="160"
                      >
                        <!-- <template slot-scope="scope">
                    <span @click="clickWord(scope.row.name)" style="cursor: pointer;">{{
                        scope.row.name
                    }}</span>
                </template> -->
                      </el-table-column>
                      <el-table-column
                          align="center"
                          label="次数"
                          prop="countNum"
                          width="130"
                      >
                        <template #default="scope">
                          <div
                              style="
                              display: flex;
                              align-items: center;
                              justify-content: space-between;
                            "
                          >
                            <SvgIcon
                                :iconStyle="{ width: '12px', height: '12px' }"
                                name="toLeft"
                                @click="prevWords(scope.row)"
                            />
                            <div
                                v-if="
                                wordIndexInfo.name == scope.row.name &&
                                wordIndexInfo.wordsType === scope.row.wordsType
                              "
                            >
                              {{ wordIndexInfo.wordIndex + 1 + '/' }}
                            </div>
                            <div>{{ scope.row.countNum }}</div>
                            <SvgIcon
                                :iconStyle="{ width: '12px', height: '12px' }"
                                name="toRight"
                                @click="nextWords(scope.row)"
                            />
                          </div>
                        </template>
                      </el-table-column>
                      <el-table-column
                          align="center"
                          label="来源"
                          prop="resourceTypeStr"
                          width="110"
                      >
                      </el-table-column>
                      <el-table-column
                          align="center"
                          label="场景描述"
                          prop="remarks"
                          show-overflow-tooltip
                      >
                      </el-table-column>
                      <!-- <el-table-column prop="platformTypeStr" label="平台" align="center">
                      </el-table-column> -->
                      <el-table-column
                          align="center"
                          label="行业"
                          prop="tradeStr"
                          width="260"
                      >
                      </el-table-column>

                      <!-- <el-table-column prop="levelStr" label="等级" align="center">
                      </el-table-column> -->
                    </el-table>
                    <!-- 分页 -->
                    <div class="page-style">
                      <el-pagination
                          :current-page="pageIndex"
                          :page-size="pageSize"
                          :page-sizes="[2, 4, 6, 8]"
                          :total="totalCount"
                          layout="total, sizes, prev, pager, next, jumper"
                          style="text-align: center; margin-top: 6px"
                          @size-change="sizeChangeHandle"
                          @current-change="currentChangeHandle"
                      >
                      </el-pagination>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </template>
      </div>
    </div>
    <!-- </el-dialog> -->
    <AiAnalysisDialog
        v-if="AiAnalysisDialogVisible"
        ref="AiAnalysisD"
    ></AiAnalysisDialog>
  </div>
</template>

<script>
import myUtils from '@/utils/utils'
import AiAnalysisDialog from './AiAnalysisDialog.vue'
import api from '@/utils/request-api'

export default {
  components: {
    AiAnalysisDialog
  },
  computed: {
    // 将marks转换为对象
    marksObject() {
      return this.marks.reduce((acc, mark) => {
        acc[mark.markTime] = mark.markText
        return acc
      }, {})
    }
  },
  data() {
    return {
      visible: false,
      showWordTable: true,
      initedTabalSize: false,
      showVisible: false,
      keywordDialogVisible: false,
      complaintDialogVisible: false,
      AiAnalysisDialogVisible: false,
      // 存放选中的文本
      selectedText: '',
      // 自定义菜单
      contextmenuVisible: false,
      keywordMarkVisible: false, // 标记关键词弹窗
      videoDurationInt: 0, // 视频时长，单位：秒
      sliderValue: 0, // 进度条绑定值
      marks: [], // 进度条标记
      silderActive: false, // 控制视频进度条显示，视频加载后才显示
      videoAndTextActive: 'textActive', // 文字识别和视频标记切换
      customerServiceQrCodeVisible: false, // 显示客服二维码弹窗
      markDialogVisible: false, // 标注资源确定弹窗
      pageSize: 8,
      totalCount: 0,
      pageIndex: 1,
      wordTabelList: [],
      videoCurrentTime: 0, // 播放器当前进度，毫秒
      // 播放器参数
      playerOptions: {
        // playbackRates: [0.7, 1.0, 1.5, 2.0], //播放速度
        autoplay: false, // 如果true,浏览器准备好时开始播放。
        muted: false, // 默认情况下将会消除任何音频。
        loop: true, // 循环播放
        preload: 'auto', // 建议浏览器在<video>加载元素后是否应该开始下载视频数据。auto浏览器选择最佳行为,立即开始加载视频（如果浏览器支持）
        language: 'zh-CN',
        // aspectRatio: '16:9', // 将播放器置于流畅模式，并在计算播放器的动态大小时使用该值。值应该代表一个比例 - 用冒号分隔的两个数字（例如"16:9"或"4:3"）
        fluid: true, // 当true时，Video.js player将拥有流体大小。换句话说，它将按比例缩放以适应其容器。
        sources: [
          // {
          //     type: 'video/mp4', // 这里的种类支持很多种：基本视频格式、直播、流媒体等，具体可以参看git网址项目
          //     src: this.sentenceMarkData.playUrl // url地址
          // }
        ],
        hls: true,
        notSupportedMessage: '此视频暂无法播放，请稍后再试', // 允许覆盖Video.js无法播放媒体源时显示的默认信息。
        controlBar: {
          timeDivider: false, // 当前时间和持续时间的分隔符
          durationDisplay: false, // 显示持续时间
          remainingTimeDisplay: true, // // 是否显示剩余时间功能
          fullscreenToggle: true // 全屏按钮
        }
      },
      // 关键词/敏感词数据
      wordsInfo: {
        wordsList: [], // 关键词/敏感词列表
        cruxWordsNum: 0, // 关键词个数
        sensitiveWordsNum: 0, // 敏感词个数
        markCrux: true, // 是否标注关键词
        markSensitive: true // 是否标注敏感词
      },
      sentenceMarkList: null,
      videoInfo: null,
      anchorInfo: null,
      fileInfo: null,
      playUrl: '',
      wordsBodyContainerHeight: '',
      tradeTreeList: [],
      tradeIdArr: [],
      oldTradeIdArr: [],
      wordIndexInfo: {
        id: '',
        name: '',
        wordIndex: 0,
        paragraphIndex: 0,
        wordsType: '',
        recordNeedsWordList: []
      },
      // 搜索标注关键字
      searchWordIndexInfo: {
        name: '',
        wordIndex: 0,
        paragraphIndex: 0,
        count: 0
      },
      durationStr: '',
      currentParagraphIndex: 0, // 当前段落
      videoDuration: '', // 视频时长
      charCountNum: '', // 文字总数量
      analysisChar: false, // 显示语速分析
      wordTabelRefresh: false, // 用于重新渲染词语列表
      isMark: 0, // 是否允许标注，0：否 1：是
      sufficient: false, // 资源是否足够
      duration: '', // 视频时长，单位：分钟
      wordNum: '', // 文字总数量
      userProperty: {}, // 用户资产信息
      markResourceType: '', // 标注的来源类型 0：初次标注 1：换行业标注

      // 视频ID
      videoId: '',
      // 文件ID
      fileId: '',
      // 文章文本
      essayText: '',
      // 视频分析时间、在线人数
      videoOnlineNumList: {},
      // AI分析时需要带着的行业ID
      aiAnalysisTradeId: 0
    }
  },
  watch: {
    videoCurrentTime(newValue, oldValue) {
      // 设置进度条的值
      this.sliderValue = parseInt(newValue / 1000)
    }
  },
  mounted() {
    // document.onkeydown = function (event) {
    //     if (event.ctrlKey && window.event.keyCode == 67) {	//禁用ctrl + c 功能
    //         return false;
    //     }
    // };
    // this.getAnalysisInfo();
  },
  create() {
  },

  methods: {
    init() {
      // this.visible = true;
      // this.videoId = videoIds;
      // this.fileId = fileIds;
      // 取到videoId和fileId
      this.videoId = this.$route.query.videoId || ''
      this.fileId = this.$route.query.fileId || ''
      this.getAnalysisInfo()
    },

    // 打开AI分析弹窗
    clickAiAnalysis() {
      // this.AiAnalysisDialogVisible = true;
      let videoId = this.videoId
      let fileId = this.fileId
      let aiAnalysisTradeId = this.aiAnalysisTradeId
      let text = ''
      // 判断是视频还是txt文本
      if (
          this.sentenceMarkList[0].startTime != undefined &&
          this.sentenceMarkList[0].startTime >= 0
      ) {
        // 增加当前主播名称
        if (this.anchorInfo != null) {
          text += '这场直播的主播名称为：' + this.anchorInfo.anchorName + '\n'
        }

        // 判断是录制视频还是上传视频
        if (this.videoInfo != null) {
          // 录制视频分析：有自然时间，在线人数，文本开始时间
          this.sentenceMarkList.forEach((sentenceItem) => {
            text += '该段的自然时间：' + sentenceItem.naturalTime
            text += '；在线人数：' + sentenceItem.onlinePeopleObj.number
            text +=
                '；视频开始时间：' +
                myUtils.toformatTime(sentenceItem.startTime) +
                '\n'
            text += sentenceItem.content + '\r\n\r'
            text += '\n'
          })
        } else {
          // 上传视频分析：只有文本开始时间
          this.sentenceMarkList.forEach((sentenceItem) => {
            text +=
                '视频开始时间：' +
                myUtils.toformatTime(sentenceItem.startTime) +
                '\n'
            text += sentenceItem.content + '\r\n\r'
            text += '\n'
          })
        }
      } else {
        // txt文本
        this.sentenceMarkList.forEach((sentenceItem) => {
          text += sentenceItem.content + '\r\n\r'
        })
      }

      // 根据videoId和fileId非空的情况将其作为存入分析文本的对应key
      if (videoId != '' && videoId != null) {
        sessionStorage.setItem(videoId, JSON.stringify({text}))
      } else if (fileId != '' && fileId != null) {
        sessionStorage.setItem(fileId, JSON.stringify({text}))
      }

      const url = this.$router.resolve({
        name: 'aiAnalysis',
        query: {
          videoId: videoId,
          fileId: fileId,
          aiAnalysisTradeId: aiAnalysisTradeId
        }
      })
      window.open(url.href, '_blank')

      // this.$nextTick(() =>{
      //     this.$refs.AiAnalysisD.init(videoId,fileId,text);
      // })
    },
    // 显隐词语列表
    showWordTableHandle(flag) {
      this.showWordTable = flag
      this.getWordsBodyContainerHeight()
    },
    // 格式化滑块弹窗信息
    tooltip(value) {
      return myUtils.toformatTimeMM_ssChinse(value * 1000)
    },
    // 获取在线复盘信息
    getAnalysisInfo() {
      let videoId = this.videoId
      let fileId = this.fileId
      this.sentenceMarkList = []
      api.AnchorVideo.getAnalysisInfo({fileId, videoId}).then((res) => {
        if (res.code == 0 && res.data) {
          // 在线人数列表
          let onlineNumList = res.data.onlineNumList
          this.videoOnlineNumList = res.data.onlineNumList
          if (onlineNumList != null && onlineNumList.length > 0) {
            onlineNumList.sort((a, b) =>
                a.recordDate.localeCompare(b.recordDate)
            )
          }

          // 设置视频/文件/主播信息
          this.videoInfo = res.data.videoInfo
          this.anchorInfo = res.data.anchorInfo
          this.fileInfo = res.data.uploadFile

          if (this.fileInfo == null) {
            this.fileInfo = {fileType: ''}
          } else {
            this.aiAnalysisTradeId = this.fileInfo.tradeId
          }
          // 设置段落信息
          if (res.data.analysisList && res.data.analysisList.length > 0) {
            res.data.analysisList.forEach((item) => {
              if (item.status == 0) {
                this.sentenceMarkList.push(JSON.parse(item.dataJson))
              }
            })
            this.sentenceMarkList.forEach((item, index) => {
              if (item.items && item.items.length > 0) {
                if (index == 0) {
                  item.startTime = 0
                } else {
                  item.startTime = item.items[0].startTime
                }
                item.endTime = item.items[item.items.length - 1].endTime
              }

              // 设置每个段落的自然时间
              if (this.videoInfo != null) {
                this.aiAnalysisTradeId = this.videoInfo.tradeId
                let videoStartTime = this.videoInfo.startTime.substring(11)
                item.naturalTime = myUtils.toformatTime(
                    myUtils.toSecond(videoStartTime) * 1000 + item.startTime
                )

                // 设置每个段落的在线人数情况
                let onlinePeopleObj = {
                  number: 0,
                  difference: 0,
                  show: false
                }

                if (onlineNumList != null && onlineNumList.length > 0) {
                  let startTimeSecond =
                      myUtils.toSecondByDate(this.videoInfo.startTime) +
                      Math.floor(item.startTime / 1000)
                  let endTimeSecond =
                      myUtils.toSecondByDate(this.videoInfo.startTime) +
                      Math.floor(item.endTime / 1000)

                  onlineNumList.forEach((onlineNumItem) => {
                    let onlineNumRecordSecond = myUtils.toSecondByDate(
                        onlineNumItem.recordDate
                    )
                    if (
                        startTimeSecond < onlineNumRecordSecond &&
                        onlineNumRecordSecond < endTimeSecond
                    ) {
                      if (onlineNumItem.peopleNum.indexOf('万') != -1) {
                        onlineNumItem.peopleNum = parseInt(
                            onlineNumItem.peopleNum.split('万')[0] * 10000
                        )
                      }
                      onlinePeopleObj.number = parseInt(onlineNumItem.peopleNum)
                    }
                  })

                  if (index > 0) {
                    // 如果当前段落人数为0，取上一段的人数
                    if (onlinePeopleObj.number == 0) {
                      let upItem = this.sentenceMarkList[index - 1]
                      onlinePeopleObj.number = upItem.onlinePeopleObj.number
                    }

                    // 如果最后是一段，并且没有匹配上在线人数，取最后一条在线人数
                    if (
                        index + 1 == this.sentenceMarkList.length &&
                        onlinePeopleObj.number == 0
                    ) {
                      onlinePeopleObj.number = parseInt(
                          onlineNumList[onlineNumList.length - 1].peopleNum
                      )
                    }

                    onlinePeopleObj.difference =
                        onlinePeopleObj.number -
                        this.sentenceMarkList[index - 1].onlinePeopleObj.number
                  }

                  onlinePeopleObj.show = true
                  item.onlinePeopleObj = onlinePeopleObj
                } else {
                  item.onlinePeopleObj = onlinePeopleObj
                }
              }
            })
          }
          // 设置播放地址
          if (res.data.playUrl) {
            this.playUrl = res.data.playUrl
            this.playerOptions.sources.push({
              type: 'video/mp4',
              src: res.data.playUrl
            })
          }

          // 计算时长
          if (this.videoInfo && this.videoInfo.duration) {
            this.durationStr = myUtils.toformatHourChinse(
                this.videoInfo.duration * 1000
            )
          }
          this.showVisible = true
          this.getTradeTreeList()
          this.countWords()
          this.getWordsBodyContainerHeight()
        } else {
          this.$message.error('复盘数据不存在，请检查链接是否正确')
        }
      })
    },
    // 获取行业列表树形
    getTradeTreeList() {
      this.tradeTreeList = []
      api.trade.listTree({}).then((res) => {
        if (res && res.code === 0) {
          this.tradeTreeList = res.data
          this.tradeIdArr =
              this.videoInfo && this.videoInfo.videoId
                  ? this.videoInfo.tradeId
                  : this.fileInfo.tradeId
          this.oldTradeIdArr = this.tradeIdArr
        }
      })
    },
    // 点击敏感词
    clickWord(word) {
      this.selectedText = word
      this.keywordDialogVisible = true
      this.$nextTick(() => {
        this.$refs.contextmenuDialog.init()
      })
    },
    // 选中文本
    handleSelectText(event) {
      // 获取当前选中的文本
      const selection = window.getSelection()
      event.preventDefault()
      if (selection.rangeCount > 0) {
        const selectedTextString = selection.toString()
        this.selectedText = selectedTextString.trim()
      }
    },

    //右键打开自定义菜单
    rightTickContextMenu(event) {
      this.contextmenuVisible = true
      this.$nextTick(() => {
        this.$refs.contextmenu.rightContextMenu(event)
      })
    },
    // 滑块值改变的回调
    sliderChange(value) {
      this.playerReadied(value * 1000)
    },
    // 根据时长划分
    marksTitle() {
      let time = this.videoDurationInt
      let arr = []
      arr.push({markTime: 0, markText: '00:00'})
      for (let i = 0; time > 0; i++) {
        time = time - 1800
        if (time > 0) {
          let time = (i + 1) * 1800
          let str = ((i + 1) * 1800) / 60 + 'min'
          arr.push({markTime: time, markText: str})
        }
      }
      arr.push({
        markTime: this.videoDurationInt,
        markText: myUtils.toformatTimeMM_ss(this.videoDurationInt * 1000)
      })
      if (arr.length > 2) {
        arr.splice(-2, 1)
      }

      this.marks = arr
    },
    // 接受子组件传来的数据
    videoProgress(videoSecond) {
      // 赋值给进度条
      this.videoCurrentTime = videoSecond * 1000
      // 跳转到视频进度
      this.playerReadied(videoSecond * 1000)
    },
    // 取消消耗资源标注
    cancalMark() {
      this.markDialogVisible = false
      if (this.markResourceType == 1) {
        this.tradeIdArr = this.oldTradeIdArr
        // 关闭级联列表下拉
        this.$refs.tradeCascader.dropDownVisible = false
      }
    },

    // 显示客服二维码
    showCustomerServiceQrCode() {
      this.markDialogVisible = false
      this.customerServiceQrCodeVisible = true
      this.$nextTick(() => {
        this.$refs.customerServiceQrCode.init()
      })
    },
    // 复制分享链接
    copyShareAnalysisLink() {
      this.$message.error('功能暂未开放')
    },

    // 初始化词语跳转信息
    initWordIndexInfo() {
      this.totalCount = 0
      this.pageIndex = 1
      this.wordIndexInfo = {
        name: '',
        wordIndex: 0,
        paragraphIndex: 0
      }
      this.searchWordIndexInfo = {
        name: '',
        wordIndex: 0,
        paragraphIndex: 0,
        count: 0
      }
    },
    // 视频加载完成回调事件
    canplay() {
      if (this.$refs.videoPlayer) {
        this.videoDuration = this.$refs.videoPlayer.player.duration()
        this.charCountNum = 0
        // 计算语速
        if (this.sentenceMarkList && this.sentenceMarkList.length > 0) {
          this.sentenceMarkList.forEach((item) => {
            let tempContent = item.content
                .replaceAll('，', '')
                .replaceAll('。', '')
                .replaceAll('？', '')
                .replaceAll('、', '')
            this.charCountNum += tempContent.length
            item.charNumSecond =
                (tempContent.length / ((item.endTime - item.startTime) / 1000)) *
                60
          })
        }

        this.videoDurationInt = Math.round(this.videoDuration)
        this.marksTitle()
        this.silderActive = true
      }
    },
    // 跳转到下一个词语
    nextWords(obj) {
      if (
          this.wordIndexInfo.name &&
          this.wordIndexInfo.name == obj.name &&
          this.wordIndexInfo.wordsType === obj.wordsType
      ) {
        if (this.wordIndexInfo.wordIndex >= obj.countNum - 1) {
          this.wordIndexInfo.wordIndex = 0
        } else {
          this.wordIndexInfo.wordIndex++
        }
      } else {
        // 没有存在，从第一段开始找
        this.wordIndexInfo.id = obj.id
        this.wordIndexInfo.name = obj.name
        this.wordIndexInfo.wordIndex = 0
        this.wordIndexInfo.wordsType = obj.wordsType
        this.recordNeedsWordList = obj.recordNeedsWordList
      }

      let oldParagraphIndex = this.wordIndexInfo.paragraphIndex
      this.markSearch()
      this.$nextTick(() => {
        if (this.wordIndexInfo.paragraphIndex != oldParagraphIndex) {
          this.$refs.dom[this.wordIndexInfo.paragraphIndex].scrollIntoView({
            behavior: 'smooth'
          })
        }
        if (this.$refs.videoPlayer) {
          this.$refs.videoPlayer.player.pause()
        }

        this.wordTabelRefresh = !this.wordTabelRefresh
      })
    },
    // 跳转到上一个词语
    prevWords(obj) {
      if (
          this.wordIndexInfo.name &&
          this.wordIndexInfo.name == obj.name &&
          this.wordIndexInfo.wordsType === obj.wordsType
      ) {
        if (this.wordIndexInfo.wordIndex <= 0) {
          this.wordIndexInfo.wordIndex = obj.countNum - 1
        } else {
          this.wordIndexInfo.wordIndex--
        }
      } else {
        // 没有存在，从最后一段开始找
        this.wordIndexInfo.id = obj.id
        this.wordIndexInfo.name = obj.name
        this.wordIndexInfo.wordIndex = obj.countNum - 1
        this.wordIndexInfo.wordsType = obj.wordsType
        this.recordNeedsWordList = obj.recordNeedsWordList
      }

      let oldParagraphIndex = this.wordIndexInfo.paragraphIndex
      this.markSearch()
      this.$nextTick(() => {
        if (this.wordIndexInfo.paragraphIndex != oldParagraphIndex) {
          this.$refs.dom[this.wordIndexInfo.paragraphIndex].scrollIntoView({
            behavior: 'smooth'
          })
        }
        if (this.$refs.videoPlayer) {
          this.$refs.videoPlayer.player.pause()
        }

        this.wordTabelRefresh = !this.wordTabelRefresh
      })
    },
    // 跳转到下一个搜索词
    nextSearchWords() {
      if (
          this.searchWordIndexInfo.wordIndex >=
          this.searchWordIndexInfo.count - 1
      ) {
        this.searchWordIndexInfo.wordIndex = 0
      } else {
        this.searchWordIndexInfo.wordIndex++
      }
      let oldParagraphIndex = this.searchWordIndexInfo.paragraphIndex
      this.markSearch(true)
      this.$nextTick(() => {
        if (oldParagraphIndex != this.searchWordIndexInfo.paragraphIndex) {
          this.$refs.dom[
              this.searchWordIndexInfo.paragraphIndex
              ].scrollIntoView({behavior: 'smooth'})
          if (this.$refs.videoPlayer) {
            this.$refs.videoPlayer.player.pause()
          }
        }
      })
    },
    // 跳转到上一个搜索词
    prevSearchWords() {
      if (this.searchWordIndexInfo.wordIndex <= 0) {
        this.searchWordIndexInfo.wordIndex = this.searchWordIndexInfo.count - 1
      } else {
        this.searchWordIndexInfo.wordIndex--
      }
      let oldParagraphIndex = this.searchWordIndexInfo.paragraphIndex
      this.markSearch(true)
      this.$nextTick(() => {
        if (oldParagraphIndex != this.searchWordIndexInfo.paragraphIndex) {
          this.$refs.dom[
              this.searchWordIndexInfo.paragraphIndex
              ].scrollIntoView({behavior: 'smooth'})
          if (this.$refs.videoPlayer) {
            this.$refs.videoPlayer.player.pause()
          }
        }
      })
    },
    // 切换分页大小回调
    sizeChangeHandle(val) {
      this.pageIndex = 1
      this.pageSize = val
      this.addWordTabelList()
    },
    // 切换分页回调
    currentChangeHandle(val) {
      this.pageIndex = val
      this.addWordTabelList()
    },
    // 将词语列表分页添加到表格
    addWordTabelList() {
      this.wordTabelList = []

      if (this.wordsInfo.wordsList && this.wordsInfo.wordsList.length > 0) {
        let tempArr = []
        this.wordsInfo.wordsList.forEach((item) => {
          if (this.wordsInfo.markCrux && item.wordsType == 1) {
            tempArr.push(item)
          }
          if (this.wordsInfo.markSensitive && item.wordsType == 0) {
            tempArr.push(item)
          }
        })

        if (tempArr.length > 0) {
          let startIndex = this.pageSize * (this.pageIndex - 1)
          let endIndex = startIndex + this.pageSize
          for (let i = startIndex; i < endIndex; i++) {
            if (tempArr[i]) {
              this.wordTabelList.push(tempArr[i])
            }
          }
        }
        this.totalCount = tempArr.length

        this.getWordsBodyContainerHeight()
      }
    },
    // 计算文本段落区域的高度
    getWordsBodyContainerHeight() {
      this.$nextTick(() => {
        this.wordsBodyContainerHeight = {'max-height': '60%'}
        if (!this.initedTabalSize) {
          this.pageSize = 6
          this.pageIndex = 1
          this.addWordTabelList()
          this.initedTabalSize = true
        }
      })
    },
    // 导出文字内容
    exportTxt() {
      let text = ''
      this.sentenceMarkList.forEach((sentenceItem) => {
        text += sentenceItem.content + '\r\n\r\n'
      })
      // 创建 Blob 对象
      const blob = new Blob([text], {type: 'text/plain;charset=utf-8'})
      // 创建 URL
      const url = URL.createObjectURL(blob)
      // 创建隐藏的可下载链接
      const link = document.createElement('a')
      link.href = url
      let fileName =
          this.videoInfo && this.videoInfo.videoId
              ? this.videoInfo.videoName
              : this.fileInfo.fileName
      link.setAttribute('download', fileName + '.txt') // 设置下载的文件名
      link.style.display = 'none'
      // 添加到 DOM
      document.body.appendChild(link)
      // 触发点击事件
      link.click()
      // 清理工作
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    },
    // 标注/取消标注按钮回调
    markClick(type, flag) {
      if (type == 'sensitive') {
        // 敏感词
        this.wordsInfo.markSensitive = flag
      } else if (type == 'crux') {
        // 关键词
        this.wordsInfo.markCrux = flag
      }
      this.$emit('countWords', JSON.parse(JSON.stringify(this.wordsInfo)))
      this.mark()
      this.markSearch()
      this.pageIndex = 1
      this.addWordTabelList()
    },
    // 搜索关键字
    markKeywords() {
      this.markSearch()
      this.$nextTick(() => {
        if (this.searchWordIndexInfo.name) {
          // 跳转到第一个搜索词所在的段落
          let index = -1
          for (let i = 0; i < this.sentenceMarkList.length; i++) {
            let item = this.sentenceMarkList[i]
            if (item.content.indexOf(this.searchWordIndexInfo.name) != -1) {
              index = i
              break
            }
          }
          if (index != -1) {
            this.$refs.dom[index].scrollIntoView({behavior: 'smooth'})
            if (this.$refs.videoPlayer) {
              this.$refs.videoPlayer.player.pause()
            }
          }
        }
      })
    },
    // 标注搜索词和跳转词
    markSearch(isSearch) {
      this.searchWordIndexInfo.count = 0
      if (this.sentenceMarkList) {
        let wordIndex = 0

        this.sentenceMarkList.forEach((sentenceItem, index) => {
          sentenceItem.searchMarkContent = sentenceItem.content

          // 标注搜索词
          if (this.searchWordIndexInfo.name) {
            sentenceItem.searchMarkContent =
                sentenceItem.searchMarkContent.replace(
                    new RegExp(this.searchWordIndexInfo.name, 'g'),
                    '<span style=\'background: darkorange\'>' +
                    this.searchWordIndexInfo.name +
                    '</span>'
                )
            // 累计搜索词的数量
            const regex = new RegExp(this.searchWordIndexInfo.name, 'g')
            let count = sentenceItem.searchMarkContent.match(regex)
                ? sentenceItem.searchMarkContent.match(regex).length
                : 0
            this.searchWordIndexInfo.count += count
          }

          if (isSearch) {
            // 标注上一个/下一个搜索词
            const regex = new RegExp(this.searchWordIndexInfo.name, 'g')
            let count = sentenceItem.searchMarkContent.match(regex)
                ? sentenceItem.searchMarkContent.match(regex).length
                : 0

            for (let i = 0; i < count; i++) {
              if (wordIndex == this.searchWordIndexInfo.wordIndex) {
                let text = sentenceItem.searchMarkContent
                // 找到第i个词的索引
                let searchStartIndex = 0
                for (let j = 0; j <= i; j++) {
                  if (j == 0) {
                    // 从第0个字符开始找
                    searchStartIndex = text.indexOf(
                        this.searchWordIndexInfo.name,
                        0
                    )
                  } else {
                    // 从上次索引+词语长度开始找
                    searchStartIndex = text.indexOf(
                        this.searchWordIndexInfo.name,
                        searchStartIndex + this.searchWordIndexInfo.name.length
                    )
                  }
                }

                // 拼接标注颜色
                sentenceItem.searchMarkContent = text.substring(
                    0,
                    searchStartIndex
                )
                sentenceItem.searchMarkContent +=
                    '<span style=\'background:indianred\'>' +
                    this.searchWordIndexInfo.name +
                    '</span>'
                sentenceItem.searchMarkContent += text.substring(
                    searchStartIndex + this.searchWordIndexInfo.name.length
                )

                // 要跳转的段落索引
                this.searchWordIndexInfo.paragraphIndex = index
              }
              wordIndex++
            }
          } else if (this.wordIndexInfo.name) {
            // 标注上一个/下一个词语
            // 计算当前段落出现词语的次数
            const regex = new RegExp(this.wordIndexInfo.name, 'g')
            let count = sentenceItem.searchMarkContent.match(regex)
                ? sentenceItem.searchMarkContent.match(regex).length
                : 0

            // 遍历当前段落当前词语需要标注的列表
            let recordNeedsWordList = []
            sentenceItem.wordsList.forEach((item) => {
              if (
                  item.name == this.wordIndexInfo.name &&
                  item.wordsType === this.wordIndexInfo.wordsType
              ) {
                recordNeedsWordList = item.recordNeedsWordList
              }
            })

            for (let i = 0; i < count; i++) {
              recordNeedsWordList.forEach((recordNeedsWordItem) => {
                if (recordNeedsWordItem.recordNeedsNum == i) {
                  if (wordIndex == this.wordIndexInfo.wordIndex) {
                    let text = sentenceItem.searchMarkContent
                    // 找到第i个词的索引
                    let searchStartIndex = 0
                    for (let j = 0; j <= i; j++) {
                      if (j == 0) {
                        // 从第0个字符开始找
                        searchStartIndex = text.indexOf(
                            this.wordIndexInfo.name,
                            0
                        )
                      } else {
                        // 从上次索引+词语长度开始找
                        searchStartIndex = text.indexOf(
                            this.wordIndexInfo.name,
                            searchStartIndex + this.wordIndexInfo.name.length
                        )
                      }
                    }

                    // 拼接标注颜色
                    sentenceItem.searchMarkContent = text.substring(
                        0,
                        searchStartIndex
                    )
                    sentenceItem.searchMarkContent +=
                        '<span style=\'background:limegreen\'>' +
                        this.wordIndexInfo.name +
                        '</span>'
                    sentenceItem.searchMarkContent += text.substring(
                        searchStartIndex + this.wordIndexInfo.name.length
                    )

                    // 要跳转的段落索引
                    this.wordIndexInfo.paragraphIndex = index
                  }
                  wordIndex++
                }
              })
            }
          }
        })
      }
    },
    // 标注段落的关键词
    mark() {
      this.searchWordIndexInfo.count = 0
      if (this.sentenceMarkList) {
        this.sentenceMarkList.forEach((sentenceItem, index) => {
          sentenceItem.markContent = sentenceItem.content
          sentenceItem.restricMarkContent = sentenceItem.content

          // 判断当前段落有没有词语需要标注
          if (sentenceItem.wordsList && sentenceItem.wordsList.length > 0) {
            // 按词语长度倒序排序，保证被覆盖的词语也能正确标注
            // let tempWordsList = JSON.parse(JSON.stringify(this.wordsInfo.wordsList));
            let tempWordsList = JSON.parse(
                JSON.stringify(sentenceItem.wordsList)
            )
            tempWordsList.sort((a, b) => b.name.length - a.name.length)

            // 标注段落中的敏感词
            if (this.wordsInfo.markSensitive) {
              tempWordsList.forEach((wordItem) => {
                if (
                    wordItem.wordsType == 0 &&
                    sentenceItem.content.indexOf(wordItem.name) != -1
                ) {
                  // 获取当前段落存在词语的数量
                  const regex = new RegExp(wordItem.name, 'g')
                  let count = sentenceItem.markContent.match(regex)
                      ? sentenceItem.markContent.match(regex).length
                      : 0
                  // 遍历段落中的每个词语，判断是否需要标注
                  let wordCurrentINdex = 0
                  let wordCurrentIndexRestrist = 0
                  for (let i = 0; i < count; i++) {
                    // 当前敏感词下标，用于标注敏感词
                    wordCurrentINdex =
                        wordCurrentINdex == 0
                            ? 0
                            : wordCurrentINdex + wordItem.name.length
                    wordCurrentINdex = sentenceItem.markContent.indexOf(
                        wordItem.name,
                        wordCurrentINdex
                    )
                    // 当前敏感词下标，用于标注限定词
                    wordCurrentIndexRestrist =
                        wordCurrentIndexRestrist == 0
                            ? 0
                            : wordCurrentIndexRestrist + wordItem.name.length
                    wordCurrentIndexRestrist =
                        sentenceItem.restricMarkContent.indexOf(
                            wordItem.name,
                            wordCurrentIndexRestrist
                        )

                    wordItem.recordNeedsWordList.forEach(
                        (recordNeedsWordItem) => {
                          if (recordNeedsWordItem.recordNeedsNum == i) {
                            // 标注敏感词
                            let leftContent = sentenceItem.markContent.substring(
                                0,
                                wordCurrentINdex
                            )
                            let markWordContent =
                                '<span style=\'background: pink\'>' +
                                wordItem.name +
                                '</span>'
                            let rightContent = sentenceItem.markContent.substring(
                                wordCurrentINdex + wordItem.name.length
                            )
                            sentenceItem.markContent =
                                leftContent + markWordContent + rightContent
                            wordCurrentINdex =
                                wordCurrentINdex +
                                markWordContent.length -
                                wordItem.name.length

                            // 存在限定词，标注限定词
                            if (recordNeedsWordItem.restrictWord) {
                              let markRestricLeft = false
                              let restrictWordContent =
                                  '<span style=\'background: gold\'>' +
                                  recordNeedsWordItem.restrictWord +
                                  '</span>'

                              // 标注左边限定词
                              let restrictLeftText =
                                  sentenceItem.restricMarkContent.substring(
                                      0,
                                      wordCurrentIndexRestrist
                                  )
                              // 取出左边限定词范围内容
                              let rangeLeftText = restrictLeftText.substring(
                                  restrictLeftText.length -
                                  recordNeedsWordItem.restrictRange <
                                  0
                                      ? 0
                                      : restrictLeftText.length -
                                      recordNeedsWordItem.restrictRange
                              )
                              if (
                                  rangeLeftText.indexOf(
                                      recordNeedsWordItem.restrictWord
                                  ) != -1
                              ) {
                                markRestricLeft = true
                                let restrictIndex = restrictLeftText.lastIndexOf(
                                    recordNeedsWordItem.restrictWord
                                )
                                let restrictLeftContent =
                                    restrictLeftText.substring(0, restrictIndex)
                                let restrictRightConent =
                                    restrictLeftText.substring(
                                        restrictIndex +
                                        recordNeedsWordItem.restrictWord.length
                                    )

                                restrictLeftText =
                                    restrictLeftContent +
                                    restrictWordContent +
                                    restrictRightConent
                              }

                              let restrictRightText =
                                  sentenceItem.restricMarkContent.substring(
                                      wordCurrentIndexRestrist + wordItem.name.length
                                  )
                              // 左边还没有标注限定词
                              if (!markRestricLeft) {
                                // 取出右边限定词范围内容
                                let rangeRightText = restrictRightText.substring(
                                    0,
                                    recordNeedsWordItem.restrictRange >
                                    restrictRightText.length
                                        ? restrictRightText.length
                                        : recordNeedsWordItem.restrictRange
                                )
                                if (
                                    rangeRightText.indexOf(
                                        recordNeedsWordItem.restrictWord
                                    ) != -1
                                ) {
                                  let restrictIndex = rangeRightText.indexOf(
                                      recordNeedsWordItem.restrictWord
                                  )
                                  let restrictLeftContent =
                                      restrictRightText.substring(0, restrictIndex)
                                  let restrictRightConent =
                                      restrictRightText.substring(
                                          restrictIndex +
                                          recordNeedsWordItem.restrictWord.length
                                      )

                                  restrictRightText =
                                      restrictLeftContent +
                                      restrictWordContent +
                                      restrictRightConent
                                }
                              }

                              if (markRestricLeft) {
                                wordCurrentIndexRestrist =
                                    wordCurrentIndexRestrist +
                                    restrictWordContent.length -
                                    recordNeedsWordItem.restrictWord.length
                              }

                              sentenceItem.restricMarkContent =
                                  restrictLeftText +
                                  wordItem.name +
                                  restrictRightText
                            }
                          }
                        }
                    )
                  }
                }
              })
            }

            // 标注段落中的关键词
            if (this.wordsInfo.markCrux) {
              tempWordsList.forEach((wordItem) => {
                if (
                    wordItem.wordsType == 1 &&
                    sentenceItem.content.indexOf(wordItem.name) != -1
                ) {
                  // 获取当前段落存在词语的数量
                  const regex = new RegExp(wordItem.name, 'g')
                  let count = sentenceItem.markContent.match(regex)
                      ? sentenceItem.markContent.match(regex).length
                      : 0
                  // 遍历段落中的每个词语，判断是否需要标注
                  let wordCurrentINdex = 0
                  let wordCurrentIndexRestrist = 0
                  for (let i = 0; i < count; i++) {
                    // 当前关键词下标，用于标注关键词
                    wordCurrentINdex =
                        wordCurrentINdex == 0
                            ? 0
                            : wordCurrentINdex + wordItem.name.length
                    wordCurrentINdex = sentenceItem.markContent.indexOf(
                        wordItem.name,
                        wordCurrentINdex
                    )
                    // 当前关键词下标，用于标注限定词
                    wordCurrentIndexRestrist =
                        wordCurrentIndexRestrist == 0
                            ? 0
                            : wordCurrentIndexRestrist + wordItem.name.length
                    wordCurrentIndexRestrist =
                        sentenceItem.restricMarkContent.indexOf(
                            wordItem.name,
                            wordCurrentIndexRestrist
                        )

                    wordItem.recordNeedsWordList.forEach(
                        (recordNeedsWordItem) => {
                          if (recordNeedsWordItem.recordNeedsNum == i) {
                            // 标注关键词
                            let leftContent = sentenceItem.markContent.substring(
                                0,
                                wordCurrentINdex
                            )
                            let markWordContent =
                                '<span style=\'background: paleturquoise\'>' +
                                wordItem.name +
                                '</span>'
                            let rightContent = sentenceItem.markContent.substring(
                                wordCurrentINdex + wordItem.name.length
                            )
                            sentenceItem.markContent =
                                leftContent + markWordContent + rightContent
                            wordCurrentINdex =
                                wordCurrentINdex +
                                markWordContent.length -
                                wordItem.name.length

                            // 存在限定词，标注限定词
                            if (recordNeedsWordItem.restrictWord) {
                              let markRestricLeft = false
                              let restrictWordContent =
                                  '<span style=\'background: gold\'>' +
                                  recordNeedsWordItem.restrictWord +
                                  '</span>'

                              // 标注左边限定词
                              let restrictLeftText =
                                  sentenceItem.restricMarkContent.substring(
                                      0,
                                      wordCurrentIndexRestrist
                                  )
                              // 取出左边限定词范围内容
                              let rangeLeftText = restrictLeftText.substring(
                                  restrictLeftText.length -
                                  recordNeedsWordItem.restrictRange <
                                  0
                                      ? 0
                                      : restrictLeftText.length -
                                      recordNeedsWordItem.restrictRange
                              )
                              if (
                                  rangeLeftText.indexOf(
                                      recordNeedsWordItem.restrictWord
                                  ) != -1
                              ) {
                                markRestricLeft = true
                                let restrictIndex = restrictLeftText.lastIndexOf(
                                    recordNeedsWordItem.restrictWord
                                )
                                let restrictLeftContent =
                                    restrictLeftText.substring(0, restrictIndex)
                                let restrictRightConent =
                                    restrictLeftText.substring(
                                        restrictIndex +
                                        recordNeedsWordItem.restrictWord.length
                                    )

                                restrictLeftText =
                                    restrictLeftContent +
                                    restrictWordContent +
                                    restrictRightConent
                              }

                              let restrictRightText =
                                  sentenceItem.restricMarkContent.substring(
                                      wordCurrentIndexRestrist + wordItem.name.length
                                  )
                              // 左边还没有标注限定词
                              if (!markRestricLeft) {
                                // 取出右边限定词范围内容
                                let rangeRightText = restrictRightText.substring(
                                    0,
                                    recordNeedsWordItem.restrictRange >
                                    restrictRightText.length
                                        ? restrictRightText.length
                                        : recordNeedsWordItem.restrictRange
                                )
                                if (
                                    rangeRightText.indexOf(
                                        recordNeedsWordItem.restrictWord
                                    ) != -1
                                ) {
                                  let restrictIndex = rangeRightText.indexOf(
                                      recordNeedsWordItem.restrictWord
                                  )
                                  let restrictLeftContent =
                                      restrictRightText.substring(0, restrictIndex)
                                  let restrictRightConent =
                                      restrictRightText.substring(
                                          restrictIndex +
                                          recordNeedsWordItem.restrictWord.length
                                      )

                                  restrictRightText =
                                      restrictLeftContent +
                                      restrictWordContent +
                                      restrictRightConent
                                }
                              }

                              if (markRestricLeft) {
                                wordCurrentIndexRestrist =
                                    wordCurrentIndexRestrist +
                                    restrictWordContent.length -
                                    recordNeedsWordItem.restrictWord.length
                              }

                              sentenceItem.restricMarkContent =
                                  restrictLeftText +
                                  wordItem.name +
                                  restrictRightText
                            }
                          }
                        }
                    )
                  }
                }
              })
            }
          }
        })

        this.sentenceMarkList = JSON.parse(
            JSON.stringify(this.sentenceMarkList)
        )
      }
    },
    // 整理关键词、敏感词数据
    countWords() {
      this.wordsInfo.wordsList = []
      this.wordsInfo.cruxWordsNum = 0
      this.wordsInfo.sensitiveWordsNum = 0

      if (this.sentenceMarkList && this.sentenceMarkList.length > 0) {
        this.sentenceMarkList.forEach((item) => {
          let wordsList = item.wordsList
          if (wordsList && wordsList.length > 0) {
            wordsList.forEach((wordsItem) => {
              if (this.wordsInfo.wordsList.length < 1) {
                this.wordsInfo.wordsList.push(wordsItem)
              } else {
                // 判断数组里面是否已经存在当前关键词/敏感词，如果是，则只增加出现数量
                let exist = false
                this.wordsInfo.wordsList.forEach((haveItem) => {
                  if (
                      haveItem.name == wordsItem.name &&
                      haveItem.wordsType == wordsItem.wordsType
                  ) {
                    exist = true
                    haveItem.countNum += wordsItem.countNum
                  }
                })
                if (!exist) {
                  this.wordsInfo.wordsList.push(wordsItem)
                }
              }
            })
          }
        })
      }

      if (this.wordsInfo.wordsList.length > 0) {
        this.wordsInfo.wordsList.forEach((wordsItem) => {
          // 统计数量
          if (wordsItem.wordsType == 0) {
            // 敏感词
            this.wordsInfo.sensitiveWordsNum += wordsItem.countNum
          } else if (wordsItem.wordsType == 1) {
            // 关键词
            this.wordsInfo.cruxWordsNum += wordsItem.countNum
          }
        })
      }

      // 排序
      if (this.wordsInfo.wordsList && this.wordsInfo.wordsList.length > 0) {
        this.wordsInfo.wordsList.forEach((item) => {
          if (!item.groupStr) {
            item.groupStr = ''
          }
          if (!item.tradeIdArr) {
            item.tradeIdArr = '[1]'
          }
        })

        this.wordsInfo.wordsList.sort((a, b) => {
          // 先按词语类型升序排序
          if (a.wordsType !== b.wordsType) {
            return a.wordsType - b.wordsType
          }

          // 再按词语分组降序排序
          if (a.groupStr !== b.groupStr) {
            return b.groupStr.localeCompare(a.groupStr)
          }

          // 再按行业排序，从小行业到大行业
          if (a.tradeIdArr !== b.tradeIdArr) {
            return b.tradeIdArr.length - a.tradeIdArr.length
          }

          // 最后按出现的次数降序排序
          return b.countNum - a.countNum
        })
      }

      this.mark()
      this.markSearch()
      this.addWordTabelList()
      this.$emit('countWords', JSON.parse(JSON.stringify(this.wordsInfo)))
    },
    // 设置播放器进度，豪秒
    playerReadied(second) {
      if (this.$refs.videoPlayer) {
        this.$refs.videoPlayer.player.currentTime(second / 1000)
        this.$refs.videoPlayer.player.play()
      }
    },
    // 播放器进度回调
    onPlayerTimeupdate(player) {
      this.videoCurrentTime = player.cache_.currentTime * 1000

      // 滚动滚动条
      if (this.videoAndTextActive == 'textActive') {
        if (this.sentenceMarkList && this.sentenceMarkList.length > 0) {
          // 获取当前正在播放的段落索引
          let currentIndex = 0

          let setFlag = false
          for (let index = 0; index < this.sentenceMarkList.length; index++) {
            let item = this.sentenceMarkList[index]
            let nextItem =
                index + 1 == this.sentenceMarkList.length
                    ? null
                    : this.sentenceMarkList[index + 1]

            if (nextItem) {
              if (
                  item.startTime < this.videoCurrentTime &&
                  this.videoCurrentTime < nextItem.startTime
              ) {
                currentIndex = index
                setFlag = true
              }
            }
          }

          if (!setFlag) {
            currentIndex = this.sentenceMarkList.length - 1
          }

          this.sentenceMarkList.forEach((item, index) => {
            if (
                item.startTime < this.videoCurrentTime &&
                this.videoCurrentTime < item.endTime
            ) {
              currentIndex = index
            }
          })

          // 滚动滚动条到指定的段落元素
          // currentIndex -= 1;
          if (this.currentParagraphIndex != currentIndex) {
            if (currentIndex < 0) {
              this.$refs.dom[0].scrollIntoView({behavior: 'smooth'})
            } else {
              this.$refs.dom[currentIndex].scrollIntoView({
                behavior: 'smooth'
              })
            }
            this.currentParagraphIndex = currentIndex
          }
        }
      }
    },
    // 毫秒时间戳转成时分秒格式
    toformatTime(val) {
      return myUtils.toformatTime(val)
    },
    // 申诉弹窗
    Complaint() {
      const secUid = this.anchorInfo.secUid
      const videoId = this.videoInfo.videoId
      this.complaintDialogVisible = true
      this.$nextTick(() => {
        this.$refs.complaint.init(secUid, videoId)
      })
    }
  },

  beforeRouteEnter(to, from, next) {
    // 在导航完成之前执行的操作
    next((vm) => {
      // 通过 `vm` 访问组件实例
      vm.init()
    })
  }
}
</script>
<style lang="scss" scoped>
.searchMarkContent {
  z-index: 988 !important;
  letter-spacing: 3px;
  color: transparent;

  span {
    display: inline;
  }
}

.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  color: #151719;
  font-size: 16px;
  height: 100vh;
}

.markContent {
  color: transparent;
  letter-spacing: 3px;

  span {
    display: inline;
  }
}

.restricMarkContent {
  z-index: 999 !important;
  letter-spacing: 3px;

  span {
    display: inline;
  }
}

.words-box {
  z-index: 1000 !important;
  letter-spacing: 3px;

  .word {
    color: transparent;
    display: inline;
  }
}

.onlineNumDifferenceContainer {
  margin-left: 7px;
  font-size: 12px;
}

.onlineNumContainer {
  display: flex;
  align-items: center;
  font-size: 12px;
  margin-top: 8px;
  margin-left: 20px;
  color: #4d4d4d;
}

/* 快速定位文本 */
.video-slider-text {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 69px;
  height: 60px;
  border: 1px solid #dce0e7;
  border-radius: 6px 0px 0px 6px;
  font-size: 16px;
  color: #2e3742;
  font-weight: 600;
  z-index: 1;
}

/* 视频定位条 */
.video-slider {
  display: flex;
  justify-content: center;
  flex-direction: column;
  height: 60px;
  flex: 1;
  background-color: #edf6ff;
  border: 1px solid #cae3ff;
  border-radius: 6px;
  padding: 0px 20px 0px 20px;
  z-index: 2;
  margin-left: -4px;
}

/* 改变定位条原点样式 */
:deep(.el-slider__button ) {
  width: 12px;
  height: 12px;
  background-color: #0077ff;
}

/* 定位条未选中样式 */
:deep(.el-slider__runway) {
  background-color: #b8d9ff;
}

/* 文字识别和视频标记切换 */
.video-and-text {
  display: flex;
  justify-content: center;
  margin-top: 17px;
  font-size: 14px;
  color: #677583;
  cursor: pointer;
}

/* 选择文字识别或视频标记后切换样式 */
.videoAndTActiveStyle {
  color: #2e3742;
  font-size: 14px;
  padding-bottom: 3px;
  border-bottom: 2px solid #0077ff;
}

/* 联系客服 */
.contact-kefu {
  display: flex;
  align-items: center;
  position: absolute;
  right: 18px;
  bottom: 10px;
  cursor: pointer;
}

.page-style {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.official-bottom {
  position: absolute;
  right: 0;
  top: 11px;
  font-weight: normal;
}

.markDialogBtn2 {
  width: 110px;
  height: 40px;
  background: #5f3a00;
  border-radius: 4px;
  margin-left: 20px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.markDialogBtn1 {
  width: 110px;
  height: 40px;
  border-radius: 4px;
  border: 1px solid #b26d00;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.markDialogBtnContainer {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 24px;
}

.markDialogBodyText2 {
  margin-top: 10px;
}

.markDialogBodyContainer {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.markDialogCloseImg {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.markDialogTitleContainer {
  display: flex;
  width: 100%;
  justify-content: end;
  padding: 14px;
}

.markDialogContainer {
  background: linear-gradient(#ffeed9, #fffcf9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-bottom: 39px;
}

:deep(.el-dialog__header) {
  padding: 0px;
}

:deep(.el-dialog__body) {
  padding: 0;
}

:deep(.el-dialog__headerbtn ) {
  display: none;
}

:deep(.el-table--mini .el-table__cell) {
  padding: 0;
}

:deep(.el-table .el-table__cell) {
  padding: 0;
}

.wordsExportContainer {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  background: #f5f7f9;
  font-size: 13px;
  font-weight: 400;
  color: #2e3742;
  height: 28px;
}

.wordsSummaryContentItem {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 12px;
  font-weight: 400;
  color: #2e3742;
  height: 24px;
}

.wordsSummaryContentItemContainer {
  display: flex;
  align-items: center;
}

/* .wordsSummaryContentContainer::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 4px;
}

.wordsSummaryContentContainer::-webkit-scrollbar {
    width: 4px;
} */

.wordsSummaryContentContainer {
  /* overflow-y: auto; */
}

.wordsSummaryTitleContainer {
  display: flex;
  align-items: center;
}

.wordsSummaryBodyContainer {
  margin-top: 6px;
}

.wordsSummaryContainer {
  padding-top: 6px;
  font-weight: 600;
  font-size: 14px;
  color: #2e3742;
  border-top: 0.5px solid #eee;
}

.videoAndTradeContainer {
  display: flex;
  flex-direction: column;
}

.videoAndWordContainer {
  display: flex;
  margin-top: 12px;
  height: calc(100vh - 164px);
}

.rightBorder {
  border-right: 0.5px solid #dce0e7;
}

.discernSearchContainer {
  padding-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: end;
  margin-top: 10px;
}

.wordTimeHover {
  cursor: pointer;
  background: rgb(51, 109, 244);
  border-radius: 4px;
  color: #fff;
  z-index: 99;
}

.word {
  cursor: pointer;
  background: transparent;
  border-radius: 0px;
  color: rgba(0, 0, 0, 0);
}

.paragraphContainer-txt {
  font-size: 14px;
  color: #2e3742;
  margin-top: 6px;
  position: relative;
  line-height: 22px;
  letter-spacing: 1px;
}

.paragraphContainer {
  /* font-weight: 400; */
  font-size: 14px;
  color: #2e3742;
  margin-top: 6px;
  position: relative;
  line-height: 22px;
  letter-spacing: 1px;
}

.wordsBodyContentText2 {
  font-weight: 400;
  font-size: 12px;
  color: #95a1af;
  margin-left: 4px;
  margin-top: 8px;
}

.wordsBodyContentText1 {
  font-weight: 400;
  font-size: 13px;
  color: #2e3742;
  margin-top: 8px;
}

.wordsBodyContentContainer {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  margin-left: 8px;
  flex-grow: 1;
}

.wordsBodyAvatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 0.5px #ccc solid;
}

.wordsBodyItemContainer {
  margin-bottom: 12px;
  display: flex;
  word-wrap: break-word;
  word-break: break-all;
}

.wordsBodyContainer::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 4px;
}

.wordsBodyContainer::-webkit-scrollbar {
  width: 4px;
}

.wordsBodyContainer {
  overflow-y: auto;
  box-sizing: border-box;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
}

.discernContainer::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 4px;
}

.discernContainer::-webkit-scrollbar {
  width: 4px;
}

.discernContainer {
  margin-left: 12px;
  box-sizing: border-box;
  flex-grow: 1;
  width: 0;
}

:deep(.vjs-play-control) {
  display: none;
}

:deep(.video-js .vjs-big-play-button) {
  top: calc(50% - 22px);
  left: calc(50% - 42px);
}

.video {
  width: calc(100% - 0.5px);
}

.videoContainer {
  background-color: #fff;
  display: flex;
  flex-direction: column;
  align-items: end;
  box-sizing: border-box;
  margin-top: 6px;
}

.videoAnalysisContainer {
  display: flex;
  margin-top: 8px;
  flex-grow: 1;
  /* min-height: calc(100% - 191px); */
}

.cancelIndiciaBtn {
  width: 72px;
  height: 22px;
  border-radius: 4px;
  border: 1px solid #b4bcca;
  font-size: 13px;
  color: #2e3742;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 6px;
  cursor: pointer;
}

.indiciaBtn {
  width: 72px;
  height: 22px;
  border-radius: 4px;
  border: 1px solid #0077ff;
  font-size: 13px;
  color: #0077ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 6px;
  cursor: pointer;
}

.aiAnalysisClick {
  padding: 10px;
  height: 22px;
  border-radius: 4px;
  border: 1px solid #0077ff;
  font-size: 13px;
  color: #0077ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 6px;
  cursor: pointer;
}

.aiAnalysisClick:hover {
  color: orangered;
}

.disableClick {
  padding: 10px;
  height: 22px;
  border-radius: 4px;
  border: 1px solid #ddd;
  background-color: #ddd;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 6px;
  cursor: not-allowed;
}

.wordsItemText {
  font-weight: 500;
  font-size: 13px;
  color: #2e3742;
  margin-left: 4px;
}

.wordsItemColor {
  width: 24px;
  height: 16px;
  border-radius: 4px;
}

.wordsItemColorContainer {
  display: flex;
  align-items: center;
}

.wordsItemContainer {
  display: flex;
  align-items: center;
  padding: 0 10px;
}

.wordsContainer {
  height: 46px;
  background: #f5f7f9;
  border-radius: 4px;
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
}

.analysisFileRightItemContainer {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
}

.analysisFileRightContainer {
  display: flex;
  align-items: center;
  font-weight: 400;
  font-size: 13px;
  color: #2e3742;
}

.analysisCompereName {
  color: #2e3742;
  font-size: 14px;
  margin-left: 10px;
  max-width: 300px;
}

.analysisCompereImg {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 0.5px #ccc solid;
}

.analysisFileLeftContainer {
  display: flex;
  align-items: center;
}

.analysisFileContainer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.onlineAnalysisContainer {
  display: flex;
  flex-direction: column;
  /* border: 1px solid #ccc; */
  height: calc(100vh - 2px);
  padding: 10px 20px;
}
</style>
