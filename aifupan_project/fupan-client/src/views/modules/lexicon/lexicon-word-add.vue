<template>
    <div>
        <el-dialog :title="!dataForm.id ? '添加词语' : '修改词语'" :visible.sync="keywordDialogVisible"
            :close-on-click-modal="false" width="456px">
            <el-form :model="dataForm" :rules="dataRule" ref="dataForm" label-width="90px" size="default">

                <el-form-item label="词语" prop="name">
                    <el-input v-model="dataForm.name" style="width: 280px;"></el-input>
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
                <afp-button style="margin-right: 15px; width: 110px;" @click="closeClick" size="default">取消</afp-button>
                <afp-button @click="dataFormSubmit()" type="primary" size="default" :plain="false" style="width: 110px;">确定</afp-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import wordClass from '@/components/wordClass'
export default {
    components: { wordClass },
    data() {
        return {
            keywordDialogVisible: false,
            dataForm: {
                id: 0,
                resourceType: 1,
                type: '',
                platformType: 1,
                platformTypeList: [1],
                level: 0,
                name: '',
                tradeId: '',
                tradeIdArr: '',
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
            }
        }
    },
    methods: {
        init(id, wordType, tradeId, lexiconId, tradeIdArr) {

            this.dataForm.id = id || 0;
            this.dataForm.wordsType = wordType;

            if (this.dataForm.wordsType == 0) {
                this.dataForm.type = 4;
            } else if (this.dataForm.wordsType == 1) {
                this.dataForm.type = 2;
            } else if (this.dataForm.wordsType == 2) {
                this.dataForm.type = 0;
            }
            this.dataForm.tradeId = tradeId;
            this.dataForm.tradeIdArr = tradeIdArr;
            this.dataForm.lexiconId = lexiconId;
            this.keywordDialogVisible = true;
            this.$nextTick(() => {
                this.$refs['dataForm'].resetFields();
                this.dataForm.wordsType = wordType;
                if (this.dataForm.id) {
                    this.$httpBack.sensitivewords.info({ id: this.dataForm.id }).then((data) => {
                        if (data && data.code === 0) {
                            this.dataForm = data.data;
                            this.dataForm.oldName = this.dataForm.name;
                        }
                    })
                }
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