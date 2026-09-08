<!--
@description 录制配置表单块：承载录制清晰度、分段规则、自动分析与自动删除等可复用录制设置。
注意：
1) 添加主播与直播间基础设置会复用本组件，自动删除选项需按场景切换默认值与可选项；
2) 自动删除删除内容的必填校验由外层表单统一处理。
-->
<template>
    <div class="record-config">
        <div :class="{'flex-form':isSettingChange}">
            <el-form-item label="录制清晰度" prop="recordDefinition" label-width="112px"
                          :class="{'flex-form-item':true, 'flex-form-item-set':!isSettingChange}">
                <el-radio-group :value="value.recordDefinition" @input="update('recordDefinition', $event)">
                    <el-radio
                        v-for="item in [{label:'跟随系统设置',value:-1},{label:'标清',value:0},{label:'高清',value:1},{label:'超清',value:2},{label:'蓝光',value:3}]"
                        :key="item.value" :label="item.value"
                        class="radio-as-checkbox">
                        {{ item.label }}
                    </el-radio>
                </el-radio-group>
                <span class="tips"
                      v-if="!isSettingChange">需要剪辑短视频切片的，可选择超清或蓝光</span>
            </el-form-item>
        </div>
        <div class="flex-form">
            <el-form-item label="每段录制时长" prop="recordLimitValue" label-width="112px"
                          class="flex-form-item">
                <el-radio-group :value="value.recordLimitValue" @input="update('recordLimitValue', $event)">
                    <el-radio :label="0" class="radio-as-checkbox">
                        跟随系统设置
                    </el-radio>
                    <el-radio :label="1" class="radio-as-checkbox">
                        单独设置
                    </el-radio>
                </el-radio-group>
            </el-form-item>

            <el-form-item label-width="0px" class="flex-form-item" style="margin-left: 20px"
                          v-if="value.recordLimitValue===1">
                <el-slider :value="value.recordDuration" @input="update('recordDuration', $event)" :min="30" :marks="{
                                0: '无限制',
                                120: '2h',
                                240: '4h',
                                360: '6h',
                                480: '8h',
                                600: '10h',
                            }" :max="600"
                           style="width: 200px;" :show-tooltip="false">
                </el-slider>
            </el-form-item>
            <template v-if="value.recordLimitValue===1">
                <el-input-number step-strictly controls-position="right" class="subsectionTime"
                                 v-model="value.recordDuration" @change="val => update('subsectionTime',val)" :min="30"
                                 :max="600"
                                 placeholder="请输入分钟时长"></el-input-number>
                <span style="padding-inline: 12px">/分钟每段（不低于30分钟）</span>
            </template>
            <!--            <div style="margin-left: 12px;color: red;width: 90px;margin-bottom: 24px;"-->
            <!--                 v-if="value.recordLimitValue===1">-->
            <!--                {{-->
            <!--                    value.recordDuration ?-->
            <!--                        (value.recordDuration % 60 == 0 ? value.recordDuration / 60 + '小时' :-->
            <!--                            parseInt(value.recordDuration / 60) + '小时' + value.recordDuration % 60 + '分')-->
            <!--                        :-->
            <!--                        '无限制'-->
            <!--                }}-->
            <!--            </div>-->
            <span class="tips"
                  v-if="!isSettingChange">将录屏视频按一定的时长进行分段，不会停止直播间录制</span>
        </div>
        <div class="flex-form">
            <el-form-item label="按时间点分段" prop="segmentTimePoints" label-width="112px" class="flex-form-item">
                <div style="width: 300px;">
                    <FormItemAry v-model="segmentTimePointsArray" dataType="string" :min="1" :max="99">
                        <template #default="{index}">
                            <el-time-picker
                                v-model="segmentTimePointsArray[index]"
                                value-format="HH:mm"
                                format="HH:mm"
                                placeholder="选择时间">
                            </el-time-picker>
                        </template>
                    </FormItemAry>
                </div>
                <div class="tips w100" style="margin-left: 0 !important;" v-if="!isSettingChange">
                    不管录制时长多长，到时间点就分段，可以同时设置48个时间点 考虑到电脑性能消耗、分段时间点稍有误差，属于正常现象
                </div>
            </el-form-item>
        </div>
        <div class="flex-form">
            <el-form-item label="录制完成操作" prop="recordLimitType" label-width="112px"
                          class="flex-form-item">
                <el-radio-group :value="value.recordLimitType" @input="update('recordLimitType', $event)">
                    <el-radio
                        v-for="item in [{label:'跟随系统设置',value:-1},{label:'继续录制',value:2},{label:'停止录制',value:1}]"
                        :key="item.value" :label="item.value"
                        class="radio-as-checkbox">
                        {{ item.label }}
                    </el-radio>
                </el-radio-group>
            </el-form-item>
            <span class="tips"
                  v-if="!isSettingChange">停止录制后，需要手动单独开启或者手动全局开启</span>
        </div>
        <div class="flex-form">
            <el-form-item :label="showAutoAnalysis?'智能分析设置':'文案提取设置'" prop="isAutoAnalysis" label-width="112px"
                          class="flex-form-item">
                <el-radio-group :value="value.isAutoAnalysis" @input="update('isAutoAnalysis', $event)">
                    <el-radio :label="1" class="radio-as-checkbox">
                        自动{{ showAutoAnalysis ? '分析' : '提取' }}
                    </el-radio>
                    <el-radio :label="0" class="radio-as-checkbox"
                              :disabled="showAutoAnalysis?value.accountType===0:false">
                        手动{{ showAutoAnalysis ? '分析' : '提取' }}
                    </el-radio>
                </el-radio-group>
            </el-form-item>
            <template v-if="showAutoAnalysis">
                <span class="tips" v-if="!isSettingChange">同行业账号才能修改为手动分析</span>
            </template>
        </div>
        <AutoDeleteConfig
            v-model="autoDeleteConfig"
            prop="autoDeleteTime"
            label="自动删除"
            label-width="112px"
            form-item-class="flex-form-item"
            content-layout="stack"
            :content-required="false"
            tips="智能分析或提取文案完成后的视频才自动删除" />
    </div>
</template>

<script>
import FormItemAry from '@/components/formItemAry/index.vue'
import AutoDeleteConfig from '@/components/autoDeleteConfig/index.vue'
export default {
    name: 'RecordConfig',
    components:{FormItemAry, AutoDeleteConfig},
    props: {
        value: {
            type: Object,
            required: true
        },
        isSettingChange: {
            type: Boolean,
            default: false
        },
        showAutoAnalysis: {
            type: Boolean,
            default: true
        }
    },
    data() {
        return {
            segmentTimePointsArray: []
        }
    },
    computed: {
        /**
         * @description 将外层表单对象映射为自动删除组件所需结构，并在变更后同步回父级表单。
         * @returns {{autoDeleteTime: string, deleteContent: string}}
         */
        autoDeleteConfig: {
            get() {
                return {
                    autoDeleteTime: String(this.value?.autoDeleteTime ?? '-1'),
                    deleteContent: String(this.value?.deleteContent ?? '')
                }
            },
            set(val) {
                this.$emit('input', {
                    ...this.value,
                    autoDeleteTime: String(val?.autoDeleteTime ?? '-1'),
                    deleteContent: String(val?.deleteContent ?? '')
                })
            }
        }
    },
    watch: {
        'value.segmentTimePoints': {
            handler(val) {
                const nextArr = this.normalizeSegmentTimePointsToArray(val)
                if (this.isSameStringArray(nextArr, this.segmentTimePointsArray)) {
                    return
                }
                this.segmentTimePointsArray = nextArr
            },
            immediate: true
        },
        segmentTimePointsArray: {
            handler(val) {
                const nextStr = this.normalizeSegmentTimePointsToString(val)
                if (this.value.segmentTimePoints === nextStr) {
                    return
                }
                this.update('segmentTimePoints', nextStr)
            },
            deep: true
        }
    },
    methods: {
        normalizeSegmentTimePointsToArray(val) {
            if (Array.isArray(val)) {
                return val.filter(v => v)
            }
            if (typeof val === 'string') {
                return val
                    .split(',')
                    .map(v => v.trim())
                    .filter(v => v)
            }
            return []
        },
        normalizeSegmentTimePointsToString(val) {
            if (!Array.isArray(val)) {
                return ''
            }
            return val
                .filter(v => v)
                .join(',')
        },
        isSameStringArray(a = [], b = []) {
            if (a === b) {
                return true
            }
            if (!Array.isArray(a) || !Array.isArray(b)) {
                return false
            }
            if (a.length !== b.length) {
                return false
            }
            for (let i = 0; i < a.length; i++) {
                if (a[i] !== b[i]) {
                    return false
                }
            }
            return true
        },
        update(key, val) {
            this.$emit('input', {...this.value, [key]: val})
        }
    }
}
</script>

<style lang="scss" scoped>
.record-config {
    .flex-form {
        display: flex;
        align-items: center;
        justify-content: flex-start;

        .flex-form-item {
            width: auto;
        }

        .flex-form-item-set {
            margin-bottom: 2px;
        }
    }

    .tips {
        color: #F4BE34;
        font-size: 12px;
        margin-left: 12px;
    }

    ::v-deep(.el-form-item) {
        max-width: 520px;
    }

    .subsectionTime {
        width: 80px;
        margin-left: 20px;
        border-radius:28px;

        ::v-deep(.el-input-number__decrease) {
            display: none;
        }

        ::v-deep(.el-input-number__increase) {
            display: none;
        }

        ::v-deep(.el-input) {
            .el-input__inner {
                padding: 0;
                border-radius:40px;
            }
        }
    }
}
</style>
