<template>
    <div class="restoration-form-panel">
        <div class="drawer-content">
            <div style="width: 90%;">
                <el-alert type="warning" class="mg-b10" :closable="false">
                    <template #default>
                        <div class="pd-b6">提示：生成标准稿件时间较长，可添加完直播间后前往<span class="text-colorTheme">直播间列表-更多-基础设置里去配置</span></div>
                    </template>
                </el-alert>
            </div>
            <el-form
                ref="restorationForm"
                class="restoration-form"
                :model="value"
                :rules="restorationRules"
                label-position="top">
                <el-form-item label="选择模式" class="mode-section" prop="scriptType">
                    <el-radio-group v-model="value.scriptType" class="mode-list">
                        <el-radio class="mode-card" :label="1" border>
                            <div class="mode-text">
                                <div class="mode-title">循环话术</div>
                                <div class="mode-desc">话术循环讲解</div>
                            </div>
                            <span class="check-icon" v-if="value.scriptType === 1">
                                <i class="el-icon-check"></i>
                            </span>
                        </el-radio>
                        <el-radio class="mode-card" :label="2" border disabled>
                            <div class="mode-text">
                                <div class="mode-title">非循环话术</div>
                                <div class="mode-desc">话术循环讲解</div>
                            </div>
                            <span class="check-icon" v-if="value.scriptType === 2">
                                <i class="el-icon-check"></i>
                            </span>
                        </el-radio>
                    </el-radio-group>
                </el-form-item>

                <template v-if="value.scriptType">
                    <div class="duration-section">
                        <el-form-item
                            v-if="value.scriptType === 1"
                            label="循环话术预估时长"
                            prop="loopDuration"
                            class="field-group">
                            <div class="inline-control">
                                <el-input
                                    v-model="value.loopDuration"
                                    class="pill-input"
                                    size="small"></el-input>
                                <span class="unit">分钟</span>
                            </div>
                        </el-form-item>
                        <el-form-item label="主播语速" prop="wordsPerMinute" class="field-group speed-group">
                            <div class="inline-control">
                                <el-input
                                    v-model="value.wordsPerMinute"
                                    class="pill-input"
                                    size="small"></el-input>
                                <span class="unit">字/分钟</span>
                            </div>
                        </el-form-item>
                    </div>

                    <div
                        v-if="value.scriptType === 1"
                        class="tip-bar">
                        <i class="el-icon-s-opportunity tip-icon"></i>
                        <span>循环话术时长应预留正常互动时长，建议填写略大于实际单循环的时间</span>
                    </div>

                    <el-form-item class="script-section" prop="referenceScript" label-width="200px">
                        <template slot="label">
                            <div class="script-label-line">
                                <span>录入参考直播脚本</span>
                                <el-button class="example-btn" type="text" @click="$emit('open-example')">查看示例</el-button>
                            </div>
                        </template>
                        <el-input
                            v-model="value.referenceScript"
                            class="script-textarea"
                            type="textarea"
                            maxlength="10000"
                            resize="none"
                            :rows="16"
                            show-word-limit
                            placeholder="请粘贴或输入直播参考脚本内容...">
                        </el-input>
                    </el-form-item>
                </template>
            </el-form>
        </div>

        <el-button class="generate-btn" type="primary" @click="submit">生成标准直播稿</el-button>
    </div>
</template>

<script>
export default {
    name: 'ScriptRestorationForm',
    props: {
        value: {
            type: Object,
            required: true
        }
    },
    mounted() {
        if (Number(this.value?.scriptType) !== 1) this.$set(this.value, 'scriptType', 1)
    },
    data() {
        return {
            restorationRules: {
                scriptType:{required: true, message: '请选择话术还原度监控模式', trigger: ['blur', 'change']},
                loopDuration: [
                    {required: true, message: '请输入循环话术预估时长', trigger: ['blur', 'change']},
                    {
                        pattern: /^(?:[1-9]\d*|[1-9]\d*\.\d{1,2}|0\.(?:0[1-9]|[1-9]\d?))$/,
                        message: '请输入大于0的整数或最多两位小数',
                        trigger: ['blur', 'change']
                    }
                ],
                wordsPerMinute: [
                    {required: true, message: '请输入直播间语速', trigger: 'blur'},
                    {pattern: /^[1-9]\d*$/, message: '请输入大于0的整数', trigger: ['blur', 'change']}
                ],
                referenceScript: {required: true, message: '请录入参考直播脚本', trigger: ['blur', 'change']},
            }
        }
    },
    methods: {
        submit() {
            this.$refs.restorationForm.validate(valid => {
                if (!valid) return
                this.$emit('submit')
            })
        }
    }
}
</script>

<style scoped lang="scss">
.restoration-form-panel {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
}

.drawer-content {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 21px 0 24px 30px;
}

.restoration-form {
    width: 524px;
}

::v-deep(.restoration-form .el-form-item) {
    margin-bottom: 0;
}

::v-deep(.restoration-form .el-form-item__label) {
    padding: 0 0 8px;
    color: #484a4d;
    font-size: 14px;
    font-weight: 500;
    line-height: 22px;
}

::v-deep(.restoration-form .el-form-item.is-required:not(.is-no-asterisk) > .el-form-item__label::before) {
    color: #e43b32;
    margin-right: 0;
}

.mode-list {
    display: flex;
    gap: 24px;
}

::v-deep(.mode-list .el-radio) {
    height: 68px;
}

::v-deep(.mode-list .el-radio.is-bordered) {
    padding: 0 12px;
}

::v-deep(.mode-list .el-radio.is-bordered + .el-radio.is-bordered) {
    margin-left: 0;
}

.mode-card {
    position: relative;
    display: flex !important;
    align-items: center;
    width: 250px;
    height: 76px;
    padding: 12px;
    border: 1px solid #dfeaf6;
    border-radius: 4px;
    background: #fff;
    box-sizing: border-box;
    margin-right: 0 !important;

    &.is-checked {
        border-color: #444dff;
        background: #f5f6ff;

        .mode-title {
            color: #444dff;
        }
    }
}

::v-deep(.mode-card.is-disabled) {
    border-color: #e4e7ed;
    background: #f5f7fa;
    cursor: not-allowed;
}

::v-deep(.mode-card.is-disabled .mode-title),
::v-deep(.mode-card.is-disabled .mode-desc) {
    color: #c0c4cc;
}

::v-deep(.mode-card .el-radio__input) {
    display: none;
}

::v-deep(.mode-card .el-radio__label) {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding-left: 0;
}

.mode-title {
    color: #151719;
    font-size: 14px;
    font-weight: 500;
    line-height: 22px;
}

.mode-desc {
    margin-top: 4px;
    color: #7a7c7f;
    font-size: 12px;
    line-height: 16px;
}

.check-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    border: 1px solid #444dff;
    border-radius: 50%;
    color: #444dff;
    font-size: 12px;
}

.duration-section {
    display: flex;
    gap: 60px;
    margin-top: 11px;
}

.field-group {
    width: 214px;
}

.speed-group {
    width: 250px;
}

.inline-control {
    display: flex;
    align-items: center;
}

::v-deep(.pill-input .el-input__inner) {
    height: 34px;
    padding: 4px 16px;
    border: 1px solid #dfeaf6;
    border-radius: 49px;
    color: #151719;
    font-size: 14px;
    line-height: 22px;
}

.unit {
    margin-left: 8px;
    color: #151719;
    font-size: 14px;
    line-height: 22px;
    white-space: nowrap;
}

.tip-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 524px;
    height: 30px;
    margin-top: 20px;
    padding: 4px 12px;
    border-radius: 130px;
    background: #fff7ed;
    box-sizing: border-box;
    color: #f97316;
    font-size: 14px;
    line-height: 22px;
    white-space: nowrap;
}

.tip-icon {
    flex: 0 0 16px;
    font-size: 16px;
}

.script-section {
    margin-top: 20px;
}

::v-deep(.script-section .el-form-item__label) {
    display: flex;
    align-items: center;
    width: 524px;
    white-space: nowrap;
}

.script-label-line {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex: 1;
    min-width: 0;
}

.example-btn {
    padding: 0;
    color: #444dff;
    font-size: 14px;
    line-height: 22px;
}

::v-deep(.script-textarea) {
    position: relative;
}

::v-deep(.script-textarea .el-textarea__inner) {
    padding-bottom: 28px;
}

::v-deep(.script-textarea .el-input__count) {
    background: rgba(255, 255, 255, 0.9);
    border-radius: 2px;
    line-height: 18px;
    padding: 0 4px;
    right: 10px;
    bottom: 10px;
    pointer-events: none;
}

.generate-btn {
    flex: 0 0 auto;
    align-self: flex-start;
    margin: 12px 0 15px 30px;
    height: 34px;
    padding: 4px 12px;
    border-color: #444dff;
    border-radius: 54px;
    background: #444dff;
    font-size: 14px;
    font-weight: 500;
    line-height: 22px;
}
</style>
