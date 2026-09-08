<template>
  <div class="schedule-shift-form">
    <div class="section">
      <div class="section-title">
        <span class="required">*</span>
        班次信息
      </div>
      <div class="section-body">
        <div class="date-row">
          <div class="label" style="width: 60px">班次日期</div>
          <el-input :model-value="form.date" disabled class="date-input rounded" />
        </div>
      </div>
    </div>

    <div class="section" v-for="(shift, index) in form.shifts" :key="shift.key">
      <div class="section-body scheduling-section-body">
        <div class="shift-list">
          <div class="shift-card">
            <div class="shift-card-header">
              <div class="shift-tag">第{{ index + 1 }}场排班</div>
              <el-button
                v-if="mode === 'add' && form.shifts.length > 1"
                type="danger"
                plain
                class="remove-btn"
                @click="removeShift(index)"
              >
                删除
              </el-button>
            </div>

            <div class="shift-container">
              <div class="shift-top-row">
                <div class="staff-row">
                  <div class="label">人员选择</div>
                  <el-input
                    readonly
                    v-model="shift.memberName"
                    placeholder="请输入成员名称搜索"
                    class="staff-input rounded"
                  />
                  <div class="schedule-shift-form__pick">
                    <el-button
                      type="primary"
                      link
                      class="pick-btn"
                      :disabled="disablePickMember"
                      @click="emitPickMember(index)"
                      >选择成员</el-button
                    >
                    <div v-if="shift.memberId" class="schedule-shift-form__pick-hint"
                      >已选择：{{ shift.memberName }}</div
                    >
                  </div>
                </div>

                <div class="time-range-row">
                  <div class="label">排班时间</div>
                  <div class="time-range">
                    {{ getShiftRangeText(shift) }}
                  </div>
                </div>
              </div>

              <div class="shift-config">
                <div class="config-row">
                  <div class="label duration-label">班次时长</div>
                  <div class="check-options">
                    <el-checkbox
                      v-for="i in mergedDurationOptionList"
                      :key="`d_${i}`"
                      :model-value="shift.durationMin === i"
                      @change="(checked) => handleDurationOptionToggle({ shift, value: i, checked })"
                    >
                      {{ i }}分钟
                    </el-checkbox>
                  </div>
                </div>

                <div class="config-row">
                  <div class="label duration-label">休息时长</div>
                  <div class="check-options">
                    <el-checkbox
                      v-for="i in mergedRestOptionList"
                      :key="`r_${i}`"
                      :model-value="shift.restMin === i"
                      @change="(checked) => handleRestOptionToggle({ shift, value: i, checked })"
                    >
                      {{ i }}分钟
                    </el-checkbox>
                  </div>
                </div>

                <div class="config-row-time">
                  <div class="config-row-top">
                    <div class="label">开播时间</div>
                    <el-time-picker
                      v-model="shift.startTime"
                      value-format="HH:mm"
                      format="HH:mm"
                      class="time-picker rounded"
                      :clearable="false"
                      @change="syncEndTime(shift)"
                    />
                  </div>
                  <div class="config-row-bottom">
                    <div class="svg-container">
                      <SvgIcon
                        name="describe"
                        :iconStyle="{
                          width: '16px',
                          height: '16px'
                        }"
                      />
                    </div>
                    <p>所选开播时间与当前时间的间隔不得少于一小时。</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="mode === 'add'" class="add-shift-row">
      <el-button text class="add-shift-btn" @click="addShift" :icon="Plus"> 新增一场 </el-button>
    </div>

    <div class="section" v-if="mode === 'add'">
      <div class="section-title">批量排班</div>
      <div class="section-body">
        <div class="batch-row">
          <div class="label">班次循环</div>
          <el-checkbox-group v-model="form.batch.weekdays" class="weekdays">
            <el-checkbox :label="1">周一</el-checkbox>
            <el-checkbox :label="2">周二</el-checkbox>
            <el-checkbox :label="3">周三</el-checkbox>
            <el-checkbox :label="4">周四</el-checkbox>
            <el-checkbox :label="5">周五</el-checkbox>
            <el-checkbox :label="6">周六</el-checkbox>
            <el-checkbox :label="0">周日</el-checkbox>
          </el-checkbox-group>
        </div>

        <div class="batch-row">
          <div class="label">排班结束日期</div>
          <el-date-picker
            v-model="form.batch.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            placeholder="选择日期"
            class="end-date rounded"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file ScheduleShiftForm.vue
   * @description 排班弹窗表单（支持多场排班与批量排班配置）
   */
  import { computed, reactive, watch } from 'vue'
  import dayjs from 'dayjs'
  import { Plus } from '@element-plus/icons-vue'

  const props = defineProps({
    mode: {
      type: String,
      default: 'add'
    },
    initial: {
      type: Object,
      default: () => ({})
    },
    durationOptions: {
      type: Array,
      default: () => []
    },
    restOptions: {
      type: Array,
      default: () => []
    },
    disablePickMember: {
      type: Boolean,
      default: false
    }
  })

  const emit = defineEmits(['pick-member'])

  const normalizeOptions = (raw, fallback) => {
    const list = (Array.isArray(raw) ? raw : []).map((x) => Number(x)).filter((x) => Number.isFinite(x) && x >= 0)
    const uniq = Array.from(new Set(list))
    uniq.sort((a, b) => a - b)
    return uniq.length ? uniq : fallback
  }

  const durationOptionList = computed(() => normalizeOptions(props.durationOptions, [60, 120, 180]))
  const restOptionList = computed(() => normalizeOptions(props.restOptions, [0, 15, 30, 60, 120]))

  const mergeOptionList = (baseList, extraList) => {
    const base = Array.isArray(baseList) ? baseList : []
    const extra = Array.isArray(extraList) ? extraList : []
    const merged = Array.from(
      new Set([...base, ...extra].map((x) => Number(x)).filter((x) => Number.isFinite(x) && x >= 0))
    )
    merged.sort((a, b) => a - b)
    return merged
  }

  const createShift = (seed = {}) => {
    const startTime = seed?.startTime ? String(seed.startTime) : ''
    const hasDuration = seed && Object.prototype.hasOwnProperty.call(seed, 'durationMin')
    const hasRest = seed && Object.prototype.hasOwnProperty.call(seed, 'restMin')
    const durationSeed = seed?.durationMin === null || seed?.durationMin === undefined ? NaN : Number(seed.durationMin)
    const restSeed = seed?.restMin === null || seed?.restMin === undefined ? NaN : Number(seed.restMin)
    const durationMin =
      Number.isFinite(durationSeed) && durationSeed > 0
        ? durationSeed
        : props.mode === 'add' && hasDuration
          ? null
          : Number(durationOptionList.value?.[0] || 120)
    const restMin =
      Number.isFinite(restSeed) && restSeed >= 0
        ? restSeed
        : props.mode === 'add' && hasRest
          ? null
          : Number(restOptionList.value?.[0] || 0)
    return {
      key: `${Date.now()}_${Math.floor(Math.random() * 10000)}`,
      memberId: seed.memberId || '',
      memberName: seed.memberName || '',
      startTime,
      durationMin,
      restMin
    }
  }

  const ensureShiftDefaults = () => {
    const durationList = durationOptionList.value || []
    const restList = restOptionList.value || []
    if (!durationList.length || !restList.length) return
    ;(form.shifts || []).forEach((shift) => {
      if (!shift) return
      const duration = Number(shift.durationMin)
      if (props.mode === 'edit') {
        if (!Number.isFinite(duration) || duration <= 0) {
          shift.durationMin = durationList[0]
          syncEndTime(shift)
        }
      }
      const rest = Number(shift.restMin)
      if (!Number.isFinite(rest) || rest < 0) {
        shift.restMin = restList[0]
      }
    })
  }

  const mergedDurationOptionList = computed(() => {
    const extra = (form.shifts || [])
      .map((s) => (s?.durationMin === null || s?.durationMin === undefined ? NaN : Number(s.durationMin)))
      .filter((x) => Number.isFinite(x) && x > 0)
    return mergeOptionList(durationOptionList.value, extra)
  })

  const mergedRestOptionList = computed(() => {
    const extra = (form.shifts || [])
      .map((s) => (s?.restMin === null || s?.restMin === undefined ? NaN : Number(s.restMin)))
      .filter((x) => Number.isFinite(x) && x >= 0)
    return mergeOptionList(restOptionList.value, extra)
  })

  const form = reactive({
    date: '',
    shifts: [],
    batch: {
      weekdays: [],
      endDate: ''
    }
  })

  const applyInitial = () => {
    const init = props.initial || {}
    form.date = init.date || ''
    form.shifts = (init.shifts || []).map((x) => createShift(x))
    if (!form.shifts.length) form.shifts = [createShift({})]
    ensureShiftDefaults()
    form.batch = {
      weekdays: Array.isArray(init.batch?.weekdays) ? [...init.batch.weekdays] : [],
      endDate: init.batch?.endDate || ''
    }
  }

  /**
   * @description 新增一场排班
   */
  const addShift = () => {
    form.shifts.push(createShift(form.shifts[form.shifts.length - 1] || {}))
    ensureShiftDefaults()
  }

  /**
   * @description 删除指定场次
   * @param {number} index - 场次索引
   */
  const removeShift = (index) => {
    if (form.shifts.length <= 1) return
    form.shifts.splice(index, 1)
  }

  /**
   * @description 触发选择成员
   * @param {number} index - 场次索引
   */
  const emitPickMember = (index) => {
    if (props.disablePickMember) return
    emit('pick-member', { index, date: form.date })
  }

  /**
   * @description 同步结束时间展示（用于触发重渲染）
   * @param {Object} shift - 班次
   */
  const syncEndTime = (shift) => {
    const n = Number(shift?.durationMin)
    if (!Number.isFinite(n) || n <= 0) return
    shift.durationMin = n
  }

  const handleDurationOptionToggle = ({ shift, value, checked }) => {
    if (!shift) return
    if (checked) {
      shift.durationMin = Number(value)
      syncEndTime(shift)
      return
    }
    if (shift.durationMin === value) {
      shift.durationMin = Number(value)
      syncEndTime(shift)
    }
  }

  const handleRestOptionToggle = ({ shift, value, checked }) => {
    if (!shift) return
    if (checked) {
      shift.restMin = Number(value)
      return
    }
    if (shift.restMin === value) {
      shift.restMin = Number(value)
    }
  }

  /**
   * @description 获取单场排班时间范围文案
   * @param {Object} shift - 班次
   * @returns {string}
   */
  const getShiftRangeText = (shift) => {
    const start = shift?.startTime
    const durationMin = Number(shift?.durationMin) || 0
    if (!form.date || !start || durationMin <= 0) return '-'
    const startDt = dayjs(`${form.date} ${start}`)
    const endDt = startDt.add(durationMin, 'minute')
    if (!startDt.isValid() || !endDt.isValid()) return '-'
    return `${startDt.format('HH:mm')} - ${endDt.format('HH:mm')}`
  }

  /**
   * @description 获取提交数据（父组件在确认按钮处调用）
   * @returns {Object}
   */
  const getSubmitData = () => {
    return {
      date: form.date,
      shifts: form.shifts.map((s) => ({
        memberId: s.memberId,
        memberName: s.memberName,
        startTime: s.startTime,
        durationMin: Number(s.durationMin) || 0,
        restMin: Number(s.restMin) || 0
      })),
      batch: {
        weekdays: Array.isArray(form.batch.weekdays) ? [...form.batch.weekdays] : [],
        endDate: form.batch.endDate
      }
    }
  }

  const setMember = (index, member) => {
    const s = form.shifts?.[index]
    if (!s) return
    s.memberId = member?.id !== undefined && member?.id !== null ? String(member.id) : ''
    s.memberName = member?.name || ''
  }

  defineExpose({ getSubmitData, setMember })

  watch(
    () => props.initial,
    () => applyInitial(),
    { immediate: true, deep: true }
  )

  watch([durationOptionList, restOptionList], () => ensureShiftDefaults(), { immediate: true })
</script>

<style scoped lang="scss">
  .schedule-shift-form {
    padding: 0;
  }

  .section {
    margin-bottom: 24px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .section-title {
    display: flex;
    align-items: center;
    font-size: 16px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 10px;
  }

  .required {
    color: #f56c6c;
    margin-right: 4px;
  }

  .section-body {
    display: flex;
    flex-direction: column;
    row-gap: 24px;
    background: #f7f8fa;
    border-radius: 8px;
    padding: 24px;
    overflow: hidden;
  }
  .scheduling-section-body {
    padding: 0;
    .shift-container {
      padding: 0 24px 24px 24px;
      .shift-top-row {
        display: flex;
        gap: 12px;
        margin-bottom: 22px;
      }
    }
  }

  .date-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .label {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-shrink: 0;
    color: #151719;
    font-size: 14px;
    text-wrap: nowrap;
  }

  .date-input {
    width: 230px;

    :deep(.el-input__wrapper) {
      background-color: #dcdcdc;
      box-shadow: none;
    }

    :deep(.el-input__inner) {
      color: #151719 !important;
      -webkit-text-fill-color: #151719 !important;
    }
  }

  .shift-list {
    margin-top: 0;
  }

  .shift-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24px;
  }

  .shift-tag {
    display: flex;
    align-items: center;
    padding-left: 12px;
    width: 103px;
    height: 33px;
    border-bottom-right-radius: 30px;
    background: #444dff;
    color: #fff;
    font-size: 12px;
  }

  .remove-btn {
    width: 68px;
    height: 28px;
    border-radius: 61px;
    border: 1px solid #dcdcdc;
    font-size: 14px;
    margin: 9px 24px 0 0;
  }

  .staff-row {
    display: flex;
    gap: 10px;
    margin-right: 78px;
  }

  .staff-input {
    width: 200px;
  }

  .pick-btn {
    font-size: 14px;
    color: #444dff;
  }

  .schedule-shift-form__pick {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  .schedule-shift-form__pick-hint {
    font-size: 12px;
    color: #909399;
    line-height: 1.2;
    margin-top: 2px;
  }

  .time-range-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }

  .time-range {
    font-size: 14px;
    font-weight: 500;
    color: #151719;
    text-align: right;
  }

  .shift-config {
    display: flex;
    flex-direction: column;
    row-gap: 20px;
    padding: 20px;
    border-radius: 4px;
    background: rgba(223, 234, 246, 0.7);
  }

  .config-row {
    display: flex;
    align-items: center;
    gap: 12px;
    .duration-label {
      margin-right: 26px;
    }
  }

  .config-row-time {
    display: flex;
    flex-direction: column;
    justify-content: flex-start;

    .config-row-top {
      justify-content: flex-start;
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .config-row-bottom {
      display: flex;
      align-items: center;
      column-gap: 5px;
      font-size: 12px;
      color: #909399;
      margin-top: 10px;
      .svg-container {
        display: flex;
        align-items: center;
      }
    }
  }

  .check-options {
    display: flex;
    flex: 1;
    flex-wrap: wrap;
    column-gap: 32px;

    .el-checkbox {
      margin-right: 0;
    }
  }

  :deep(.time-picker) {
    width: 130px;
  }

  .add-shift-row {
    display: flex;
    justify-content: center;
    margin-bottom: 36px;
    border-radius: 8px;
    background: #f7f7f7;
  }

  .add-shift-btn {
    color: #151719;
    width: 100%;
    font-size: 14px;
  }

  .plus {
    display: inline-block;
    width: 18px;
    text-align: center;
    margin-right: 4px;
  }

  .batch-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .weekdays {
    display: flex;
    flex-wrap: wrap;
    gap: 10px 14px;
  }

  :deep(.end-date) {
    width: 230px;
  }
</style>
