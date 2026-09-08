<template>
    <el-table :data="convertListToBFormat" :header-cell-style="{ background: '#E8F5FF' }" border style="width: 100%" size="mini"
              max-height="420" ref="myTable">
        <!--        <el-table-column type="index" label="序号">-->
        <!--            &lt;!&ndash; 自定义序号 &ndash;&gt;-->
        <!--            <template #default="scope">-->
        <!--                {{ scope.$index + 1 }}-->
        <!--            </template>-->
        <!--        </el-table-column>-->
        <el-table-column v-if="convertListToBFormat.some(item => item.date)" prop="date" label="视频时间" align="center"
                         label-class-name="video-time" width="80">
        </el-table-column>
        <el-table-column :width="convertListToBFormat.some(item => item.date) ? '335' : '415'" prop="content"
                         label="相关句子" align="left" label-class-name="about-name">
            <template slot-scope="scope">
                <div v-html="scope.row.content"></div>
            </template>
        </el-table-column>
    </el-table>
</template>

<script>
import myUtils from '../../utils/utils'

export default {
    components: {},
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => {
                return {}
            }
        },
        row: {
            type: Object,
            default: () => {
                return {}
            }
        },
        // words: {
        //     type: Array,
        //     default: () => {
        //         return []
        //     }
        // }
    },
    data() {
        return {}
    },
    computed: {
        convertListToBFormat() {
            const list = this.sentenceMarkData?.sentenceMarkList || []
            const result = []

            list.forEach(item => {
                const {content, startTime, wordsList} = item

                wordsList.forEach(word => {
                    const {name, wordsType, recordNeedsWordList} = word
                    const keyword = name

                    // 根据句号、问号、感叹号分割内容为句子，并过滤空句子
                    const sentences = content.split(/[。？！]/).filter(s => s.trim() !== '')

                    // 查找内容中所有关键字出现的位置
                    let occurrenceIndex = -1
                    let lastIndex = -1

                    while ((lastIndex = content.indexOf(keyword, lastIndex + 1)) !== -1) {
                        occurrenceIndex++
                        // 检查 recordNeedsWordList 中的每个条件
                        recordNeedsWordList.forEach(condition => {
                            const {recordNeedsNum, restrictWord, restrictRange} = condition

                            // 当出现次数匹配 recordNeedsNum 时进行处理
                            if (occurrenceIndex === recordNeedsNum) {
                                // 如果有 restrictWord，检查范围条件
                                let matchesRange = true
                                if (restrictWord) {
                                    const start = Math.max(0, lastIndex - restrictRange)
                                    const end = Math.min(content.length, lastIndex + keyword.length + restrictRange)
                                    const rangeText = content.substring(start, end)
                                    matchesRange = rangeText.includes(restrictWord)
                                }

                                if (matchesRange && wordsType === this.row.wordsType) {
                                    // 找到包含该关键字的句子
                                    let targetSentence = ''
                                    let sentenceStartIndex = -1
                                    for (const sentence of sentences) {
                                        // 计算句子的起始索引
                                        const currentIndex = content.indexOf(sentence, sentenceStartIndex + 1)
                                        if (currentIndex === -1) continue // 跳过无法找到的句子
                                        sentenceStartIndex = currentIndex
                                        const sentenceEndIndex = sentenceStartIndex + sentence.length

                                        // 检查关键字是否在句子范围内
                                        if (sentence.includes(keyword) &&
                                            sentenceStartIndex <= lastIndex &&
                                            lastIndex < sentenceEndIndex) {
                                            targetSentence = sentence
                                            break
                                        }
                                    }

                                    // 在 targetSentence 中找到目标关键字的确切位置并格式化
                                    let formattedSentence = targetSentence
                                    let keywordCount = -1
                                    let targetPositionInSentence = -1

                                    // 计算目标关键字在句子中的相对位置
                                    let tempIndex = -1
                                    while ((tempIndex = targetSentence.indexOf(keyword, tempIndex + 1)) !== -1) {
                                        keywordCount++
                                        const globalPosition = sentenceStartIndex + tempIndex
                                        if (globalPosition === lastIndex) {
                                            targetPositionInSentence = tempIndex
                                            break
                                        }
                                    }

                                    // 只格式化目标位置的关键字
                                    if (targetPositionInSentence !== -1 && targetSentence) {
                                        const beforeTarget = targetSentence.substring(0, targetPositionInSentence)
                                        const afterTarget = targetSentence.substring(targetPositionInSentence + keyword.length)
                                        formattedSentence = `${beforeTarget}<span style="color: #537DB7; font-weight: bold;font-size:14px">${keyword}</span>${afterTarget}`
                                        result.push({
                                            date: startTime !== undefined ? myUtils.toformatTime(startTime) : null,
                                            keyword: keyword,
                                            content: formattedSentence
                                        })
                                    }
                                }
                            }
                        })
                    }
                })
            })
            const hoverkeyword = this.row.name
            const resultData = result.filter(item => item.keyword === hoverkeyword)
            return resultData
        }
    },
    watch: {},
    methods: {
        reportParagraph() {
            const rows = this.convertListToBFormat.map(row => `视频时间：${row.date}\n相关句子：${row.content.replace(/<[^>]*>/g, '')}`)
            const content = rows.join('\n\n')
            const blob = new Blob([content], {type: 'text/plain;charset=utf-8'})
            if (this.$isWeb) {
                const link = document.createElement('a')
                link.href = URL.createObjectURL(blob)
                link.download = `${this.sentenceMarkData?.videoInfo?.VideoName?.replace(/\.ts$/, '')}相关句子`
                link.click()
            } else {
                let formData = new FormData();
                formData.append("file", blob, `${this.sentenceMarkData?.videoInfo?.VideoName?.replace(/\.ts$/, '')}相关句子.txt`);
                formData.append("uploadType", 0);
                formData.append("generationType", 1);
                formData.append("otherObj", JSON.stringify({notFolder: 0}));
                this.$httpClient.uploadFile.frontUpload(formData).then(res => {
                    if (res.code === 0 && this.notFolder === 0) {
                        this.$message.success('相关句子已导出');
                    }
                }).finally(() => {
                })
            }
        }
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() {
    }, //生命周期 - 创建之前
    beforeMount() {
    }, //生命周期 - 挂载之前
    beforeUpdate() {
    }, //生命周期 - 更新之前
    updated() {
    }, //生命周期 - 更新之后
    beforeDestroy() {
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped></style>