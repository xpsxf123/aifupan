<template>
    <div class="slice-analysis">
        <el-drawer
            :visible.sync="drawer"
            :close-on-press-escape="false"
            :show-close="false"
            :destroy-on-close="true"
            :size="820"
            @close="initForm"
            :wrapperClosable="false">
            <div slot="title" class="flex items-center">
                <i class="el-icon-close text-xl cursor-pointer" @click="drawer=false"></i>
                <span class="font-bold text-base" style="padding-left: 12px">选择切片</span>
            </div>
            <el-form class="form-container" ref="dataForm" :rules="rules" :model="form" label-width="80px"
                     label-position="top"
                     size="default">
                <div style="height: calc(100vh - 117px);overflow: auto;padding-right: 30px">
                    <el-form-item required>
                            <span slot="label">
                                <span>切片时间</span>
                                <el-popover
                                    placement="bottom"
                                    trigger="hover"
                                    v-if="form.sliceTimeType===0"
                                    content="按照视频时间去切，而不是北京时间。">
                                    <i class="el-icon-question" slot="reference"
                                       style="color: #ABAEB3;padding-inline:5px"></i>
                                </el-popover>
                            </span>
                        <el-form-item style="margin-bottom: 0">
                            <el-radio-group v-model="form.sliceTimeType" class="slice-type" :disabled="isSliceAnalysis">
                                <el-radio :label="0">视频时间</el-radio>
                                <el-radio :label="1" v-if="getReplayType!=='fileAll'">北京时间</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item prop="timesRange" style="margin-bottom: 0;">
                            <el-slider
                                style="width: calc(100% - 40px);margin-left: 12px"
                                range
                                :disabled="isSliceAnalysis"
                                @change="sliderChange"
                                v-model="form.timesRange"
                                :format-tooltip="formatTooltip"
                                :max="rangeMax()">
                            </el-slider>
                        </el-form-item>
                        <div class="flex items-center">
                            <span style="color: var(--color-main)">{{ formatTooltip(form.timesRange[0]) }}</span>
                            <span class="pd-l30" style="color: var(--color-main)">
                                {{ formatTooltip(form.timesRange[1]) }}
                            </span>
                        </div>
                    </el-form-item>
                    <el-row>
                        <el-col :span="11">
                            <el-form-item label="切片类型" required prop="mainGroup">
                                <el-cascader
                                    style="width: 100%"
                                    v-model="form.mainGroup"
                                    :options="options">
                                </el-cascader>
                            </el-form-item>
                        </el-col>
                    </el-row>
                    <el-row>
                        <el-col :span="11">
                            <el-form-item label="输入文件名" required prop="videoName">
                                <el-input v-model="form.videoName" :disabled="isSliceAnalysis"
                                          placeholder="请输入文件名"></el-input>
                            </el-form-item>
                        </el-col>
                        <el-col :span="1" style="opacity: 0">-</el-col>
                        <el-col :span="12" v-if="getReplayType!=='fileAll' && form.mainGroup?.[0]!=1">
                            <el-form-item label="自动上传云空间">
                                <el-switch
                                    :disabled="isSliceAnalysis"
                                    v-model="form.isAutoUploadCloud"
                                    :active-value="1"
                                    :inactive-value="0"
                                    active-color="#13ce66">
                                </el-switch>
                            </el-form-item>
                        </el-col>
                    </el-row>
                    <el-form-item label="备注">
                        <el-input type="textarea" v-model="form.remarks" placeholder="请输入备注" :rows="3"
                                  :maxlength="100" show-word-limit
                                  resize="none"></el-input>
                    </el-form-item>

                    <el-row>
                        <el-col :span="11">
                            <el-form-item label="存储位置">
                                <el-select style="width: 100%" v-model="form.savePath" :disabled="isSliceAnalysis"
                                           placeholder="请存储位置"
                                           popper-class="savePosition">
                                    <el-option label="固定输出到视频文件夹里面" :value="0"></el-option>
                                    <el-option :label="form.savePath||'自定义路径'" :value="savePath">
                                        <div style="width: 100%" @click="checkFileBox">
                                            {{ form.savePath || '点击选择路径' }}
                                        </div>
                                    </el-option>
                                </el-select>
                            </el-form-item>
                        </el-col>

                    </el-row>
                    <el-form-item label="切片话术">
                        <el-input class="script" type="textarea" v-model="form.script" :rows="20" readonly
                                  resize="none"></el-input>
                    </el-form-item>
                </div>
                <el-form-item style="margin:12px 0 8px 0">
                    <afp-button type="primary" @click="onSubmit" :plain="false" size="default" :disabled="loading">
                        {{ isSliceAnalysis ? '确认修改切片' : '确认切片' }}
                    </afp-button>
                    <afp-button v-if="isSliceAnalysis" type="primary" @click="drawer=false" :plain="false"
                                size="default">
                        取消
                    </afp-button>
                </el-form-item>
            </el-form>
        </el-drawer>
    </div>
</template>

<script>
import {cloneDeep, isEqual} from "lodash";
import {Notification} from 'element-ui';
import myUtils from "@/utils/utils";

export default {
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => ({})
        },
        textScriptInfo: {
            type: Object,
            default: () => ({})
        },
    },
    data() {
        return {
            drawer: false,
            options: [],
            form: {
                timesRange: [],
                sliceTimeType: 0,
                videoName: '',
                mainGroup: '',
                isAutoUploadCloud: 0,
                remarks: '',
                savePath: 0,
                script: ''
            },
            loading: false,
            diffForm: {},
            rules: {
                timesRange: [
                    {required: true, message: '选择时间范围', trigger: 'change'},
                ],
                mainGroup: [
                    {required: true, message: '请选择分组', trigger: 'change'}
                ],
                videoName: [
                    {required: true, message: '请输入文件名', trigger: ['change', 'blur']},
                    {
                        required: true,
                        pattern: /^[^\\/:*?"<>|]+$/,
                        message: '文件名不能包含以下字符：\\ / : * ? " < > |',
                        trigger: ['change', 'blur']
                    }
                ],
            },
            savePath: ''
        };
    },
    computed: {
        getReplayType() {
            return myUtils.getReplayType(this.sentenceMarkData)
        },
        isSliceAnalysis() {
            return ['replaySection', 'replayShort', 'fileSection', 'fileShort'].includes(this.getReplayType)
        },
    },
    watch: {},
    methods: {
        initForm() {
            this.form = {
                timesRange: [],
                sliceTimeType: 0,
                videoName: '',
                mainGroup: [],
                isAutoUploadCloud: 0,
                remarks: '',
                savePath: 0,
                script: ''
            }
            this.savePath = ''
            this.$refs.dataForm?.resetFields()
        },
        rangeMax() {
            if (['fileAll', 'fileSection', 'fileShort'].includes(this.getReplayType)) {
                return this.isSliceAnalysis ? Number(this.sentenceMarkData?.uploadFile?.parentFileInfo?.fileDuration) : Number(this.sentenceMarkData?.uploadFile?.durationTime)
            }
            if (['replayAll', 'replaySection', 'replayShort'].includes(this.getReplayType)) {
                return this.isSliceAnalysis ? Number(this.sentenceMarkData?.videoInfo?.parentVideoInfo?.duration) : Number(this.sentenceMarkData?.videoInfo?.durationTime)
            }
        },
        async getDictDataListByCode() {
            const result = await this.$httpBack.dictdata.dictDataListByCode({
                code: 'video_slice_type'
            })
            const list = result.data || []

            const typeIndexMap = {
                fileSection: 0,
                replaySection: 0,
                fileShort: 1,
                replayShort: 1
            };
            const currentIndex = typeIndexMap[this.getReplayType] ?? null;

            list.map((item, index) => {
                const value = cloneDeep(item.value)
                item.children = JSON.parse(value)
                item.value = index
                item.disabled = currentIndex !== null && index !== currentIndex;
            })
            this.options = list
        },
        open({pureText, contenxtData, rangeSecond}) {
            let startItem = {}
            let endItem = {}
            let script = pureText || ''// 时间范围内的所有文本
            if (contenxtData) {   // 右键框选
                const initStartItem = contenxtData?.data?.items?.startItem
                let initEndItem = contenxtData?.data?.items?.endItem

                if (!initEndItem) initEndItem = this.getTextTimeRange(pureText || '', initStartItem)

                const result = this.getChartTimesText(initStartItem.startTime, initEndItem.endTime)
                startItem.startTime = result.startTime || 0
                endItem.endTime = result.endTime || 0
                script = result.script

            } else if (rangeSecond) { //数据曲线选择/编辑
                const initStartTime = (rangeSecond.data1?.[0] || 0) * 1000
                const initEndTime = (rangeSecond.data1?.[1] || 0) * 1000
                const result = this.getChartTimesText(initStartTime, initEndTime)
                startItem.startTime = result.startTime || 0
                endItem.endTime = result.endTime || 0
                script = result.script
            } else {//直接点击按钮切片
                if (['fileAll', 'fileSection', 'fileShort'].includes(this.getReplayType)) {
                    const {uploadFile} = this.sentenceMarkData
                    startItem.startTime = 0
                    endItem.endTime = (uploadFile?.durationTime > 600 ? 600 : (uploadFile?.durationTime || 0)) * 1000
                }
                if (['replayAll', 'replaySection', 'replayShort'].includes(this.getReplayType)) {
                    const {videoInfo} = this.sentenceMarkData
                    startItem.startTime = 0
                    endItem.endTime = (videoInfo?.durationTime > 600 ? 600 : (videoInfo?.durationTime || 0)) * 1000
                }
            }

            this.drawer = true;
            if (this.isSliceAnalysis) {
                const {videoInfo, uploadFile} = this.sentenceMarkData
                const {videoSliceInfo: sliceInfo} = videoInfo || uploadFile || {}
                this.form = {
                    sliceTimeType: sliceInfo.sliceTimeType || 0,
                    videoName: videoInfo?.VideoName || uploadFile?.fileName,
                    mainGroup: [sliceInfo?.sliceType, sliceInfo?.sliceClass],
                    isAutoUploadCloud: sliceInfo?.isAutoUploadCloud,
                    remarks: sliceInfo?.remarks,
                    savePathType: sliceInfo?.savePathType,
                    savePath: sliceInfo?.savePath
                }
                this.savePath = sliceInfo?.savePath
            }
            this.getDictDataListByCode()
            const {videoInfo, uploadFile, anchorInfo} = this.sentenceMarkData
            this.form = {
                ...this.form,
                script: script,
                videoName: (anchorInfo?.AnchorName || videoInfo?.VideoName || uploadFile?.fileName || '').replace('.mp4', ''),
                timesRange: [(startItem?.startTime) / 1000, (endItem?.endTime) / 1000]
            }
            this.diffForm = {
                ...this.form,
                script: script,
                videoName: (anchorInfo?.AnchorName || videoInfo?.VideoName || uploadFile?.fileName || '').replace('.mp4', ''),
                timesRange: [(startItem?.startTime) / 1000, (endItem?.endTime) / 1000]
            }
        },
        getChartTimesText(initStart, initEnd) {
            const originalInitStart = cloneDeep(initStart)
            const originalInitEnd = cloneDeep(initEnd)
            let initStartTime = initStart
            let initEndTime = initEnd
            if (this.isSliceAnalysis) {
                initStartTime = 0
                initEndTime = (this.rangeMax() || 0) * 1000
            }
            const {sentenceMarkList = []} = this.sentenceMarkData
            const allItems = sentenceMarkList.flatMap(block => block.items);
            //1弹幕数item.isBulletScreen  2成交量  3互动率item.isBulletScreen  4成交率  5语速
            const {
                isBulletScreen,
                dealVisible,
                salesVisible,
                uvVisible,
                interaction,
                dealRateVisible,
                analysisChar
            } = this.textScriptInfo

            const findStartItem = allItems.find(item => initStartTime >= item.startTime && initStartTime < item.endTime)
            const findEndItem = allItems.find(item => initEndTime > item.startTime && initEndTime <= item.endTime)
            const startTime = findStartItem ? findStartItem.startTime : initStartTime
            const endTime = findEndItem ? findEndItem.endTime : initEndTime

            const result = sentenceMarkList
                .filter(obj => obj.items.some(_item => initStartTime < _item.endTime))
                .find(obj => obj.items);

            let chartTimesText = ''

            sentenceMarkList.forEach((item, index) => {
                const currentItems = item.items?.filter(item => item.startTime >= startTime).filter(_item => _item.endTime <= endTime)
                let sectionChartTimesText = ''
                //选中的第一段与目标第一段，内容是否相同，不同则不展示时间与其它
                if (
                    (index + 1 === result?.currentSort && currentItems.length && currentItems.length === result?.items?.length)
                    || (index + 1 !== result.currentSort && currentItems.length)
                ) {
                    const isVideoSlice = ['replayAll', 'replaySection', 'replayShort'].includes(this.getReplayType)
                    const number = item.onlinePeopleObj?.number || 0
                    const bullet = isVideoSlice && isBulletScreen && item.isBulletScreen ? `弹幕数：${item.bulletScreenNum}\n` : ''
                    const onlineUsers = isVideoSlice ? `在线人数：${number}\n比上段：${item.onlinePeopleObj?.difference}\n\n` : ''
                    const deal = isVideoSlice && dealVisible ? `成交量：${item.dealCount}\n` : ''
                    const sales = isVideoSlice && salesVisible ? `销售额：${item.salesCount}\n` : ''
                    const uv = isVideoSlice && uvVisible ? `uv：${item.uv}\n` : ''


                    const interact = isVideoSlice && interaction && item.isBulletScreen && number ? `互动率：${((item?.bulletScreenNum / number) * 100).toFixed(2)}%\n` : ''
                    const dealRate = isVideoSlice && dealRateVisible && number ? `${((item?.dealCount / number) * 100).toFixed(2)}%\n` : ''
                    const char = analysisChar ? `${parseInt(item.charNumSecond || 0)}字/分钟\n` : ''
                    const bjTime = isVideoSlice ? `自然时间：${item.naturalTime}\n` : ''

                    sectionChartTimesText = `${item.startHm}\n${bullet}${onlineUsers}${deal}${sales}${uv}${interact}${dealRate}${char}${bjTime}`
                }
                currentItems.forEach((__item, __index) => {
                    const text = __item.word || "";
                    sectionChartTimesText += text;
                })
                if (sectionChartTimesText) {
                    chartTimesText += `${sectionChartTimesText}\n\n`
                }
            })
            return {
                script: chartTimesText,
                startTime: this.isSliceAnalysis ? originalInitStart : (findStartItem ? findStartItem.startTime : originalInitStart),
                endTime: this.isSliceAnalysis ? originalInitEnd : (findEndItem ? findEndItem.endTime : originalInitEnd)
            }
        },
        findLongestPrefixInArray(startItem, text) {
            const word = [startItem.word]
            let result = '';
            for (let i = 1; i <= text.length; i++) {
                const sub = text.slice(0, i);
                // 判断是否匹配
                if (word.some(str => str.includes(sub))) {
                    result = sub; // 保存当前最长匹配
                } else {
                    break; // 一旦不匹配就停止
                }
            }
            return result;
        },
        getTextTimeRange(text, startItem) {
            const {sentenceMarkList = []} = this.sentenceMarkData
            const allItems = sentenceMarkList.flatMap(block => block.items);
            const indexMap = [];

            // 拼接所有文字并建立索引映射
            allItems.forEach((item, idx) => {
                const text = (item.word || "")?.replace(/\s+/g, "");
                for (let i = 0; i < text.length; i++) {
                    indexMap.push(idx);
                }
            });

            // 去除多余换行/空格
            const target = text?.replace(/\s+/g, "");
            const currentItems = allItems.filter(_item => _item.startTime < startItem.startTime)
            let currentFullText = ''
            currentItems.forEach((item) => {
                const text = item.word || "";
                for (let i = 0; i < text.length; i++) {
                    currentFullText += text[i]?.replace(/\s+/g, "");
                }
            });

            const startIndex = currentFullText.length;
            if (startIndex === -1) {
                console.warn("未在原文中找到匹配文本");
                return null;
            }

            // 框选的词语可能是连续的词语 计算连续词语与框选词语的差值
            const continuousText = this.findLongestPrefixInArray(startItem, text)
            const differenceValue = startItem.word.length - continuousText.length

            //框选结束的下标
            const endIndex = startIndex + target.length + differenceValue - 1;

            // 对应的 items
            const endItem = allItems[indexMap[endIndex]];

            return {
                startTime: endItem.startTime,
                endTime: endItem.endTime,
                word: endItem.word,
            };
        },
        //手动修改时间 重新切片话术
        sliderChange(val) {
            if (val && val.length) {
                const initStartTime = val?.[0] * 1000
                const initEndTime = val?.[1] * 1000
                const result = this.getChartTimesText(initStartTime, initEndTime)
                this.form = {
                    ...this.form,
                    timesRange: [result?.startTime / 1000, result?.endTime / 1000],
                    script: result.script
                }
            } else {
                this.form = {
                    ...this.form,
                    script: ''
                }
            }
        },
        formatTooltip(val) {
            const [start, end] = this.form.timesRange
            if (this.form.sliceTimeType === 0) {
                return `视频${val === start ? '开始' : '结束'}时间：${myUtils.formatSeconds(val?.toFixed(0))}`
            } else {
                const {videoInfo} = this.sentenceMarkData
                const startTime = videoInfo?.StartTime
                return `北京时间${val === start ? '开始' : '结束'}：${myUtils.addSeconds(startTime, val)}`
            }
        },
        checkFileBox() {
            this.$httpClient.setup.checkfilebox({}).then(res => {
                if (res.code === 0) {
                    this.savePath = res.data || ''
                    this.$set(this.form, 'savePath', res.data || 0)
                }
            });
        },
        onSubmit() {
            this.$refs?.dataForm?.validate(async (valid) => {
                if (valid) {
                    if (isEqual(this.form, this.diffForm)) {
                        this.drawer = false
                        return
                    }
                    try {
                        const [start, end] = this.form.timesRange
                        this.loading = true
                        if (!(start >= 0 && end >= 0)) {
                            this.loading = false
                            return this.$message.error('视频时间错误')
                        }
                        const requestParams = {
                            sliceTimeType: this.form.sliceTimeType,
                            videoName: this.form.videoName,
                            savePathType: this.form.savePath === 0 ? 0 : 1,
                            savePath: this.form.savePath === 0 ? '' : this.form.savePath,
                            isAutoUploadCloud: this.form.mainGroup?.[0] == 0 ? this.form.isAutoUploadCloud : 0,
                            sliceType: this.form.mainGroup?.[0],
                            sliceClass: this.form.mainGroup?.[1],
                            startTimeMs: (start * 1000).toFixed(0),
                            endTimeMs: (end * 1000).toFixed(0),
                            remarks: this.form.remarks
                        }
                        let httpServer
                        if (this.isSliceAnalysis) {
                            httpServer = this.$httpBack.v2500.updateVideoSlice
                            if (['fileSection', 'fileShort'].includes(this.getReplayType)) {
                                requestParams.id = this.sentenceMarkData?.uploadFile?.videoSliceInfo?.id
                            }
                            if (['replaySection', 'replayShort'].includes(this.getReplayType)) {
                                requestParams.id = this.sentenceMarkData?.videoInfo?.videoSliceInfo?.id
                            }
                        } else {
                            httpServer = this.getReplayType === 'fileAll' ? this.$httpClient.anchorvideo.addFileSlice : this.$httpClient.anchorvideo.addVideoSlice
                            if (this.getReplayType === 'fileAll') {
                                requestParams.fileId = this.sentenceMarkData?.uploadFile?.fileId
                            }
                            if (this.getReplayType === 'replayAll') {
                                requestParams.videoId = this.sentenceMarkData?.videoInfo?.VideoId
                            }
                        }
                        const result = await httpServer({...requestParams})
                        if (result.code === 0) {
                            this.initForm()
                            this.drawer = false
                            this.$message.success(this.isSliceAnalysis ? '切片修改成功' : '切片任务创建成功');
                        }
                        this.loading = false
                        let tips = null
                        if (!this.isSliceAnalysis) {
                            tips = Notification({
                                title: '友情提示',
                                type: 'warning',
                                message: '生成中，切片越长、电脑配置越低，消耗时间越久，请稍后。',
                                duration: 0
                            });
                        } else {
                            window.location.reload();
                        }
                        if (result.code === 0) tips?.close?.()
                    } catch (e) {
                        this.loading = false
                    }
                } else {
                    return false;
                }
            });
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
<style lang='scss' scoped>
.slice-analysis {
    ::v-deep(.el-drawer__header) {
        padding: 16px;
        margin-bottom: 0;
    }

    ::v-deep(.el-drawer__body) {
        padding: 0 0 0 30px;
    }

    ::v-deep(.form-container) {
        .el-form-item__label {
            padding: 0;
        }

        .slice-type {
            position: absolute;
            top: -24px;
            left: 110px;
        }

        .script {
            .el-textarea__inner {
                background: #F5F7FA;
                color: #7a7c81;
            }
        }
    }

}
</style>