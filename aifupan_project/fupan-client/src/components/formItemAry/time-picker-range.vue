<template>
    <div class="flex-ji-c">
        
        <el-time-picker v-model="timeValue[0]" :value-format="getValueFormat" @change="onTimeChange('start')" size="default"
            :format="getFormat" :placeholder="getStartPlaceholder" :default-value="defaultTimeValue1" style="width: 160px;"
            :picker-options="config.options1">
        </el-time-picker>
        <span class="mg-l6 mg-r6">至</span>
        <el-time-picker v-model="timeValue[1]" :value-format="getValueFormat" @change="onTimeChange('end')" size="default"
            :format="getFormat" :placeholder="getEndPlaceholder" :default-value="defaultTimeValue2" style="width: 160px;"
            :picker-options="config.options2">
        </el-time-picker>
    </div>
</template>

<script>
/**
 * 组件说明：时间范围选择器（双时间点）
 * 功能：
 * - 支持最小/最大可选时间间隔校验（跨天场景）
 * - 支持禁用时间段校验与自动纠正（向前优先，其次向后）
 * - 动态禁用段变更时自动适配当前选择
 * - 实时反馈提示信息，提升交互体验
 */

export default {
    components: {},
    props: {
        value: {
            type: Array,
            default: () => []
        },
        config: {
            type: Object,
            default: () => ({})
        },
        // 时间间隔配置（小时）
        maxTimeInterval: {
            type: Number,
            default: 12
        },
        // 最小时间间隔配置（分钟）
        minTimeInterval: {
            type: Number,
            default: 10
        },
        // 禁止选择的时间范围
        /*
            禁止选择的时间范围，格式为以下2种，可以是多个区间范围精致选择，也可以只指定一个时间范围
            ['HH:mm-HH:mm', 'HH:mm-HH:mm']或者
            [
                ['HH:mm-HH:mm','HH:mm-HH:mm'], 
                ['HH:mm-HH:mm','HH:mm-HH:mm'],
                ['HH:mm-HH:mm','HH:mm-HH:mm']
            ]
        */
        disabledTimes: {
            type: Array,
            default: () => {
                return []
            }
        },
        index: {
            type: Number,
            default: 0
        }
    },
        data() {
            return {
                timeValue: ['', ''],
                previousRecordTime: ['', ''],
                // 防抖控制变量，用于控制警告弹窗的触发频率
                messageDebounceTimer: null,
                lastChangedType: ''
            };
        },
    computed: {
        getDisabledTimes(){
            return this.disabledTimes.filter((a,index)=>index !== this.index)
        },
        defaultTimeValue1(){
            const tEnd = this.disabledTimes[this.index-1>=0? this.index-1 : 0];
            let times = tEnd?.[1]? tEnd[1] : '00:00';
            times = times?.split(':');
            let s = parseInt(times[0]) || 0
            let e = parseInt(times[1]) || 0
            return new Date(0, 0, 0, s, e)
        },
        defaultTimeValue2(){
            let [a,b] = this.timeValue[0] ? this.timeValue[0].split(':') : '00:00';
            let s = parseInt(a)+4 || 4
            let e = parseInt(b) || 0
            return new Date(0, 0, 0, s, e)
        },
        getValueFormat() {
            return this.config.valueFormat || 'HH:mm';
        },
        getFormat() {
            return this.config.format || 'HH:mm';
        },
        getStartPlaceholder() {
            return this.config.startPlaceholder || '任意开始时间点';
        },
        getEndPlaceholder() {
            return this.config.endPlaceholder || '任意结束时间点';
        }
    },
    watch: {
        timeValue: {
            handler(newVal){
                if(JSON.stringify(newVal) !== JSON.stringify(this.value)){
                    this.setData(newVal);
                }
            },
            deep: true
        },
        value:{
            handler(newVal){
                if(JSON.stringify(newVal) !== JSON.stringify(this.timeValue)){
                    this.setData(newVal,'setValue');
                }
            },
            deep: true,
            immediate: true
        }
    },
    methods: {
        /**
         * 设置组件内部时间值并同步到父组件
         * @param {Array<string>} value - 时间数组 [start, end]，格式为 HH:mm
         * @param {string} [type] - 调用类型，'init' 表示初始化不回传
         * @returns {void}
         */
        setData(value, type) {
            if (type === 'init') {
                this.timeValue = ['',''];
                return;
            }
            this.timeValue = value;   
            if(type === 'setValue'){
                return;
            }
            this.$emit('input', value);
        },
        /**
         * 初始化数据，将传入的 v-model 值同步到组件
         * @returns {void}
         */
        initData() {
            this.setData(this.value);
        },
        onTimeChange(type) {
            this.lastChangedType = type || '';
            this.checkAndCorrectTimeRange(this.timeValue[0], this.timeValue[1]);
        },
        /**
         * 检查并自动纠正时间范围，确保不超过2小时
         * @param {string} startTime - 开始时间 HH:mm 格式
         * @param {string} endTime - 结束时间 HH:mm 格式
         */
        /**
         * 检查并自动调整时间范围，支持跨天情况，确保时间间隔在允许范围内（10分钟-12小时）
         * @param {string} startTime - 开始时间 HH:mm 格式
         * @param {string} endTime - 结束时间 HH:mm 格式
         */
        /**
         * 主校验与纠正入口：
         * 1) 校验最小/最大间隔
         * 2) 校验禁用时间段重叠，自动按策略纠正
         * 3) 实时提示纠正结果
         * @param {string} startTime - 开始时间 HH:mm 格式
         * @param {string} endTime - 结束时间 HH:mm 格式
         * @returns {void}
         * @throws {Error} 当内部算法解析异常时抛出错误
         */
        checkAndCorrectTimeRange(startTime, endTime) {
            // 允许单边选择：仅结束或仅开始
            const hasStart = !!(startTime && startTime.trim() !== '');
            const hasEnd = !!(endTime && endTime.trim() !== '');
            if (!hasStart && !hasEnd) { return; }

            try {
                /**
                 * 计算工具：统一用秒进行计算
                 * 辅助：跨天差值、禁用段归一化、交叉判断、从零点寻找可用段。
                 */
                const DAY_SEC = 24 * 60 * 60;
                const maxSec = Number(this.maxTimeInterval || 0) * 60 * 60;
                const minSec = Number(this.minTimeInterval || 0) * 60;
                const fourHoursSec = 4 * 60 * 60;
                const toSec = (t) => this.getTime(t);
                const toStr = (s) => this.getStrTime(s);
                const diffSec = (s, e) => e >= s ? (e - s) : (DAY_SEC - s + e);
                const clampDay = (s) => ((s % DAY_SEC) + DAY_SEC) % DAY_SEC;
                const normDisabled = (dList) => {
                    const raw = Array.isArray(dList) && Array.isArray(dList[0]) ? dList : (Array.isArray(dList) && dList.length === 2 ? [dList] : []);
                    const normalized = raw.map(d => {
                        const s = clampDay(Number(Array.isArray(d) ? d[0] : 0));
                        const e = clampDay(Number(Array.isArray(d) ? d[1] : 0));
                        if (s === e) return null;
                        // 忽略跨天禁用段（s>e）以符合期望行为
                        return s < e ? [s, e] : null;
                    }).filter(Boolean).sort((a,b)=>a[0]-b[0]);
                    const merged = [];
                    for (let i=0;i<normalized.length;i++){
                        const [s,e] = normalized[i];
                        if (!merged.length){ merged.push([s,e]); continue; }
                        const last = merged[merged.length-1];
                        if (s <= last[1]){ last[1] = Math.max(last[1], e); } else { merged.push([s,e]); }
                    }
                    return merged;
                };
                const intersects = (seg, merged) => {
                    const [s,e] = seg;
                    for (let i=0;i<merged.length;i++){
                        const [ds,de] = merged[i];
                        if (Math.max(s, ds) < Math.min(e, de)) return true;
                    }
                    return false;
                };
                const findFromZero = (merged) => {
                    // 从0点开始寻找第一个可用的 >= minSec 的区间
                    if (!merged.length){ return [0, minSec]; }
                    // 0点到第一个禁用段之间
                    if (merged[0][0] >= minSec){ return [0, minSec]; }
                    // 中间空隙
                    for (let i=0;i<merged.length-1;i++){
                        const endPrev = merged[i][1];
                        const startNext = merged[i+1][0];
                        if (startNext - endPrev >= minSec){ return [endPrev, endPrev + minSec]; }
                    }
                    // 尾部空隙（禁用段之后）
                    const tailStart = merged[merged.length-1][1];
                    if (tailStart + minSec <= DAY_SEC){ return [tailStart, tailStart + minSec]; }
                    // 无法满足则回环到0点（兜底）
                    return [0, minSec];
                };
                // 解析时间
                const [startHour, startMinute] = hasStart ? startTime.split(':').map(Number) : [0, 0];
                const [endHour, endMinute] = hasEnd ? endTime.split(':').map(Number) : [0, 0];

                // 转换为分钟数进行计算
                let startTotalMinutes = hasStart ? (startHour * 60 + startMinute) : 0;
                let endTotalMinutes = hasEnd ? (endHour * 60 + endMinute) : 0;
                const maxIntervalMinutes = this.maxTimeInterval * 60;
                const minIntervalMinutes = this.minTimeInterval;

                // 判断是开始时间还是结束时间发生变化
                const previousTime = this.previousRecordTime || ['', ''];
                const [prevStartTime, prevEndTime] = previousTime;
                let isStartTimeChanged = hasStart ? (startTime !== prevStartTime) : false;
                let isEndTimeChanged = hasEnd ? (endTime !== prevEndTime) : false;
                if (this.lastChangedType === 'start') { isStartTimeChanged = true; isEndTimeChanged = false; }
                if (this.lastChangedType === 'end') { isEndTimeChanged = true; isStartTimeChanged = false; }

                let correctedTime = null;
                let warningMessage = '';
                const startSec = hasStart ? toSec(startTime) : 0;
                const endSec = hasEnd ? toSec(endTime) : 0;
                const mergedDisabled = normDisabled(this.getDisabledTimes?.map(d => Array.isArray(d) ? d.map(t=>this.getTime(t)) : this.getTime(d)) || []);
                // console.log('[time-range] 输入', { startTime, endTime, startSec, endSec, maxSec, minSec, lastChangedType: this.lastChangedType });

                // 当仅选择了结束时间时，自动回推最小间隔设置开始时间
                if (!hasStart && hasEnd) {
                    let correctedStartTotalMinutes = endTotalMinutes - minIntervalMinutes;
                    if (correctedStartTotalMinutes < 0) {
                        correctedStartTotalMinutes += 24 * 60;
                    }
                    const correctedStartHour = Math.floor(correctedStartTotalMinutes / 60);
                    const correctedStartMinute = correctedStartTotalMinutes % 60;
                    const correctedStartTime = `${String(correctedStartHour).padStart(2, '0')}:${String(correctedStartMinute).padStart(2, '0')}`;
                    const candidate = [correctedStartTime, endTime];
                    this.$nextTick(() => { this.setData(candidate); });
                    this.previousRecordTime = candidate;
                    return;
                }
                // 当仅选择了开始时间时，自动前推最大间隔设置结束时间
                if (hasStart && !hasEnd) {
                    const correctedEndTotalMinutes = (startTotalMinutes + maxIntervalMinutes) % (24 * 60);
                    const correctedEndHour = Math.floor(correctedEndTotalMinutes / 60);
                    const correctedEndMinute = correctedEndTotalMinutes % 60;
                    const correctedEndTime = `${String(correctedEndHour).padStart(2, '0')}:${String(correctedEndMinute).padStart(2, '0')}`;
                    const candidate = [startTime, correctedEndTime];
                    this.$nextTick(() => { this.setData(candidate); });
                    this.previousRecordTime = candidate;
                    return;
                }

                // 计算时间差（支持跨天情况）
                let timeDifference;
                if (endTotalMinutes >= startTotalMinutes) {
                    timeDifference = endTotalMinutes - startTotalMinutes;
                } else {
                    timeDifference = (24 * 60) - startTotalMinutes + endTotalMinutes;
                }
                let currentDiffSec = diffSec(startSec, endSec);
                const desiredLenSec = Math.min(Math.max(currentDiffSec, minSec), maxSec);
                const noOverlap = !intersects([startSec, endSec], mergedDisabled);
                // console.log('[time-range] 当前差值(秒)', currentDiffSec, '无禁用重叠', noOverlap);

                // 检查时间间隔是否超出允许范围
                if (timeDifference < minIntervalMinutes) {
                    // 时间间隔小于最小值，调整为最小间隔
                    if (isStartTimeChanged) {
                        // 开始时间变化，调整结束时间
                        const correctedEndTotalMinutes = (startTotalMinutes + minIntervalMinutes) % (24 * 60);
                        const correctedEndHour = Math.floor(correctedEndTotalMinutes / 60);
                        const correctedEndMinute = correctedEndTotalMinutes % 60;
                        const correctedEndTime = `${correctedEndHour.toString().padStart(2, '0')}:${correctedEndMinute.toString().padStart(2, '0')}`;

                        correctedTime = [startTime, correctedEndTime];
                        warningMessage = `时间间隔不能少于${this.minTimeInterval}分钟，已自动调整`;
                    } else if (isEndTimeChanged) {
                        // 结束时间变化，调整开始时间
                        let correctedStartTotalMinutes = endTotalMinutes - minIntervalMinutes;
                        if (correctedStartTotalMinutes < 0) {
                            correctedStartTotalMinutes += 24 * 60;
                        }

                        const correctedStartHour = Math.floor(correctedStartTotalMinutes / 60);
                        const correctedStartMinute = correctedStartTotalMinutes % 60;
                        const correctedStartTime = `${correctedStartHour.toString().padStart(2, '0')}:${correctedStartMinute.toString().padStart(2, '0')}`;

                        correctedTime = [correctedStartTime, endTime];
                        warningMessage = `时间间隔不能少于${this.minTimeInterval}分钟，已自动调整`;
                    }
                } else if (timeDifference > maxIntervalMinutes) {
                    // 时间间隔大于最大值，调整为最大间隔
                    if (isStartTimeChanged) {
                        // 开始时间变化，调整结束时间
                        const correctedEndTotalMinutes = (startTotalMinutes + maxIntervalMinutes) % (24 * 60);
                        const correctedEndHour = Math.floor(correctedEndTotalMinutes / 60);
                        const correctedEndMinute = correctedEndTotalMinutes % 60;
                        const correctedEndTime = `${correctedEndHour.toString().padStart(2, '0')}:${correctedEndMinute.toString().padStart(2, '0')}`;

                        correctedTime = [startTime, correctedEndTime];
                        warningMessage = `时间间隔不能超过${this.maxTimeInterval}小时，已自动调整`;
                    } else if (isEndTimeChanged) {
                        // 结束时间变化，调整开始时间
                        let correctedStartTotalMinutes = endTotalMinutes - maxIntervalMinutes;
                        if (correctedStartTotalMinutes < 0) {
                            correctedStartTotalMinutes += 24 * 60;
                        }

                        const correctedStartHour = Math.floor(correctedStartTotalMinutes / 60);
                        const correctedStartMinute = correctedStartTotalMinutes % 60;
                        const correctedStartTime = `${correctedStartHour.toString().padStart(2, '0')}:${correctedStartMinute.toString().padStart(2, '0')}`;

                        correctedTime = [correctedStartTime, endTime];
                        warningMessage = `时间间隔不能超过${this.maxTimeInterval}小时，已自动调整`;
                    }
                }
                // 额外规则：当选择超过4小时且无禁用重叠，自动调整为最大允许时间
                if (!correctedTime && noOverlap && currentDiffSec > fourHoursSec) {
                    if (isStartTimeChanged) {
                        correctedTime = [startTime, toStr(clampDay(startSec + desiredLenSec))];
                    } else if (isEndTimeChanged) {
                        correctedTime = [toStr(clampDay(endSec - desiredLenSec)), endTime];
                    }
                    warningMessage = `已按最大允许时长${this.maxTimeInterval}小时自动调整`;
                }
                // ===== 禁用时间段校验与纠正逻辑 =====
                if (this.getDisabledTimes?.length>0) {
                    // 有禁用时间段
                    let disabledTimes = this.getDisabledTimes?.map(d => {
                        if (Array.isArray(d)) {
                            return d.map(t => this.getTime(t))
                        }
                        return this.getTime(d)
                    });
                    let times = [this.getTime(startTime), this.getTime(endTime)]
                    let isError = this.isError(times, disabledTimes);
                    console.log(    times, disabledTimes, isError)
                    if (isError) {
                        let scopeTime = this.getScopeTime(times, disabledTimes, { isStartTimeChanged, isEndTimeChanged });
                        correctedTime = scopeTime.map(t => this.getStrTime(t));
                        warningMessage = warningMessage || '所选时间与禁用时间重叠，已自动避让';
                    }
                }

                // 如果仍未得到纠正结果，但当前区间与禁用段重叠，且无法满足条件，回退到从0点寻找首个可用区间
                if (!correctedTime && intersects([startSec, endSec], mergedDisabled)) {
                    const [fs, fe] = findFromZero(mergedDisabled);
                    correctedTime = [toStr(fs), toStr(fe)];
                    warningMessage = warningMessage || '无法满足当前选择，已从0点起顺延至第一个可用时间段';
                }

                // 更新时间并显示提示
                if (correctedTime) {
                    this.$nextTick(() => {
                        this.setData(correctedTime)
                    });

                    // 防抖处理：3秒钟内只能触发一次弹窗
                    if (warningMessage && !this.messageDebounceTimer) {
                        this.$message.info(warningMessage);
                        this.messageDebounceTimer = setTimeout(() => {
                            this.messageDebounceTimer = null;
                        }, 3000);
                    }

                    // 保存调整后的时间作为下次比较的基准
                    this.previousRecordTime = correctedTime;
                    return;
                }

                // 保存当前时间作为下次比较的基准
                this.previousRecordTime = [startTime, endTime];

            } catch (error) {
                console.error('时间范围检查出错:', error);
            }
        },
        // 按照禁止时间范围，配合最小时间，自动选择禁止时间范围外的满足最小时间的值，如果2个时间范围都被禁止，则按照大时间往后返回最小时间范围的时间。
        getScopeTime(times,disabledTimes,option={}){
            /**
             * 自动选择满足最小间隔的可用时间段
             *
             * 说明：
             * - 单位均为秒；`minTimeInterval` 为分钟，需要转换为秒。
             * - 输入 `times` 为当前选中的时间范围 [start, end]；`disabledTimes` 为禁用时间段集合。
             * - 若禁用时间段相邻或重叠导致两段间的空隙不足以容纳最小间隔，则取禁用段最大结束时间之后的最小间隔段。
             * - 若相邻禁用段之间存在满足最小间隔的空隙，则选择“最小的可用空隙”并在该空隙的起点返回一个长度为最小间隔的时间段。
             *
             * 示例：
             * - [[120*60,140*60],[150*60,160*60]]，最小间隔为10*60，则返回 [140*60,150*60]
             * - [[120*60,140*60],[140*60,150*60]] 或 [[120*60,140*60],[145*60,160*60]]，无满足最小间隔的空隙，返回 [maxEnd, maxEnd+min]
             *
             * @param {Array<number>} times 当前时间范围 [start, end]，单位秒
             * @param {Array<Array<number>>|Array<number>} disabledTimes 禁止时间段集合，形如 [[s,e],[s,e]] 或 [s,e]
             * @param {Object} option 方向选项，依据最近一次变更的时间点
             * @param {boolean} option.isStartTimeChanged 是否为开始时间被重新选择
             * @param {boolean} option.isEndTimeChanged 是否为结束时间被重新选择
             * @returns {Array<number>} 自动选择后的时间范围 [start, end]，单位秒
             * @throws {Error} 不抛出异常；若无法计算则回退为基于当前开始时间的最小间隔
             */
            // 基本参数与边界
            const MIN_SEC = (Number(this.minTimeInterval) || 0) * 60;
            const MAX_SEC = (Number(this.maxTimeInterval) || 0) * 60 * 60;
            const DAY_START = 0;
            const DAY_END = 24 * 60 * 60;
            const FOUR_HOURS = 4 * 60 * 60;

            // 若最小间隔未配置或无效，直接返回原时间范围
            if (!MIN_SEC || !Array.isArray(times) || times.length !== 2) {
                return times;
            }

            const currentStart = Number(times[0]) || DAY_START;
            const currentEnd = Number(times[1]) || (currentStart + MIN_SEC);
            const { isStartTimeChanged = false, isEndTimeChanged = false } = option || {};

            // 规范化禁用段：统一为 [[s,e], ...]
            const rawDisabled = Array.isArray(disabledTimes) && Array.isArray(disabledTimes[0])
                ? disabledTimes
                : (Array.isArray(disabledTimes) && disabledTimes.length === 2 ? [disabledTimes] : []);

            // 过滤与修正非法段（确保 s <= e）
            const normalized = rawDisabled
                .map(d => {
                    const s = Math.max(DAY_START, Number(d[0]) || DAY_START);
                    const e = Math.min(DAY_END, Number(d[1]) || DAY_START);
                    // 忽略跨天禁用段（s>e）以符合预期
                    return s < e ? [s, e] : null;
                })
                .filter(d => d && d[0] < d[1]);

            // 若无禁用段，保证返回至少为最小间隔
            if (normalized.length === 0) {
                const start = currentStart;
                const end = Math.max(start + MIN_SEC, currentEnd);
                return [start, end];
            }

            // 按起点排序
            normalized.sort((a, b) => a[0] - b[0]);

            // 合并相邻/重叠禁用段（a.start <= b.end 表示相邻或重叠）
            const merged = [];
            for (let i = 0; i < normalized.length; i++) {
                const [s, e] = normalized[i];
                if (merged.length === 0) {
                    merged.push([s, e]);
                } else {
                    const last = merged[merged.length - 1];
                    if (s <= last[1]) {
                        // 合并相邻或重叠
                        last[1] = Math.max(last[1], e);
                    } else {
                        merged.push([s, e]);
                    }
                }
            }

            // 计算可用空隙（包含日初与日末边界）
            const gaps = [];
            // 日初到第一段之前
            if (normalized[0] && (normalized[0][0] - DAY_START) >= MIN_SEC) {
                gaps.push([DAY_START, normalized[0][0]]);
            }
            // 中间空隙（禁用段之间）
            for (let i = 0; i < merged.length - 1; i++) {
                const prev = merged[i];
                const next = merged[i + 1];
                const gapStart = prev[1];
                const gapEnd = next[0];
                if (gapEnd - gapStart >= MIN_SEC) {
                    gaps.push([gapStart, gapEnd]);
                }
            }
            // 日末之后尾部空隙
            const tailStart = merged[merged.length - 1][1];
            if (DAY_END - tailStart >= MIN_SEC) {
                gaps.push([tailStart, DAY_END]);
            }
            // 记录最大禁用结束时间，用于无法找到空隙时的后移选择
            const lastEnd = merged[merged.length - 1][1];

            // 判断窗口内是否存在禁用段碰撞
            const hasIntersect = (winStart, winEnd) => {
                const s = Math.max(DAY_START, winStart);
                const e = Math.min(DAY_END, winEnd);
                if (e <= s) return false;
                for (let i = 0; i < merged.length; i++) {
                    const [ds, de] = merged[i];
                    if (Math.max(s, ds) < Math.min(e, de)) {
                        return true;
                    }
                }
                return false;
            };

            const intersectsForward4h = hasIntersect(currentStart, currentStart + FOUR_HOURS);
            const intersectsBackward4h = hasIntersect(currentEnd - FOUR_HOURS, currentEnd);

            // 目标长度：基于当前选择差值，限制在[min,max]
            const currentLen = currentEnd >= currentStart ? (currentEnd - currentStart) : (DAY_END - currentStart + currentEnd);
            const desiredLen = Math.min(Math.max(currentLen, MIN_SEC), MAX_SEC);

            // 定向扫描：当变更的是开始时间且其后4小时内没有禁用段，则从开始时间向后寻找可用段
            if (isStartTimeChanged && !intersectsForward4h) {
                let pos = currentStart;
                // 限制在当天范围内扫描
                while (pos < DAY_END) {
                    // 若 pos 落在某个禁用段内，则跳到该禁用段结束处
                    let jumped = false;
                    for (let i = 0; i < merged.length; i++) {
                        const [ds, de] = merged[i];
                        if (pos >= ds && pos < de) {
                            pos = de;
                            jumped = true;
                            break;
                        }
                    }
                    if (jumped) continue;

                    const candidateStart = pos;
                    const candidateEnd = candidateStart + desiredLen;
                    // 判断候选区间是否与禁用段相交
                    const conflict = merged.some(([ds, de]) => Math.max(candidateStart, ds) < Math.min(candidateEnd, de));
                    if (!conflict) {
                        return [candidateStart, candidateEnd];
                    }
                    // 与禁用段冲突，则将 pos 推到冲突段的结束处继续
                    for (let i = 0; i < merged.length; i++) {
                        const [ds, de] = merged[i];
                        if (Math.max(candidateStart, ds) < Math.min(candidateEnd, de)) {
                            pos = de;
                            break;
                        }
                    }
                }
                // 扫描失败则回退通用策略
            }

            // 定向扫描：当变更的是结束时间且其前4小时内没有禁用段，则从结束时间向前寻找可用段
            if (isEndTimeChanged && !intersectsBackward4h) {
                let posEnd = currentEnd;
                while (posEnd > DAY_START) {
                    // 若 posEnd 落在禁用段内，则跳到该禁用段起点处
                    let jumped = false;
                    for (let i = merged.length - 1; i >= 0; i--) {
                        const [ds, de] = merged[i];
                        if (posEnd > ds && posEnd <= de) {
                            posEnd = ds;
                            jumped = true;
                            break;
                        }
                    }
                    if (jumped) continue;

                    const candidateEnd = posEnd;
                    const candidateStart = candidateEnd - desiredLen;
                    if (candidateStart < DAY_START) break;
                    // 判断候选区间是否与禁用段相交
                    const conflict = merged.some(([ds, de]) => Math.max(candidateStart, ds) < Math.min(candidateEnd, de));
                    if (!conflict) {
                        return [candidateStart, candidateEnd];
                    }
                    // 与禁用段冲突，则将 posEnd 拉到冲突段起点继续
                    for (let i = merged.length - 1; i >= 0; i--) {
                        const [ds, de] = merged[i];
                        if (Math.max(candidateStart, ds) < Math.min(candidateEnd, de)) {
                            posEnd = ds;
                            break;
                        }
                    }
                }
                // 扫描失败则回退通用策略
            }

            // 若存在可用空隙：选取“最早且能容纳目标长度”的空隙，并返回从空隙起点开始的该长度
            if (gaps.length > 0) {
                const ordered = gaps.map(g => ({ start: g[0], end: g[1], len: g[1] - g[0] }))
                    .filter(g => g.len >= desiredLen)
                    .sort((a, b) => a.start - b.start);
                if (ordered.length > 0) {
                    const c = ordered[0];
                    return [c.start, c.start + desiredLen];
                }
            }

            // 无可用空隙：取最大禁用段结束时间之后的最小间隔
            const start = lastEnd;
            const end = start + MIN_SEC;
            if (end <= DAY_END) {
                return [start, end];
            }
            // 超过24小时兜底：从0点开始寻找首个可用区间
            const zeroStart = DAY_START;
            // 若0点到第一个禁用段之间可用
            if (merged[0] && merged[0][0] - zeroStart >= MIN_SEC) {
                return [zeroStart, zeroStart + MIN_SEC];
            }
            // 在禁用段之间寻找第一个满足最小间隔的空隙
            for (let i = 0; i < merged.length - 1; i++) {
                const endPrev = merged[i][1];
                const startNext = merged[i + 1][0];
                if (startNext - endPrev >= MIN_SEC) {
                    return [endPrev, endPrev + MIN_SEC];
                }
            }
            // 否则直接返回0点起的最小间隔
            return [zeroStart, zeroStart + MIN_SEC];
        },
        // 获取时间戳按秒算.通过格式来转换,'HH:mm:ss','HH:mm'
        getTime(timeStr) {
            if (!timeStr) {
                return 0;
            }
            return this.smartConvertTimeToTimestamp(timeStr)
        },
        // 更智能的格式检测版本（处理边界情况）
        smartConvertTimeToTimestamp(timeString) {
            /**
             * 智能转换时间字符串为“当日零点起的秒数”
             *
             * 说明：
             * - 返回的是相对当天零点的秒数（0-86399），而非 Unix Epoch 秒。
             * - 支持三种输入格式：`HH:mm:ss`、`HH:mm`、`mm:ss`，依据 `getValueFormat` 解析。
             * - 修正之前以 Epoch 秒参与计算导致的时区偏移问题（出现 09:52 这类错误）。
             *
             * @param {string} timeString 时间字符串
             * @returns {number} 当日零点起的秒数（若无效返回 0）
             * @throws 无
             */
            // 移除所有空格
            const cleanTime = String(timeString || '').replace(/\s/g, '');
            // 验证基本格式（允许 1 或 2 个冒号）
            if (!/^(\d{1,2}:){1,2}\d{1,2}$/.test(cleanTime)) {
                return 0;
            }
            const parts = cleanTime.split(':');
            let hours = 0, minutes = 0, seconds = 0;

            // 按当前配置格式解析
            if (this.getValueFormat === 'HH:mm:ss') {
                hours = parseInt(parts[0]);
                minutes = parseInt(parts[1], this.minTimeInterval);
                seconds = parseInt(parts[2]);
            } else if (this.getValueFormat === 'HH:mm') {
                hours = parseInt(parts[0]);
                minutes = parseInt(parts[1], this.minTimeInterval);
            } else if (this.getValueFormat === 'mm:ss') {
                minutes = parseInt(parts[0], this.minTimeInterval);
                seconds = parseInt(parts[1]);
            } else {
                return 0;
            }

            // 合法性校验
            if (
                !Number.isFinite(hours) || !Number.isFinite(minutes) || !Number.isFinite(seconds) ||
                hours < 0 || hours > 23 ||
                minutes < 0 || minutes > 59 ||
                seconds < 0 || seconds > 59
            ) {
                return 0;
            }

            // 返回相对当天零点的秒数，避免时区问题
            return hours * 3600 + minutes * 60 + seconds;
        },
        // 将秒级时间数据按照getValueFormat格式转换为字符串
        getStrTime(time){
            /**
             * 将秒级时间转换为字符串格式
             *
             * 说明：
             * - 输入为距当天零点的秒数（0-86400），内部会按需取模。
             * - 根据 `getValueFormat` 输出对应格式：`HH:mm:ss`、`HH:mm`、`mm:ss`。
             * - 当输入无效时，返回对应格式的零值字符串。
             *
             * @param {number} time 距当日零点的秒数
             * @returns {string} 格式化后的时间字符串
             * @throws 无
             */
            const fmt = this.getValueFormat;
            const sec = Number(time);
            const pad2 = (n) => String(n).padStart(2, '0');
            const invalidReturn = fmt === 'HH:mm:ss' ? '00:00:00' : (fmt === 'HH:mm' ? '00:00' : '00:00');

            if (!Number.isFinite(sec) || sec < 0) {
                return invalidReturn;
            }

            if (fmt === 'HH:mm:ss') {
                const s = sec % (24 * 60 * 60);
                const h = Math.floor(s / 3600);
                const m = Math.floor((s % 3600) / 60);
                const ss = s % 60;
                return `${pad2(h)}:${pad2(m)}:${pad2(ss)}`;
            }

            if (fmt === 'HH:mm') {
                const s = sec % (24 * 60 * 60);
                const h = Math.floor(s / 3600);
                const m = Math.floor((s % 3600) / 60);
                return `${pad2(h)}:${pad2(m)}`;
            }

            if (fmt === 'mm:ss') {
                // 仅保留一小时内的分钟与秒；超过1小时按 3600 取模
                const s = sec % 3600;
                const m = Math.floor(s / 60);
                const ss = s % 60;
                return `${pad2(m)}:${pad2(ss)}`;
            }

            return invalidReturn;
        },
        
        /**
         * 判断选中时间范围的起点或终点是否落入任一禁用时间段
         *
         * @param {Array<number>} times 当前选择的时间范围 [start, end]（秒）
         * @param {Array<Array<number>>|Array<number>} disabledTimes 禁用时间段集合，形如 [[s,e],[s,e]] 或 [s,e]
         * @returns {boolean} 若开始或结束点位于任一禁用段内，则返回 true
         * @throws {Error} 不抛出异常；输入不合法时返回 false
         */
        isError(times, disabledTimes) {
            // 基本形态校验
            if (!Array.isArray(times) || times.length !== 2) { return false }
            if (!Array.isArray(disabledTimes) || disabledTimes.length === 0) { return false }
            const start = Number(times[0]);
            const end = Number(times[1]);
            // 统一禁用段为二维数组 [[s,e], ...]
            const dTimes = Array.isArray(disabledTimes[0]) ? disabledTimes : [disabledTimes];
            // 只要开始或结束点落在某个禁用段内，则返回 true
            return dTimes.some(d => {
                if (!Array.isArray(d) || d.length !== 2) { return false }
                let ds = Number(d[0]);
                let de = Number(d[1]);
                if (Number.isNaN(ds) || Number.isNaN(de)) { return false }
                // 纠正无序输入
                if (ds > de) { const tmp = ds; ds = de; de = tmp; }
                const startIn = start > ds && start < de;
                const endIn = end > ds && end < de;
                return startIn || endIn;
            });
        }
    },
    created() {

    },
    mounted() {
        this.initData();
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() {
        // 清理防抖定时器，避免内存泄漏
        if (this.messageDebounceTimer) {
            clearTimeout(this.messageDebounceTimer);
            this.messageDebounceTimer = null;
        }
    }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped></style>
