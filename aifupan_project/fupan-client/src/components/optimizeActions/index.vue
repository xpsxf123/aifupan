<template>
    <div>
        <el-drawer
            title="我是标题"
            :visible.sync="drawer"
            :close-on-press-escape="false"
            :destroy-on-close="false"
            :wrapperClosable="false"
            size="48vw"
            custom-class="optimize-actions-drawer"
            :with-header="false">
            <div class="optimize-actions">
                <div class="optimize-header flex items-center">
                    <i class="el-icon-close cursor-pointer text-lg" @click="change(false)"></i>
                    <span class="pd-l12">优化计划</span>
                </div>
                <div class="optimize-content">
                    <div class="pd-t12 text-sm">
                        想<span class="text-colorErr">监控主播每一场</span>是否按照要求做了优化？<br>
                        想知道<span class="text-colorErr">优化动作是否有效</span>？<br>
                        请记录每一场运营优化动作和想达到的效果！<br>
                        在AI对比上一场的时候，快速<span class="text-colorTheme">分析主播执行力度</span>和<span class="text-colorTheme">优化动作效果</span>！
                    </div>
                    <el-form ref="ruleForm" label-position="top" :model="form" label-width="80px">
                        <!-- TODO: 计划日期/计划对象板块已迁移至 2.6.2.4 -->
                        <div v-if="false" class="optimize-plan-meta">
                            <el-row :gutter="16">
                                <el-col :span="12">
                                    <el-form-item label="计划日期">
                                        <el-date-picker
                                            v-model="planDateRange"
                                            type="daterange"
                                            value-format="yyyy-MM-dd"
                                            format="yyyy-M-d"
                                            range-separator="→"
                                            start-placeholder="开始日期"
                                            end-placeholder="结束日期"
                                            :disabled="planMetaDisabled || isViewOnly"
                                            style="width: 100%;" />
                                    </el-form-item>
                                </el-col>
                                <el-col :span="12">
                                    <el-form-item label="计划对象">
                                        <div class="plan-target">
                                            <el-input
                                                :value="planTargetsText"
                                                readonly
                                                placeholder="请选择计划对象" />
                                            <span
                                                class="plan-target__link"
                                                :class="{ disabled: planMetaDisabled || isViewOnly }"
                                                @click="openSelectDrawer">
                                                选择对象
                                            </span>
                                        </div>
                                    </el-form-item>
                                </el-col>
                            </el-row>
                            <div v-if="planMetaHintType === 'enterprise'" class="plan-meta-hint">
                                *计划对象<span class="text-colorTheme">仅支持企业及以上版本</span>才可以使用。
                            </div>
                            <div v-else-if="planMetaHintType === 'schedule'" class="plan-meta-hint">
                                当前无排班，计划日期、计划对象暂不可选
                            </div>
                        </div>
                        <el-form-item label="优化动作">
                            <el-input
                                type="textarea"
                                placeholder="请输入本场具体优化动作，2000个字以内"
                                v-model="form.optimizeAction"
                                maxlength="2000"
                                show-word-limit
                                :autosize="{ minRows: 8, maxRows: 8}"
                                resize="none"
                                :disabled="isViewOnly" />
                        </el-form-item>
                        <el-form-item label="优化目的">
                            <el-input
                                type="textarea"
                                placeholder="请输入优化后想要达到的效果，500个字以内"
                                v-model="form.optimizePurpose"
                                maxlength="500"
                                show-word-limit
                                :autosize="{ minRows: 8, maxRows: 8}"
                                resize="none"
                                :disabled="isViewOnly" />
                        </el-form-item>
                        <div class="optimize-footer">
                            <afp-button
                                v-if="showCancelView"
                                type="primary"
                                size="default"
                                :plain="false"
                                @click="init">
                                取消查看
                            </afp-button>
                            <afp-button
                                v-if="showConfirmBtn"
                                type="primary"
                                size="default"
                                :plain="false"
                                :disabled="confirmDisabled"
                                @click="submitForm()">
                                {{ confirmText }}
                            </afp-button>
                            <afp-button
                                v-if="showComparedBtn"
                                type="primary"
                                size="default"
                                :plain="true"
                                @click="compared">
                                对比上一场
                            </afp-button>
                            <!-- TODO: 复制计划/删除计划已迁移至 2.6.2.4 -->
                            <afp-button
                                v-if="false"
                                type="primary"
                                size="default"
                                :plain="true"
                                @click="enterCopyMode">
                                复制计划
                            </afp-button>
                            <span
                                v-if="false"
                                class="delete-plan"
                                :class="{ disabled: deleteDisabled }"
                                @click="openDeleteConfirm">
                                删除计划
                            </span>
                        </div>
                    </el-form>
                </div>
            </div>
        </el-drawer>

        <Compare ref="compare" :rowItem="rowItem" @closeDialog="closeDialog"/>

        <el-drawer
            :visible.sync="selectDrawerVisible"
            :append-to-body="true"
            :close-on-press-escape="false"
            :destroy-on-close="true"
            :wrapperClosable="false"
            size="42vw"
            custom-class="optimize-targets-drawer"
            :with-header="false">
            <div class="optimize-targets">
                <div class="optimize-targets__header flex items-center">
                    <i class="el-icon-close cursor-pointer text-lg" @click="selectDrawerVisible = false"></i>
                    <span class="pd-l12">选择对象</span>
                </div>
                <div class="optimize-targets__content">
                    <div class="targets-search">
                        <el-input v-model="selectKeyword" placeholder="请输入姓名" clearable style="width: 220px;" />
                        <afp-button class="targets-search__btn" type="primary" size="default" :plain="true" @click="handleSelectSearch">
                            查找
                        </afp-button>
                    </div>
                    <el-table
                        ref="targetsTable"
                        :data="pagedTargets"
                        row-key="__rowKey"
                        @selection-change="handleTargetsSelectionChange"
                        height="540">
                        <el-table-column type="selection" width="52" />
                        <el-table-column prop="name" label="姓名" min-width="120" />
                        <el-table-column prop="company" label="公司" min-width="180" />
                        <el-table-column prop="org" label="所属组织" min-width="180" />
                        <el-table-column prop="position" label="岗位" min-width="120" />
                    </el-table>
                    <div class="targets-footer">
                        <div class="targets-footer__actions">
                            <afp-button type="primary" size="default" :plain="false" @click="confirmSelectTargets">
                                确定
                            </afp-button>
                            <afp-button type="primary" size="default" :plain="true" @click="selectDrawerVisible = false">
                                取消
                            </afp-button>
                        </div>
                        <el-pagination
                            background
                            layout="prev, pager, next"
                            :page-size="selectPageSize"
                            :current-page.sync="selectPage"
                            :total="filteredTargets.length" />
                    </div>
                </div>
            </div>
        </el-drawer>

        <el-dialog
            :visible.sync="deleteConfirmVisible"
            width="360px"
            :append-to-body="true"
            :close-on-click-modal="false"
            :show-close="false"
            class="optimize-plan-delete-dialog">
            <div class="delete-dialog-content">
                <div class="delete-dialog-title">确认删除该计划？</div>
                <div class="delete-dialog-desc">删除后不可恢复</div>
                <div class="delete-dialog-actions">
                    <afp-button type="primary" size="default" :plain="true" @click="deleteConfirmVisible = false">
                        取消
                    </afp-button>
                    <afp-button type="primary" size="default" :plain="false" @click="confirmDeletePlan">
                        确认删除
                    </afp-button>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import Compare from './Compare.vue'

export default {
    components: {Compare},
    props: {},
    data() {
        return {
            drawer: false,
            rowItem: {},
            aiOptimizePurposeId: '',
            callback: null,
            form: {
                optimizeAction: '',
                optimizePurpose: ''
            },
            planDateRange: [],
            planTargets: [],
            planStatus: 'normal',
            mode: 'create',
            selectDrawerVisible: false,
            selectKeyword: '',
            selectPage: 1,
            selectPageSize: 10,
            tempSelectedTargets: [],
            deleteConfirmVisible: false
        };
    },
    computed: {
        permission() {
            if (!this.drawer) return false
            const {id: currentUserId} = this.$store?.state?.userInfo;
            return this.rowItem?.UserId === currentUserId || this.rowItem?.userId === currentUserId
        },
        packageLevel() {
            return Number(this.$store?.state?.userInfo?.packageLevel || 0)
        },
        isEnterprise() {
            return this.packageLevel >= 20
        },
        hasSchedule() {
            return this.rowItem?.__optimizePlanExt?.hasSchedule === true
        },
        planMetaDisabled() {
            if (!this.isEnterprise) return true
            return !this.hasSchedule
        },
        planMetaHintType() {
            if (!this.isEnterprise) return 'enterprise'
            if (this.isEnterprise && !this.hasSchedule) return 'schedule'
            return ''
        },
        planTargetsText() {
            return (Array.isArray(this.planTargets) ? this.planTargets : [])
            .map(t => t?.name)
            .filter(Boolean)
            .join('、')
        },
        isExpired() {
            return this.planStatus === 'expired'
        },
        isViewOnly() {
            return this.mode === 'expiredView'
        },
        showCancelView() {
            return this.mode === 'edit' || this.mode === 'expiredView'
        },
        showConfirmBtn() {
            return this.permission
        },
        showComparedBtn() {
            return this.rowItem?.videoSliceType === 0 && !!this.callback
        },
        showCopyPlan() {
            return this.mode === 'expiredView'
        },
        showDeletePlan() {
            return this.mode === 'edit' || this.mode === 'expiredView'
        },
        deleteDisabled() {
            return this.mode === 'expiredView'
        },
        confirmText() {
            if (this.mode === 'create' || this.mode === 'copy') return '立即创建'
            return '确定修改'
        },
        confirmDisabled() {
            if (!this.permission) return true
            if (this.mode === 'expiredView') return true
            if (this.mode === 'copy') {
                if (this.planMetaDisabled) return false
                return !(Array.isArray(this.planDateRange) && this.planDateRange.length === 2)
            }
            return false
        },
        filteredTargets() {
            const keyword = String(this.selectKeyword || '').trim()
            const list = this.getTargetsList()
            if (!keyword) return list
            return list.filter(item => String(item?.name || '').includes(keyword))
        },
        pagedTargets() {
            const start = (Number(this.selectPage || 1) - 1) * Number(this.selectPageSize || 10)
            const end = start + Number(this.selectPageSize || 10)
            return this.filteredTargets.slice(start, end)
        }
    },
    watch: {},
    methods: {
        async change(val, item, callback) {
            this.drawer = val;
            this.rowItem = item;
            this.callback = callback
            if (val) {
                await this.getAiOptimizePurpose()
                this.loadPlanMock()
            }
        },
        init() {
            this.rowItem = {}
            this.aiOptimizePurposeId = ''
            this.form = {
                optimizeAction: '',
                optimizePurpose: ''
            }
            this.planDateRange = []
            this.planTargets = []
            this.planStatus = 'normal'
            this.mode = 'create'
            this.selectDrawerVisible = false
            this.selectKeyword = ''
            this.selectPage = 1
            this.tempSelectedTargets = []
            this.deleteConfirmVisible = false
            this.drawer = false
        },
        getSourceId() {
            return String(this.rowItem?.videoId || this.rowItem?.VideoId || '')
        },
        readMockMap() {
            const raw = localStorage.getItem('AI_OPTIMIZE_PLAN_MOCK_MAP_V1') || '{}'
            try {
                const parsed = JSON.parse(raw)
                return parsed && typeof parsed === 'object' ? parsed : {}
            } catch (e) {
                return {}
            }
        },
        writeMockMap(map) {
            localStorage.setItem('AI_OPTIMIZE_PLAN_MOCK_MAP_V1', JSON.stringify(map || {}))
        },
        loadPlanMock() {
            const id = this.getSourceId()
            if (!id) return
            const map = this.readMockMap()
            const mock = map?.[id] || {}

            if (mock?.deleted === true) {
                this.planDateRange = []
                this.planTargets = []
                this.planStatus = 'normal'
                this.mode = 'create'
                return
            }

            this.planDateRange = Array.isArray(mock?.planDateRange) ? mock.planDateRange : []
            this.planTargets = Array.isArray(mock?.planTargets) ? mock.planTargets : []
            this.planStatus = mock?.status || 'normal'
            const hasPlan = this.aiOptimizePurposeId || mock?.hasPlan === true
            if (hasPlan && this.planStatus === 'expired') {
                this.mode = 'expiredView'
            } else if (hasPlan) {
                this.mode = 'edit'
            } else {
                this.mode = 'create'
            }
        },
        savePlanMock(partial) {
            const id = this.getSourceId()
            if (!id) return
            const map = this.readMockMap()
            const prev = map?.[id] || {}
            const next = {
                ...prev,
                ...partial
            }
            map[id] = next
            this.writeMockMap(map)
            this.$emit('mock-change')
        },
        async getAiOptimizePurpose() {
            const result = await this.$httpBack.video.getOptimizePurpose({
                sourceId: this.rowItem?.videoId || this.rowItem?.VideoId
            })
            if (result.code !== 0) return
            this.form = {
                optimizeAction: result.data?.optimizeAction || '',
                optimizePurpose: result.data?.optimizePurpose || '',
            }
            this.aiOptimizePurposeId = result.data?.id
        },
        async confirmHttp(callback) {
            try {
                const httpServer = this.aiOptimizePurposeId ? this.$httpBack.video.aiOptimizePurposeUpdate : this.$httpBack.video.aiOptimizePurposeSave
                const params = this.aiOptimizePurposeId ? {
                    id: this.aiOptimizePurposeId,
                    ...this.form
                } : {
                    ...this.form
                }
                const result = await httpServer({
                    sourceId: this.rowItem?.videoId || this.rowItem?.VideoId,
                    sourceType: 0,//来源类型（0：视频 1：文件 2：对比）
                    ...params
                })
                if (result.code !== 0) return
                this.savePlanMock({
                    hasPlan: true,
                    planDateRange: this.planDateRange,
                    planTargets: this.planTargets,
                    status: this.planStatus,
                    deleted: false
                })
                this.$message.success(`${this.mode === 'edit' ? '确定修改' : (this.mode === 'copy' ? '复制计划' : '创建计划')}成功`)
                this.callback?.()
                callback?.()
                if (!callback) this.init()
            } catch (e) {

            }
        },
        submitForm(callback) {
            if (this.confirmDisabled) return
            this.$refs.ruleForm?.validate(async (valid) => {
                if (!valid) return false
                if (Array.isArray(this.planDateRange) && this.planDateRange.length === 2) {
                    const end = String(this.planDateRange?.[1] || '')
                    const today = new Date()
                    const y = today.getFullYear()
                    const m = String(today.getMonth() + 1).padStart(2, '0')
                    const d = String(today.getDate()).padStart(2, '0')
                    const todayText = `${y}-${m}-${d}`
                    this.planStatus = end && end < todayText ? 'expired' : 'normal'
                } else {
                    this.planStatus = 'normal'
                }
                await this.confirmHttp(callback)
            })
        },
        compared() {
            if (this.isViewOnly) {
                this.$refs.compare?.openDialog?.()
                return
            }
            this.submitForm(this.$refs.compare?.openDialog)
        },
        closeDialog() {
            this.init()
        },
        openSelectDrawer() {
            if (this.planMetaDisabled || this.isViewOnly) return
            this.selectKeyword = ''
            this.selectPage = 1
            this.tempSelectedTargets = Array.isArray(this.planTargets) ? [...this.planTargets] : []
            this.selectDrawerVisible = true
            this.$nextTick(() => {
                const rows = this.pagedTargets || []
                const selectedKeys = new Set(
                    this.tempSelectedTargets.map(t => {
                        const id = t?.id ? String(t.id) : ''
                        const name = t?.name ? String(t.name) : ''
                        return id ? `id:${id}` : (name ? `name:${name}` : '')
                    }).filter(Boolean)
                )
                rows.forEach(row => {
                    const key = row?.id ? `id:${String(row.id)}` : `name:${String(row?.name || '')}`
                    if (selectedKeys.has(key)) {
                        this.$refs.targetsTable?.toggleRowSelection(row, true)
                    }
                })
            })
        },
        getRawEmployees() {
            const list = Array.isArray(this.rowItem?.__optimizePlanExt?.employees) ? this.rowItem.__optimizePlanExt.employees : []
            return list
        },
        toTargetRow(employee, index) {
            const name = employee?.employeeName || employee?.name || employee?.nickName || employee?.employeeNickName || ''
            const id = employee?.employeeId || employee?.id || ''
            const company = employee?.companyName || employee?.company || employee?.tenantName || ''
            const org = employee?.deptName || employee?.departmentName || employee?.orgName || ''
            const position = employee?.positionName || employee?.position || '主播'
            return {
                __rowKey: `${String(id || name || index)}`,
                id: id ? String(id) : '',
                name,
                company,
                org,
                position
            }
        },
        getTargetsList() {
            const employees = this.getRawEmployees()
            const mapped = employees.map((e, i) => this.toTargetRow(e, i)).filter(x => !!x.name)
            const uniq = []
            const seen = new Set()
            for (const item of mapped) {
                const key = item.id ? `id:${item.id}` : `name:${item.name}`
                if (seen.has(key)) continue
                seen.add(key)
                uniq.push(item)
            }
            return uniq
        },
        handleSelectSearch() {
            this.selectPage = 1
        },
        handleTargetsSelectionChange(list) {
            this.tempSelectedTargets = Array.isArray(list) ? list : []
        },
        confirmSelectTargets() {
            const next = (Array.isArray(this.tempSelectedTargets) ? this.tempSelectedTargets : []).map(t => ({
                id: t?.id || '',
                name: t?.name || ''
            })).filter(t => !!t.name)
            this.planTargets = next
            this.selectDrawerVisible = false
            this.savePlanMock({
                planTargets: next,
                deleted: false
            })
        },
        enterCopyMode() {
            this.mode = 'copy'
            this.planStatus = 'normal'
            this.planDateRange = []
        },
        openDeleteConfirm() {
            if (this.deleteDisabled) return
            this.deleteConfirmVisible = true
        },
        async confirmDeletePlan() {
            if (this.deleteDisabled) return
            this.deleteConfirmVisible = false

            if (this.aiOptimizePurposeId) {
                try {
                    const result = await this.$httpBack.video.aiOptimizePurposeUpdate({
                        sourceId: this.rowItem?.videoId || this.rowItem?.VideoId,
                        sourceType: 0,
                        id: this.aiOptimizePurposeId,
                        optimizeAction: '',
                        optimizePurpose: ''
                    })
                    if (result.code !== 0) return
                } catch (e) {
                    return
                }
            }

            this.savePlanMock({
                hasPlan: false,
                deleted: true,
                planDateRange: [],
                planTargets: [],
                status: 'normal'
            })
            this.$message.success('删除成功')
            this.callback?.()
            this.init()
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
::v-deep(.optimize-actions-drawer) {

    .optimize-actions {
        padding-inline: 24px;

        .optimize-header {
            padding-block: 12px;
        }

        .optimize-content {
            .el-form-item__label {
                padding: 0;
            }
        }
    }
}

.optimize-plan-meta {
    margin-top: 16px;
}

.plan-target {
    position: relative;
}

.plan-target__link {
    position: absolute;
    top: 0;
    right: 0;
    height: 100%;
    display: flex;
    align-items: center;
    padding: 0 8px;
    color: #444DFF;
    cursor: pointer;
    user-select: none;
}

.plan-target__link.disabled {
    color: #C0C4CC;
    cursor: not-allowed;
}

.plan-meta-hint {
    margin-top: -10px;
    margin-bottom: 10px;
    color: #909399;
    font-size: 12px;
    line-height: 18px;
}

.optimize-footer {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 4px 0 14px;
}

.delete-plan {
    margin-left: auto;
    color: #F56C6C;
    cursor: pointer;
    user-select: none;
}

.delete-plan.disabled {
    color: #C0C4CC;
    cursor: not-allowed;
}

::v-deep(.optimize-targets-drawer) {
    .optimize-targets {
        padding-inline: 24px;
    }
}

.targets-search {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
}

.targets-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 0 0;
}

.targets-footer__actions {
    display: flex;
    gap: 12px;
}

::v-deep(.optimize-plan-delete-dialog .el-dialog) {
    border-radius: 10px;
}

.delete-dialog-content {
    padding: 6px 0 0;
    text-align: center;
}

.delete-dialog-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
}

.delete-dialog-desc {
    margin-top: 10px;
    font-size: 14px;
    color: #909399;
}

.delete-dialog-actions {
    display: flex;
    justify-content: center;
    gap: 12px;
    padding: 22px 0 8px;
}
</style>
