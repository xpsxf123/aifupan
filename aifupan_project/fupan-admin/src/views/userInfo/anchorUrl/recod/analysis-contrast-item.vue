<template>
  <div style="display: flex; flex-direction: column;height: 100%;">
    <!-- 视频信息 -->
    <div v-if="videoInfo && videoInfo.videoId" class="analysisFileContainer">
      <div class="analysisFileLeftContainer">
        <img :src="anchorInfo.anchorAvatar" class="analysisCompereImg">
        <div class="analysisCompereName">{{ anchorInfo.anchorName }}</div>
      </div>
      <div class="analysisFileRightContainer">
        <div class="analysisFileRightItemContainer rightBorder">
          <div>录制开始时间</div>
          <div style="margin-top: 4px;">{{ videoInfo.startTime.substring(0, 16) }}</div>
        </div>
        <div class="analysisFileRightItemContainer rightBorder">
          <div>录制结束时间</div>
          <div style="margin-top: 4px;">{{ videoInfo.endTime.substring(0, 16) }}</div>
        </div>
        <div class="analysisFileRightItemContainer">
          <div>录制时长</div>
          <div style="margin-top: 4px;">{{ durationStr }}</div>
        </div>
      </div>
    </div>
    <!-- 上传的文件信息 -->
    <div v-if="fileInfo && fileInfo.fileId" class="analysisFileContainer">
      <div class="analysisFileLeftContainer">
        <img class="analysisCompereImg" src="@/assets/images/video.png">
        <div class="analysisCompereName">{{ fileInfo.fileName }}</div>
      </div>
      <div class="analysisFileRightContainer">
        <div class="analysisFileRightItemContainer rightBorder">
          <div>上传时间</div>
          <div style="margin-top: 4px;">{{ fileInfo.uploadTime.substring(0, 16) }}</div>
        </div>
        <div class="analysisFileRightItemContainer rightBorder">
          <div>分析时间</div>
          <div style="margin-top: 4px;">{{ fileInfo.analysisTime.substring(0, 16) }}</div>
        </div>
      </div>
    </div>

    <!-- 敏感词/关键词/语速汇总 -->
    <div class="wordsContainer">
      <div class="wordsItemContainer">
        <div class="wordsItemColorContainer">
          <div class="wordsItemColor" style="background: rgba(255, 58, 25, 0.2);"></div>
          <div class="wordsItemText">智能敏感词：{{ wordsInfo.sensitiveWordsNum }}次</div>
        </div>
        <div v-if="!wordsInfo.markSensitive" class="indiciaBtn" @click="markClick('sensitive', true)">点击标注
        </div>
        <div v-else class="cancelIndiciaBtn" @click="markClick('sensitive', false)">取消标注</div>
      </div>
      <div class="wordsItemContainer">
        <div class="wordsItemColorContainer">
          <div class="wordsItemColor" style="background: rgba(0,183,255,0.2);"></div>
          <div class="wordsItemText">关键词：{{ wordsInfo.cruxWordsNum }}次</div>
        </div>
        <div v-if="!wordsInfo.markCrux" class="indiciaBtn" @click="markClick('crux', true)">点击标注</div>
        <div v-else class="cancelIndiciaBtn" @click="markClick('crux', false)">取消标注</div>
      </div>
      <div v-if="sentenceMarkData.playUrl" class="wordsItemContainer">
        <div class="wordsItemColorContainer">
          <div v-if="!analysisChar" class="wordsItemText">语速：**字/分钟</div>
          <div v-else class="wordsItemText">语速：{{ parseInt(charCountNum / (videoDuration / 60)) }}字/分钟</div>
        </div>
        <div v-if="!analysisChar" class="indiciaBtn" @click="analysisChar = true">点击分析</div>
        <div v-if="analysisChar" class="indiciaBtn" @click="analysisChar = false">隐藏分析</div>
      </div>
    </div>
    <!-- 视频/音频分析 -->
    <div class="videoAnalysisContainer">
      <!-- 视频/音频 -->
      <div v-if="sentenceMarkData.playUrl" :style="'width: ' + videoWidth" class="videoContainer">
        <video-player ref="videoPlayer" :options="playerOptions" :playsinline="true" class="video"
                      @canplay="canplay" @timeupdate="onPlayerTimeupdate"></video-player>
        <div style="display: flex;margin-top: 8px;">
          <div style="color: #4D4D4D;font-size: 12px;margin-right: 8px;">对此视频有异议？</div>
          <div style="color: #0077FF;font-size: 12px;border-bottom: 1px solid #0077FF;cursor: pointer;"
               @click="Complaint">点我申诉
          </div>
        </div>
      </div>
      <!-- 文字识别 -->
      <div class="discernContainer">
        <div class="discernSearchContainer">
          <!-- 搜索内容标注关键字 -->
          <div v-if="searchWordIndexInfo.name" style="display: flex; align-items: center;">
            <div v-if="searchWordIndexInfo.count > 0" style="display: flex; align-items: center;">
              <i class="el-icon-caret-left" style="font-size: 26px; color: dodgerblue; cursor: pointer;"
                 @click="prevSearchWords"></i>
              <div style="font-size: 14px; color: #444;margin: 0 6px;">{{
                  (searchWordIndexInfo.wordIndex + 1) + ' / ' + searchWordIndexInfo.count
                }}
              </div>
              <i class="el-icon-caret-right" style="font-size: 26px; color: dodgerblue; cursor: pointer;"
                 @click="nextSearchWords"></i>
            </div>
            <div v-else style="font-size: 14px; color: #444;margin-right: 10px;">
              无结果
            </div>
          </div>
          <el-input v-model="searchWordIndexInfo.name" clearable placeholder="输入内容标注关键字"
                    size="small" style="width: 200px;" suffix-icon="el-icon-search" @input="markKeywords">
          </el-input>
          <!-- 导出文字内容 -->
          <el-button size="small" style="margin-left: 10px;" type="primary" @click="exportTxt">导出文字</el-button>
        </div>
        <!-- 音频/视频文件的文字段落 -->
        <div class="wordsBodyContainer">
          <div v-for="(item, index) in sentenceMarkList" :key="`sentence-${index}`" ref="dom"
               class="wordsBodyItemContainer">
            <img v-if="anchorInfo && anchorInfo.anchorAvatar" :src="anchorInfo.anchorAvatar"
                 class="wordsBodyAvatar">
            <img v-else class="wordsBodyAvatar" src="@/assets/images/avatar.png">
            <div class="wordsBodyContentContainer">
              <div style="display: flex; align-items: center; justify-content: space-between;">
                <div style="display: flex; align-items: center;">
                  <div class="wordsBodyContentText1">说话人</div>
                  <div v-if="fileInfo.fileType == 0 || videoInfo" class="wordsBodyContentText2">
                    {{ toformatTime(item.items[0].startTime) }}
                  </div>
                  <div v-if="videoInfo && videoInfo.videoId && item.onlinePeopleObj.show"
                       class="onlineNumContainer">
                    <div>在线人数：{{ item.onlinePeopleObj.number }}</div>
                    <div class="onlineNumDifferenceContainer">
                      <div v-if="item.onlinePeopleObj.difference > 0"
                           style="color: #FC4F52;display: flex;align-items: center;">
                        <img src="@/assets/images/plus.png"
                             style="width: 8px;height: 8px;margin-right: 1px">
                        <span>{{ item.onlinePeopleObj.difference }}</span>
                        <img src="@/assets/images/up.png" style="width: 16px;height: 16px;">
                      </div>
                      <div v-else-if="item.onlinePeopleObj.difference < 0"
                           style="color: #28BD6C;display: flex;align-items: center;">
                        <img src="@/assets/images/minus.png"
                             style="width: 8px;height: 8px;margin-right: 1px">
                        <span>{{ Math.abs(item.onlinePeopleObj.difference) }}</span>
                        <img src="@/assets/images/down.png" style="width: 16px;height: 16px;">
                      </div>
                      <div v-else style="display: flex">
                        <img src="@/assets/images/flat.png" style="width: 8px;height: 8px;">
                      </div>
                    </div>
                  </div>
                </div>
                <div class="wordsBodyContentText2" style="margin-right: 20px;">
                  <span v-if="item.naturalTime" style="margin-left: 6px;">自然时间：{{ item.naturalTime }}</span>
                </div>
              </div>
              <!-- 段落 -->
              <div class="paragraphContainer">
                <!-- 让两个元素重叠 -->
                <div class="searchMarkContent" style="position: relative;"
                     v-html="'<span>' + item.searchMarkContent + '</span>'">
                </div>
                <div class="markContent" style="position: absolute; left: 0; top: 0;"
                     v-html="'<span>' + item.markContent + '</span>'">
                </div>
                <div class="restricMarkContent" style="position: absolute; left: 0; top: 0;"
                     v-html="'<span>' + item.restricMarkContent + '</span>'">
                </div>
                <div class="words-box" style="position: absolute; left: 0; top: 0;">
                                    <span
                                        v-for="(wordsItem, wordsIndex) in item.items"
                                        :key="`word-${index}-${wordsIndex}`"
                                        :class="(wordsItem.startTime < videoCurrentTime && videoCurrentTime < wordsItem.endTime)
                                            && (wordsItem.word != '，' && wordsItem.word != '。' && wordsItem.word != '！' && wordsItem.word != '？' && wordsItem.word != '、') ? 'wordTimeHover' : 'word'"
                                        @click="playerReadied(wordsItem.startTime)">{{ wordsItem.word }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!-- 文本文件的文字段落 -->
        <!-- <div class="wordsBodyContainer" v-else>
            <div v-for="item in sentenceMarkList" class="wordsBodyItemContainer" ref="dom">
                <img :src="anchorInfo.AnchorAvatar" class="wordsBodyAvatar"
                    v-if="anchorInfo && anchorInfo.AnchorAvatar">
                <img src="@/assets/images/avatar.png" class="wordsBodyAvatar" v-else>
                <div class="wordsBodyContentContainer">
                    <div style="display: flex; align-items: center;">
                        <div class="wordsBodyContentText1">说话人</div>
                        <div class="wordsBodyContentText2" v-if="videoInfo">
                            <span>{{ toformatTime(item.items[0].startTime) }}</span>
                        </div>
                        <div class="onlineNumContainer"
                            v-if="videoInfo && videoInfo.videoId && item.onlinePeopleObj.show">
                            <div>在线人数：{{ item.onlinePeopleObj.number }}</div>
                            <div class="onlineNumDifferenceContainer">
                                <div v-if="item.onlinePeopleObj.difference > 0"
                                    style="color: #FC4F52;display: flex;align-items: center;">
                                    <img src="@/assets/images/plus.png"
                                        style="width: 8px;height: 8px;margin-right: 1px">
                                    <span>{{ item.onlinePeopleObj.difference }}</span>
                                    <img src="@/assets/images/up.png"
                                        style="width: 16px;height: 16px;">
                                </div>
                                <div v-else-if="item.onlinePeopleObj.difference < 0"
                                    style="color: #28BD6C;display: flex;align-items: center;">
                                    <img src="@/assets/images/minus.png"
                                        style="width: 8px;height: 8px;margin-right: 1px">
                                    <span>{{ Math.abs(item.onlinePeopleObj.difference) }}</span>
                                    <img src="@/assets/images/down.png"
                                        style="width: 16px;height: 16px;">
                                </div>
                                <div v-else style="display: flex">
                                    <img src="@/assets/images/flat.png"
                                        style="width: 8px;height: 8px;">
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="paragraphContainer-txt">
                        <div class="searchMarkContent"
                            style="position: relative; white-space: pre-wrap;margin-top: 10px;"
                            v-html="'<span>' + item.searchMarkContent + '</span>'">
                        </div>
                        <div class="markContent"
                            style="position: absolute; left: 0; top: 0; white-space: pre-wrap;margin-top: 10px;"
                            v-html="'<span>' + item.markContent + '</span>'">
                        </div>
                        <div class="restricMarkContent"
                            style="position: absolute; left: 0; top: 0; white-space: pre-wrap;margin-top: 10px;"
                            v-html="'<span>' + item.restricMarkContent + '</span>'">
                        </div>
                    </div>
                </div>
            </div>
        </div> -->
      </div>
    </div>
  </div>
</template>

<script>
import myUtils from '@/utils/utils'

export default {
  props: {
    // 数据
    sentenceMarkData: {
      type: Object,
      required: true
    },
    // 视频播放器宽度，如：28%
    videoWidth: {
      type: String,
      required: true
    }
  },
  data() {
    return {
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
          {
            type: 'video/mp4', // 这里的种类支持很多种：基本视频格式、直播、流媒体等，具体可以参看git网址项目
            src: this.sentenceMarkData.playUrl // url地址
          }
        ],
        hls: true,
        notSupportedMessage: '此视频暂无法播放，请稍后再试', // 允许覆盖Video.js无法播放媒体源时显示的默认信息。
        controlBar: {
          timeDivider: false, // 当前时间和持续时间的分隔符
          durationDisplay: false,  // 显示持续时间
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
      sentenceMarkList: this.sentenceMarkData.sentenceMarkList,
      videoInfo: this.sentenceMarkData.videoInfo,
      anchorInfo: this.sentenceMarkData.anchorInfo,
      fileInfo: this.sentenceMarkData?.fileInfo ?? {fileType: ''},
      durationStr: '',
      currentParagraphIndex: 0, // 当前段落
      // 搜索标注关键字
      searchWordIndexInfo: {
        name: '',
        wordIndex: 0,
        paragraphIndex: 0,
        count: 0
      },
      videoDuration: '', // 视频时长
      charCountNum: '', // 文字总数量
      analysisChar: false
    }
  },

  mounted() {
    this.countWords()

    // 计算时长
    if (this.videoInfo && this.videoInfo.duration) {
      this.durationStr = ''
      let arr = this.videoInfo.duration.split(':')
      if (arr[0] && parseInt(arr[0])) {
        this.durationStr += arr[0] + '时'
      }
      if (arr[1] && parseInt(arr[1])) {
        this.durationStr += arr[1] + '分'
      } else {
        this.durationStr = '01分'
      }
    }
  },

  methods: {

    // 视频加载完成回调事件
    canplay() {
      this.videoDuration = this.$refs.videoPlayer.player.duration()
      this.charCountNum = 0
      if (this.sentenceMarkList && this.sentenceMarkList.length > 0) {
        this.sentenceMarkList.forEach(item => {
          let tempContent = item.content.replaceAll('，', '').replaceAll('。', '').replaceAll('？', '').replaceAll('、', '')
          this.charCountNum += tempContent.length
          item.charNumSecond = tempContent.length / ((item.endTime - item.startTime) / 1000) * 60
        })
      }
    },
    // 跳转到下一个搜索词
    nextSearchWords() {
      if (this.searchWordIndexInfo.wordIndex >= this.searchWordIndexInfo.count - 1) {
        this.searchWordIndexInfo.wordIndex = 0
      } else {
        this.searchWordIndexInfo.wordIndex++
      }
      let oldParagraphIndex = this.searchWordIndexInfo.paragraphIndex
      this.markSearch(true)
      this.$nextTick(() => {
        if (oldParagraphIndex != this.searchWordIndexInfo.paragraphIndex) {
          this.$refs.dom[this.searchWordIndexInfo.paragraphIndex].scrollIntoView({behavior: 'smooth'})
          this.$refs.videoPlayer.player.pause()
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
          this.$refs.dom[this.searchWordIndexInfo.paragraphIndex].scrollIntoView({behavior: 'smooth'})
          this.$refs.videoPlayer.player.pause()
        }
      })
    },
    // 搜索关键字
    markKeywords() {
      this.markSearch()
      this.$nextTick(() => {
        if (this.searchWordIndexInfo.name) {
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
            this.$refs.videoPlayer.player.pause()
          }
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
      let fileName = this.videoInfo && this.videoInfo.videoId ? this.videoInfo.videoName : this.fileInfo.fileName
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
      this.$emit('markwords', JSON.parse(JSON.stringify(this.wordsInfo)))
      this.mark()
      this.markSearch()
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
            sentenceItem.searchMarkContent = sentenceItem.searchMarkContent.replace(new RegExp(this.searchWordIndexInfo.name, 'g'), '<span style=\'background: darkorange\'>' + this.searchWordIndexInfo.name + '</span>')
            // 累计搜索词的数量
            const regex = new RegExp(this.searchWordIndexInfo.name, 'g')
            let count = sentenceItem.searchMarkContent.match(regex) ? sentenceItem.searchMarkContent.match(regex).length : 0
            this.searchWordIndexInfo.count += count
          }

          if (isSearch) {
            // 标注上一个/下一个搜索词
            const regex = new RegExp(this.searchWordIndexInfo.name, 'g')
            let count = sentenceItem.searchMarkContent.match(regex) ? sentenceItem.searchMarkContent.match(regex).length : 0

            for (let i = 0; i < count; i++) {
              if (wordIndex == this.searchWordIndexInfo.wordIndex) {
                let text = sentenceItem.searchMarkContent
                // 找到第i个词的索引
                let searchStartIndex = 0
                for (let j = 0; j <= i; j++) {
                  searchStartIndex = text.indexOf(this.searchWordIndexInfo.name, searchStartIndex == 0 ? 0 : searchStartIndex + 1)
                }

                // 拼接标注颜色
                sentenceItem.searchMarkContent = text.substring(0, searchStartIndex)
                sentenceItem.searchMarkContent += '<span style=\'background:indianred\'>' + this.searchWordIndexInfo.name + '</span>'
                sentenceItem.searchMarkContent += text.substring(searchStartIndex + this.searchWordIndexInfo.name.length)

                // 要跳转的段落索引
                this.searchWordIndexInfo.paragraphIndex = index
              }
              wordIndex++
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

          if (sentenceItem.wordsList && sentenceItem.wordsList.length > 0) {
            // 按词语长度倒序排序，保证被覆盖的词语也能正确标注
            let tempWordsList = JSON.parse(JSON.stringify(sentenceItem.wordsList))
            tempWordsList.sort((a, b) => b.name.length - a.name.length)

            // 标注段落中的敏感词
            if (this.wordsInfo.markSensitive) {
              tempWordsList.forEach(wordItem => {
                if (wordItem.wordsType == 0 && sentenceItem.content.indexOf(wordItem.name) != -1) {
                  // 获取当前段落存在词语的数量
                  const regex = new RegExp(wordItem.name, 'g')
                  let count = sentenceItem.markContent.match(regex) ? sentenceItem.markContent.match(regex).length : 0
                  // 遍历段落中的每个词语，判断是否需要标注
                  let wordCurrentINdex = 0
                  let wordCurrentIndexRestrist = 0
                  for (let i = 0; i < count; i++) {
                    // 当前敏感词下标，用于标注敏感词
                    wordCurrentINdex = wordCurrentINdex == 0 ? 0 : wordCurrentINdex + wordItem.name.length
                    wordCurrentINdex = sentenceItem.markContent.indexOf(wordItem.name, wordCurrentINdex)
                    // 当前敏感词下标，用于标注限定词
                    wordCurrentIndexRestrist = wordCurrentIndexRestrist == 0 ? 0 : wordCurrentIndexRestrist + wordItem.name.length
                    wordCurrentIndexRestrist = sentenceItem.restricMarkContent.indexOf(wordItem.name, wordCurrentIndexRestrist)

                    wordItem.recordNeedsWordList.forEach(recordNeedsWordItem => {
                      if (recordNeedsWordItem.recordNeedsNum == i) {
                        // 标注敏感词
                        let leftContent = sentenceItem.markContent.substring(0, wordCurrentINdex)
                        let markWordContent = '<span style=\'background: pink\'>' + wordItem.name + '</span>'
                        let rightContent = sentenceItem.markContent.substring(wordCurrentINdex + wordItem.name.length)
                        sentenceItem.markContent = leftContent + markWordContent + rightContent
                        wordCurrentINdex = wordCurrentINdex + markWordContent.length - wordItem.name.length

                        // 存在限定词，标注限定词
                        if (recordNeedsWordItem.restrictWord) {
                          let markRestricLeft = false
                          let restrictWordContent = '<span style=\'background: gold\'>' + recordNeedsWordItem.restrictWord + '</span>'

                          // 标注左边限定词
                          let restrictLeftText = sentenceItem.restricMarkContent.substring(0, wordCurrentIndexRestrist)
                          // 取出左边限定词范围内容
                          let rangeLeftText = restrictLeftText.substring(restrictLeftText.length - recordNeedsWordItem.restrictRange < 0 ? 0 : restrictLeftText.length - recordNeedsWordItem.restrictRange)
                          if (rangeLeftText.indexOf(recordNeedsWordItem.restrictWord) != -1) {
                            markRestricLeft = true
                            let restrictIndex = restrictLeftText.lastIndexOf(recordNeedsWordItem.restrictWord)
                            let restrictLeftContent = restrictLeftText.substring(0, restrictIndex)
                            let restrictRightConent = restrictLeftText.substring(restrictIndex + recordNeedsWordItem.restrictWord.length)

                            restrictLeftText = restrictLeftContent + restrictWordContent + restrictRightConent
                          }

                          let restrictRightText = sentenceItem.restricMarkContent.substring(wordCurrentIndexRestrist + wordItem.name.length)
                          // 左边还没有标注限定词
                          if (!markRestricLeft) {
                            // 取出右边限定词范围内容
                            let rangeRightText = restrictRightText.substring(0, recordNeedsWordItem.restrictRange > restrictRightText.length ? restrictRightText.length : recordNeedsWordItem.restrictRange)
                            if (rangeRightText.indexOf(recordNeedsWordItem.restrictWord) != -1) {
                              let restrictIndex = rangeRightText.indexOf(recordNeedsWordItem.restrictWord)
                              let restrictLeftContent = restrictRightText.substring(0, restrictIndex)
                              let restrictRightConent = restrictRightText.substring(restrictIndex + recordNeedsWordItem.restrictWord.length)

                              restrictRightText = restrictLeftContent + restrictWordContent + restrictRightConent
                            }
                          }

                          if (markRestricLeft) {
                            wordCurrentIndexRestrist = wordCurrentIndexRestrist + restrictWordContent.length - recordNeedsWordItem.restrictWord.length
                          }

                          sentenceItem.restricMarkContent = restrictLeftText + wordItem.name + restrictRightText

                        }

                      }
                    })
                  }
                }
              })
            }

            // 标注段落中的关键词
            if (this.wordsInfo.markCrux) {
              tempWordsList.forEach(wordItem => {
                if (wordItem.wordsType == 1 && sentenceItem.content.indexOf(wordItem.name) != -1) {
                  // 获取当前段落存在词语的数量
                  const regex = new RegExp(wordItem.name, 'g')
                  let count = sentenceItem.markContent.match(regex) ? sentenceItem.markContent.match(regex).length : 0
                  // 遍历段落中的每个词语，判断是否需要标注
                  let wordCurrentINdex = 0
                  let wordCurrentIndexRestrist = 0
                  for (let i = 0; i < count; i++) {
                    // 当前关键词下标，用于标注关键词
                    wordCurrentINdex = wordCurrentINdex == 0 ? 0 : wordCurrentINdex + wordItem.name.length
                    wordCurrentINdex = sentenceItem.markContent.indexOf(wordItem.name, wordCurrentINdex)
                    // 当前关键词下标，用于标注限定词
                    wordCurrentIndexRestrist = wordCurrentIndexRestrist == 0 ? 0 : wordCurrentIndexRestrist + wordItem.name.length
                    wordCurrentIndexRestrist = sentenceItem.restricMarkContent.indexOf(wordItem.name, wordCurrentIndexRestrist)

                    wordItem.recordNeedsWordList.forEach(recordNeedsWordItem => {
                      if (recordNeedsWordItem.recordNeedsNum == i) {
                        // 标注关键词
                        let leftContent = sentenceItem.markContent.substring(0, wordCurrentINdex)
                        let markWordContent = '<span style=\'background: paleturquoise\'>' + wordItem.name + '</span>'
                        let rightContent = sentenceItem.markContent.substring(wordCurrentINdex + wordItem.name.length)
                        sentenceItem.markContent = leftContent + markWordContent + rightContent
                        wordCurrentINdex = wordCurrentINdex + markWordContent.length - wordItem.name.length

                        // 存在限定词，标注限定词
                        if (recordNeedsWordItem.restrictWord) {
                          let markRestricLeft = false
                          let restrictWordContent = '<span style=\'background: gold\'>' + recordNeedsWordItem.restrictWord + '</span>'

                          // 标注左边限定词
                          let restrictLeftText = sentenceItem.restricMarkContent.substring(0, wordCurrentIndexRestrist)
                          // 取出左边限定词范围内容
                          let rangeLeftText = restrictLeftText.substring(restrictLeftText.length - recordNeedsWordItem.restrictRange < 0 ? 0 : restrictLeftText.length - recordNeedsWordItem.restrictRange)
                          if (rangeLeftText.indexOf(recordNeedsWordItem.restrictWord) != -1) {
                            markRestricLeft = true
                            let restrictIndex = restrictLeftText.lastIndexOf(recordNeedsWordItem.restrictWord)
                            let restrictLeftContent = restrictLeftText.substring(0, restrictIndex)
                            let restrictRightConent = restrictLeftText.substring(restrictIndex + recordNeedsWordItem.restrictWord.length)

                            restrictLeftText = restrictLeftContent + restrictWordContent + restrictRightConent
                          }

                          let restrictRightText = sentenceItem.restricMarkContent.substring(wordCurrentIndexRestrist + wordItem.name.length)
                          // 左边还没有标注限定词
                          if (!markRestricLeft) {
                            // 取出右边限定词范围内容
                            let rangeRightText = restrictRightText.substring(0, recordNeedsWordItem.restrictRange > restrictRightText.length ? restrictRightText.length : recordNeedsWordItem.restrictRange)
                            if (rangeRightText.indexOf(recordNeedsWordItem.restrictWord) != -1) {
                              let restrictIndex = rangeRightText.indexOf(recordNeedsWordItem.restrictWord)
                              let restrictLeftContent = restrictRightText.substring(0, restrictIndex)
                              let restrictRightConent = restrictRightText.substring(restrictIndex + recordNeedsWordItem.restrictWord.length)

                              restrictRightText = restrictLeftContent + restrictWordContent + restrictRightConent
                            }
                          }

                          if (markRestricLeft) {
                            wordCurrentIndexRestrist = wordCurrentIndexRestrist + restrictWordContent.length - recordNeedsWordItem.restrictWord.length
                          }

                          sentenceItem.restricMarkContent = restrictLeftText + wordItem.name + restrictRightText

                        }

                      }
                    })
                  }
                }
              })
            }

          }
        })
      }

    },
    // 整理关键词、敏感词数据
    countWords() {
      this.wordsInfo.wordsList = []
      this.wordsInfo.cruxWordsNum = 0
      this.wordsInfo.sensitiveWordsNum = 0

      if (this.sentenceMarkList && this.sentenceMarkList.length > 0) {
        this.sentenceMarkList.forEach(item => {

          let wordsList = JSON.parse(JSON.stringify(item.wordsList))
          if (wordsList && wordsList.length > 0) {

            wordsList.forEach(wordsItem => {
              if (this.wordsInfo.wordsList.length < 1) {
                this.wordsInfo.wordsList.push(wordsItem)
              } else {
                // 判断数组里面是否已经存在当前关键词/敏感词，如果是，则只增加出现数量
                let exist = false
                this.wordsInfo.wordsList.forEach(haveItem => {
                  if (haveItem.name == wordsItem.name && haveItem.wordsType == wordsItem.wordsType) {
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
        this.wordsInfo.wordsList.forEach(wordsItem => {
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
      if (this.wordsInfo.wordsList) {
        this.wordsInfo.wordsList.sort((a, b) => b.countNum - a.countNum)
      }

      this.mark()
      this.markSearch()

      // this.$emit("countWords", JSON.parse(JSON.stringify(this.wordsInfo)));
    },
    // 设置播放器进度，秒
    playerReadied(second) {
      this.$refs.videoPlayer.player.currentTime(second / 1000)
      this.$refs.videoPlayer.player.play()
    },
    // 播放器进度回调
    onPlayerTimeupdate(player) {
      this.videoCurrentTime = player.cache_.currentTime * 1000

      // 滚动滚动条
      if (this.sentenceMarkList && this.sentenceMarkList.length > 0) {
        // 获取当前正在播放的段落索引
        let currentIndex = 0

        let setFlag = false
        for (let index = 0; index < this.sentenceMarkList.length; index++) {

          let item = this.sentenceMarkList[index]
          let nextItem = index + 1 == this.sentenceMarkList.length ? null : this.sentenceMarkList[index + 1]

          if (nextItem) {
            if (item.startTime < this.videoCurrentTime && this.videoCurrentTime < nextItem.startTime) {
              currentIndex = index
              setFlag = true
            }
          }

        }

        if (!setFlag) {
          currentIndex = this.sentenceMarkList.length - 1
        }

        this.sentenceMarkList.forEach((item, index) => {
          if (item.startTime < this.videoCurrentTime && this.videoCurrentTime < item.endTime) {
            currentIndex = index
          }
        })

        // 滚动滚动条到指定的段落元素
        // currentIndex -= 1;
        if (this.currentParagraphIndex != currentIndex) {
          if (currentIndex < 0) {
            this.$refs.dom[0].scrollIntoView({behavior: 'smooth'})
          } else {
            this.$refs.dom[currentIndex].scrollIntoView({behavior: 'smooth'})
          }
          this.currentParagraphIndex = currentIndex
        }
      }
    },
    // 毫秒时间戳转成时分秒格式
    toformatTime(val) {
      return myUtils.toformatTime(val)
    },
    // 申诉弹窗
    Complaint() {
      const secUid = this.sentenceMarkData.anchorInfo.secUid
      const videoId = this.sentenceMarkData.videoInfo.videoId
      this.complaintDialogVisible = true
      this.$nextTick(() => {
        this.$refs.complaint.init(secUid, videoId)
      })
      console.log(this.sentenceMarkData)

    }
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
  color: #4D4D4D;
}

.discernSearchContainer {
  padding-bottom: 10px;
  display: flex;
  justify-content: end;
}

.wordTimeHover {
  cursor: pointer;
  background: rgb(51, 109, 244);
  border-radius: 4px;
  color: #FFF;
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
  color: #2E3742;
  margin-top: 6px;
  position: relative;
  line-height: 22px;
  letter-spacing: 1px;
}

.paragraphContainer {
  font-weight: 400;
  font-size: 13px;
  color: #2E3742;
  margin-top: 6px;
  position: relative;
  line-height: 22px;
  letter-spacing: 1px;
}

.wordsBodyContentText2 {
  font-weight: 400;
  font-size: 12px;
  color: #95A1AF;
  margin-top: 8px;
  margin-left: 4px;
}

.wordsBodyContentText1 {
  font-weight: 400;
  font-size: 13px;
  color: #2E3742;
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
  height: calc(100% - 48px);
  overflow-y: auto;
  box-sizing: border-box;
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
  margin-top: 10px;
  flex-grow: 1;
  height: calc(100% - 116px);
}

.cancelIndiciaBtn {
  width: 72px;
  height: 22px;
  border-radius: 4px;
  border: 1px solid #B4BCCA;
  font-size: 13px;
  color: #2E3742;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 6px;
  cursor: pointer;
}

.indiciaBtn {
  width: 72px;
  height: 22px;
  border-radius: 4px;
  border: 1px solid #0077FF;
  font-size: 13px;
  color: #0077FF;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 6px;
  cursor: pointer;
}

.wordsItemText {
  font-weight: 500;
  font-size: 13px;
  color: #2E3742;
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
  flex-direction: column;
  align-items: center;
  padding: 0 10px;
}

.wordsContainer {
  height: 58px;
  background: #F5F7F9;
  border-radius: 4px;
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
}

.analysisFileRightItemContainer {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
}

.analysisFileRightContainer {
  display: flex;
  align-items: center;
  font-weight: 400;
  font-size: 13px;
  color: #2E3742;
}

.analysisCompereName {
  color: #2E3742;
  font-size: 14px;
  margin-left: 10px;

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
  max-width: 280px;
}

.analysisFileContainer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>