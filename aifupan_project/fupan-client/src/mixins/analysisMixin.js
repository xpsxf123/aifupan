/**
 * @description 复盘详情数据归一化 mixin：负责把详情接口返回的原始结构转换为页面可直接渲染的段落数据。
 * 注意：在线人数、弹幕、成交与 ROI 等按时间范围落到段落上的字段，都在这里统一做兼容映射。
 */
import myUtils from "@/utils/utils";
import {PLATFORM_TYPE_ENUM} from '@/enum';
export default {
    data() {
        return {
        }
    },
    methods: {
        /**
         * @description 从单条分时数据中提取可用数值，兼容 `barrageNum/value/valueNum` 三种后端字段名。
         * @param {Object} item 分时项
         * @returns {number|null}
         */
        getTimelineMetricValue(item = {}) {
            const candidates = [item?.barrageNum, item?.value, item?.valueNum];
            const rawValue = candidates.find(v => v !== undefined && v !== null && v !== '');
            if (rawValue === undefined) {
                return null;
            }
            const value = Number(rawValue);
            return Number.isNaN(value) ? null : value;
        },
        /**
         * @description 按段落时间范围汇总分时数据，兼容毫秒/秒级时间戳。
         * 说明：ROI 与弹幕数组都可能在同一段落内出现多条数据，这里统一做分段汇总而不是只取第一条。
         * @param {Array} list 分时数组
         * @param {Object} item 当前段落
         * @returns {number|null}
         */
        getParagraphTimeMetric(list = [], item = {}) {
            if (!Array.isArray(list) || !list.length) {
                return null;
            }
            const matchList = list.filter(d => {
                const rawTime = d?.dateTime;
                const second = Number(rawTime) > 9999999999 ? Number(rawTime) / 1000 : Number(rawTime);
                return item.startTimeSecond <= second && second < item.endTimeSecond;
            });
            if (!matchList.length) {
                return null;
            }
            let total = 0;
            let hasValue = false;
            matchList.forEach(d => {
                const metricValue = this.getTimelineMetricValue(d);
                if (metricValue === null) {
                    return;
                }
                total += metricValue;
                hasValue = true;
            });
            return hasValue ? total : null;
        },
        initAudioaAlyses(data, callback) {
            // 视频信息
            if (data?.videoInfo?.Duration) {
                data.videoInfo.durationTime = data.videoInfo.Duration;
                data.videoInfo.Duration = myUtils.toformatTime(data.videoInfo.Duration * 1000);
            }
            // 文件信息
            if (data?.uploadFile?.fileDuration) {
                data.uploadFile.durationTime = data?.uploadFile?.fileDuration;
                data.uploadFile.fileDuration = myUtils.toformatTime(data?.uploadFile?.fileDuration * 1000);
            }

            // 在线人数对象 如果没有则为空（用于取实际值）
            let onlineNumMap = Object.fromEntries((data?.onlineNumList || [])?.map(d => {
                // 进行单位转换
                if (d.PeopleNum?.indexOf("万") != -1) {
                    d.PeopleNum = parseInt(d.PeopleNum?.split("万")?.[0] * 10000);
                }
                d.second = myUtils.toSecondByDate(d.RecordDate);
                // 进行对象转换会默认序列化（升序）。
                return [d.second, d];
            }));
            // 获取升序过后的序列化数据，用于判断取值
            let onlineNumList = Object.keys(onlineNumMap);

            const getPlatform = data?.videoInfo?.PlatformType || data?.fileInfo?.platformType;
            const isKuaishou = getPlatform == PLATFORM_TYPE_ENUM.kuaishou;

            // 分段数据
            let sentenceMarkList = (data?.fileAudioaAlyses || data?.audioaAlyses || []).filter(d => {
                return d.Status == 0 || d.status == 0
            }).map(d => {
                return JSON.parse(d?.dataJson || d?.DataJson)
            }).sort((a, b) => a.currentSort - b.currentSort);

            // 所有字段总字数
            let allCharCountNum = 0;

            sentenceMarkList.forEach((item, index, ary) => {
                item.charNum = item.content?.length || 0;
                // 设置段落的开始时间和结束时间(文本文件不设置)
                if (data?.uploadFile?.fileType == 2) { return }
                if (item?.items?.length) {
                    // 视频分析有时间分段
                    if (index == 0) {
                        item.startTime = 0;
                    } else {
                        item.startTime = item?.items[0].startTime;
                    }
                    item.endTime = item?.items[item?.items?.length - 1]?.endTime;
                }

                // 段落有自然时间开始时间和结束时间，上传可能不会有当前时间,主要用于做在线人数处理
                if (data?.videoInfo?.StartTime) {
                    item.startTimeSecond = myUtils.toSecondByDate(data.videoInfo.StartTime) + Math.floor(item.startTime / 1000)
                    item.endTimeSecond = myUtils.toSecondByDate(data.videoInfo.StartTime) + Math.floor(item.endTime / 1000);
                };
                item.startHm = myUtils.toformatTime(item.startTime);
                item.endHm= myUtils.toformatTime(item.endTime);
                
                if(data?.totalBarrageNum){
                    const barrageNum = this.getParagraphTimeMetric(data?.barrageDataList, item);
                    item.bulletScreenNum = barrageNum || 0;
                }
                
                item.isBulletScreen = !!data?.totalBarrageNum

                // ROI 段落数据与弹幕/在线人数一样，按时间段映射到当前段落上。
                const qianchuanCost = this.getParagraphTimeMetric(data?.qianchuanCostDataList, item);
                const netTransactionRoi = this.getParagraphTimeMetric(data?.netTransactionRoiDataList, item);
                item.qianchuanCost = qianchuanCost;
                item.netTransactionRoi = netTransactionRoi;
                
                
                
                // 视频才需要设置每个段落的自然时间
                if (data?.videoInfo?.VideoId) {
                    let videoStartTime = data.videoInfo.StartTime.substring(11);
                    // 处理自然事件转换
                    let currentTime = myUtils.toSecond(videoStartTime) * 1000 + item.startTime;
                    // 时间超过1天进行处理
                    if (currentTime >= 86400000) {
                        currentTime -= 86400000;
                    }
                    // 处理说话时间
                    item.naturalTime = myUtils.toformatTime(currentTime);
                    // 24点时间转换00:00:00
                    if (item.naturalTime == '24:00:00') {
                        item.naturalTime = '00:00:00';
                    }
                    
                    if(data?.totalBarrageNum){
                        const barrageNum = this.getParagraphTimeMetric(data?.barrageDataList, item);
                        item.bulletScreenNum = barrageNum || 0;
                    }
                    
                    item.isBulletScreen = !!data?.totalBarrageNum
                    
                    // 设置每个段落的在线人数情况
                    let onlinePeopleObj = {
                        number: 0,
                        difference: 0,
                        show: false,
                    }

                    if (onlineNumList?.length && !isKuaishou) {
                        let times = onlineNumList?.filter(d => item.startTimeSecond < d && d < item.endTimeSecond);
                        // 判断时间戳数组最后一条数据是否是空,如果是空 则取前一个段落的在线人数数据,如果是第一条数据并且为空则直接设置为0;
                        let number = 0;


                        let popData = onlineNumMap?.[times.pop()];
                        // 如果时间段内没有最后一条数据,则取上一条.
                        if (typeof popData == 'undefined') {
                            if (typeof ary[index - 1]?.onlinePeopleObj?.number !== 'undefined') {
                                number = parseInt(ary[index - 1]?.onlinePeopleObj?.number)
                            }
                        } else {
                            // 取时间段内最后一条在线人数
                            number = parseInt(popData?.PeopleNum);
                        }

                        onlinePeopleObj.number = number;
                        // 计算除去第一段的在线人数涨幅
                        if (index != 0) {
                            onlinePeopleObj.difference = parseInt(onlinePeopleObj.number) - parseInt(ary[index - 1]?.onlinePeopleObj?.number);
                        }
                        // 显示在线人数
                        onlinePeopleObj.show = true
                    }
                    item.onlinePeopleObj = onlinePeopleObj;
                    //成交量
                    if(data?.juLiangDataList?.length){
                        let juliangData = data?.juLiangDataList?.filter(d=>{
                            let dTime = d.dateTime/1000;
                            return item.startTimeSecond<=dTime && dTime<item.endTimeSecond;
                        })?.[0];
                        const salesCountValue = juliangData?.salesCount || 0
                        const uv = onlinePeopleObj?.number > 0 ? (salesCountValue / onlinePeopleObj.number) : 0
                        item.dealCount = juliangData?.payComboCnt || 0;
                        item.uv = uv > 0 ? uv.toFixed(2) : uv.toFixed(0)
                        item.salesCount = salesCountValue
                    }
                }

                // 计算每一段的语速
                if (item.content) {
                    let tempContent = item.content.replaceAll("，", "").replaceAll("。", "").replaceAll("？", "").replaceAll("、", "");
                    allCharCountNum += tempContent.length;
                    item.charNumSecond = tempContent.length / ((item.endTime - item.startTime) / 1000) * 60;
                    // console.log(item.charNumSecond ,tempContent.length, (item.endTime - item.startTime) / 1000, tempContent.length / ((item.endTime - item.startTime) / 1000) * 60)
                }
            });
            // 包含标点符号的全文总字数。
            data.allTextCountNum = sentenceMarkList?.map(d=>{
                return d.charNum
            })?.reduce((a,b)=>{return a+b},0);
            // 去除标点符号的全文总字数。
            data.allCharCountNum = allCharCountNum;

            if (typeof callback === 'function') {
                // 也可设置回调函数设置参数
                callback({
                    // playUrl,anchorInfo,fileInfo,videoInfo,sentenceMarkList
                    ...data,
                    cruxTypeList: data?.cruxTypeList,
                    wordsCollect: data?.wordsCollect,
                    wordsTabList: data?.wordsTabList,
                    fileInfo: data?.uploadFile,
                    sentenceMarkList
                })
            } else {
                // 默认渲染当前环境下的sentenceMarkData
                if (this.sentenceMarkData) {
                    Object.keys(data)?.forEach(key => {
                        this.sentenceMarkData[key] = data[key];
                    })
                    // this.sentenceMarkData = {
                    //     ...data
                    // }
                    // 主播信息
                    this.sentenceMarkData.anchorInfo = data.anchorInfo;
                    // 视频/音频播放地址
                    this.sentenceMarkData.playUrl = data.playUrl;
                    // 上传文件分析数据
                    if (data?.uploadFile) {
                        this.sentenceMarkData.fileInfo = data?.uploadFile;
                    }
                    // 录制视频分析数据
                    if (data?.videoInfo) {
                        this.sentenceMarkData.videoInfo = data?.videoInfo;
                    }
                    // 分析段落数据
                    this.sentenceMarkData.sentenceMarkList = sentenceMarkList;
                    // 关键词分类数据
                    this.sentenceMarkData.cruxTypeList = data?.cruxTypeList;
                    // 
                    this.sentenceMarkData.wordsCollect = data?.wordsCollect;
                    // 运营关键词列表数据
                    this.sentenceMarkData.wordsTabList = data?.wordsTabList;
                }
            }
        },
    }
}
