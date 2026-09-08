<template>
    <div class="create-prompt">
        <el-dialog
            :close-on-click-modal="false"
            :close-on-press-escape="false"
            title="新建提示词"
            :visible.sync="dialogVisible"
            custom-class="create-prompt-dialog"
            width="30%">
            <template #title>
                <div class="font-s16 text-left title">{{ isEdit ? '修改' : '新建' }}提示词</div>
            </template>
            <div class="content">
                <el-form ref="form" label-position="top" :model="form" label-width="80px" :rules="rules" size="default">
                    <el-form-item label="问题概要" prop="promptTitle" required>
                        <el-input v-model.trim="form.promptTitle" placeholder="请输入问题概要，10字以内" :maxlength="10"></el-input>
                    </el-form-item>
                    <el-form-item label="具体提示词" prop="promptContent" required>
                        <el-input v-model.trim="form.promptContent" placeholder="请输入具体提示词，300字以内" type="textarea"
                                  :maxlength="15000"
                                  resize="none" :autosize="{ minRows: 4, maxRows: 6}" show-word-limit></el-input>
                    </el-form-item>
                    <el-form-item label="排序" prop="promptSort">
                        <el-input v-model.number="form.promptSort" placeholder="请输入序号，数字越小，排在越前面" :max="99"></el-input>
                    </el-form-item>
                    <el-form-item style="text-align: center;margin-top: 34px">
                        <afp-button type="primary" :plain="false" size="default"
                                    @click="submitForm">确 定
                        </afp-button>
                    </el-form-item>
                </el-form>
            </div>
        </el-dialog>
    </div>
</template>

<script>
export default {
    components: {},
    props: {},
    data() {
        return {
            dialogVisible: false,
            isEdit: false,
            form: {
                promptTitle: '',
                promptContent: '',
                promptSort: '',
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
    computed: {},
    watch: {
        dialogVisible: {
            handler(val) {
                if (!val) {
                    this.close()
                }
            },
            deep: true,
            immediate: true
        }
    },
    methods: {
        init(item) {
            this.close()
            this.isEdit = false
            this.dialogVisible = true
            if (item) {
                this.isEdit = true
                this.form = {...item}
            }
        },
        close() {
            this.form = {
                promptTitle: '',
                promptContent: '',
                promptSort: '',
            }
            this.$refs.form?.resetFields();
        },
        submitForm() {
            this.$refs.form.validate((valid) => {
                if (valid) {
                    this.$emit('createPromptForm', {...this.form})
                    this.close()
                    this.dialogVisible = false
                }
            });
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
.create-prompt {
    ::v-deep(.create-prompt-dialog) {
        border-radius: 10px;

        .el-dialog__header {
            padding: 12px 32px;
            background: #F4F9FF;
        }

        .el-dialog__headerbtn {
            top: 10px;
        }

        .el-dialog__body {
            padding: 20px 32px 32px 32px;
        }

        .el-dialog__footer {
            text-align: center;
        }

        .content {
            width: 90%;
            margin: 0 auto;
        }

        .el-form-item {
            text-align: left;
            margin-bottom: 16px;

            .el-form-item__label {
                padding: 0;
            }
        }
    }
}
</style>