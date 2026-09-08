<template>
  <div>
    <div class="contrastContainer" @contextmenu="preventRightClick">
      <!-- 窗体控制栏 -->
      <div class="windowCtrlContainer"></div>
      <!-- 导航栏 -->
      <div class="analysisNavContainer">
        <div style="display: flex; align-items: center">
          <div class="analysisNavText">智能复盘分析</div>
          <div class="wordsItemContainer">
            <div
                v-if="sentenceMarkData != null"
                class="aiAnalysisClick"
                @click="clickAiAnalysis"
            >
              使用AI分析
            </div>
            <div v-if="sentenceMarkData == null" class="disableClick">
              暂无文本，无法AI分析
            </div>
          </div>
        </div>
      </div>

      <!-- 视频分析 -->
      <div v-if="showVideo" class="contrastInfoContainer">
        <div
            :style="wordsBodyContainerHeight"
            class="analysisLeftItemContainer"
        >
          <analysis-item
              :sentenceMarkData="sentenceMarkData.data1"
              :videoWidth="'24%'"
              @markwords="markWords1"
          ></analysis-item>
        </div>
        <div :style="wordsBodyContainerHeight" class="analysisItemContainer">
          <analysis-item
              :sentenceMarkData="sentenceMarkData.data2"
              :videoWidth="'24%'"
              @markwords="markWords2"
          ></analysis-item>
        </div>
      </div>

      <!-- 关键词/敏感词列表 -->
      <div ref="wordsSummaryContainer" class="wordsSummaryContainer">
        <div class="wordsExportContainer">
          <div
              v-if="showWordTable"
              style="display: flex; align-items: center; cursor: pointer"
              @click="showWordTableHandle(false)"
          >
            <img
                src="@/assets/images/word_list_down.png"
                style="width: 16px; height: 16px"
            />
            <div>运营关键词汇总</div>
          </div>
          <div
              v-if="!showWordTable"
              style="display: flex; align-items: center; cursor: pointer"
              @click="showWordTableHandle(true)"
          >
            <img
                src="@/assets/images/word_list_up.png"
                style="width: 16px; height: 16px"
            />
            <div>运营关键词汇总</div>
          </div>
        </div>
        <div v-if="showWordTable" class="wordsSummaryBodyContainer">
          <el-table
              :data="wordTabelList"
              :element-loading-spinner="customSvg"
              :header-cell-style="{ background: 'rgb(245,247,249)' }"
              border
              header-row-class-name="my-header-row"
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
              <template #default="{ row }">
                {{ row?.wordsType === 0 ? '智能敏感词' : '运营关键词' }}
              </template>
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
            ></el-table-column>
            <el-table-column
                align="center"
                label="次数-视频1"
                prop="countNum1"
                width="130"
            >
            </el-table-column>
            <el-table-column
                align="center"
                label="次数-视频2"
                prop="countNum2"
                width="130"
            >
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
            <el-table-column
                align="center"
                label="行业"
                prop="tradeStr"
                width="260"
            >
            </el-table-column>
          </el-table>
          <!-- 分页 -->
          <div class="page-style">
            <el-pagination
                :current-page="pageIndex"
                :page-size="pageSize"
                :page-sizes="[2, 4, 6, 8]"
                :total="totalCount"
                background
                layout="sizes,total,->,prev, pager, next, jumper"
                style="text-align: center; margin-top: 6px"
                @current-change="currentChangeHandle"
                @size-change="sizeChangeHandle"
            >
            </el-pagination>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import myUtils from '@/utils/utils'
import analysisItem from './analysis-contrast-item.vue'
import api from '@/utils/request-api'

export default {
  components: {
    analysisItem
  },
  data() {
    return {
      showWordTable: true,
      initedTabalSize: false,
      togglemaxsizeFlag: 'normal',
      // 关键词/敏感词列表
      wordsList: [],
      showVideo: false,
      sentenceMarkData: {},
      contrastInfo: {},
      wordTabelList: [],
      pageSize: 6,
      totalCount: 0,
      pageIndex: 1,
      wordsBodyContainerHeight: '',
      wordsInfo1: {
        markCrux: true,
        markSensitive: true
      },
      wordsInfo2: {
        markCrux: true,
        markSensitive: true
      },
      // 是否拥有数据展示
      noDataFlag: {},
      cruxTypeList: [], // 关键词类型列表
      // 对比分析唯一标识
      contrastId: ''
    }
  },
  methods: {
    // 打开AI分析弹窗
    clickAiAnalysis() {
      let contrastId = this.contrastId
      // let aiAnalysisTradeId = this.aiAnalysisTradeId;
      let text1 = ''
      let text2 = ''
      // 判断是视频还是txt文本
      let sentenceMarkList1 = this.sentenceMarkData.data1.sentenceMarkList
      let sentenceMarkList2 = this.sentenceMarkData.data2.sentenceMarkList
      if (
          sentenceMarkList1[0].startTime != undefined &&
          sentenceMarkList1[0].startTime >= 0
      ) {
        // 判断是录制视频还是上传视频
        if (sentenceMarkList1[0].onlinePeopleObj.number != null) {
          // 录制视频一分析：有自然时间，在线人数，文本开始时间
          text1 += '以下是视频文本一：' + '\n'
          sentenceMarkList1.forEach((sentenceItem) => {
            text1 += '该段的自然时间：' + sentenceItem.naturalTime
            text1 += '；在线人数：' + sentenceItem.onlinePeopleObj.number
            text1 +=
                '；视频开始时间：' +
                myUtils.toformatTime(sentenceItem.startTime) +
                '\n'
            text1 += sentenceItem.content + '\r\n\r'
            text1 += '\n'
          })
          // 录制视频一分析：有自然时间，在线人数，文本开始时间
          text2 += '\n' + '以下是视频文本二：' + '\n'
          sentenceMarkList2.forEach((sentenceItem2) => {
            text2 += '该段的自然时间：' + sentenceItem2.naturalTime
            text2 += '；在线人数：' + sentenceItem2.onlinePeopleObj.number
            text2 +=
                '；视频开始时间：' +
                myUtils.toformatTime(sentenceItem2.startTime) +
                '\n'
            text2 += sentenceItem2.content + '\r\n\r'
            text2 += '\n'
          })
        } else {
          // 上传视频一分析：只有文本开始时间
          text1 += '以下是视频文本一：' + '\n'
          sentenceMarkList1.forEach((sentenceItem) => {
            text1 +=
                '视频开始时间：' +
                myUtils.toformatTime(sentenceItem.startTime) +
                '\n'
            text1 += sentenceItem.content + '\r\n\r'
            text1 += '\n'
          })
          // 上传视频二分析：只有文本开始时间
          text2 += '\n' + '以下是视频文本二：' + '\n'
          sentenceMarkList1.forEach((sentenceItem2) => {
            text2 +=
                '视频开始时间：' +
                myUtils.toformatTime(sentenceItem2.startTime) +
                '\n'
            text2 += sentenceItem2.content + '\r\n\r'
            text2 += '\n'
          })
        }
      } else {
        // txt文本
        text1 += '以下是视频文本一：' + '\n'
        sentenceMarkList1.forEach((sentenceItem) => {
          text1 += sentenceItem.content + '\r\n\r'
        })
        text2 += '\n' + '以下是视频文本二：' + '\n'
        sentenceMarkList2.forEach((sentenceItem2) => {
          text2 += sentenceItem2.content + '\r\n\r'
        })
      }
      let text = text1 + text2

      // 根据contrastId将其作为存入分析文本的对应key
      sessionStorage.setItem(contrastId, JSON.stringify({text}))
      const url = this.$router.resolve({
        name: 'aiAnalysis',
        query: {contrastId: contrastId}
      })
      window.open(url.href, '_blank')
    },
    // 显隐词语列表
    showWordTableHandle(flag) {
      this.showWordTable = flag
      this.getWordsBodyContainerHeight()
    },
    markWords1(obj) {
      this.wordsInfo1 = obj
      this.countWords()
    },
    markWords2(obj) {
      this.wordsInfo2 = obj
      this.countWords()
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

      if (this.wordsList && this.wordsList.length > 0) {
        let tempArr = []
        this.wordsList.forEach((item) => {
          tempArr.push(item)
        })
        let startIndex = this.pageSize * (this.pageIndex - 1)
        let endIndex = startIndex + this.pageSize
        for (let i = startIndex; i < endIndex; i++) {
          if (tempArr[i]) {
            this.wordTabelList.push(tempArr[i])
          }
        }

        this.totalCount = tempArr.length

        this.getWordsBodyContainerHeight()
      }
    },
    // 计算文本段落区域的高度
    getWordsBodyContainerHeight() {
      this.$nextTick(() => {
        let wordsSummaryContainerHeight = '0px'
        if (this.wordTabelList.length > 0) {
          wordsSummaryContainerHeight =
              this.$refs.wordsSummaryContainer.offsetHeight + 'px'
        }
        this.wordsBodyContainerHeight = {
          'max-height': `calc(100vh - 110px - ${wordsSummaryContainerHeight})`
        }

        if (!this.initedTabalSize) {
          // 获取视口高度
          const viewportHeight = window.innerHeight
          // 定义要减去的像素值
          const subtractValues = [
            160,
            this.$refs.wordsSummaryContainer.offsetHeight
          ]
          // 计算结果
          const result =
              viewportHeight - subtractValues.reduce((acc, val) => acc + val, 0)

          if (result > 600) {
            this.pageSize = 6
          } else if (result > 500) {
            this.pageSize = 4
          } else {
            this.pageSize = 2
          }
          this.pageIndex = 1
          this.addWordTabelList()
          this.initedTabalSize = true
        }
      })
    },
    // 禁止鼠标右键
    preventRightClick(event) {
      event.preventDefault()
    },
    init() {
      this.contrastId = this.$route.query.contrastId || ''
      this.getContrastInfo()
      this.getCruxWordType()
    },
    // 获取对比信息
    getContrastInfo() {
      this.sentenceMarkData.data1 = {}
      this.sentenceMarkData.data2 = {}
      let contrastId = this.contrastId

      api.synccontrast
          .getContrastAnalysisInfo({contrastId: contrastId})
          .then((res) => {
            if (res.code == 0 && res.data) {
              this.contrastInfo = res.data.VideoContrast
              this.noDataFlag = res.data.sentenceMark1

              this.sentenceMarkData.data1.sentenceMarkList = []
              this.sentenceMarkData.data2.sentenceMarkList = []

              // 设置数据1的段落词语信息
              if (
                  res.data.sentenceMark1.analysisList &&
                  res.data.sentenceMark1.analysisList.length > 0
              ) {
                res.data.sentenceMark1.analysisList.forEach((item) => {
                  if (item.status == 0) {
                    this.sentenceMarkData.data1.sentenceMarkList.push(
                        JSON.parse(item.dataJson)
                    )
                  }
                })
                this.sentenceMarkData.data1.sentenceMarkList.sort(
                    (a, b) => a.currentSort - b.currentSort
                )

                // 在线人数
                let onlineNumList = res.data.sentenceMark1.onlineNumList
                if (onlineNumList != null && onlineNumList.length > 0) {
                  onlineNumList.sort((a, b) =>
                      a.recordDate.localeCompare(b.recordDate)
                  )
                }

                // 设置段落的开始时间和结束时间
                this.sentenceMarkData.data1.sentenceMarkList.forEach(
                    (item, index) => {
                      item.markContent = item.content
                      if (item.items && item.items.length > 0) {
                        if (index == 0) {
                          item.startTime = 0
                        } else {
                          item.startTime = item.items[0].startTime
                        }
                        item.endTime = item.items[item.items.length - 1].endTime
                      }
                      // 自然时间
                      if (res.data.sentenceMark1.videoInfo != null) {
                        let videoStartTime =
                            res.data.sentenceMark1.videoInfo.startTime.substring(11)
                        item.naturalTime = myUtils.toformatTime(
                            myUtils.toSecond(videoStartTime) * 1000 + item.startTime
                        )
                      }
                      // 设置每个段落的在线人数情况
                      let onlinePeopleObj = {
                        number: 0,
                        difference: 0,
                        show: false
                      }
                      if (onlineNumList != null && onlineNumList.length > 0) {
                        let startTimeSecond =
                            myUtils.toSecondByDate(
                                res.data.sentenceMark1.videoInfo.startTime
                            ) + Math.floor(item.startTime / 1000)
                        let endTimeSecond =
                            myUtils.toSecondByDate(
                                res.data.sentenceMark1.videoInfo.startTime
                            ) + Math.floor(item.endTime / 1000)

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
                            onlinePeopleObj.number = parseInt(
                                onlineNumItem.peopleNum
                            )
                          }
                        })

                        if (index > 0) {
                          // 如果当前段落人数为0，取上一段的人数
                          if (onlinePeopleObj.number == 0) {
                            let upItem =
                                this.sentenceMarkData.data1.sentenceMarkList[
                                index - 1
                                    ]
                            onlinePeopleObj.number = upItem.onlinePeopleObj.number
                          }

                          // 如果最后是一段，并且没有匹配上在线人数，取最后一条在线人数
                          if (
                              index + 1 ==
                              this.sentenceMarkData.data1.sentenceMarkList.length &&
                              onlinePeopleObj.number == 0
                          ) {
                            onlinePeopleObj.number = parseInt(
                                onlineNumList[onlineNumList.length - 1].peopleNum
                            )
                          }

                          onlinePeopleObj.difference =
                              onlinePeopleObj.number -
                              this.sentenceMarkData.data1.sentenceMarkList[index - 1]
                                  .onlinePeopleObj.number
                        }

                        onlinePeopleObj.show = true
                        item.onlinePeopleObj = onlinePeopleObj
                      } else {
                        item.onlinePeopleObj = onlinePeopleObj
                      }
                    }
                )
                console.log(this.sentenceMarkData.data1.sentenceMarkList)
              }

              // 设置数据2的段落词语信息
              if (
                  res.data.sentenceMark2.analysisList &&
                  res.data.sentenceMark2.analysisList.length > 0
              ) {
                res.data.sentenceMark2.analysisList.forEach((item) => {
                  if (item.status == 0) {
                    this.sentenceMarkData.data2.sentenceMarkList.push(
                        JSON.parse(item.dataJson)
                    )
                  }
                })
                this.sentenceMarkData.data2.sentenceMarkList.sort(
                    (a, b) => a.currentSort - b.currentSort
                )

                // 在线人数
                let onlineNumList = res.data.sentenceMark2.onlineNumList
                if (onlineNumList != null && onlineNumList.length > 0) {
                  onlineNumList.sort((a, b) =>
                      a.recordDate.localeCompare(b.recordDate)
                  )
                }

                // 设置段落的开始时间和结束时间
                this.sentenceMarkData.data2.sentenceMarkList.forEach(
                    (item, index) => {
                      item.markContent = item.content
                      if (item.items && item.items.length > 0) {
                        if (index == 0) {
                          item.startTime = 0
                        } else {
                          item.startTime = item.items[0].startTime
                        }
                        item.endTime = item.items[item.items.length - 1].endTime
                      }

                      // 自然时间
                      if (res.data.sentenceMark2.videoInfo != null) {
                        let videoStartTime =
                            res.data.sentenceMark2.videoInfo.startTime.substring(11)
                        item.naturalTime = myUtils.toformatTime(
                            myUtils.toSecond(videoStartTime) * 1000 + item.startTime
                        )
                      }
                      // 设置每个段落的在线人数情况
                      let onlinePeopleObj = {
                        number: 0,
                        difference: 0,
                        show: false
                      }
                      if (onlineNumList != null && onlineNumList.length > 0) {
                        let startTimeSecond =
                            myUtils.toSecondByDate(
                                res.data.sentenceMark2.videoInfo.startTime
                            ) + Math.floor(item.startTime / 1000)
                        let endTimeSecond =
                            myUtils.toSecondByDate(
                                res.data.sentenceMark2.videoInfo.startTime
                            ) + Math.floor(item.endTime / 1000)

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
                            onlinePeopleObj.number = parseInt(
                                onlineNumItem.peopleNum
                            )
                          }
                        })

                        if (index > 0) {
                          // 如果当前段落人数为0，取上一段的人数
                          if (onlinePeopleObj.number == 0) {
                            let upItem =
                                this.sentenceMarkData.data2.sentenceMarkList[
                                index - 1
                                    ]
                            onlinePeopleObj.number = upItem.onlinePeopleObj.number
                          }

                          // 如果最后是一段，并且没有匹配上在线人数，取最后一条在线人数
                          if (
                              index + 1 ==
                              this.sentenceMarkData.data2.sentenceMarkList.length &&
                              onlinePeopleObj.number == 0
                          ) {
                            onlinePeopleObj.number = parseInt(
                                onlineNumList[onlineNumList.length - 1].peopleNum
                            )
                          }

                          onlinePeopleObj.difference =
                              onlinePeopleObj.number -
                              this.sentenceMarkData.data2.sentenceMarkList[index - 1]
                                  .onlinePeopleObj.number
                        }

                        onlinePeopleObj.show = true
                        item.onlinePeopleObj = onlinePeopleObj
                      } else {
                        item.onlinePeopleObj = onlinePeopleObj
                      }
                    }
                )
              }

              // 视频播放地址
              this.sentenceMarkData.data1.playUrl = res.data.sentenceMark1.playUrl
              this.sentenceMarkData.data2.playUrl = res.data.sentenceMark2.playUrl

              // 视频信息
              if (res.data.sentenceMark1.videoInfo) {
                this.sentenceMarkData.data1.videoInfo =
                    res.data.sentenceMark1.videoInfo
                if (this.sentenceMarkData.data1.videoInfo.duration) {
                  this.sentenceMarkData.data1.videoInfo.duration =
                      myUtils.toformatTime(
                          this.sentenceMarkData.data1.videoInfo.duration * 1000
                      )
                }
              }
              if (res.data.sentenceMark2.videoInfo) {
                this.sentenceMarkData.data2.videoInfo =
                    res.data.sentenceMark2.videoInfo
                if (this.sentenceMarkData.data2.videoInfo.duration) {
                  this.sentenceMarkData.data2.videoInfo.duration =
                      myUtils.toformatTime(
                          this.sentenceMarkData.data2.videoInfo.duration * 1000
                      )
                }
              }

              // 主播信息
              this.sentenceMarkData.data1.anchorInfo =
                  res.data.sentenceMark1.anchorInfo
              this.sentenceMarkData.data2.anchorInfo =
                  res.data.sentenceMark2.anchorInfo

              // 文件信息
              if (res.data.sentenceMark1.uploadFile) {
                this.sentenceMarkData.data1.fileInfo =
                    res.data.sentenceMark1.uploadFile
                if (this.sentenceMarkData.data1.fileInfo.fileDuration) {
                  this.sentenceMarkData.data1.fileInfo.fileDuration =
                      myUtils.toformatTime(
                          this.sentenceMarkData.data1.fileInfo.fileDuration * 1000
                      )
                }
              }
              if (res.data.sentenceMark2.uploadFile) {
                this.sentenceMarkData.data2.fileInfo =
                    res.data.sentenceMark2.uploadFile
                if (this.sentenceMarkData.data2.fileInfo.fileDuration) {
                  this.sentenceMarkData.data2.fileInfo.fileDuration =
                      myUtils.toformatTime(
                          this.sentenceMarkData.data2.fileInfo.fileDuration * 1000
                      )
                }
              }

              this.showVideo = true

              this.countWords()
            } else {
              this.$message.error('对比数据不存在')
            }
          })
    },
    // 整理敏感词/关键词列表
    countWords() {
      this.wordsList = []

      if (
          this.sentenceMarkData.data1.sentenceMarkList &&
          this.sentenceMarkData.data1.sentenceMarkList.length > 0
      ) {
        let sentenceMarkList = JSON.parse(
            JSON.stringify(this.sentenceMarkData.data1.sentenceMarkList)
        )
        // 遍历段落
        sentenceMarkList.forEach((sentence) => {
          if (sentence.wordsList) {
            // 遍历段落的敏感词/关键词
            sentence.wordsList.forEach((sentenceWords) => {
              // 添加敏感词
              if (
                  this.wordsInfo1.markSensitive &&
                  this.wordsInfo2.markSensitive &&
                  sentenceWords.wordsType === 0
              ) {
                let exist = false
                this.wordsList.forEach((words) => {
                  // 如果已存在，则增加数量
                  if (
                      words.name === sentenceWords.name &&
                      words.wordsType === sentenceWords.wordsType
                  ) {
                    words.countNum1 += sentenceWords.countNum
                    exist = true
                  }
                })
                if (!exist) {
                  let word = JSON.parse(JSON.stringify(sentenceWords))
                  word.countNum1 = sentenceWords.countNum
                  this.wordsList.push(word)
                }
              }
              // 添加关键词
              if (
                  this.wordsInfo1.markCrux &&
                  this.wordsInfo2.markCrux &&
                  sentenceWords.wordsType === 1
              ) {
                let exist = false
                this.wordsList.forEach((words) => {
                  // 如果已存在，则增加数量
                  if (
                      words.name === sentenceWords.name &&
                      words.wordsType === sentenceWords.wordsType
                  ) {
                    words.countNum1 += sentenceWords.countNum
                    exist = true
                  }
                })
                if (!exist) {
                  let word = JSON.parse(JSON.stringify(sentenceWords))
                  word.countNum1 = sentenceWords.countNum
                  this.wordsList.push(word)
                }
              }
            })
          }
        })
      }

      if (
          this.sentenceMarkData.data2.sentenceMarkList &&
          this.sentenceMarkData.data2.sentenceMarkList.length > 0
      ) {
        let sentenceMarkList = JSON.parse(
            JSON.stringify(this.sentenceMarkData.data2.sentenceMarkList)
        )
        // 遍历段落2，进行比对
        sentenceMarkList.forEach((sentence) => {
          if (sentence.wordsList) {
            sentence.wordsList.forEach((sentenceWords) => {
              // 添加敏感词
              if (
                  this.wordsInfo1.markSensitive &&
                  this.wordsInfo2.markSensitive &&
                  sentenceWords.wordsType === 0
              ) {
                let exist = false
                this.wordsList.forEach((words) => {
                  if (
                      words.name === sentenceWords.name &&
                      words.wordsType === sentenceWords.wordsType
                  ) {
                    // 如果已经存在，增加数量
                    words.countNum2 = words.countNum2
                        ? words.countNum2 + sentenceWords.countNum
                        : sentenceWords.countNum
                    exist = true
                  }
                })
                if (!exist) {
                  let word = JSON.parse(JSON.stringify(sentenceWords))
                  word.countNum2 = sentenceWords.countNum
                  this.wordsList.push(word)
                }
              }
              // 添加关键词
              if (
                  this.wordsInfo1.markCrux &&
                  this.wordsInfo2.markCrux &&
                  sentenceWords.wordsType === 1
              ) {
                let exist = false
                this.wordsList.forEach((words) => {
                  if (
                      words.name === sentenceWords.name &&
                      words.wordsType === sentenceWords.wordsType
                  ) {
                    // 如果已经存在，增加数量
                    words.countNum2 = words.countNum2
                        ? words.countNum2 + sentenceWords.countNum
                        : sentenceWords.countNum
                    exist = true
                  }
                })
                if (!exist) {
                  let word = JSON.parse(JSON.stringify(sentenceWords))
                  word.countNum2 = sentenceWords.countNum
                  this.wordsList.push(word)
                }
              }
            })
          }
        })
      }

      // 排序
      if (this.wordsList && this.wordsList.length > 0) {
        this.wordsList.forEach((item) => {
          if (!item.groupStr) {
            item.groupStr = ''
          }
          if (!item.tradeIdArr) {
            item.tradeIdArr = '[1]'
          }

          // 关键词类型排序
          if (item.wordsType == 1) {
            this.cruxTypeList.forEach((cruxTypeItem) => {
              if (cruxTypeItem.value == item.type) {
                item.typeSort = cruxTypeItem.sort
              }
            })
          } else {
            item.typeSort = 0
          }
        })

        this.wordsList.sort((a, b) => {
          // 先按词语类型升序排序
          if (a.wordsType !== b.wordsType) {
            return a.wordsType - b.wordsType
          }

          // 再按词语分类升序排序
          if (a.typeSort !== b.typeSort) {
            return a.typeSort - b.typeSort
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
          return b.countNum1 - a.countNum1
        })
      }

      this.addWordTabelList()
    },
    // 获取关键词类型列表
    // async getCruxWordType() {
    getCruxWordType() {
      this.cruxTypeList = []
      // const res = await this.$httpBack.dictdata.list({ limit: -1, typeLogo: "crux_words_type" });
      const res = api.dictdata.list({
        limit: -1,
        typeLogo: 'crux_words_type'
      })
      if (res.code == 0) {
        this.cruxTypeList = res.data.list
      }
    },
    // 最大化/恢复正常
    togglemaxsize() {
      this.$httpClient.form.togglemaxsize().then((res) => {
        if (res.code == 0) {
          if (this.togglemaxsizeFlag == 'normal') {
            this.togglemaxsizeFlag = 'max'
          } else {
            this.togglemaxsizeFlag = 'normal'
          }
        }
      })
    },
    // 最小化
    minsize() {
      this.$httpClient.form.minsize().then((res) => {
        console.log('最小化')
      })
    },
    // 关闭
    close() {
      this.$httpClient.form.close().then((res) => {
        console.log('关闭')
      })
    },
    // 返回上一页
    back() {
      this.$store.commit('saveReplayPageMenu', {
        page: 'tabs',
        menu: 'compare'
      })
      this.$store.commit('saveHomeMenu', 2)
      this.$router.back()
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
<style scoped>
.wordsExportContainer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 0.5px #eee solid;
  padding-top: 6px;
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

:deep(.el-dialog__body) {
  padding: 0;
}

:deep(.el-dialog__header) {
  display: none;
}

:deep(.el-table--mini .el-table__cell) {
  padding: 0;
}

:deep(.el-table .el-table__cell) {
  padding: 0;
}

.shardText {
  color: #0077ff;
  margin-top: 10px;
  margin-right: 20px;
  cursor: pointer;
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
  margin-top: 4px;
  font-weight: 600;
  font-size: 14px;
  color: #2e3742;
}

.analysisLeftItemContainer {
  flex: 1;
  padding: 10px;
  border-right: 1px solid #ccc;
}

.analysisItemContainer {
  flex: 1;
  padding: 10px;
}

.contrastInfoContainer {
  display: flex;
}

.analysisNavText {
  font-weight: 500;
  font-size: 16px;
  color: #2e3742;
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
  justify-content: space-between;
  border-bottom: 1px solid #dce0e7;
  padding-bottom: 10px;
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
}

.contrastContainer {
  padding: 20px 32px 10px 32px;
  /* border: 1px solid #ccc; */
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
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

.wordsItemContainer {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 10px;
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
</style>
