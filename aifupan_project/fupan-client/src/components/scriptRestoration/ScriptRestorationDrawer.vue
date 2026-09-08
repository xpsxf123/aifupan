<template>
    <!-- 话术还原度监控抽屉：配置录入 -> 标准稿预览/编辑 ->（按场景）确认标准稿/生成报告 -->
    <el-drawer
        :destroy-on-close="true"
        :visible.sync="drawerStatus"
        :size="782"
        :wrapperClosable="false"
        :append-to-body="true"
        :close-on-press-escape="false"
        :with-header="false"
        @close="closeDrawer">
        <!-- 抽屉容器 -->
        <div class="restoration-drawer">
            <!-- 顶部栏：标题 + 关闭 -->
            <div class="drawer-header">
                <i class="el-icon-close close-icon" @click="closeDrawer"></i>
                <span>话术还原度监控</span>
            </div>

            <!-- 第一步：录入配置（话术模式/循环时长/语速/参考脚本） -->
            <ScriptRestorationForm
                v-if="drawerStep === 'form'"
                v-model="scriptModel"
                @open-example="openExample"
                @submit="confirmStandardDraft"
            />
            <!-- 第二步：标准稿预览与本地编辑（确认/生成报告由当前场景决定） -->
            <ScriptRestorationDraft
                v-else
                :scene="normalizedScene"
                :form-model="scriptModel"
                :rows="draftRows"
                @reinput="handleReinput"
                @generate-report="handleGenerateReport"
                @confirm-standard-script="handleConfirmStandardScript"
                @update-rows="handleLocalSaveRows"
            />
        </div>

        <ScriptRestorationExampleDialog :visible.sync="exampleVisible" />
    </el-drawer>
</template>

<script>
import ScriptRestorationForm from './ScriptRestorationForm.vue'
import ScriptRestorationDraft from './ScriptRestorationDraft.vue'
import ScriptRestorationExampleDialog from './ScriptRestorationExampleDialog.vue'

/**
 * @description 话术还原度监控抽屉组件（两步式流程）
 * - 第一步：填写话术模式/语速/参考脚本等配置
 * - 第二步：生成标准稿内容（草稿不落库）-> 前端预览/修改 -> confirmStandardScript 落库拿 standardScriptId
 *
 * @description scene 用于区分弹窗使用场景：
 * - anchorConfig：直播间配置场景（新增/编辑），标准稿编辑/保存均为纯前端；点击“确认标准直播稿”只把标准稿内容写入 form，供后续 addOrUpdateAnchor 提交时统一确认落库
 * - analysis：已明确直播间/录制资源场景，可“生成还原度报告”；若本次有修改，则先 confirmStandardScript 落库再触发生成
 *
 * @description 兼容历史传值：
 * - preConfig -> anchorConfig
 * - report -> analysis
 */
export default {
    name: 'ScriptRestorationDrawer',
    components: {
        ScriptRestorationForm,
        ScriptRestorationDraft,
        ScriptRestorationExampleDialog
    },
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        /**
         * @description 弹窗使用场景
         * @type {'preConfig'|'report'}
         */
        scene: {
            type: String,
            default: 'report'
        },
        textData: {
            type: Object,
            default: () => ({})
        },
        form: {
            type: Object,
            default: null
        }
    },
    data() {
        return {
            // 当前步骤：form=配置录入；draft=标准稿预览/编辑
            drawerStep: 'form',
            scriptModel: this.getDefaultScriptModel(),
            draftRows: [],
            standardScriptId: null,
            loadingStandardScript: false,
            loadingExistingScript: false,
            // 是否存在后端已确认标准稿（从 getStandardScript 回显得到）
            hasBackendScript: false,
            // 本次是否产生了本地修改（生成/编辑/重新生成等都会置为 true）
            draftDirty: false,
            // 内部初始化/重置时禁止把默认值反写回外部 form，避免关闭抽屉后把已保存配置清空
            suppressFormSync: false,
            // 避免 form 深度监听导致同一主播稿件详情反复请求
            lastLoadedScriptKey: '',
            // 用户点击“重新录入参考直播脚本”后，暂停自动回切到第二步
            skipAutoLoadExistingScript: false,
            syncingToForm: false,
            exampleVisible: false
        }
    },
    computed: {
        drawerStatus: {
            get() {
                return this.visible
            },
            set(status) {
                this.changeDrawerStatus(status)
            }
        },
        /**
         * @description 归一化后的场景类型（兼容历史传值）
         * @returns {'anchorConfig'|'analysis'}
         */
        normalizedScene() {
            const v = String(this.scene || '')
            if (v === 'preConfig') return 'anchorConfig'
            if (v === 'report') return 'analysis'
            return (v === 'anchorConfig' || v === 'analysis') ? v : 'analysis'
        }
    },
    watch: {
        visible: {
            immediate: true,
            handler(val) {
                if (val) {
                    this.loadFromFormIfNeeded()
                    this.loadLocalDraftRowsFromFormIfNeeded()
                    this.loadExistingStandardScriptIfNeeded()
                }
            }
        },
        form: {
            deep: true,
            handler() {
                if (!this.visible) return
                if (!this.syncingToForm) this.loadFromFormIfNeeded()
                this.loadLocalDraftRowsFromFormIfNeeded()
                const nextKey = this.getExistingScriptLoadKey()
                if (nextKey && nextKey !== this.lastLoadedScriptKey) {
                    this.loadExistingStandardScriptIfNeeded()
                }
            }
        },
        scriptModel: {
            deep: true,
            handler() {
                if (this.suppressFormSync) return
                this.syncToFormIfNeeded()
            }
        }
    },
    methods: {
        openExample() {
            this.exampleVisible = true
        },
        normalizeScriptText(text) {
            if (text === null || text === undefined) return ''
            const raw = String(text)
            return raw
                .replace(/\\r\\n/g, '\n')
                .replace(/\\n/g, '\n')
                .replace(/\r\n/g, '\n')
        },
        /**
         * @description 默认表单模型（用于 ScriptRestorationForm v-model）
         * @returns {{scriptType: string, loopDuration: string, wordsPerMinute: string, referenceScript: string}}
         */
        getDefaultScriptModel() {
            return {
                scriptType: 1,
                loopDuration: '',
                wordsPerMinute: '',
                referenceScript: ''
            }
        },
        /**
         * @description 将接口返回的 timeAxisScript 映射为表格行（时间段/标题/话术）
         * @param {Array<Object>} list 接口 timeAxisScript
         * @returns {Array<{timeRange: string, title: string, content: string}>}
         */
        mapTimeAxisToRows(list = []) {
            return (Array.isArray(list) ? list : [])
                .filter(item => item && (item.timeRange || item.content))
                .map((item) => ({
                    timeRange: item.timeRange || '',
                    title: this.normalizeScriptText(item.title || ''),
                    content: this.normalizeScriptText(item.content || '')
                }))
        },
        /**
         * @description 从 form 中解析 anchorUrlUserId（直播间配置记录 ID）
         * - preConfig（新增直播间）阶段一般没有 anchorUrlUserId，按接口文档不传
         * @returns {number|null}
         */
        getFormAnchorUrlUserId() {
            const v = this.form || {}
            const primary = [
                v.anchorUrlUserId,
                v.anchor_url_user_id,
                v.basicSettingsVo?.anchorUrlUserId,
                v.basicSettingsVo?.anchor_url_user_id,
                v.anchorInfo?.anchorUrlUserId,
                v.anchorInfo?.anchor_url_user_id
            ]
            for (const item of primary) {
                const n = Number(item)
                if (Number.isFinite(n) && n > 0) return n
            }
            return null
        },
        getFormSecUid() {
            const v = this.form || {}
            const candidates = [
                v.secUid,
                v.SecUid,
                v.anchorInfo?.secUid,
                v.anchorInfo?.SecUid,
                v.basicSettingsVo?.secUid,
                v.basicSettingsVo?.SecUid
            ]
            const td = this.textData || {}
            candidates.push(
                td.secUid,
                td.SecUid,
                td.anchorInfo?.secUid,
                td.anchorInfo?.SecUid,
                td.videoInfo?.secUid,
                td.videoInfo?.SecUid,
                this.$route?.query?.secUid
            )
            for (const item of candidates) {
                if (item !== undefined && item !== null && String(item).trim()) return String(item).trim()
            }
            return ''
        },
        /**
         * @description 将 form 中“还原度配置字段”映射到内部表单模型（用于回显）
         * @param {Object} form 表单对象
         * @returns {{scriptType: string|number, loopDuration: string, wordsPerMinute: string, referenceScript: string}}
         */
        mapFormToScriptModel(form = {}) {
            const mode = Number(form?.scriptRestoreMode || 0)
            const talkSpeed = form?.scriptRestoreTalkSpeed ?? ''
            const loopDuration = form?.scriptRestoreLoopDuration ?? ''
            const referenceScript = this.normalizeScriptText(form?.scriptRestoreReferenceScript ?? '')
            return {
                scriptType: (mode === 1 || mode === 2) ? 1 : 1,
                loopDuration: loopDuration === 0 ? '' : String(loopDuration ?? ''),
                wordsPerMinute: talkSpeed === 0 ? '' : String(talkSpeed ?? ''),
                referenceScript: String(referenceScript ?? '')
            }
        },
        /**
         * @description 将接口返回的标准稿配置映射为内部表单模型（speechMode/speechSpeed/循环时长）
         * @param {Object} data 接口返回 data
         * @returns {{scriptType: string|number, loopDuration: string, wordsPerMinute: string, referenceScript: string}}
         */
        mapApiScriptToScriptModel(data = {}) {
            const speechMode = Number(data?.speechMode)
            const mappedType = speechMode === 1 ? 1 : speechMode === 0 ? 1 : 1
            return {
                scriptType: mappedType,
                loopDuration: data?.cycleDurationMinutes === 0 ? '' : String(data?.cycleDurationMinutes ?? ''),
                wordsPerMinute: data?.speechSpeed === 0 ? '' : String(data?.speechSpeed ?? ''),
                referenceScript: this.normalizeScriptText(data?.referenceScript ?? '')
            }
        },
        getExistingScriptLoadKey() {
            const anchorUrlUserId = this.getFormAnchorUrlUserId()
            const secUid = this.getFormSecUid()
            return `${anchorUrlUserId || ''}__${secUid || ''}`
        },
        /**
         * @description 编辑直播间/手动分析时：根据 anchorUrlUserId 拉取已确认标准稿进行回显
         * @returns {Promise<void>}
         */
        async loadExistingStandardScriptIfNeeded() {
            if (!this.visible) return
            if (!this.form) return
            if (!this.$httpBack?.scriptMonitor?.getStandardScript) return
            if (this.skipAutoLoadExistingScript) return
            if (this.loadingExistingScript) return

            const anchorUrlUserId = this.getFormAnchorUrlUserId()
            const secUid = this.getFormSecUid()
            if (!anchorUrlUserId && !secUid) return
            const loadKey = this.getExistingScriptLoadKey()
            if (loadKey && loadKey === this.lastLoadedScriptKey) return

            this.loadingExistingScript = true
            try {
                const res = await this.$httpBack.scriptMonitor.getStandardScript({ anchorUrlUserId, secUid })
                this.lastLoadedScriptKey = loadKey
                if (res?.code !== 0) return
                const data = res?.data || {}
                if (!data?.hasScript) return

                this.standardScriptId = data?.standardScriptId || this.standardScriptId
                this.$set(this.form, 'standardScriptId', this.standardScriptId)

                this.scriptModel = {
                    ...this.getDefaultScriptModel(),
                    ...this.mapApiScriptToScriptModel(data)
                }
                this.syncToFormIfNeeded()
                this.draftRows = this.mapTimeAxisToRows(data?.timeAxisScript || [])
                this.hasBackendScript = true
                this.draftDirty = false
                this.syncDraftRowsToFormIfNeeded(this.draftRows)
                // 已存在标准稿时，直接进入标准稿预览页（满足“已绑定直播间->已生成脚本->直接显示脚本内容”）
                if (this.draftRows?.length) this.drawerStep = 'draft'
            } catch (e) {
            } finally {
                this.loadingExistingScript = false
            }
        },
        loadFromFormIfNeeded() {
            if (!this.form) return
            this.suppressFormSync = true
            this.scriptModel = {
                ...this.getDefaultScriptModel(),
                ...this.mapFormToScriptModel(this.form)
            }
            this.standardScriptId = this.form?.standardScriptId ?? null
            this.$nextTick(() => {
                this.suppressFormSync = false
            })
        },
        loadLocalDraftRowsFromFormIfNeeded() {
            if (!this.form) return
            if (!Array.isArray(this.form?.timeAxisScript) || !this.form.timeAxisScript.length) return
            this.draftRows = (this.form.timeAxisScript || []).map((item) => ({
                timeRange: item?.timeRange || '',
                title: this.normalizeScriptText(item?.title || ''),
                content: this.normalizeScriptText(item?.content || '')
            }))
            if (this.draftRows?.length) {
                this.drawerStep = 'draft'
                this.draftDirty = false
            }
        },
        syncDraftRowsToFormIfNeeded(rows = this.draftRows) {
            if (!this.form) return
            const nextRows = (Array.isArray(rows) ? rows : []).map((item) => ({
                timeRange: item?.timeRange || '',
                title: this.normalizeScriptText(item?.title || ''),
                content: this.normalizeScriptText(item?.content || '')
            }))
            this.$set(this.form, 'timeAxisScript', nextRows)
        },
        /**
         * @description 将内部表单模型同步回 form（用于 compereForm 等外部对象复用校验/提交）
         * @returns {void}
         */
        syncToFormIfNeeded() {
            if (!this.form) return
            this.syncingToForm = true
            const form = this.form
            const loopDuration = this.scriptModel.loopDuration === '' ? 0 : Number(this.scriptModel.loopDuration)
            const wordsPerMinute = this.scriptModel.wordsPerMinute === '' ? 0 : Number(this.scriptModel.wordsPerMinute)

            this.$set(form, 'scriptRestoreMode', Number(this.scriptModel.scriptType || 0))
            this.$set(form, 'scriptRestoreLoopDuration', Number.isFinite(loopDuration) ? loopDuration : 0)
            this.$set(form, 'scriptRestoreTalkSpeed', Number.isFinite(wordsPerMinute) ? wordsPerMinute : 0)
            this.$set(form, 'scriptRestoreReferenceScript', this.scriptModel.referenceScript || '')
            this.$set(form, 'standardScriptId', this.standardScriptId || null)
            this.$nextTick(() => {
                this.syncingToForm = false
            })
        },
        /**
         * @description visible.sync 的 setter：控制打开/关闭时的状态初始化与回显
         * @param {boolean} status
         * @returns {void}
         */
        changeDrawerStatus(status) {
            this.$emit('update:visible', status)
            if (status) {
                this.skipAutoLoadExistingScript = false
                this.lastLoadedScriptKey = ''
                this.drawerStep = 'form'
                this.loadFromFormIfNeeded()
                this.loadLocalDraftRowsFromFormIfNeeded()
                this.loadExistingStandardScriptIfNeeded()
            } else {
                this.resetDrawer()
            }
        },
        closeDrawer() {
            this.changeDrawerStatus(false)
        },
        resetDrawer() {
            this.suppressFormSync = true
            this.drawerStep = 'form'
            this.scriptModel = this.getDefaultScriptModel()
            this.draftRows = []
            this.standardScriptId = null
            this.hasBackendScript = false
            this.draftDirty = false
            this.lastLoadedScriptKey = ''
            this.skipAutoLoadExistingScript = false
            this.$nextTick(() => {
                this.suppressFormSync = false
            })
        },
        handleReinput() {
            this.skipAutoLoadExistingScript = true
            this.drawerStep = 'form'
            this.draftRows = []
            this.standardScriptId = null
            this.hasBackendScript = false
            this.draftDirty = false
            if (this.form) {
                this.$set(this.form, 'timeAxisScript', [])
                this.$set(this.form, 'standardScriptId', null)
            }
        },
        /**
         * @description 生成标准稿接口入参组装（generateStandardScript）
         * @returns {Object}
         */
        buildGeneratePayload() {
            const anchorUrlUserId = this.getFormAnchorUrlUserId()
            const secUid = this.getFormSecUid()
            const scriptType = Number(this.scriptModel.scriptType || 0)
            const speechMode = scriptType === 1 ? 1 : scriptType === 2 ? 0 : null
            const speechSpeed = Number(this.scriptModel.wordsPerMinute || 0)
            const cycleDurationMinutes = Number(this.scriptModel.loopDuration || 0)
            const referenceScript = this.normalizeScriptText(this.scriptModel.referenceScript || '')

            const payload = {
                speechMode,
                speechSpeed,
                referenceScript
            }

            if (anchorUrlUserId) payload.anchorUrlUserId = anchorUrlUserId
            if (secUid) payload.secUid = secUid
            if (speechMode === 1) payload.cycleDurationMinutes = cycleDurationMinutes
            return payload
        },
        /**
         * @description 调用 generateStandardScript 生成标准稿内容（草稿不落库、不产生 standardScriptId）
         * @returns {Promise<boolean>}
         */
        async generateStandardScript() {
            if (!this.$httpBack?.scriptMonitor?.generateStandardScript) {
                this.$message?.warning?.('缺少生成标准稿接口')
                return false
            }
            if (this.loadingStandardScript) return false
            this.loadingStandardScript = true
            try {
                const payload = this.buildGeneratePayload()
                const res = await this.$httpBack.scriptMonitor.generateStandardScript(payload)

                if (res.code !== 0) return this.$message.error(res.msg)

                const data = res?.data || {}
                this.draftRows = this.mapTimeAxisToRows(data?.timeAxisScript || [])
                this.draftDirty = true
                this.scriptModel = {
                    ...this.scriptModel,
                    ...this.mapApiScriptToScriptModel(data)
                }
                this.syncToFormIfNeeded()
                this.syncDraftRowsToFormIfNeeded(this.draftRows)
                return true
            } catch (e) {

            } finally {
                this.loadingStandardScript = false
            }
        },
        /**
         * @description 点击“生成标准直播稿”：先展示语速二次确认弹窗，再调用生成接口
         * @returns {void}
         */
        confirmStandardDraft() {
            this.syncToFormIfNeeded()
            // 前置校验：避免请求后端才报参错
            if (!this.scriptModel?.wordsPerMinute) {
                this.$message?.warning?.('请先填写语速')
                return
            }
            if (!this.scriptModel?.referenceScript) {
                this.$message?.warning?.('请先填写入炉参考脚本')
                return
            }
            if (Number(this.scriptModel.scriptType) === 1 && !Number(this.scriptModel.loopDuration)) {
                this.$message?.warning?.('请填写循环话术预计时长')
                return
            }

            this.$confirm(`
                    <div style="text-align: center;height: 120px;" class="flex items-center justify-center">
                        <div>
                            <div>转换成标准直播稿件的语速是</div>
                            <div>${this.scriptModel.wordsPerMinute} 字/分钟</div>
                        </div>
                    </div>`, '友情提示', {
                confirmButtonText: '确认转换成标准稿件',
                cancelButtonText: '取消',
                customClass: 'edit-file-name',
                showClose: true,
                showCancelButton: true,
                closeOnClickModal: false,
                closeOnPressEscape: false,
                dangerouslyUseHTMLString: true,
                center: true,
                beforeClose: async (action, instance, done) => {
                    if (action !== 'confirm') {
                        done()
                        return
                    }
                    if (this.loadingStandardScript) return
                    instance.confirmButtonLoading = true
                    instance.confirmButtonText = '生成中...'
                    const ok = await this.generateStandardScript()
                    instance.confirmButtonLoading = false
                    instance.confirmButtonText = '确认转换成标准稿件'
                    if (!ok) return
                    done()
                    this.drawerStep = 'draft'
                }
            }).catch(() => {})
        },
        /**
         * @description 仅本地保存编辑后的标准稿内容（不落库）
         * @param {Array<Object>} rows 标准稿时间轴行
         * @returns {void}
         */
        handleLocalSaveRows(rows) {
            this.draftRows = (Array.isArray(rows) ? rows : []).map(item => ({...item}))
            this.draftDirty = true
            this.syncDraftRowsToFormIfNeeded(this.draftRows)
        },
        /**
         * @description 调用 confirmStandardScript 提交并落库标准稿，返回 standardScriptId，并写回到 form.standardScriptId
         * @param {Array<Object>} rows 标准稿时间轴行
         * @param {(ok: boolean) => void} done 回调
         * @returns {Promise<void>}
         */
        async confirmStandardScriptToBackend(rows, done) {
            if (!this.$httpBack?.scriptMonitor?.confirmStandardScript) {
                this.$message?.warning?.('缺少确认标准稿接口')
                done?.(false)
                return
            }
            const scriptType = Number(this.scriptModel.scriptType || 0)
            const speechMode = scriptType === 1 ? 1 : scriptType === 2 ? 0 : null
            const speechSpeed = Number(this.scriptModel.wordsPerMinute || 0)
            const cycleDurationMinutes = Number(this.scriptModel.loopDuration || 0)
            const referenceScript = String(this.scriptModel.referenceScript || '')
            const anchorUrlUserId = this.getFormAnchorUrlUserId()
            const secUid = this.getFormSecUid()
            if (!secUid) {
                this.$message?.warning?.('缺少 secUid，无法保存标准直播稿')
                done?.(false)
                return
            }
            const timeAxisScript = (Array.isArray(rows) ? rows : []).map((r) => ({
                timeRange: r?.timeRange || '',
                title: this.normalizeScriptText(r?.title || ''),
                content: this.normalizeScriptText(r?.content || '')
            }))

            if (!timeAxisScript.length) {
                this.$message?.warning?.('请先生成标准直播稿')
                done?.(false)
                return
            }

            const payload = { speechMode, speechSpeed, referenceScript: this.normalizeScriptText(referenceScript), timeAxisScript }
            if (speechMode === 1) payload.cycleDurationMinutes = cycleDurationMinutes
            if (anchorUrlUserId) payload.anchorUrlUserId = anchorUrlUserId
            payload.secUid = secUid

            try {
                const res = await this.$httpBack.scriptMonitor.confirmStandardScript(payload)
                if (res?.code !== 0) {
                    this.$message?.warning?.(res?.msg || '保存失败')
                    done?.(false)
                    return
                }
                const data = res?.data || {}
                this.standardScriptId = data?.standardScriptId || data?.scriptId || this.standardScriptId
                this.draftRows = timeAxisScript.map(item => ({...item}))
                this.syncToFormIfNeeded()
                this.syncDraftRowsToFormIfNeeded(this.draftRows)
                done?.(true)
                this.hasBackendScript = true
                this.draftDirty = false
            } catch (e) {
                this.$message?.warning?.('保存失败')
                done?.(false)
            }
        },
        /**
         * @description 确认标准直播稿（落库，返回 standardScriptId）
         * - 新增直播间：不传 anchorUrlUserId，confirmStandardScript INSERT 并返回 standardScriptId
         * - 修改直播间：传 anchorUrlUserId，confirmStandardScript 按 anchorUrlUserId UPDATE/INSERT 并返回 standardScriptId（同直播间保持不变）
         * @returns {void}
         */
        handleConfirmStandardScript() {
            if (this.normalizedScene !== 'anchorConfig') return
            const secUid = this.getFormSecUid()
            if (!secUid) {
                const timeAxisScript = (Array.isArray(this.draftRows) ? this.draftRows : []).map((r) => ({
                    timeRange: r?.timeRange || '',
                    title: this.normalizeScriptText(r?.title || ''),
                    content: this.normalizeScriptText(r?.content || '')
                }))
                if (!timeAxisScript.length) {
                    this.$message?.warning?.('请先生成标准直播稿')
                    return
                }
                if (this.form) {
                    this.$set(this.form, 'timeAxisScript', timeAxisScript)
                }
                this.syncToFormIfNeeded()
                this.$emit('confirmed', { standardScriptId: null, timeAxisScript })
                this.closeDrawer()
                return
            }

            this.confirmStandardScriptToBackend(this.draftRows, (ok) => {
                if (!ok) return
                this.$emit('confirmed', { standardScriptId: this.standardScriptId })
                this.closeDrawer()
            })
        },
        /**
         * @description analysis 场景：生成还原度报告
         * - 若本次有修改（draftDirty=true），先 confirmStandardScript 落库
         * - 否则直接向父层抛出 generate 事件，由父层调用 triggerReport
         * @returns {void}
         */
        handleGenerateReport() {
            if (this.normalizedScene !== 'analysis') return
            const next = () => {
                this.$emit('generate', { standardScriptId: this.standardScriptId })
                this.$message?.success?.('还原度报告生成中')
                this.closeDrawer()
            }
            if (!this.draftDirty && this.standardScriptId) {
                next()
                return
            }
            this.confirmStandardScriptToBackend(this.draftRows, (ok) => {
                if (!ok) return
                next()
            })
        }
    }
}
</script>

<style scoped lang="scss">
::v-deep(.el-drawer__body) {
    height: 100%;
}

.restoration-drawer {
    display: flex;
    flex-direction: column;
    height: 100%;
    background: #fff;
    color: #151719;
    overflow: hidden;
}

.drawer-header {
    flex: 0 0 48px;
    display: flex;
    align-items: center;
    gap: 8px;
    height: 48px;
    padding-left: 30px;
    border-bottom: 1px solid rgba(227, 227, 229, 0.25);
    font-size: 16px;
    font-weight: 500;
    line-height: 22px;

    .close-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 24px;
        height: 24px;
        color: #acb4c4;
        cursor: pointer;
        font-size: 16px;
    }
}
</style>
