<template>
  <div class="page-container">
    <div class="curd-wrapper" v-empty="emptyConfig">
      <Curd
        ref="curdRef"
        :table-columns="tableColumns"
        :search-config="searchConfig"
        :api="api"
        :auto-load="false"
        title="人员"
        totalUnit="个"
        show-total
        :show-add="false"
        :show-operation="false"
        :show-toolbar-right="false"
        :action-config="{ view: false, edit: false, del: false }"
        @load="handleLoad"
      >
        <template #employeeSelect="{ form }">
          <el-select
            v-model="form.employeeId"
            placeholder="请输入姓名"
            clearable
            filterable
            remote
            reserve-keyword
            :loading="employeeLoading"
            :remote-method="fetchEmployeeOptions"
            style="width: 200px"
          >
            <el-option v-for="opt in employeeOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
          </el-select>
        </template>

        <template #staff="{ row }">
          <div class="staff-cell">
            <div class="avtar-container">
              <el-avatar :size="32" :src="sanitizeUrl(row.userAvatar) || defaultImg" />
            </div>
            <div class="staff-cell__name">{{ row.name || '-' }}</div>
          </div>
        </template>

        <template #room="{ row }">
          <span class="room-cell">{{ row.roomText || '-' }}</span>
        </template>

        <template #todaySchedules="{ row }">
          <div v-if="row.todayItems?.length" class="schedule-list">
            <div v-for="it in row.todayItems" :key="it.key" class="schedule-item">
              <span class="schedule-dot schedule-dot--today"></span>
              <span class="schedule-text">{{ it.timeRange }}</span>
              <span class="schedule-duration">{{ it.durationText }}</span>
            </div>
          </div>
          <span v-else class="schedule-empty">-</span>
        </template>

        <template #tomorrowSchedules="{ row }">
          <div v-if="row.tomorrowItems?.length" class="schedule-list">
            <div v-for="it in row.tomorrowItems" :key="it.key" class="schedule-item">
              <span class="schedule-dot schedule-dot--tomorrow"></span>
              <span class="schedule-text">{{ it.timeRange }}</span>
              <span class="schedule-duration">{{ it.durationText }}</span>
            </div>
          </div>
          <span v-else class="schedule-empty">-</span>
        </template>

        <template #action="{ row }">
          <el-button type="primary" link style="color: #15acfe" @click="handleViewDetail(row)">查看排班</el-button>
        </template>
      </Curd>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 个人排班列表页
   */
  import { computed, onMounted, reactive, ref, watch } from 'vue'
  import dayjs from 'dayjs'
  import { useRouter } from 'vue-router'
  import Curd from '@/components/Curd/index.vue'
  import apiModule from '@/http/api'
  import { tableColumns } from './constants'
  import { normalizeKeyLabelOptions } from '@/utils/options'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const router = useRouter()
  const curdRef = ref(null)

  const positionOptions = ref([])
  const employeeOptions = ref([])
  const employeeLoading = ref(false)
  const defaultDateRange = [dayjs().subtract(6, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]
  const firstLoaded = ref(false)

  const orgTreeOptions = ref([])
  const deptNodesCache = ref({})
  const teamNodesCache = ref({})
  const employeeRemoteSeq = ref(0)

  const emptyConfig = reactive({
    visible: false,
    title: '',
    description: '',
    blur: 2,
    buttons: []
  })

  const refreshEmptyConfig = () => {
    emptyConfig.title = '暂无排班数据'
    emptyConfig.description = '请调整筛选条件后重试。'
    emptyConfig.buttons = []
  }

  const isDefaultSearch = () => {
    const params = curdRef.value?.searchParams || {}
    const orgPath = Array.isArray(params?.orgPath) ? params.orgPath : []
    return !params?.employeeId && !orgPath?.length && !params?.positionId
  }

  const handleLoad = (list) => {
    if (!firstLoaded.value) firstLoaded.value = true
    const isEmpty = Array.isArray(list) && list.length === 0
    emptyConfig.visible = firstLoaded.value && isEmpty && isDefaultSearch()
  }

  const fetchEmployeeOptions = async (keyword) => {
    const q = String(keyword || '').trim()
    if (!q) {
      employeeOptions.value = []
      employeeLoading.value = false
      return
    }

    const seq = (employeeRemoteSeq.value += 1)
    employeeLoading.value = true

    try {
      const params = { page: 1, limit: 20, name: q }
      const orgPath = Array.isArray(curdRef.value?.searchParams?.orgPath) ? curdRef.value.searchParams.orgPath : []
      const companyId = orgPath?.[0] ? String(orgPath[0]) : ''
      const deptId = orgPath?.[1] ? String(orgPath[1]) : ''
      const teamId = orgPath?.[2] ? String(orgPath[2]) : ''
      if (companyId) params.companyIds = [companyId]
      if (deptId) params.deptIds = [deptId]
      if (teamId) params.teamIds = [teamId]

      const res = await apiModule.employee.list(params)
      if (seq !== employeeRemoteSeq.value) return

      const list = Array.isArray(res?.data?.list) ? res.data.list : []
      employeeOptions.value = list
        .map((x) => {
          const id = x?.id
          if (id === undefined || id === null || id === '') return null
          const name = String(x?.name || '').trim()
          const mobile = String(x?.mobile || '').trim()
          const label = [name, mobile ? `(${mobile})` : ''].filter(Boolean).join('')
          return { key: String(id), label: label || String(id) }
        })
        .filter(Boolean)
    } catch (e) {
      void e
      if (seq !== employeeRemoteSeq.value) return
      employeeOptions.value = []
    } finally {
      if (seq === employeeRemoteSeq.value) employeeLoading.value = false
    }
  }

  const searchConfig = computed(() => {
    const posOptions = [{ label: '全部', key: '' }, ...(positionOptions.value || [])]
    return [
      { label: '人员', prop: 'employeeId', slotName: 'employeeSelect', width: '200px' },
      {
        label: '所属组织',
        prop: 'orgPath',
        type: 'cascader',
        options: orgTreeOptions,
        placeholder: '请选择',
        props: {
          value: 'value',
          label: 'label',
          children: 'children',
          emitPath: true,
          checkStrictly: true,
          lazy: true,
          lazyLoad: async (node, resolve) => {
            try {
              if (node.level === 0) {
                if (Array.isArray(orgTreeOptions.value) && orgTreeOptions.value.length) {
                  resolve(orgTreeOptions.value)
                  return
                }
                const res = await apiModule.subCompany.options({ keyword: '', limit: 200 })
                const list = normalizeKeyLabelOptions(res?.data || [])
                const nodes = list.map((x) => ({
                  value: x.key,
                  label: x.label,
                  leaf: false
                }))
                orgTreeOptions.value = nodes
                resolve(nodes)
                return
              }
              if (node.level === 1) {
                const companyId = String(node?.value || '')
                if (!companyId) {
                  resolve([])
                  return
                }
                if (!deptNodesCache.value?.[companyId]) {
                  const res = await apiModule.dept.options({ companyId, limit: 200 })
                  const list = normalizeKeyLabelOptions(res?.data || [])
                  deptNodesCache.value = {
                    ...(deptNodesCache.value || {}),
                    [companyId]: list.map((x) => ({
                      value: x.key,
                      label: x.label,
                      leaf: false
                    }))
                  }
                }
                resolve(deptNodesCache.value?.[companyId] || [])
                return
              }
              if (node.level === 2) {
                const deptId = String(node?.value || '')
                if (!deptId) {
                  resolve([])
                  return
                }
                if (!teamNodesCache.value?.[deptId]) {
                  const res = await apiModule.team.options({ deptId, limit: 200 })
                  const list = normalizeKeyLabelOptions(res?.data || [])
                  teamNodesCache.value = {
                    ...(teamNodesCache.value || {}),
                    [deptId]: list.map((x) => ({
                      value: x.key,
                      label: x.label,
                      leaf: true
                    }))
                  }
                }
                resolve(teamNodesCache.value?.[deptId] || [])
                return
              }
              resolve([])
            } catch (e) {
              void e
              resolve([])
            }
          }
        },
        opetion: { filterable: true }
      },
      { label: '所属岗位', prop: 'positionId', type: 'select', options: posOptions, width: '200px' }
    ]
  })

  const extractTime = (str) => {
    const s = String(str || '')
    if (!s) return ''
    if (s.includes('T')) return s.split('T')[1]?.slice(0, 5) || s
    if (s.includes(' ')) return s.split(' ')[1]?.slice(0, 5) || s
    return s.slice(0, 5)
  }

  const sanitizeUrl = (raw) => {
    const s = String(raw || '')
      .replace(/`/g, '')
      .trim()
    return s || ''
  }

  const formatDurationMin = (rawMin) => {
    const n = Number(rawMin)
    if (!Number.isFinite(n) || n <= 0) return '-'
    const total = Math.floor(n)
    const h = Math.floor(total / 60)
    const m = total % 60
    if (h > 0 && m > 0) return `${h}小时${m}分钟`
    if (h > 0) return `${h}小时`
    return `${m}分钟`
  }

  const buildScheduleItems = (rawList) => {
    const list = Array.isArray(rawList) ? rawList.filter(Boolean) : []
    const items = list
      .map((s) => {
        const start = extractTime(s?.startWork || s?.startTime || '')
        const end = extractTime(s?.endWork || s?.endTime || '')
        const timeRange = start && end ? `${start}-${end}` : '-'
        const durationText = formatDurationMin(s?.scheduleDuration ?? s?.durationMin ?? s?.duration)
        const key = String(s?.roomSchedulesId || s?.id || `${s?.workDay || ''}_${start}_${end}_${durationText}`)
        return { key, timeRange, durationText, roomId: s?.roomId }
      })
      .filter((x) => x.timeRange !== '-')

    items.sort((a, b) => String(a.timeRange).localeCompare(String(b.timeRange)))
    return items
  }

  const api = {
    list: async (params) => {
      const page = params?.page || 1
      const pageSize = params?.pageSize || 10
      const [startDate, endDate] = defaultDateRange
      const orgPath = Array.isArray(params?.orgPath) ? params.orgPath : []
      const companyId = orgPath?.[0] ? String(orgPath[0]) : undefined
      const deptId = orgPath?.[1] ? String(orgPath[1]) : params?.deptId || undefined
      const teamId = orgPath?.[2] ? String(orgPath[2]) : undefined
      const res = await apiModule.employeeSchedule.page({
        page,
        limit: pageSize,
        employeeId: params?.employeeId || undefined,
        companyId,
        deptId,
        teamId,
        positionId: params?.positionId || undefined,
        startDate,
        endDate
      })

      const payload = res?.data || {}
      const sourceList = Array.isArray(payload?.list) ? payload.list : []

      const all = sourceList.map((item) => {
        const i = item && typeof item === 'object' ? item : {}
        const roomInfos = Array.isArray(i.roomInfos) ? i.roomInfos.filter(Boolean) : []
        const roomText = Array.from(
          new Set(
            roomInfos
              .map((r) => r?.anchorName || r?.anchorNumber || r?.name)
              .filter(Boolean)
              .map((x) => String(x))
          )
        ).join('、')

        const todayItems = buildScheduleItems(i.thatDaySchedules)
        const tomorrowItems = buildScheduleItems(i.tomorrowSchedules)
        return {
          ...i,
          positionName: i.positionName || '',
          roomText,
          todayItems,
          tomorrowItems
        }
      })

      const total = payload?.totalCount ?? payload?.total ?? payload?.count ?? all.length
      return { list: all, total: Number(total) || 0 }
    }
  }

  const fetchPositionOptions = async () => {
    const res = await apiModule.position.options({ limit: 200 })
    positionOptions.value = normalizeKeyLabelOptions(res.data || [])
  }

  const handleViewDetail = (row) => {
    if (!row?.id) return
    router.push({
      name: 'StaffScheduleDetail',
      query: {
        id: String(row.id),
        name: row.name || ''
      }
    })
  }

  onMounted(async () => {
    await Promise.all([fetchPositionOptions()])
    refreshEmptyConfig()
    if (curdRef.value) {
      curdRef.value.getData()
    }
  })

  watch([orgTreeOptions, positionOptions], () => refreshEmptyConfig())
</script>

<style scoped>
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .staff-cell {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .schedule-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .schedule-item {
    display: flex;
    align-items: center;
    gap: 8px;
    line-height: 1.2;
  }

  .schedule-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    flex: none;
  }

  .schedule-dot--today {
    background: #67c23a;
  }

  .schedule-dot--tomorrow {
    background: #409eff;
  }

  .schedule-duration {
    margin-left: 6px;
  }

  .schedule-item {
    display: flex;
    align-items: center;
    font-size: 14px;
    line-height: 1.5;
  }

  .schedule-item .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    margin-right: 8px;
  }

  .schedule-item .time {
    margin-right: 8px;
  }

  .position-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
</style>
