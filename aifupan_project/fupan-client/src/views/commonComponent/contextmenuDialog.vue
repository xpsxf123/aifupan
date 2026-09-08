<template>
    <div>
        <el-dialog v-dialogDrag :title="!dataForm.id ? '添加词语' : '修改词语'" :visible.sync="keywordDialogVisible"
            :close-on-click-modal="false" width="456px" style="overflow: hidden" :append-to-body="true">
            <el-form :model="dataForm" :rules="dataRule" ref="dataForm" label-width="90px">
                
                <el-form-item label="词语" prop="name">
                    <el-input v-model="dataForm.name" style="width: 280px;"></el-input>
                </el-form-item>

                <el-form-item label="词库" prop="lexiconId">
                    <el-select v-model="dataForm.lexiconId" style="width: 280px;" filterable @change="lexiconChange">
                        <el-option v-for="item in lexiconList" :key="item.id"
                            :label="item.name + '(' + item.tradeName + ')'" :value="item.id">
                        </el-option>
                    </el-select>
                </el-form-item>

                <el-form-item label="类型" prop="wordsType">
                    <el-select v-model="dataForm.wordsType" style="width: 280px;">
                        <el-option v-for="item in typeList" :key="item.value" :label="item.label" :value="item.value">
                        </el-option>
                    </el-select>
                </el-form-item>

                 <el-form-item v-if="dataForm.wordsType=== 1" label="分类" prop="cruxTypeId">
                    <wordClass v-model="dataForm.cruxTypeId" @input="val=>dataForm.cruxTypeId = val" style="width: 280px;"></wordClass>
                </el-form-item>

                <el-form-item label="状态" prop="status">
                    <el-select v-model="dataForm.status" style="width: 280px;">
                        <el-option v-for="(item, index) in ['启用', '禁用']" :key="item" :label="item" :value="index">
                        </el-option>
                    </el-select>
                </el-form-item>

                <el-form-item label="备注" prop="remarks">
                    <el-input v-model="dataForm.remarks" style="width: 280px;"></el-input>
                </el-form-item>

            </el-form>

            <div style="display: flex; justify-content: center;">
                <afp-button style="margin-right: 15px; width: 110px;" @click="closeClick">取消</afp-button>
                <afp-button @click="dataFormSubmit()" type="primary" style="width: 110px;">确定</afp-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import wordClass from '@/components/wordClass'
export default {
    components: { wordClass },
    props: {
        keywordSelectedText: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            keywordDialogVisible: false,
            lexiconList: [],
            dataForm: {
                id: 0,
                resourceType: 1,
                type: '',
                platformType: 1,
                platformTypeList: [1],
                level: 0,
                name: "",
                tradeId: '',
                wordsType: 0,
                remarks: "",
                status: "",
                oldName: "",
                lexiconId: "",
                cruxTypeId: ''
            },
            typeList: [
                { value: 0, label: '敏感词' },
                { value: 1, label: '关键词' },
                { value: 2, label: '敏感词白名单' },
            ],
            dataRule: {
                name: [
                    { required: true, message: '词语不能为空', trigger: 'blur' }
                ],
                wordsType: [
                    { required: true, message: '类型不能为空', trigger: 'blur' }
                ],
                cruxTypeId: [
                    { required: true, message: '分类不能为空', trigger: 'blur' }
                ],
                status: [
                    { required: true, message: '状态不能为空', trigger: 'blur' }
                ],
                lexiconId: [
                    { required: true, message: '词库不能为空', trigger: 'blur' }
                ],
            }
        }
    },
    created() {
        this.getLexiconList();
    },
    methods: {
        lexiconChange(lexiconId) {
            this.lexiconList.forEach(item => {
                if (item.id == lexiconId) {
                    this.dataForm.tradeId = item.tradeId;
                }
            });
        },
        // 获取词库列表
        getLexiconList() {
            this.lexiconList = [];
            this.$httpBack.lexicon.list({ limit: -1 }).then((res) => {
                if (res && res.code === 0) {
                    this.lexiconList = res.data.list;
                }
            });
        },
        init() {

            this.keywordDialogVisible = true;

            this.$nextTick(() => {
                this.$refs['dataForm'].resetFields();
                this.dataForm.name = this.keywordSelectedText;
            })

        },
        dataFormSubmit() {
            this.$refs["dataForm"].validate((valid) => {
                if (valid) {
                    let requestData = JSON.parse(JSON.stringify(this.dataForm));
                    if(requestData.wordsType !== 1){
                        delete requestData.cruxTypeId
                    }
                    requestData.platformTypeList = [1];
                    if (requestData.wordsType == 0) {
                        requestData.type = 4;
                    } else if (requestData.wordsType == 1) {
                        requestData.type = 2;
                    } else if (requestData.wordsType == 2) {
                        requestData.type = 0;
                    }

                    if (this.dataForm.id) {
                        // 修改
                        this.$httpBack.sensitivewords.update(requestData).then((res) => {
                            if (res && res.code == 0) {
                                this.$message({
                                    message: "修改成功",
                                    type: "success",
                                    duration: 1500,
                                    onClose: () => {
                                        this.keywordDialogVisible = false
                                        this.$emit("refreshDataList");
                                    },
                                });
                            } else {
                                this.$message.error(res.msg);
                            }
                        });
                    } else {
                        // 新增
                        requestData.id = "";
                        this.$httpBack.sensitivewords.save(requestData).then((res) => {
                            if (res && res.code === 0) {
                                this.$message({
                                    message: "添加成功",
                                    type: 'success',
                                    duration: 1500,
                                    onClose: () => {
                                        this.keywordDialogVisible = false
                                        this.$emit("refreshDataList");
                                    },
                                });
                            } else {
                                this.$message.error(res.msg);
                            }
                        });
                    }
                }
            });
        },
        closeClick() {
            this.keywordDialogVisible = false
        }
    }
}
</script>

<style scoped>
.dialog-button {
    width: 110px;
    height: 40px;
    background-color: var(--color-main);
    color: #FFFFFF;
}

.dialog-button:hover {
    cursor: pointer;
    color: red;
}
</style>