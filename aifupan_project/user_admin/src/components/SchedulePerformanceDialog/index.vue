<template>
  <el-drawer
    :model-value="modelValue"
    :show-close="false"
    header-class="common-drawer-header-style"
    body-class="common-drawer-body-style"
    footer-class="common-drawer-footer-style"
    :size="902"
    destroy-on-close
    @close="handleClose"
  >
    <template #header>
      <div class="schedule-performance-dialog__header">
        <CloseSvg @click="$emit('update:modelValue', false)" />
        <div class="schedule-performance-dialog__header-title">{{ props.performanceId ? '编辑' : '录入数据' }}</div>
      </div>
    </template>
    <div class="schedule-performance-dialog__body">
      <el-form ref="formRef" :model="form" :rules="rules" class="schedule-performance-dialog__form">
        <div class="schedule-performance-dialog__section">
          <div class="schedule-performance-dialog__section-title">直播信息</div>
          <div class="schedule-performance-dialog__section-body">
            <el-row class="schedule-performance-dialog__form-row">
              <el-col :span="24">
                <el-form-item label="直播日期" prop="liveDate" required>
                  <el-date-picker
                    v-model="form.liveDate"
                    type="date"
                    placeholder="请选择日期"
                    value-format="YYYY-MM-DD"
                    :disabled="isEditingWithScheduleId"
                    style="width: 250px"
                    class="rounded"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item v-if="!!form.liveDate" prop="scheduleId">
                  <template #label>直播班次<span class="schedule-performance-dialog__label-required">*</span></template>
                  <el-select
                    v-model="form.scheduleId"
                    clearable
                    filterable
                    placeholder="请选择"
                    :disabled="scheduleSelectDisabled"
                    :loading="scheduleListLoading"
                    style="width: 250px"
                    class="rounded"
                  >
                    <el-option
                      v-for="(opt, idx) in scheduleOptions"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                      :disabled="!!opt.disabled"
                    >
                      <span class="schedule-performance-dialog__schedule-option">
                        <span
                          class="schedule-performance-dialog__schedule-option-prefix"
                          :style="{ color: opt.disabled ? '#ABAEB3' : '#151719' }"
                          >{{ formatScheduleIndexLabel(idx) }}：</span
                        >
                        <span
                          class="schedule-performance-dialog__schedule-option-range"
                          :style="{ color: opt.disabled ? '#ABAEB3' : '#151719' }"
                          >{{ opt.label }}</span
                        >
                      </span>
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item v-if="!!form.liveDate" prop="liveTimeRange">
                  <template #label>直播时间<span class="schedule-performance-dialog__label-required">*</span></template>
                  <el-time-picker
                    v-model="form.liveTimeRange"
                    is-range
                    clearable
                    range-separator="至"
                    start-placeholder="开始时间"
                    end-placeholder="结束时间"
                    value-format="HH:mm:ss"
                    format="HH:mm"
                    placeholder="请选择"
                    :disabled="timeRangeDisabled"
                    style="width: 250px"
                    class="rounded"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>

        <div class="schedule-performance-dialog__section">
          <div class="schedule-performance-dialog__section-title">人员信息</div>
          <div class="schedule-performance-dialog__section-body">
            <div class="schedule-performance-dialog__staff-list">
              <el-form-item
                label-width="90px"
                v-for="(pick, idx) in form.staffPicks"
                :key="idx"
                label="直播人员"
                v-bind="idx === 0 ? { prop: 'staffPicks', required: true } : {}"
              >
                <div class="schedule-performance-dialog__staff-row">
                  <el-cascader
                    v-model="form.staffPicks[idx]"
                    :options="staffOptions"
                    :props="staffCascaderProps"
                    :popper-options="{ modifiers: [{ name: 'flip', enabled: false }] }"
                    clearable
                    filterable
                    :debounce="300"
                    placeholder="请选择"
                    :disabled="isStaffPickLocked({ idx })"
                    class="schedule-performance-dialog__control rounded"
                    @change="(val) => handleStaffPickChange({ idx, val })"
                    style="width: 250px"
                  />
                  <el-button
                    v-if="shouldShowStaffDelete({ idx })"
                    type="danger"
                    link
                    :disabled="isStaffPickLocked({ idx })"
                    class="schedule-performance-dialog__staff-delete"
                    @click="removeStaffPickRow({ idx })"
                  >
                    删除
                  </el-button>
                </div>
              </el-form-item>
            </div>
          </div>
        </div>

        <div class="schedule-performance-dialog__section">
          <div class="schedule-performance-dialog__section-title">数据填写</div>
          <div class="schedule-performance-dialog__section-body schedule-performance-dialog__section-body--data">
            <div class="schedule-performance-dialog__metrics">
              <div v-for="item in metricItems" :key="item.key" class="schedule-performance-dialog__metric">
                <div class="schedule-performance-dialog__metric-label">
                  {{ item.label }}
                </div>
                <div class="schedule-performance-dialog__metric-control">
                  <el-input
                    v-model="form.performance[item.key]"
                    placeholder="请输入"
                    class="schedule-performance-dialog__control rounded"
                  />
                  <div
                    v-if="!!form.performanceImages[item.key]"
                    class="schedule-performance-dialog__upload-btn schedule-performance-dialog__upload-btn--done"
                    @click="handleMetricPreview({ url: form.performanceImages[item.key] })"
                  >
                    <div class="schedule-performance-dialog__upload-icon">
                      <SvgIcon iconColor="#13CC63" name="photo" :iconStyle="iconStyle" />
                    </div>
                    <span>上传成功</span>
                    <div class="svg-container" @click.stop="handleMetricRemove({ metricKey: item.key })">
                      <SvgIcon name="round-close" :iconStyle="closeStyle" />
                    </div>
                  </div>
                  <el-upload
                    v-else
                    :show-file-list="false"
                    :http-request="(req) => handleMetricUpload(item.key, req)"
                    :disabled="!!metricUploading[item.key]"
                    accept="image/*"
                    class="schedule-performance-dialog__upload"
                  >
                    <div class="schedule-performance-dialog__upload-btn">
                      <div class="schedule-performance-dialog__upload-icon">
                        <SvgIcon iconColor="#444DFF" name="photo" :iconStyle="iconStyle" />
                      </div>
                      <span>上传截图</span>
                    </div>
                  </el-upload>
                </div>
              </div>
            </div>

            <div class="schedule-performance-dialog__summary">
              <div v-for="s in summaryItems" :key="s.key" class="schedule-performance-dialog__summary-item">
                <div class="schedule-performance-dialog__summary-title">{{ s.label }}</div>
                <div class="schedule-performance-dialog__summary-value">{{ s.value }}</div>
                <div class="schedule-performance-dialog__summary-sub">自动计算</div>
              </div>
            </div>
          </div>
        </div>
      </el-form>
      <el-image
        ref="previewImageRef"
        :src="previewImageUrl"
        :preview-src-list="previewImageUrl ? [previewImageUrl] : []"
        :preview-teleported="true"
        style="display: none"
      />
    </div>

    <template #footer>
      <el-button
        v-auth="props.performanceId ? 'room:performance:update' : 'room:performance:add'"
        class="schedule-performance-dialog__footer-btn"
        type="primary"
        :loading="submitting"
        @click="handleSubmit"
      >
        确定
      </el-button>
      <el-button class="schedule-performance-dialog__footer-btn" @click="handleClose">取消</el-button>
    </template>
  </el-drawer>
</template>

<script setup>
  import { computed, nextTick, ref, watch } from 'vue'
  import { ElMessage } from 'element-plus'
  import apiModule from '@/http/api'
  import { normalizeKeyLabelOptions } from '@/utils/options'

  const props = defineProps({
    modelValue: { type: Boolean, default: false },
    liveRoomId: { type: [Number, String], default: '' },
    liveRoomName: { type: String, default: '' },
    secUid: { type: String, default: '' },
    platformType: { type: [Number, String], default: '' },
    performanceId: { type: [Number, String], default: '' }
  })

  const emit = defineEmits(['update:modelValue', 'success'])

  const formRef = ref(null)
  const submitting = ref(false)
  const previewImageRef = ref(null)
  const previewImageUrl = ref('')

  const staffLoading = ref(false)
  const staffOptions = ref([])
  const staffOptionsLoaded = ref(false)
  const positionsLoadingPromise = ref(null)
  const employeesLoadingPromiseMap = ref({})
  const positionLabelMap = ref({})
  const employeeNameMap = ref({})
  const employeeNodesCache = ref({})
  const scheduleOptions = ref([])
  const scheduleListLoading = ref(false)
  const scheduleListLoadingPromise = ref(null)
  const scheduleListQueryKey = ref('')
  const isSyncingLiveInfo = ref(false)
  const effectiveSecUid = ref('')
  const effectivePlatformType = ref('')

  const form = ref({
    liveDate: '',
    scheduleId: '',
    liveTimeRange: [],
    startTime: '',
    endTime: '',
    staffPicks: [[]],
    staffPickLocks: [false],
    performance: {
      exposureCount: '',
      viewCount: undefined,
      salesRevenue: undefined,
      refund: undefined,
      investment: undefined,
      refundQuantity: '',
      payComboCnt: '',
      followCount: '',
      clickPaymentRate: '',
      interactionRate: '',
      maxOnline: ''
    },
    performanceImages: {
      exposureCount: '',
      viewCount: '',
      salesRevenue: '',
      refund: '',
      investment: '',
      refundQuantity: '',
      payComboCnt: '',
      followCount: '',
      clickPaymentRate: '',
      interactionRate: '',
      maxOnline: ''
    }
  })
  const closeStyle = computed(() => ({ width: '15px', height: '15px' }))
  const canQuerySchedule = computed(() => {
    const secUid = String(effectiveSecUid.value || '').trim()
    const platformType = Number(effectivePlatformType.value)
    return !!secUid && Number.isFinite(platformType)
  })
  const isEditingWithScheduleId = computed(() => {
    const pid = String(props.performanceId || '').trim()
    const scheduleId = normalizeScheduleId(form.value?.scheduleId)
    return !!pid && !!scheduleId
  })
  const scheduleSelectDisabled = computed(
    () => isEditingWithScheduleId.value || !!form.value?.liveTimeRange?.length || !canQuerySchedule.value
  )
  const timeRangeDisabled = computed(() => isEditingWithScheduleId.value || !!form.value?.scheduleId)
  const validateScheduleOrTime = (rule, value, callback) => {
    const scheduleId = normalizeScheduleId(form.value?.scheduleId)
    const timeRange = Array.isArray(form.value?.liveTimeRange) ? form.value.liveTimeRange : []
    const hasTime = timeRange.length === 2 && !!timeRange[0] && !!timeRange[1]
    if (!scheduleId && !hasTime) {
      callback(new Error('请选择直播班次或直播时间'))
      return
    }
    if (hasTime) {
      const [start, end] = timeRange.map((x) => String(x || '').trim())
      if (!start || !end) {
        callback(new Error('请选择直播时间'))
        return
      }
      if (start >= end) {
        callback(new Error('结束时间需晚于开始时间'))
        return
      }
    }
    callback()
  }
  const rules = {
    liveDate: [{ required: true, message: '请选择直播日期', trigger: 'change' }],
    scheduleId: [{ trigger: 'change', validator: validateScheduleOrTime }],
    liveTimeRange: [{ trigger: 'change', validator: validateScheduleOrTime }],
    staffPicks: [
      {
        required: true,
        trigger: 'change',
        validator: (rule, value, callback) => {
          const list = Array.isArray(value) ? value : []
          const ok = list.some((x) => Array.isArray(x) && x.length === 2 && x[0] && x[1])
          if (!ok) callback(new Error('请选择直播人员'))
          else callback()
        }
      }
    ]
  }

  const iconStyle = computed(() => ({ width: '13px', height: '13px' }))

  const resetForm = () => {
    form.value = {
      liveDate: '',
      scheduleId: '',
      liveTimeRange: [],
      startTime: '',
      endTime: '',
      staffPicks: [[]],
      staffPickLocks: [false],
      performance: {
        exposureCount: '',
        viewCount: undefined,
        salesRevenue: undefined,
        refund: undefined,
        investment: undefined,
        refundQuantity: '',
        payComboCnt: '',
        followCount: '',
        clickPaymentRate: '',
        interactionRate: '',
        maxOnline: ''
      },
      performanceImages: {
        exposureCount: '',
        viewCount: '',
        salesRevenue: '',
        refund: '',
        investment: '',
        refundQuantity: '',
        payComboCnt: '',
        followCount: '',
        clickPaymentRate: '',
        interactionRate: '',
        maxOnline: ''
      }
    }
  }

  const handleClose = () => {
    emit('update:modelValue', false)
  }

  const isStaffPickLocked = ({ idx }) => {
    const locks = Array.isArray(form.value?.staffPickLocks) ? form.value.staffPickLocks : []
    return !!locks?.[idx]
  }

  const syncStaffPicksByScheduleId = async ({ scheduleId }) => {
    const id = String(scheduleId || '').trim()
    if (!id) return

    const ensureScheduleOptionsReady = async () => {
      if ((scheduleOptions.value || []).some((x) => String(x?.value || '') === id)) return
      await fetchScheduleOptions()
    }

    await ensureScheduleOptionsReady()

    const opt = (scheduleOptions.value || []).find((x) => String(x?.value || '') === id)
    const workUserList = Array.isArray(opt?.workUserList) ? opt.workUserList : []
    if (!workUserList.length) return

    const incoming = workUserList
      .map((u) => {
        const pid = u?.positionId
        const eid = u?.employeeId
        if (pid === undefined || pid === null || eid === undefined || eid === null) return null
        const pidStr = String(pid).trim()
        const eidStr = String(eid).trim()
        if (!pidStr || !eidStr) return null
        return [pidStr, eidStr]
      })
      .filter(Boolean)

    const uniqMap = new Map()
    incoming.forEach((pair) => {
      const k = `${pair[0]}_${pair[1]}`
      if (!uniqMap.has(k)) uniqMap.set(k, pair)
    })
    const uniqIncoming = Array.from(uniqMap.values())
    if (!uniqIncoming.length) return
    const incomingKeySet = new Set(uniqIncoming.map((x) => `${x[0]}_${x[1]}`))

    const currentList = Array.isArray(form.value?.staffPicks) ? form.value.staffPicks : []
    const currentPicked = currentList
      .filter((x) => Array.isArray(x) && x.length === 2 && x[0] && x[1])
      .map((x) => [String(x[0]).trim(), String(x[1]).trim()])
      .filter((x) => x[0] && x[1])

    const shouldReplace = currentPicked.length === 0
    const merged = shouldReplace ? [] : [...currentPicked]
    const exists = new Set(merged.map((x) => `${x[0]}_${x[1]}`))
    uniqIncoming.forEach((pair) => {
      const k = `${pair[0]}_${pair[1]}`
      if (exists.has(k)) return
      exists.add(k)
      merged.push(pair)
    })

    const nextLocks = merged.map((pair) => incomingKeySet.has(`${pair[0]}_${pair[1]}`))

    const posNameMapPatch = {}
    const empNameMapPatch = {}
    workUserList.forEach((u) => {
      const pid = u?.positionId
      const eid = u?.employeeId
      const positionName = String(u?.positionName || '').trim()
      const employeeName = String(u?.employeeName || '').trim()
      if (pid !== undefined && pid !== null && positionName) posNameMapPatch[String(pid)] = positionName
      if (eid !== undefined && eid !== null && employeeName) empNameMapPatch[String(eid)] = employeeName
    })
    if (Object.keys(posNameMapPatch).length) {
      positionLabelMap.value = { ...(positionLabelMap.value || {}), ...posNameMapPatch }
    }
    if (Object.keys(empNameMapPatch).length) {
      employeeNameMap.value = { ...(employeeNameMap.value || {}), ...empNameMapPatch }
    }

    await ensurePositionsLoaded()
    Object.keys(posNameMapPatch).forEach((pid) => {
      const has = (staffOptions.value || []).some((x) => String(x?.value || '') === String(pid))
      if (has) return
      staffOptions.value = [
        ...(staffOptions.value || []),
        { value: String(pid), label: posNameMapPatch[pid] || String(pid), leaf: false }
      ]
    })

    const uniqPosIds = Array.from(new Set(merged.map((x) => String(x[0] || '').trim()))).filter(Boolean)
    for (const pid of uniqPosIds) {
      await ensureEmployeesLoaded(pid)
    }

    form.value.staffPicks = [...merged, []]
    form.value.staffPickLocks = [...nextLocks, false]
    normalizeStaffPicks()
  }

  const buildScheduleOptionLabel = (item) => {
    const startTime = String(item?.startTime || '').trim()
    const endTime = String(item?.endTime || '').trim()
    const startClock = startTime.length >= 16 ? startTime.slice(11, 16) : ''
    const endClock = endTime.length >= 16 ? endTime.slice(11, 16) : ''
    const range =
      startClock && endClock ? `${startClock}-${endClock}` : [startTime, endTime].filter(Boolean).join(' - ')
    return range || '-'
  }

  const formatScheduleIndexLabel = (idx) => {
    const map = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']
    const n = Number(idx) + 1
    const text = map[n - 1] || String(n)
    return `第${text}场排班`
  }

  const resolveScheduleTimeById = ({ scheduleId }) => {
    const id = String(scheduleId || '').trim()
    if (!id) return { startTime: '', endTime: '' }
    const opt = (scheduleOptions.value || []).find((x) => String(x?.value || '') === id)
    const startTime = String(opt?.startTime || '').trim()
    const endTime = String(opt?.endTime || '').trim()
    return { startTime, endTime }
  }

  const fetchScheduleOptions = async () => {
    const liveRoomId = props.liveRoomId
    const dateStr = String(form.value?.liveDate || '').trim()
    const secUid = String(effectiveSecUid.value || '').trim()
    const platformType = Number(effectivePlatformType.value)
    if (!liveRoomId || !dateStr || !secUid || !Number.isFinite(platformType)) {
      scheduleOptions.value = []
      scheduleListQueryKey.value = ''
      return
    }

    const key = `${String(liveRoomId)}_${dateStr}_${secUid}_${platformType}`
    if (scheduleListLoadingPromise.value && scheduleListQueryKey.value === key) return scheduleListLoadingPromise.value

    scheduleListQueryKey.value = key
    scheduleListLoading.value = true
    scheduleListLoadingPromise.value = (async () => {
      const schedulePerformanceId = (() => {
        const raw = props.performanceId
        if (raw === undefined || raw === null || raw === '') return undefined
        const n = Number(raw)
        return Number.isFinite(n) ? n : undefined
      })()
      const res = await apiModule.liveRoomPerformance.scheduleList({
        date: dateStr,
        secUid,
        platformType,
        schedulePerformanceId
      })
      const list = Array.isArray(res?.data) ? res.data : []
      const filteredList = list.filter((item) => {
        const rid = item?.roomId ?? item?.liveRoomId
        return String(rid || '') === String(liveRoomId)
      })
      const currentScheduleId = String(form.value?.scheduleId || '').trim()
      scheduleOptions.value = filteredList
        .map((item) => {
          const id = item?.scheduleId
          if (id === undefined || id === null) return null
          const scheduleId = String(id)
          const startTime = String(item?.startTime || '').trim()
          const endTime = String(item?.endTime || '').trim()
          return {
            value: scheduleId,
            label: buildScheduleOptionLabel(item),
            disabled: Number(item?.hasPerformance) === 1 && scheduleId !== currentScheduleId,
            startTime,
            endTime,
            workUserList: Array.isArray(item?.workUserList) ? item.workUserList : []
          }
        })
        .filter(Boolean)
    })()

    return scheduleListLoadingPromise.value.finally(() => {
      scheduleListLoadingPromise.value = null
      scheduleListLoading.value = false
    })
  }

  const buildEmployeeLabel = (item) => {
    const name = item?.name || ''
    const mobile = item?.mobile ? `(${item.mobile})` : ''
    if (!name && mobile) return mobile
    return `${name}${mobile}`
  }

  const ensurePositionsLoaded = async () => {
    if (staffOptionsLoaded.value) return
    if (positionsLoadingPromise.value) return positionsLoadingPromise.value

    staffLoading.value = true
    positionsLoadingPromise.value = (async () => {
      const res = await apiModule.position.options({ keyword: '', limit: 50 })
      const options = normalizeKeyLabelOptions(res?.data || []).map((o) => ({
        value: String(o.key),
        label: o.label,
        leaf: false
      }))
      const map = {}
      options.forEach((o) => {
        map[String(o.value)] = o.label
      })
      positionLabelMap.value = map
      staffOptions.value = options
      staffOptionsLoaded.value = true
    })()

    return positionsLoadingPromise.value.finally(() => {
      positionsLoadingPromise.value = null
      staffLoading.value = false
    })
  }

  const ensureEmployeesLoaded = async (positionId) => {
    const pid = String(positionId || '').trim()
    if (!pid) return
    const cached = employeeNodesCache.value?.[pid]
    if (Array.isArray(cached) && cached.length) return
    if (Array.isArray(cached) && cached.length === 0) {
      const pos = (staffOptions.value || []).find((x) => String(x.value) === pid)
      if (pos) pos.children = []
      return
    }
    if (employeesLoadingPromiseMap.value?.[pid]) return employeesLoadingPromiseMap.value[pid]

    staffLoading.value = true
    const p = (async () => {
      try {
        const res = await apiModule.employee.list({ page: 1, limit: 50, positionIds: [pid] })
        const list = res?.data?.list || []
        const nodes = list
          .map((item) => {
            const id = item?.id
            if (id === undefined || id === null) return null
            const eid = String(id)
            const name = item?.name || ''
            employeeNameMap.value = { ...(employeeNameMap.value || {}), [eid]: name }
            return { value: eid, label: buildEmployeeLabel(item), leaf: true }
          })
          .filter(Boolean)

        employeeNodesCache.value = { ...(employeeNodesCache.value || {}), [pid]: nodes }

        const pos = (staffOptions.value || []).find((x) => String(x.value) === pid)
        if (pos) pos.children = nodes
      } catch (e) {
        employeeNodesCache.value = { ...(employeeNodesCache.value || {}), [pid]: [] }
        const pos = (staffOptions.value || []).find((x) => String(x.value) === pid)
        if (pos) pos.children = []
        throw e
      }
    })()

    employeesLoadingPromiseMap.value = { ...(employeesLoadingPromiseMap.value || {}), [pid]: p }
    return p.finally(() => {
      const next = { ...(employeesLoadingPromiseMap.value || {}) }
      delete next[pid]
      employeesLoadingPromiseMap.value = next
      staffLoading.value = false
    })
  }

  const normalizeStaffPicks = () => {
    const picks = Array.isArray(form.value.staffPicks) ? [...form.value.staffPicks] : []
    const locks = Array.isArray(form.value.staffPickLocks) ? [...form.value.staffPickLocks] : []
    while (locks.length < picks.length) locks.push(false)
    while (locks.length > picks.length) locks.pop()
    const isEmpty = (x) => !Array.isArray(x) || x.length === 0 || !x[0] || !x[1]
    while (picks.length > 1 && isEmpty(picks[picks.length - 1]) && isEmpty(picks[picks.length - 2])) {
      picks.pop()
      locks.pop()
    }
    if (!picks.length) {
      picks.push([])
      locks.push(false)
    }
    form.value.staffPicks = picks
    form.value.staffPickLocks = locks
  }

  const handleStaffPickChange = async ({ idx, val }) => {
    if (!Array.isArray(form.value.staffPicks)) form.value.staffPicks = [[]]
    if (isStaffPickLocked({ idx })) return
    if (!Array.isArray(val) || val.length !== 2) {
      normalizeStaffPicks()
      return
    }
    const [pid, eid] = val.map((x) => String(x))
    await ensureEmployeesLoaded(pid)
    if (positionLabelMap.value?.[pid]) {
      positionLabelMap.value = { ...(positionLabelMap.value || {}), [pid]: positionLabelMap.value[pid] }
    }
    if (employeeNameMap.value?.[eid]) {
      employeeNameMap.value = { ...(employeeNameMap.value || {}), [eid]: employeeNameMap.value[eid] }
    }
    const isLast = idx === form.value.staffPicks.length - 1
    const last = form.value.staffPicks[form.value.staffPicks.length - 1]
    const hasTrailingEmpty = Array.isArray(last) && last.length === 0
    if (isLast && !hasTrailingEmpty) {
      form.value.staffPicks = [...form.value.staffPicks, []]
      const locks = Array.isArray(form.value.staffPickLocks) ? [...form.value.staffPickLocks] : []
      form.value.staffPickLocks = [...locks, false]
    }
    normalizeStaffPicks()
  }

  const shouldShowStaffDelete = ({ idx }) => {
    const list = Array.isArray(form.value.staffPicks) ? form.value.staffPicks : []
    if (list.length <= 1) return false
    if (idx < 0 || idx >= list.length) return false
    if (idx === list.length - 1) return false
    return true
  }

  const removeStaffPickRow = ({ idx }) => {
    if (isStaffPickLocked({ idx })) return
    const list = Array.isArray(form.value.staffPicks) ? [...form.value.staffPicks] : []
    if (idx < 0 || idx >= list.length) return
    if (idx === list.length - 1) return
    list.splice(idx, 1)
    form.value.staffPicks = list.length ? list : [[]]
    const locks = Array.isArray(form.value.staffPickLocks) ? [...form.value.staffPickLocks] : []
    if (idx >= 0 && idx < locks.length) locks.splice(idx, 1)
    form.value.staffPickLocks = locks.length ? locks : [false]
    normalizeStaffPicks()
  }

  const staffCascaderProps = {
    value: 'value',
    label: 'label',
    children: 'children',
    emitPath: true,
    lazy: true,
    lazyLoad: async (node, resolve) => {
      try {
        if (node.level === 0) {
          await ensurePositionsLoaded()
          resolve(staffOptions.value || [])
          return
        }
        if (node.level === 1) {
          const pid = String(node.value || '').trim()
          await ensureEmployeesLoaded(pid)
          resolve(employeeNodesCache.value?.[pid] || [])
          return
        }
        resolve([])
      } catch (e) {
        void e
        resolve([])
      }
    }
  }

  const buildPerformanceData = () => {
    const p = form.value.performance || {}
    const imgs = form.value.performanceImages || {}
    const build = ({ value, imageUrl, kind }) => {
      const hasImage = !!imageUrl
      const raw = value === undefined || value === null ? '' : String(value).trim()
      let hasValue = raw !== ''
      if (!hasValue && !hasImage) return undefined
      const data = {}
      if (hasValue) {
        const n = kind === 'int' ? Number.parseInt(raw, 10) : Number.parseFloat(raw)
        if (Number.isFinite(n)) data.value = n
        else hasValue = false
      }
      if (hasImage) data.imageUrl = imageUrl
      if (!hasValue && !hasImage) return undefined
      return data
    }
    const data = {}
    const exposureCount = build({ value: p.exposureCount, imageUrl: imgs.exposureCount, kind: 'int' })
    const viewCount = build({ value: p.viewCount, imageUrl: imgs.viewCount, kind: 'int' })
    const salesRevenue = build({ value: p.salesRevenue, imageUrl: imgs.salesRevenue, kind: 'float' })
    const refund = build({ value: p.refund, imageUrl: imgs.refund, kind: 'float' })
    const investment = build({ value: p.investment, imageUrl: imgs.investment, kind: 'float' })
    const refundQuantity = build({ value: p.refundQuantity, imageUrl: imgs.refundQuantity, kind: 'int' })
    const payComboCnt = build({ value: p.payComboCnt, imageUrl: imgs.payComboCnt, kind: 'int' })
    const followCount = build({ value: p.followCount, imageUrl: imgs.followCount, kind: 'int' })
    const clickPaymentRate = build({ value: p.clickPaymentRate, imageUrl: imgs.clickPaymentRate, kind: 'float' })
    const interactionRate = build({ value: p.interactionRate, imageUrl: imgs.interactionRate, kind: 'float' })
    const maxOnline = build({ value: p.maxOnline, imageUrl: imgs.maxOnline, kind: 'int' })
    if (exposureCount) data.exposureCount = exposureCount
    if (viewCount) data.viewCount = viewCount
    if (salesRevenue) data.salesRevenue = salesRevenue
    if (refund) data.refund = refund
    if (investment) data.investment = investment
    if (refundQuantity) data.refundQuantity = refundQuantity
    if (payComboCnt) data.payComboCnt = payComboCnt
    if (followCount) data.followCount = followCount
    if (clickPaymentRate) data.clickPaymentRate = clickPaymentRate
    if (interactionRate) data.interactionRate = interactionRate
    if (maxOnline) data.maxOnline = maxOnline
    return data
  }

  const metricUploading = ref({})

  const handleMetricPreview = ({ metricKey, url }) => {
    const safeUrl = String(url || '').trim()
    const key = safeUrl ? '' : String(metricKey || '')
    const resolvedUrl = safeUrl || String(form.value?.performanceImages?.[key] || '').trim()
    if (!resolvedUrl) return
    previewImageUrl.value = resolvedUrl
    nextTick(() => {
      previewImageRef.value?.showPreview?.()
    })
  }

  const handleMetricRemove = ({ metricKey }) => {
    const key = String(metricKey || '').trim()
    if (!key) return
    const imgs = form.value?.performanceImages || {}
    const currentUrl = String(imgs[key] || '').trim()
    if (!currentUrl) return
    form.value.performanceImages = { ...imgs, [key]: '' }
    if (previewImageUrl.value === currentUrl) previewImageUrl.value = ''
  }

  const handleMetricUpload = async (metricKey, req) => {
    const file = req?.file
    if (!file) return
    if (!/^image\//.test(file.type)) {
      ElMessage.error('仅支持图片文件')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.error('图片大小不能超过 10MB')
      return
    }

    metricUploading.value = { ...(metricUploading.value || {}), [metricKey]: true }
    try {
      const suffix = file.name.includes('.') ? file.name.slice(file.name.lastIndexOf('.')) : '.png'
      const presignedRes = await apiModule.employeeProfile.getImagePresignedUpload({ suffix })
      const uploadInfo = presignedRes?.data || {}
      if (!presignedRes || presignedRes.code !== 0 || !uploadInfo.uploadUrl) {
        ElMessage.error(presignedRes?.msg || '获取上传地址失败')
        return
      }
      const accessUrl =
        String(uploadInfo.accessUrl || uploadInfo.url || uploadInfo.fileUrl || uploadInfo.downloadUrl || '').trim() ||
        String(uploadInfo.uploadUrl || '').split('?')[0]
      if (!accessUrl) {
        ElMessage.error('获取图片地址失败')
        return
      }
      const uploadResponse = await fetch(uploadInfo.uploadUrl, {
        method: 'PUT',
        body: await file.arrayBuffer()
      })
      if (!uploadResponse.ok) {
        ElMessage.error('图片上传失败')
        return
      }
      form.value.performanceImages[metricKey] = accessUrl
      ElMessage.success('上传成功')
    } catch (e) {
      if (e?.response || e?.config) return
      void e
      ElMessage.error('图片上传失败')
    } finally {
      metricUploading.value = { ...(metricUploading.value || {}), [metricKey]: false }
      if (typeof req?.onSuccess === 'function') req.onSuccess()
    }
  }

  const handleSubmit = async () => {
    if (!props.liveRoomId) {
      ElMessage.warning('请先选择直播间')
      return
    }

    if (!formRef.value) return
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    const dateStr = String(form.value.liveDate || '').trim()
    const scheduleId = normalizeScheduleId(form.value?.scheduleId)
    const timeRange = Array.isArray(form.value?.liveTimeRange) ? form.value.liveTimeRange : []
    const hasTimeRange = timeRange.length === 2 && !!timeRange[0] && !!timeRange[1]
    let startTime = String(form.value?.startTime || '').trim()
    let endTime = String(form.value?.endTime || '').trim()
    if (scheduleId) {
      if (!startTime || !endTime) {
        const resolved = resolveScheduleTimeById({ scheduleId })
        startTime = resolved.startTime
        endTime = resolved.endTime
      }
    } else {
      startTime = hasTimeRange && dateStr ? `${dateStr} ${timeRange[0]}` : ''
      endTime = hasTimeRange && dateStr ? `${dateStr} ${timeRange[1]}` : ''
    }
    if (!startTime || !endTime) {
      ElMessage.warning('请补充开始时间和结束时间')
      return
    }
    const staffList = (form.value.staffPicks || [])
      .filter((x) => Array.isArray(x) && x.length === 2 && x[0] && x[1])
      .map(([pid, eid]) => ({
        positionId: String(pid),
        positionName: positionLabelMap.value?.[String(pid)] || '',
        employeeId: String(eid),
        employeeName: employeeNameMap.value?.[String(eid)] || ''
      }))

    submitting.value = true
    try {
      const res = await apiModule.liveRoomPerformance.schedulePerformanceSave({
        id: props.performanceId || undefined,
        scheduleId: scheduleId || undefined,
        liveRoomId: props.liveRoomId,
        startTime,
        endTime,
        staffList,
        performanceData: buildPerformanceData()
      })
      ElMessage.success('保存成功')
      emit('success', res?.data)
      handleClose()
    } finally {
      submitting.value = false
    }
  }

  const pickDateFromDateTime = (val) => {
    const s = String(val || '').trim()
    const m = s.match(/\d{4}-\d{2}-\d{2}/)
    return m ? m[0] : ''
  }

  const pickClockFromDateTime = (val) => {
    const s = String(val || '').trim()
    const m = s.match(/(\d{2}:\d{2})(:\d{2})?/)
    if (!m) return ''
    return `${m[1]}${m[2] || ':00'}`
  }

  const normalizeScheduleId = (val) => {
    const s = String(val ?? '').trim()
    if (!s || s === '0' || s === 'null' || s === 'undefined') return ''
    return s
  }

  const fillByDetail = async (detail) => {
    isSyncingLiveInfo.value = true
    try {
      const start = String(detail?.startTime || '')
      const end = String(detail?.endTime || '')
      const secUid = String(detail?.secUid || '').trim()
      const platformTypeRaw = detail?.platformType ?? detail?.platformTypeEnum ?? detail?.platform ?? detail?.source
      const platformType =
        platformTypeRaw === undefined || platformTypeRaw === null ? '' : String(platformTypeRaw).trim()
      if (!String(effectiveSecUid.value || '').trim() && secUid) effectiveSecUid.value = secUid
      if (!String(effectivePlatformType.value || '').trim() && platformType) effectivePlatformType.value = platformType
      const dateStr = pickDateFromDateTime(start) || pickDateFromDateTime(end)
      form.value.liveDate = dateStr
      const scheduleId = normalizeScheduleId(detail?.scheduleId)
      form.value.scheduleId = scheduleId
      form.value.startTime = start
      form.value.endTime = end
      const startClock = pickClockFromDateTime(start)
      const endClock = pickClockFromDateTime(end)
      form.value.liveTimeRange = scheduleId ? [] : startClock && endClock ? [startClock, endClock] : []
      const list = Array.isArray(detail?.staffList) ? detail.staffList : []
      const staffItems = list
        .map((s) => {
          const pid = s?.positionId !== undefined && s?.positionId !== null ? String(s.positionId).trim() : ''
          const eid = s?.employeeId !== undefined && s?.employeeId !== null ? String(s.employeeId).trim() : ''
          const pName = s?.positionName || ''
          const eName = s?.employeeName || ''
          if (pid && pName) positionLabelMap.value = { ...(positionLabelMap.value || {}), [pid]: pName }
          if (eid && eName) employeeNameMap.value = { ...(employeeNameMap.value || {}), [eid]: eName }
          if (!pid || !eid) return null
          return { pick: [pid, eid], locked: s?.isScheduleStaff === true }
        })
        .filter(Boolean)
      const picks = staffItems.map((x) => x.pick)
      const locks = staffItems.map((x) => !!x.locked)
      form.value.staffPicks = [...picks, []]
      form.value.staffPickLocks = [...locks, false]

      if (scheduleId) {
        await fetchScheduleOptions()
        const has = (scheduleOptions.value || []).some((x) => String(x?.value || '') === String(scheduleId))
        if (!has) {
          const fallback = {
            value: String(scheduleId),
            label: buildScheduleOptionLabel({ startTime: start, endTime: end }),
            disabled: false,
            startTime: start,
            endTime: end,
            workUserList: []
          }
          scheduleOptions.value = [fallback, ...(scheduleOptions.value || [])]
        }
        if (!String(form.value.liveDate || '').trim()) {
          const resolved = resolveScheduleTimeById({ scheduleId })
          const nextDate = pickDateFromDateTime(resolved.startTime) || pickDateFromDateTime(resolved.endTime)
          if (nextDate) form.value.liveDate = nextDate
        }
      }

      const pd = detail?.performanceData || {}
      form.value.performance = {
        exposureCount: pd?.exposureCount?.value ?? '',
        viewCount: pd?.viewCount?.value ?? '',
        salesRevenue: pd?.salesRevenue?.value ?? '',
        refund: pd?.refund?.value ?? '',
        investment: pd?.investment?.value ?? '',
        refundQuantity: pd?.refundQuantity?.value ?? '',
        payComboCnt: pd?.payComboCnt?.value ?? '',
        followCount: pd?.followCount?.value ?? '',
        clickPaymentRate: pd?.clickPaymentRate?.value ?? '',
        interactionRate: pd?.interactionRate?.value ?? '',
        maxOnline: pd?.maxOnline?.value ?? ''
      }

      form.value.performanceImages = {
        exposureCount: pd?.exposureCount?.imageUrl || '',
        viewCount: pd?.viewCount?.imageUrl || '',
        salesRevenue: pd?.salesRevenue?.imageUrl || '',
        refund: pd?.refund?.imageUrl || '',
        investment: pd?.investment?.imageUrl || '',
        refundQuantity: pd?.refundQuantity?.imageUrl || '',
        payComboCnt: pd?.payComboCnt?.imageUrl || '',
        followCount: pd?.followCount?.imageUrl || '',
        clickPaymentRate: pd?.clickPaymentRate?.imageUrl || '',
        interactionRate: pd?.interactionRate?.imageUrl || '',
        maxOnline: pd?.maxOnline?.imageUrl || ''
      }
    } finally {
      await nextTick()
      isSyncingLiveInfo.value = false
    }
  }

  const initOnOpen = async () => {
    resetForm()
    effectiveSecUid.value = String(props.secUid || '').trim()
    effectivePlatformType.value = String(props.platformType ?? '').trim()
    await ensurePositionsLoaded()
    if (!props.performanceId) return
    const res = await apiModule.liveRoomPerformance.schedulePerformanceDetail(props.performanceId)
    await fillByDetail(res?.data)
    const posIds = (form.value.staffPicks || [])
      .map((x) => (Array.isArray(x) ? String(x[0] || '') : ''))
      .filter(Boolean)
    Array.from(new Set(posIds.map((x) => String(x || '').trim()).filter(Boolean))).forEach((pid) =>
      ensureEmployeesLoaded(pid)
    )
  }

  watch(
    () => props.modelValue,
    (v) => {
      if (!v) return
      initOnOpen()
    }
  )

  watch(
    () => form.value.liveDate,
    async (v, ov) => {
      if (String(v || '') === String(ov || '')) return
      if (!isSyncingLiveInfo.value) {
        form.value.scheduleId = ''
        form.value.liveTimeRange = []
        form.value.startTime = ''
        form.value.endTime = ''
      }
      if (!v) {
        scheduleOptions.value = []
        scheduleListQueryKey.value = ''
        return
      }
      await fetchScheduleOptions()
    }
  )

  watch(
    () => form.value.scheduleId,
    (v, ov) => {
      if (String(v || '') === String(ov || '')) return
      if (isSyncingLiveInfo.value) return
      if (v) {
        form.value.liveTimeRange = []
        const resolved = resolveScheduleTimeById({ scheduleId: v })
        form.value.startTime = resolved.startTime
        form.value.endTime = resolved.endTime
        void syncStaffPicksByScheduleId({ scheduleId: v })
      } else {
        form.value.startTime = ''
        form.value.endTime = ''
        const list = Array.isArray(form.value.staffPicks) ? form.value.staffPicks : [[]]
        form.value.staffPickLocks = list.map(() => false)
        normalizeStaffPicks()
      }
    }
  )

  watch(
    () => form.value.liveTimeRange,
    (v) => {
      if (isSyncingLiveInfo.value) return
      const list = Array.isArray(v) ? v : []
      const hasValue = list.length === 2 && !!list[0] && !!list[1]
      if (hasValue) {
        form.value.scheduleId = ''
        const dateStr = String(form.value.liveDate || '').trim()
        form.value.startTime = dateStr ? `${dateStr} ${list[0]}` : ''
        form.value.endTime = dateStr ? `${dateStr} ${list[1]}` : ''
      } else if (!form.value.scheduleId) {
        form.value.startTime = ''
        form.value.endTime = ''
      }
    },
    { deep: true }
  )

  const metricItems = computed(() => [
    { key: 'exposureCount', label: '曝光次数', kind: 'int' },
    { key: 'viewCount', label: '观看人数', kind: 'int' },
    { key: 'refundQuantity', label: '退款单量', kind: 'int' },
    { key: 'payComboCnt', label: '成交单量', kind: 'int' },
    { key: 'maxOnline', label: '最高在线', kind: 'int' },
    { key: 'salesRevenue', label: '销售额', kind: 'float' },
    { key: 'refund', label: '退款金额', kind: 'float' },
    { key: 'investment', label: '投放金额', kind: 'float' },
    { key: 'followCount', label: '涨粉人数', kind: 'int' },
    { key: 'clickPaymentRate', label: '点击-成交率', kind: 'float' },
    { key: 'interactionRate', label: '互动率', kind: 'float' }
  ])

  const toNum = (v) => {
    const n = Number(v)
    return Number.isFinite(n) ? n : 0
  }

  const fmtPercent = (v) => `${(Number.isFinite(v) ? v : 0).toFixed(2)}%`
  const fmtNumber = (v) => `${(Number.isFinite(v) ? v : 0).toFixed(2)}`

  const summaryItems = computed(() => {
    const p = form.value.performance || {}
    const viewCount = toNum(p.viewCount)
    const salesRevenue = toNum(p.salesRevenue)
    const refund = toNum(p.refund)
    const investment = toNum(p.investment)
    const refundQuantity = toNum(p.refundQuantity)
    const payComboCnt = toNum(p.payComboCnt)
    const followCount = toNum(p.followCount)

    const netSales = salesRevenue - refund
    const conversionRate = viewCount > 0 ? (payComboCnt / viewCount) * 100 : 0
    const uvValue = viewCount > 0 ? salesRevenue / viewCount : 0
    const followRate = viewCount > 0 ? (followCount / viewCount) * 100 : 0
    const refundRate = payComboCnt > 0 ? (refundQuantity / payComboCnt) * 100 : 0
    const roi = investment > 0 ? salesRevenue / investment : 0
    const thousandSales = viewCount > 0 ? (salesRevenue / viewCount) * 1000 : 0

    return [
      { key: 'conversionRate', label: '带货转化率', value: fmtPercent(conversionRate) },
      { key: 'uvValue', label: 'UV价值', value: fmtNumber(uvValue) },
      { key: 'followRate', label: '涨粉率', value: fmtPercent(followRate) },
      { key: 'netSales', label: '净销售额', value: fmtNumber(netSales) },
      { key: 'refundRate', label: '退款率', value: fmtPercent(refundRate) },
      { key: 'roi', label: 'ROI', value: fmtNumber(roi) },
      { key: 'thousandSales', label: '千次成交', value: fmtNumber(thousandSales) }
    ]
  })
</script>

<style scoped lang="scss">
  .schedule-performance-dialog__header {
    display: flex;
    align-items: center;
    gap: 13px;
  }
  .schedule-performance-dialog__section:not(:last-child) {
    margin-bottom: 14px;
  }

  .upload-schedule-performance-dialog__section {
    margin-bottom: 50px;
  }

  .schedule-performance-dialog__section-title {
    font-weight: 600;
    margin-bottom: 14px;
    color: #303133;
  }

  .schedule-performance-dialog__section-body {
    padding: 24px;
    border-radius: 4px;
    background: #f7f7f7;
  }

  .schedule-performance-dialog__form-row {
    row-gap: 20px;
  }
  .schedule-performance-dialog__control {
    width: 250px !important;
  }

  .schedule-performance-dialog__schedule-option {
    display: inline-flex;
    align-items: center;
    font-weight: normal;
    gap: 6px;
  }

  .schedule-performance-dialog__label-required {
    margin-left: 4px;
    color: var(--el-color-danger);
  }

  .schedule-performance-dialog__metrics {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 15px 80px;
    margin-bottom: 36px;
  }

  .schedule-performance-dialog__metric-label {
    font-size: 14px;
    white-space: nowrap;
  }

  .schedule-performance-dialog__metric-control {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .schedule-performance-dialog__upload-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 5px;
    border-radius: 8px;
    height: 32px;
    padding: 10px;
    box-sizing: border-box;
    border: 1px dashed #444dff;
    background: #f4f9ff;
    color: #444dff;
    font-size: 12px;
    cursor: pointer;
    user-select: none;
  }

  .schedule-performance-dialog__upload-btn--done {
    position: relative;
    border: 1px dashed #13cc63;
    color: #13cc63;
    .svg-container {
      line-height: 0;
      position: absolute;
      top: -9px;
      right: -7px;
      cursor: pointer;
    }
  }

  .schedule-performance-dialog__upload-icon {
    line-height: 1;
  }

  .schedule-performance-dialog__summary {
    display: grid;
    grid-template-columns: repeat(auto-fill, 180px);
    justify-content: space-between;
    row-gap: 20px;
  }

  .schedule-performance-dialog__summary-item {
    background: #dcdcdc;
    border-radius: 8px;
    padding: 15px 12px;
    display: grid;
    grid-template-columns: 1fr auto;
    grid-template-rows: auto auto;
    row-gap: 4px;
    align-items: end;
  }

  .schedule-performance-dialog__summary-title {
    font-size: 14px;
    color: #151719;
  }

  .schedule-performance-dialog__summary-value {
    font-size: 20px;
    font-weight: 600;
    color: #151719;
    grid-column: 2 / 3;
    grid-row: 1 / 3;
    align-self: center;
  }

  .schedule-performance-dialog__summary-sub {
    font-size: 12px;
    color: #7a7c7f;
  }

  .schedule-performance-dialog__footer-btn {
    width: 84px;
    height: 34px;
    border-radius: 50px;
  }

  .schedule-performance-dialog__staff-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .schedule-performance-dialog__staff-row {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .schedule-performance-dialog__staff-delete {
    padding: 0;
  }

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  :deep(.el-form-item__label) {
    color: #151719;
  }
</style>
