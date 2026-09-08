<template>
    <drawer
        ref="drawer"
        class="drawer-table-box"
        width="60%"
        :title="`${isEdit ? '修改' : '新建'}提示词`"
        :visible.sync="drawerVisible"
        :wrapperClosable="false"
        :modal="false"
        :append-to-body="true"
    >
        <div class="content">
            <el-form ref="form" label-position="top" :model="form" label-width="80px" :rules="rules" size="default">
                <el-form-item label="问题概要" prop="promptTitle" required>
                    <el-input v-model.trim="form.promptTitle" placeholder="请输入问题概要，20字以内" :maxlength="20"></el-input>
                </el-form-item>
                <el-form-item v-if="showMoreConfig" class="prompt-more-config-item">
                    <template #label>
                        <div class="prompt-more-config-label">
                            <span>更多参数</span>
                            <el-button type="text" class="pd-0" @click="toggleMoreConfigExpand">
                                {{ moreConfigExpanded ? '收起配置' : '展开配置' }}
                            </el-button>
                        </div>
                    </template>
                    <div class="prompt-more-config-tip">
                        该配置仅作用于当前自建问题的打包提问，不会影响页面外部的问答配置与展示状态。
                    </div>
                    <div class="prompt-more-config-box" :class="{'is-expand': moreConfigExpanded}">
                        <moreConfigPanel
                            v-model="form.promptMoreConfig"
                            v-bind="moreConfigProps"
                            :panelMaxHeight="moreConfigExpanded ? '' : 160"
                        ></moreConfigPanel>
                    </div>
                </el-form-item>
                <el-form-item label="具体提示词" prop="promptContent" required>
                    <el-input v-model="form.promptContent" :placeholder="`请输入具体提示词，${maxLen}字以内`" type="textarea"
                                :maxlength="maxLen"
                                resize="none" :autosize="{ minRows: 19, maxRows: 19}" show-word-limit></el-input>
                </el-form-item>
                <el-form-item label="排序" prop="promptSort">
                    <el-input v-model.number="form.promptSort" placeholder="请输入序号，数字越小，排在越前面" :max="99"></el-input>
                </el-form-item>
                <el-form-item>
                    <afp-button type="primary" :plain="false" size="default"
                                @click="submitForm">确 定
                    </afp-button>
                </el-form-item>
            </el-form>
        </div>
    </drawer>
</template>

<script>
/**
 * @file 自建问题新增/编辑抽屉。
 * @description 负责维护自建问题的基础表单与独立预设配置，保存后的配置仅作用于当前提示词，不联动页面外层更多配置状态。
 */
import drawerMixin from '@/mixins/drawer.js'
import drawer from '@/components/drawer/index.vue'
import moreConfigPanel from '../input/moreConfigPanel.vue'
import { normalizeMoreConfigValue } from '../input/moreConfigShared'
export default {
    components: {
        drawer,
        moreConfigPanel
    },
    mixins: [drawerMixin],
    props:{
        moreConfigProps: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            maxLen: 30000,
            isEdit: false,
            moreConfigExpanded: false,
            form: {
                promptTitle: '',
                promptContent: '',
                promptSort: '',
                promptMoreConfig: {},
                placeholderKeys: []
            },
            rules: {
                promptTitle: {required: true, message: '问题概要不能为空'},
                promptContent: {required: true, message: '具体提示词不能为空'},
                promptSort: {
                    pattern: /^([1-9]|[1-9][0-9])$/,
                    message: '请输入数字, 不能以0开头,最大99',
                    trigger: 'blur',
                    
                },
            }
        };
    },
    computed: {
        showMoreConfig() {
            return !this.moreConfigProps?.isCompare && !this.moreConfigProps?.hideMoreConfig
        },
        getNormalizedMoreConfig(){
            return normalizeMoreConfigValue(this.form?.promptMoreConfig || {}, this.moreConfigProps)
        }
    },
    watch: {},
    methods: {
         init(item) {
            this.isEdit = false
            this.initData();
            if (item) {
                this.isEdit = true
                this.form = {
                    ...item,
                    promptMoreConfig: normalizeMoreConfigValue(item?.promptMoreConfig || {}, this.moreConfigProps)
                }
            }
        },
        initData(){
            this.form = {
                promptTitle: '',
                promptContent: '',
                promptSort: '',
                promptMoreConfig: normalizeMoreConfigValue({}, this.moreConfigProps),
                placeholderKeys: []
            }
            this.moreConfigExpanded = false
            this.$refs.form?.resetFields();
        },
        toggleMoreConfigExpand() {
            this.moreConfigExpanded = !this.moreConfigExpanded
        },
        close() {
            
            this.hide();
        },
        submitForm() {
            this.$refs.form.validate((valid) => {
                if (valid) {
                    const promptMoreConfig = this.getNormalizedMoreConfig
                    this.form.promptMoreConfig = promptMoreConfig
                    const placeholderKeyMap = {
                        ...(promptMoreConfig?.basicData || {}),
                        ...(promptMoreConfig?.dynamicConfigs || {})
                    }
                    this.form.placeholderKeys = Object.keys(placeholderKeyMap).filter((key) => {
                        return !!placeholderKeyMap?.[key]
                    })
                    this.$emit('createPromptForm', {form:this.form,next:()=>{
                        this.initData();
                        this.close();
                    }})
                }
            });
        },
        async showCallback({data}){
            this.init(data)
        },
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.content {
    width: 90%;
    margin: 0 auto;
}

.prompt-more-config-label{
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.prompt-more-config-tip{
    margin-bottom: 10px;
    color: #909399;
    font-size: 12px;
    line-height: 18px;
}

.prompt-more-config-box{
    padding: 12px;
    border: 1px solid #EBEEF5;
    border-radius: 10px;
    background: #FAFBFF;
    transition: all 0.2s ease;
}

.prompt-more-config-box.is-expand{
    box-shadow: 0 6px 18px rgba(76, 141, 255, 0.08);
}

::v-deep(.drawer-table-box){
    .el-drawer__body{
        padding: 20px 0 32px 0;
    }
    .el-form-item {
        text-align: left;
        margin-bottom: 16px;
        .el-form-item__label {
            padding: 0;
        }
    }
}
</style>
