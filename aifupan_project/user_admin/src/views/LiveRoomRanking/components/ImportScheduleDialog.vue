<template>
  <el-dialog
    v-model="visible"
    title="导入排班"
    width="92%"
    top="5vh"
    class="schedule-import-dialog"
    :close-on-click-modal="false"
  >
    <div class="import-dialog">
      <!-- 上传区 -->
      <div v-if="!importData.length" class="upload-area">
        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :on-exceed="handleExceed"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽文件到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">
              请上传通过「下载排班模板」导出的 .xlsx 文件；解析后可再次编辑，确认后点「提交」落库。
            </div>
          </template>
        </el-upload>
        <div v-if="parsing" v-loading="parsing" class="parsing-tip">正在解析...</div>
      </div>

      <!-- 解析警告 -->
      <el-alert
        v-if="parseWarnings.length"
        class="warning-block"
        :title="parseWarnings.join('；')"
        type="warning"
        :closable="false"
        show-icon
      />

      <!-- 可编辑预览区 -->
      <div v-if="importData.length" class="preview-area">
        <div class="preview-toolbar">
          <span class="legend">
            <span class="legend-item"><span class="dot ok"></span>已匹配</span>
            <span class="legend-item"><span class="dot notfound"></span>未匹配（禁止提交）</span>
            <span class="legend-item"><span class="dot empty"></span>空</span>
          </span>
          <el-button link type="primary" @click="resetAll">重新上传</el-button>
        </div>
        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane
            v-for="(sheet, si) in importData"
            :key="sheet.positionId || si"
            :label="sheet.positionName"
            :name="String(si)"
          >
            <div v-for="(block, bi) in sheet.blocks" :key="bi" class="matrix-block">
              <div class="block-label">
                {{ block.dates.length ? formatRangeLabel(block.dates) : '（无日期，请添加日期）' }}
              </div>
              <div class="block-actions">
                <el-input
                  v-model="block.newSlotInput"
                  size="small"
                  placeholder="时间段，如 09:00-10:00"
                  class="slot-input"
                  @keyup.enter="addSlot(sheet, block)"
                />
                <el-button link type="primary" size="small" @click="addSlot(sheet, block)">添加时间段</el-button>
                <el-date-picker
                  v-model="block.newDate"
                  type="date"
                  size="small"
                  value-format="YYYY-MM-DD"
                  placeholder="选择日期"
                  class="date-picker"
                />
                <el-button link type="primary" size="small" @click="addDate(sheet, block)">添加日期</el-button>
              </div>
              <div class="matrix-table">
                <table>
                  <thead>
                    <tr>
                      <th class="slot-col">时间段</th>
                      <th v-for="(d, di) in block.dates" :key="di">
                        <span class="date-th">
                          {{ formatDateLabel(d) }}
                          <el-button
                            v-if="block.dates.length > 1"
                            link
                            type="primary"
                            size="small"
                            class="date-split"
                            title="拆分此日期，单独设置时间段"
                            @click="splitDate(sheet, block, di)"
                            >拆</el-button
                          >
                          <el-icon class="cell-remove" @click="removeDate(sheet, block, di)"><Close /></el-icon>
                        </span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(slot, ri) in block.slots" :key="ri">
                      <td class="slot-col">
                        <span v-if="!slot.editing" class="slot-label">
                          <span>{{ slot.slotLabel }}</span>
                          <el-icon class="cell-edit" @click="startEditSlot(slot)"><Edit /></el-icon>
                          <el-icon class="cell-remove" @click="removeSlot(block, ri)"><Close /></el-icon>
                        </span>
                        <span v-else class="slot-label">
                          <el-input
                            v-model="slot.editInput"
                            size="small"
                            class="slot-edit-input"
                            @keyup.enter="saveEditSlot(sheet, block, slot)"
                          />
                          <el-icon class="cell-edit" @click="saveEditSlot(sheet, block, slot)"><Check /></el-icon>
                          <el-icon class="cell-remove" @click="cancelEditSlot(slot)"><Close /></el-icon>
                        </span>
                      </td>
                      <td
                        v-for="(cell, ci) in slot.cells"
                        :key="ci"
                        :class="{ 'cell-notfound': cell.status === 'notFound' }"
                      >
                        <el-select
                          v-if="editingCell === cell"
                          :ref="setCellSelectRef"
                          v-model="cell.name"
                          filterable
                          allow-create
                          clearable
                          :reserve-keyword="false"
                          size="small"
                          placeholder="姓名"
                          class="cell-select"
                          autocomplete="new-password"
                          @change="(v) => onCellNameChange(cell, v)"
                          @blur="endEditCell"
                        >
                          <el-option
                            v-for="emp in sheetEmployeeOptions(sheet)"
                            :key="emp.id"
                            :label="emp.name"
                            :value="emp.name"
                          />
                        </el-select>
                        <span
                          v-else
                          class="cell-text"
                          :class="{ 'cell-text--empty': !cell.name }"
                          @click="startEditCell(cell)"
                        >
                          {{ cell.name || '点击选人' }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!importData.length" @click="submit"> 提交 </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
  /**
   * @file ImportScheduleDialog.vue
   * @description 直播间排班 Excel 导入：上传 → 解析 → 可编辑预览 → 提交落库
   */
  import { computed, nextTick, ref, watch } from 'vue'
  import dayjs from 'dayjs'
  import { ElMessage } from 'element-plus'
  import { Check, Close, Edit, UploadFilled } from '@element-plus/icons-vue'
  import * as XLSX from 'xlsx'
  import apiModule from '@/http/api'

  const props = defineProps({
    modelValue: { type: Boolean, default: false },
    // 直播间 id（route.query.id，字符串）
    liveRoomId: { type: String, default: '' },
    // 岗位列表 [{ id, name }]，用于 sheet 名 → 岗位 id 映射
    positionOptions: { type: Array, default: () => [] },
    // 休息时长选项（分钟），导入时统一取第一个，无则 0
    restOptions: { type: Array, default: () => [] }
  })

  const emit = defineEmits(['update:modelValue', 'saved'])

  const visible = computed({
    get: () => props.modelValue,
    set: (v) => emit('update:modelValue', v)
  })

  const parsing = ref(false)
  const submitting = ref(false)
  const activeTab = ref('0')
  const importData = ref([])
  const parseWarnings = ref([])
  const uploadRef = ref(null)

  // 员工列表：下拉选项 + 姓名 → { id, positionId } 映射
  const employeeOptions = ref([])
  const employeeNameMap = ref(new Map())
  const employeeLoaded = ref(false)

  // 按需渲染：同一时刻只挂载一个选人 el-select，避免 _24h 模板成百上千个 el-select 同时挂载导致栈溢出
  const editingCell = ref(null)
  let cellSelectEl = null

  // 岗位名 → 岗位 id（来自父组件已加载的岗位列表）
  const positionNameMap = computed(() => {
    const map = new Map()
    ;(props.positionOptions || []).forEach((p) => {
      const name = String(p?.name || '').trim()
      const id = p?.id !== undefined && p?.id !== null ? String(p.id) : ''
      if (name && id) map.set(name, id)
    })
    return map
  })

  // 导入班次统一采用的休息时长
  const defaultRestDuration = computed(() => {
    const list = Array.isArray(props.restOptions) ? props.restOptions : []
    return list.length ? Number(list[0]) || 0 : 0
  })

  /**
   * 拉取全量员工（specialPage 分页循环），构建姓名 → id 映射
   */
  async function fetchAllEmployees() {
    const all = []
    const limit = 200
    let page = 1
    for (let i = 0; i < 50; i++) {
      const res = await apiModule.employee.specialPage({ page, limit, openMain: true })
      const list = res?.data?.list || []
      all.push(...list)
      const total = Number(res?.data?.totalCount || res?.data?.total || 0)
      if (!list.length || all.length >= total) break
      page += 1
    }
    const opts = []
    const map = new Map()
    all.forEach((emp) => {
      const id = emp?.id !== undefined && emp?.id !== null ? String(emp.id) : ''
      const name = String(emp?.name || '').trim()
      if (!id || !name) return
      if (!map.has(name)) {
        const positionId = emp?.positionId !== undefined && emp?.positionId !== null ? String(emp.positionId) : ''
        map.set(name, { id, positionId })
        opts.push({ id, name, positionId })
      }
    })
    employeeNameMap.value = map
    employeeOptions.value = opts
    employeeLoaded.value = true
  }

  /** 员工下拉选项按岗位缓存：避免每次渲染都 filter 出新数组引用，导致 el-select 递归更新 */
  const employeeOptionsByPosition = computed(() => {
    const map = new Map()
    employeeOptions.value.forEach((e) => {
      const pid = e?.positionId !== undefined && e?.positionId !== null ? String(e.positionId) : ''
      if (!map.has(pid)) map.set(pid, [])
      map.get(pid).push(e)
    })
    return map
  })

  /** 当前岗位 sheet 的员工下拉选项：按员工所属岗位过滤，主播卡片只显示主播 */
  function sheetEmployeeOptions(sheet) {
    const pid = sheet?.positionId !== undefined && sheet?.positionId !== null ? String(sheet.positionId) : ''
    if (!pid) return employeeOptions.value
    return employeeOptionsByPosition.value.get(pid) || []
  }

  /** HH:mm → 当天分钟数 */
  function toMinutes(t) {
    const [h, m] = String(t)
      .split(':')
      .map((x) => Number(x))
    return (Number.isFinite(h) ? h : 0) * 60 + (Number.isFinite(m) ? m : 0)
  }

  /** 时:分 → 标准 "HH:mm"（时分 1-2 位，分隔符支持 : ： .；时 0-23、分 0-59），非法返回 '' */
  function normalizeHm(t) {
    const m = String(t ?? '')
      .trim()
      .match(/^(\d{1,2})[:：.](\d{1,2})$/)
    if (!m) return ''
    const h = Number(m[1])
    const mm = Number(m[2])
    if (h > 23 || mm > 59) return ''
    return String(h).padStart(2, '0') + ':' + String(mm).padStart(2, '0')
  }

  /** 日期 + 时间段 → 绝对时间区间 [start, end]（dayjs）；跨天 end<=start 时结束 +1 天 */
  function getAbsRange(date, slot) {
    const base = dayjs(date).startOf('day')
    const start = base.add(toMinutes(slot.startWork), 'minute')
    let end = base.add(toMinutes(slot.endWork), 'minute')
    if (toMinutes(slot.endWork) <= toMinutes(slot.startWork)) end = end.add(1, 'day')
    return { start, end }
  }

  /** 两个绝对时间区间是否重叠（边界相邻不算重叠） */
  function rangesOverlap(a, b) {
    return a.start.isBefore(b.end) && b.start.isBefore(a.end)
  }

  /** 收集 sheet 内所有已有 (日期, 时间段) 的绝对区间；excludeSlot 用于编辑时排除自身 */
  function collectExistingRanges(sheet, excludeSlot) {
    const list = []
    ;(sheet.blocks || []).forEach((b) => {
      ;(b.dates || []).forEach((d) => {
        ;(b.slots || []).forEach((s) => {
          if (excludeSlot && s === excludeSlot) return
          list.push(getAbsRange(d, s))
        })
      })
    })
    return list
  }

  /**
   * 候选时间段（作用于 dates 这些日期）是否与 sheet 内任一已有时间段重叠。
   * 跨 table（不同 block）也会参与比较，且按「绝对日期时间」计算，
   * 所以跨天班次（如 8/23 的 20:04-00:40）会延伸到 8/24 00:40，
   * 从而与 8/24 的 00:04-10:04 判为重叠。
   */
  function overlapsAnywhere(sheet, dates, slot, excludeSlot) {
    const existing = collectExistingRanges(sheet, excludeSlot)
    return (dates || []).some((d) => {
      const r = getAbsRange(d, slot)
      return existing.some((e) => rangesOverlap(r, e))
    })
  }

  /** 时间段按开始时间升序排序 */
  function sortSlots(slots) {
    slots.sort((a, b) => toMinutes(a.startWork) - toMinutes(b.startWork))
  }

  /** block 内日期按升序排序，并同步重排每个时间段行的 cell 对齐 */
  function sortDates(block) {
    block.dates.sort()
    block.slots.forEach((slot) => {
      slot.cells.sort((a, b) => String(a.date).localeCompare(String(b.date)))
    })
  }

  /** sheet 内 blocks 按首个日期升序排序 */
  function sortBlocks(sheet) {
    sheet.blocks.sort((a, b) => {
      const ad = (a.dates && a.dates[0]) || ''
      const bd = (b.dates && b.dates[0]) || ''
      return String(ad).localeCompare(String(bd))
    })
  }

  /** 解析时间段字符串（如 "09:00-12:00"），起止分隔符支持 - ~ ～ 至 到；跨天 endWork<=startWork 时结束 +24h */
  function parseSlot(slotStr) {
    const parts = String(slotStr ?? '')
      .trim()
      .split(/[-~～至到]+/)
      .map((x) => x.trim())
      .filter(Boolean)
    if (parts.length !== 2) return null
    const startWork = normalizeHm(parts[0])
    const endWork = normalizeHm(parts[1])
    if (!startWork || !endWork) return null
    const startMin = toMinutes(startWork)
    let endMin = toMinutes(endWork)
    if (endMin <= startMin) endMin += 24 * 60
    return { startWork, endWork, scheduleDuration: endMin - startMin }
  }

  /** 日期表头 → "YYYY-MM-DD"：支持 Date 对象、Excel 数字序列号及多种常见写法，无年份时按「离今天最近」推断 */
  function inferDate(s) {
    // Excel 日期格式单元格（cellDates: true 时）会返回 Date 对象
    if (s instanceof Date && !Number.isNaN(s.getTime())) {
      return dayjs(s).format('YYYY-MM-DD')
    }
    // 数字：Excel 日期序列号（1900 日期系统）兜底
    if (typeof s === 'number' && Number.isFinite(s)) {
      const dt = XLSX.SSF.parse_date_code(s)
      if (dt) return `${dt.y}-${pad2(dt.m)}-${pad2(dt.d)}`
    }
    const str = String(s ?? '').trim()
    if (!str) return ''

    // 1) 标准日期 YYYY-MM-DD / YYYY/M/D / YYYY.M.D（自带年份，无歧义）
    let m = str.match(/^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})$/)
    if (m) return buildDate(Number(m[1]), Number(m[2]), Number(m[3]))

    // 2) 中文带年 YYYY年M月D[日|号]
    m = str.match(/^(\d{4})年(\d{1,2})月(\d{1,2})[日号]?$/)
    if (m) return buildDate(Number(m[1]), Number(m[2]), Number(m[3]))

    // 3) 无年份：M月D[日|号] 或 M-D / M/D / M.D → 年份推断（跨年边界 +1 年）
    m = str.match(/^(\d{1,2})月(\d{1,2})[日号]?$/) || str.match(/^(\d{1,2})[-/.](\d{1,2})$/)
    if (m) {
      const month = Number(m[1])
      const day = Number(m[2])
      if (!month || !day) return ''
      const now = dayjs()
      let year = now.year()
      let date = dayjs(`${year}-${pad2(month)}-${pad2(day)}`)
      if (date.isBefore(now.subtract(30, 'day'))) {
        year += 1
        date = dayjs(`${year}-${pad2(month)}-${pad2(day)}`)
      }
      return date.format('YYYY-MM-DD')
    }

    return ''
  }

  /** 年/月/日 → 校验（含防 dayjs 溢出）并格式化为 "YYYY-MM-DD"，非法返回 '' */
  function buildDate(y, mo, d) {
    if (!y || mo < 1 || mo > 12 || d < 1 || d > 31) return ''
    const date = dayjs(`${y}-${pad2(mo)}-${pad2(d)}`)
    return date.isValid() && date.month() + 1 === mo && date.date() === d ? date.format('YYYY-MM-DD') : ''
  }

  function pad2(n) {
    return String(n).padStart(2, '0')
  }

  /** "YYYY-MM-DD" → "YYYY年M月D日"（预览展示，带年份，跨年清晰） */
  function formatDateLabel(dateStr) {
    const d = dayjs(dateStr)
    return d.isValid() ? `${d.year()}年${d.month() + 1}月${d.date()}日` : dateStr
  }

  /** 日期范围 → 范围标签：同年省略尾部年份，跨年两端都带年份 */
  function formatRangeLabel(dates) {
    const arr = Array.isArray(dates) ? dates : []
    if (!arr.length) return ''
    const first = dayjs(arr[0])
    const last = dayjs(arr[arr.length - 1])
    const firstLabel = formatDateLabel(arr[0])
    if (first.isSame(last, 'day')) return firstLabel
    const lastLabel =
      first.year() === last.year() ? `${last.month() + 1}月${last.date()}日` : formatDateLabel(arr[arr.length - 1])
    return `${firstLabel} ~ ${lastLabel}`
  }

  /** 根据姓名匹配员工，更新 cell 的 employeeId 与状态；员工须属于当前 sheet 岗位，否则视为错误 */
  function applyEmployeeMatch(cell) {
    const name = String(cell?.name || '').trim()
    if (!name) {
      cell.employeeId = ''
      cell.status = 'empty'
      return
    }
    const hit = employeeNameMap.value.get(name)
    const pid = cell?.positionId !== undefined && cell?.positionId !== null ? String(cell.positionId) : ''
    // 员工存在、且（当前岗位未知 或 员工岗位匹配当前岗位）才判 ok；跨岗位人员视为 notFound
    if (hit && (!pid || hit.positionId === pid)) {
      cell.employeeId = hit.id
      cell.status = 'ok'
    } else {
      cell.employeeId = ''
      cell.status = 'notFound'
    }
  }

  function onCellNameChange(cell, name) {
    cell.name = String(name || '').trim()
    applyEmployeeMatch(cell)
  }

  /** 设置当前选人 el-select 的组件实例（函数 ref，避免 v-for 内 ref 变数组） */
  function setCellSelectRef(el) {
    cellSelectEl = el
  }

  /** 点击姓名格进入编辑态：只挂载这一个 el-select，渲染后自动聚焦 */
  function startEditCell(cell) {
    editingCell.value = cell
    nextTick(() => {
      cellSelectEl?.focus?.()
    })
  }

  /** 失焦退出编辑态，el-select 卸载，恢复为文本显示 */
  function endEditCell() {
    editingCell.value = null
  }

  /** 删除某个周块里的一个时间段行 */
  function removeSlot(block, ri) {
    block.slots.splice(ri, 1)
  }

  /** 添加时间段：输入 HH:mm-HH:mm，向 block 末尾追加一行，cells 按当前日期列表初始化为空 */
  function addSlot(sheet, block) {
    const raw = String(block.newSlotInput || '').trim()
    const slot = parseSlot(raw)
    if (!slot) {
      ElMessage.warning('时间段格式应为「时:分-时:分」，如 09:00-10:00，起止分隔支持 - ~ 至')
      return
    }
    if (overlapsAnywhere(sheet, block.dates, slot)) {
      ElMessage.warning('该时间段与已有时间段重叠')
      return
    }
    // F6：新增时间段的绝对开始时间必须晚于当前时间 1 小时（以 block 内最早日期计）
    const earliest = (block.dates || []).slice().sort()[0]
    if (earliest) {
      const start = dayjs(earliest).startOf('day').add(toMinutes(slot.startWork), 'minute')
      if (!start.isAfter(dayjs().add(1, 'hour'))) {
        ElMessage.warning('时间段不能早于当前时间 1 小时')
        return
      }
    }
    const cells = (block.dates || []).map((date) => ({
      date,
      name: '',
      employeeId: '',
      status: 'empty',
      positionId: sheet.positionId
    }))
    block.slots.push({ ...slot, slotLabel: raw, cells })
    block.newSlotInput = ''
    sortSlots(block.slots)
  }

  /** 进入时间段编辑态 */
  function startEditSlot(slot) {
    slot.editing = true
    slot.editInput = slot.slotLabel
  }

  /** 取消时间段编辑 */
  function cancelEditSlot(slot) {
    slot.editing = false
  }

  /** 保存时间段修改：校验格式 + 去重（排除自身） */
  function saveEditSlot(sheet, block, slot) {
    const raw = String(slot.editInput || '').trim()
    const parsed = parseSlot(raw)
    if (!parsed) {
      ElMessage.warning('时间段格式应为「时:分-时:分」，如 09:00-10:00，起止分隔支持 - ~ 至')
      return
    }
    if (overlapsAnywhere(sheet, block.dates, parsed, slot)) {
      ElMessage.warning('该时间段与已有时间段重叠')
      return
    }
    slot.startWork = parsed.startWork
    slot.endWork = parsed.endWork
    slot.scheduleDuration = parsed.scheduleDuration
    slot.slotLabel = raw
    slot.editing = false
    sortSlots(block.slots)
  }

  /** 删除某个周块里的一列日期，并同步删除每个时间段行对应的 cell；删光日期则删除整个 block */
  function removeDate(sheet, block, di) {
    block.dates.splice(di, 1)
    block.slots.forEach((slot) => slot.cells.splice(di, 1))
    if (!block.dates.length) {
      const bi = sheet.blocks.indexOf(block)
      if (bi >= 0) sheet.blocks.splice(bi, 1)
    }
  }

  /** 把周块里的某一天拆成独立 block，单独设置时间段 */
  function splitDate(sheet, block, di) {
    if (block.dates.length <= 1) return
    const date = block.dates[di]
    // 从原 block 每个时间段里取出该日期的 cell，组成新的单日 block
    const newSlots = (block.slots || []).map((slot) => {
      const cell = slot.cells[di]
      return {
        startWork: slot.startWork,
        endWork: slot.endWork,
        slotLabel: slot.slotLabel,
        scheduleDuration: slot.scheduleDuration,
        cells: [cell ? { ...cell } : { date, name: '', employeeId: '', status: 'empty', positionId: sheet.positionId }]
      }
    })
    const newBlock = { dates: [date], slots: newSlots, newSlotInput: '', newDate: '' }
    // 从原 block 移除该日期及其对应 cell
    block.dates.splice(di, 1)
    block.slots.forEach((slot) => slot.cells.splice(di, 1))
    // 新块插入到原 block 之后，再按日期重排
    const bi = sheet.blocks.indexOf(block)
    if (bi >= 0) {
      sheet.blocks.splice(bi + 1, 0, newBlock)
    } else {
      sheet.blocks.push(newBlock)
    }
    sortBlocks(sheet)
  }

  /** 添加日期：向 block 末尾追加一列，并为每个时间段行追加一个空 cell */
  function addDate(sheet, block) {
    const raw = String(block.newDate || '').trim()
    if (!raw) return
    // F6：不能添加以前的日期（今天当天允许）
    if (dayjs(raw).startOf('day').isBefore(dayjs().startOf('day'))) {
      ElMessage.warning('不能添加以前的日期')
      return
    }
    // 同一岗位内日期必须唯一，跨 block（多个表格）也要去重
    const exists = (sheet.blocks || []).some((b) => (b.dates || []).includes(raw))
    if (exists) {
      ElMessage.warning('该日期已存在于排班表中')
      return
    }
    block.dates.push(raw)
    block.slots.forEach((slot) => {
      slot.cells.push({ date: raw, name: '', employeeId: '', status: 'empty', positionId: sheet.positionId })
    })
    block.newDate = ''
    sortDates(block)
    sortBlocks(sheet)
  }

  /**
   * 解析单个岗位 sheet 的矩阵：周块布局（表头行 + 时间段行），
   * 每个周块（最多 7 天）拆成一个 block，便于多表格分块展示与横向滚动。
   * positionName 仅用于生成带岗位前缀的告警文案。
   */
  function parseMatrix(rows, positionName, positionId) {
    const blocks = []
    const warnings = []
    const posLabel = positionName ? `岗位「${positionName}」` : ''
    let currentBlock = null
    const seenDates = new Set() // sheet 级去重：跨所有 table（周块）的日期也不能重复
    let warnedPastDate = false // 早于今天的日期告警按岗位只提示一次
    const warnedDupDates = new Set() // 重复日期告警按「岗位+日期」只提示一次

    for (const row of rows) {
      if (!Array.isArray(row) || !row.length) continue
      const first = String(row[0] ?? '').trim()
      // 表头行：第 0 格「时间」，其余为当前周块的日期；遇到即开启一个新 block
      if (first === '时间') {
        const dates = [] // 唯一日期，按首次出现列顺序
        const colDate = new Map() // 列索引 c -> 日期（去重 + 过滤后的映射）
        for (let c = 1; c <= 7; c++) {
          const rawStr = String(row[c] ?? '').trim()
          if (!rawStr) continue
          const d = inferDate(row[c])
          if (!d) {
            warnings.push(`${posLabel}第 ${c} 列日期「${rawStr}」无法解析，已忽略该列`)
            continue
          }
          if (dayjs(d).startOf('day').isBefore(dayjs().startOf('day'))) {
            if (!warnedPastDate) {
              warnings.push(`${posLabel}存在早于今天的日期，已忽略相关列`)
              warnedPastDate = true
            }
            continue
          }
          if (seenDates.has(d)) {
            if (!warnedDupDates.has(d)) {
              warnings.push(`${posLabel}日期「${d}」重复，已忽略重复列`)
              warnedDupDates.add(d)
            }
            continue
          }
          seenDates.add(d)
          dates.push(d)
          colDate.set(c, d)
        }
        if (dates.length) {
          currentBlock = { dates, colDate, slotMap: new Map() }
          blocks.push(currentBlock)
        } else {
          currentBlock = null
        }
        continue
      }
      // 数据行：第 0 格「时:分-时:分」，其余为姓名（时分 1-2 位、: ： . 分隔；起止 - ~ ～ 至 到）
      if (!/^\d{1,2}[:：.]\d{1,2}[-~～至到]\d{1,2}[:：.]\d{1,2}$/.test(first)) continue
      if (!currentBlock) continue
      const slot = parseSlot(first)
      if (!slot) {
        warnings.push(`${posLabel}时间段「${first}」无法解析，已忽略该行`)
        continue
      }
      const key = `${slot.startWork}-${slot.endWork}`
      if (!currentBlock.slotMap.has(key)) {
        currentBlock.slotMap.set(key, { ...slot, slotLabel: first, cells: [] })
      }
      const target = currentBlock.slotMap.get(key)
      for (let c = 1; c <= 7; c++) {
        const date = currentBlock.colDate.get(c)
        if (!date) continue
        const name = String(row[c] ?? '').trim()
        // 同 block 内时间段重复：按日期合并，已有非空姓名则保留，空位用新值补齐
        const existing = target.cells.find((cell) => cell.date === date)
        if (existing) {
          if (!existing.name && name) {
            existing.name = name
            existing.status = 'pending'
          }
        } else {
          target.cells.push({ date, name, employeeId: '', status: name ? 'pending' : 'empty', positionId })
        }
      }
    }

    return {
      blocks: blocks.map((block) => {
        const slots = Array.from(block.slotMap.values()).map((slot) => {
          slot.cells.forEach(applyEmployeeMatch)
          return slot
        })
        sortSlots(slots)
        return { dates: block.dates, slots, newSlotInput: '', newDate: '' }
      }),
      warnings
    }
  }

  /** 单个岗位排班上限：按「日期×时间段」统计，超出部分丢弃 */
  const MAX_SCHEDULES_PER_POSITION = 2000

  /** 截断单个岗位的 blocks，使「日期×时间段」总数不超过上限；返回截断后的 blocks 与被丢弃条数 */
  function truncateSheetBlocks(blocks) {
    let count = 0
    let dropped = 0
    const keptBlocks = []
    for (const block of blocks) {
      const dateCount = (block.dates || []).length
      const keptSlots = []
      for (const slot of block.slots) {
        if (count + dateCount > MAX_SCHEDULES_PER_POSITION) {
          dropped += dateCount
          continue
        }
        count += dateCount
        keptSlots.push(slot)
      }
      if (keptSlots.length) {
        block.slots = keptSlots
        keptBlocks.push(block)
      }
    }
    return { blocks: keptBlocks, dropped }
  }

  /** 遍历 workbook sheet，跳过「填写说明」与「员工列表*」，解析岗位排班 sheet */
  function buildImportData(wb) {
    const sheets = []
    const warnings = []
    wb.SheetNames.forEach((sheetName) => {
      if (sheetName === '填写说明') return
      if (String(sheetName).startsWith('员工列表')) return

      const positionName = String(sheetName).endsWith('排班表') ? String(sheetName).slice(0, -3) : String(sheetName)
      const positionId = positionNameMap.value.get(positionName) || ''
      if (!positionId) {
        warnings.push(`无法识别岗位「${positionName}」，已跳过该 sheet`)
        return
      }

      const ws = wb.Sheets[sheetName]
      const rows = XLSX.utils.sheet_to_json(ws, { header: 1, defval: '', raw: true, cellDates: true })
      const parsed = parseMatrix(rows, positionName, positionId)
      if (parsed && parsed.warnings.length) warnings.push(...parsed.warnings)
      if (!parsed || !parsed.blocks.length) {
        warnings.push(`岗位「${positionName}」未解析到有效数据，已跳过`)
        return
      }
      const truncated = truncateSheetBlocks(parsed.blocks)
      if (truncated.dropped > 0) {
        warnings.push(
          `岗位「${positionName}」排班超过 ${MAX_SCHEDULES_PER_POSITION} 条，超出部分 ${truncated.dropped} 条已丢弃`
        )
      }
      sheets.push({ positionId, positionName, sheetName, blocks: truncated.blocks })
    })
    importData.value = sheets
    parseWarnings.value = warnings
    activeTab.value = sheets.length ? '0' : ''
  }

  async function handleFileChange(uploadFile) {
    const file = uploadFile?.raw
    if (!file) return
    parsing.value = true
    try {
      if (!employeeLoaded.value) {
        await fetchAllEmployees()
      }
      const buffer = await file.arrayBuffer()
      const wb = XLSX.read(buffer, { type: 'array' })
      buildImportData(wb)
      if (!importData.value.length) {
        ElMessage.warning('未解析到有效的排班数据，请确认文件为正确的模板')
      }
    } catch (e) {
      importData.value = []
      parseWarnings.value = []
      ElMessage.error('解析 Excel 失败，请确认文件为正确的模板')
    } finally {
      parsing.value = false
    }
  }

  function handleFileRemove() {
    importData.value = []
    parseWarnings.value = []
    activeTab.value = ''
    editingCell.value = null
  }

  function handleExceed() {
    ElMessage.warning('一次只能上传一个文件，请先移除旧文件')
  }

  function resetAll() {
    handleFileRemove()
    uploadRef.value?.clearFiles()
  }

  // 打开弹窗时清空上次的解析结果与上传文件，避免残留
  watch(visible, (v) => {
    if (v) resetAll()
  })

  /** 收集未匹配到系统员工的姓名格（提交前校验用） */
  function collectNotFound() {
    const list = []
    importData.value.forEach((sheet) => {
      sheet.blocks.forEach((block) => {
        block.slots.forEach((slot) => {
          slot.cells.forEach((cell) => {
            if (cell.status === 'notFound') {
              list.push({
                positionName: sheet.positionName,
                slotLabel: slot.slotLabel,
                date: cell.date,
                name: cell.name
              })
            }
          })
        })
      })
    })
    return list
  }

  /** 收集 sheet 内时间重叠的时间段（提交前校验用，跨 block 也参与比较，按具体日期报告） */
  function collectOverlaps() {
    const list = []
    importData.value.forEach((sheet) => {
      // 展平 sheet 内所有 (日期, 时间段) 条目
      const entries = []
      sheet.blocks.forEach((block) => {
        block.dates.forEach((date) => {
          block.slots.forEach((slot) => {
            entries.push({ date, slot })
          })
        })
      })
      // 两两比较，每个重叠的日期/时间段条目只报告一次（报具体日期，不报整段日期范围）
      for (let i = 0; i < entries.length; i++) {
        const a = entries[i]
        for (let j = i + 1; j < entries.length; j++) {
          const b = entries[j]
          if (rangesOverlap(getAbsRange(a.date, a.slot), getAbsRange(b.date, b.slot))) {
            list.push({
              positionName: sheet.positionName,
              dateLabel: formatDateLabel(a.date),
              slotLabel: a.slot.slotLabel
            })
            break
          }
        }
      }
    })
    return list
  }

  /** 提交：先校验姓名不能为空（未匹配到员工的姓名禁止提交），再按 workDay+时间段 聚合员工落库 */
  async function submit() {
    // 提交前校验：姓名不能为空——未匹配到系统员工的姓名格必须修正或清空后才能提交
    const notFoundList = collectNotFound()
    if (notFoundList.length > 0) {
      const samples = notFoundList
        .slice(0, 5)
        .map((x) => `「${x.positionName} ${formatDateLabel(x.date)} ${x.slotLabel}：${x.name}」`)
        .join('、')
      const more = notFoundList.length > 5 ? ` 等 ${notFoundList.length} 处` : ''
      ElMessage.error(
        `提交前校验失败：有 ${notFoundList.length} 个姓名未匹配到系统员工（${samples}${more}），请修正或清空后再提交`
      )
      return
    }

    // 提交前校验：时间段不能重叠（同岗位内跨 block 也按绝对时间比较）
    const overlapList = collectOverlaps()
    if (overlapList.length > 0) {
      const samples = overlapList
        .slice(0, 5)
        .map((x) => `「${x.positionName} ${x.dateLabel} ${x.slotLabel}」`)
        .join('、')
      const more = overlapList.length > 5 ? ` 等 ${overlapList.length} 处` : ''
      ElMessage.error(`提交前校验失败：有 ${overlapList.length} 个时间段重叠（${samples}${more}），请调整后再提交`)
      return
    }

    const schedules = []
    const scheduleByKey = new Map()
    importData.value.forEach((sheet) => {
      sheet.blocks.forEach((block) => {
        block.slots.forEach((slot) => {
          slot.cells.forEach((cell) => {
            if (cell.status !== 'ok' || !cell.employeeId) return
            // 聚合 key 加入岗位维度：主播/副播同一时间段拆成两条独立排班，避免删除时互相影响
            const key = `${cell.date}|${slot.startWork}|${slot.endWork}|${sheet.positionId}`
            let s = scheduleByKey.get(key)
            if (!s) {
              s = {
                workDay: cell.date,
                startWork: slot.startWork,
                endWork: slot.endWork,
                scheduleDuration: slot.scheduleDuration,
                restDuration: defaultRestDuration.value,
                employees: []
              }
              scheduleByKey.set(key, s)
              schedules.push(s)
            }
            const exists = s.employees.some(
              (e) => e.employeeId === cell.employeeId && e.positionId === sheet.positionId
            )
            if (!exists) {
              s.employees.push({ employeeId: cell.employeeId, positionId: sheet.positionId })
            }
          })
        })
      })
    })

    // F5：某天有日期、但所有岗位所有时间段均未选人 → 收集进 emptyWorkDays，提交后删除该天已有排班
    const allDates = new Set()
    importData.value.forEach((sheet) => {
      sheet.blocks.forEach((block) => (block.dates || []).forEach((d) => allDates.add(d)))
    })
    const hasPersonDate = new Set()
    importData.value.forEach((sheet) => {
      sheet.blocks.forEach((block) => {
        block.slots.forEach((slot) => {
          slot.cells.forEach((cell) => {
            if (cell.status === 'ok' && cell.employeeId) hasPersonDate.add(cell.date)
          })
        })
      })
    })
    const emptyWorkDays = Array.from(allDates).filter((d) => !hasPersonDate.has(d))

    if (!schedules.length && !emptyWorkDays.length) {
      ElMessage.warning('没有可提交的排班数据')
      return
    }

    // 校验：排班开始时间必须晚于当前时间 + 1 小时（只能排 1 小时后的班）
    const minTime = dayjs().add(1, 'hour')
    const invalidList = schedules
      .map((s) => ({
        schedule: s,
        time: dayjs(s.workDay).startOf('day').add(toMinutes(s.startWork), 'minute')
      }))
      .filter((x) => x.time.isValid() && !x.time.isAfter(minTime))
    if (invalidList.length) {
      const samples = invalidList
        .slice(0, 5)
        .map((x) => `${x.schedule.workDay} ${x.schedule.startWork}`)
        .join('、')
      const more = invalidList.length > 5 ? ` 等 ${invalidList.length} 处` : ''
      ElMessage.error(`排班开始时间需晚于当前时间 1 小时，以下班次不满足：${samples}${more}，请调整后再提交`)
      return
    }

    submitting.value = true
    try {
      await apiModule.roomSchedule.importSchedule({ liveRoomId: props.liveRoomId, schedules, emptyWorkDays })
      ElMessage.success('导入成功')
      visible.value = false
      emit('saved')
    } finally {
      submitting.value = false
    }
  }
</script>

<style scoped lang="scss">
  .import-dialog {
    .upload-area {
      :deep(.el-upload) {
        width: 100%;
      }

      :deep(.el-upload-dragger) {
        width: 560px;
        max-width: 100%;
        margin: 0 auto;
        height: auto;
        padding: 20px 16px;
        border-radius: 8px;
        transition: border-color 0.2s;
      }

      :deep(.el-icon--upload) {
        font-size: 40px;
        color: #909399;
        margin-bottom: 6px;
      }

      :deep(.el-upload__text) {
        font-size: 14px;
        color: #606266;
      }

      :deep(.el-upload__text em) {
        color: #409eff;
        font-style: normal;
      }

      :deep(.el-upload__tip) {
        margin-top: 8px;
        font-size: 12px;
        color: #909399;
        text-align: center;
      }

      .parsing-tip {
        height: 60px;
      }
    }

    .warning-block {
      margin: 12px 0;
    }

    .preview-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .legend {
        display: flex;
        gap: 16px;
        align-items: center;
        font-size: 12px;
        color: #666;

        .legend-item {
          display: inline-flex;
          align-items: center;
        }

        .dot {
          display: inline-block;
          width: 10px;
          height: 10px;
          border-radius: 50%;
          margin-right: 4px;

          &.ok {
            background: #67c23a;
          }

          &.notfound {
            background: #f56c6c;
          }

          &.empty {
            background: #dcdfe6;
          }
        }
      }
    }

    .matrix-block {
      margin-bottom: 20px;
      padding: 12px;
      border: 1px solid #dcdfe6;
      border-radius: 6px;

      .block-label {
        font-size: 13px;
        font-weight: 600;
        color: #303133;
        margin-bottom: 8px;
      }

      .block-actions {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;

        .slot-input {
          width: 160px;
        }

        .date-picker {
          width: 140px;
          margin-left: 8px;
        }
      }

      .date-th {
        display: inline-flex;
        align-items: center;
        gap: 2px;
      }

      .date-split {
        padding: 0;
        font-size: 12px;
        margin-left: 2px;
      }

      .cell-remove {
        margin-left: 4px;
        cursor: pointer;
        color: #c0c4cc;
        font-size: 14px;
        vertical-align: middle;

        &:hover {
          color: #f56c6c;
        }
      }

      .cell-edit {
        margin-left: 4px;
        cursor: pointer;
        color: #c0c4cc;
        font-size: 14px;
        vertical-align: middle;

        &:hover {
          color: #409eff;
        }
      }

      .slot-label {
        display: inline-flex;
        align-items: center;
        gap: 4px;
      }

      .slot-edit-input {
        width: 120px;
      }
    }

    .matrix-table {
      overflow-x: auto;

      table {
        border-collapse: collapse;

        th,
        td {
          border: 1px solid #ebeef5;
          padding: 4px;
          text-align: center;
          font-size: 12px;
          white-space: nowrap;
          max-width: 160px;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        th {
          background: #f5f7fa;
          font-weight: 600;
        }

        .slot-col {
          background: #fafafa;
          font-weight: 500;
        }

        .cell-notfound {
          background: #fef0f0;
        }

        .cell-select {
          width: 110px;
        }

        .cell-text {
          display: inline-block;
          min-width: 80px;
          padding: 0 8px;
          line-height: 22px;
          cursor: pointer;
          border-radius: 4px;

          &:hover {
            background: #f0f7ff;
          }

          &--empty {
            color: #c0c4cc;
          }
        }
      }
    }
  }
</style>

<style lang="scss">
  // 导入弹窗固定高度 90vh：内容过多时在 body 内滚动，footer 按钮始终可见。
  // 注意：el-dialog 根节点 .el-dialog 由 Element Plus 子组件渲染，class 通过 $attrs fallthrough
  // 落到其上但不带本组件的 data-v scopeId，scoped :deep 后代选择器匹配不到，故用非 scoped 样式块。
  .schedule-import-dialog {
    display: flex;
    flex-direction: column;
    height: 90vh;
    margin-bottom: 0;

    .el-dialog__header {
      flex-shrink: 0;
    }

    .el-dialog__body {
      flex: 1;
      min-height: 0;
      overflow-y: auto;
    }

    .el-dialog__footer {
      flex-shrink: 0;
    }
  }
</style>
