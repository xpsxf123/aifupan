<template>
    <el-dialog title="批量添加" :close-on-click-modal="false" v-model="visible">
        <el-form :model="dataForm" :rules="dataRule" ref="dataFormRef" label-width="100px">

            <el-form-item label="关键词类型" prop="type">
                <el-select v-model="dataForm.type" placeholder="请选择">
                    <el-option v-for="item in typeList" :key="item.id" :label="item.label" :value="item.value">
                    </el-option>
                </el-select>
            </el-form-item>
            <!-- <el-form-item label="平台类型" prop="platformTypeList">
                <el-checkbox-group v-model="dataForm.platformTypeList">
                    <el-checkbox v-for="item in platformList" :key="item.id" :label="item.value">{{ item.label
                        }}</el-checkbox>
                </el-checkbox-group>
            </el-form-item> -->
            <el-form-item label="平台类型" prop="platformType">
                <el-select v-model="dataForm.platformType" placeholder="请选择">
                    <el-option v-for="item in platformList" :key="item.id" :label="item.label" :value="item.value">
                    </el-option>
                </el-select>
            </el-form-item>
            <el-form-item label="行业" prop="tradeId">
                <el-cascader v-model="dataForm.tradeIdArr" :options="tradeTreeList" :key="cascaderNum"
                    :props="{ checkStrictly: true, value: 'id', label: 'name' }" clearable filterable
                    @change="tradeChange" placeholder="请选择" style="width:100%">
                </el-cascader>
            </el-form-item>
            <el-form-item label="备注" prop="groupStr">
                <el-input v-model="dataForm.groupStr" placeholder="请输入备注"></el-input>
            </el-form-item>
            <el-form-item label="描述" prop="remarks">
                <el-input v-model="dataForm.remarks" placeholder="请输入描述"></el-input>
            </el-form-item>
            <el-form-item label="关键词" prop="words">
                <el-input type="textarea" :rows="16" v-model="dataForm.words"
                    placeholder="每行一个组，如：词1_词2_词3，第一个是关键词，后面的是相似词"></el-input>
            </el-form-item>
        </el-form>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="visible = false">取消</el-button>
                <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
            </span>
        </template>
    </el-dialog>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import api from '@/utils/request-api'

// 响应式数据
const visible = ref(false)
const tradeTreeList = ref([]) // 行业列表
const platformList = ref([]) // 平台列表
const typeList = ref([]) // 类型列表
const cascaderNum = ref(0)
const dataForm = reactive({
    id: 0,
    resourceType: 0,
    type: '',
    platformType: '',
    tradeId: '',
    words: "",
    tradeIdArr: [],
    platformTypeList: [],
    groupStr: "",
    wordsType: 1,
    remarks: "",
})

const dataRule = reactive({
    type: [
        { required: true, message: '关键词类型不能为空', trigger: 'blur' }
    ],
    platformType: [
        { required: true, message: '平台类型不能为空', trigger: 'blur' }
    ],
    platformTypeList: [
        { required: true, message: '平台类型不能为空', trigger: 'blur' }
    ],
    words: [
        { required: true, message: '内容不能为空', trigger: 'blur' }
    ],
    tradeId: [
        { required: true, message: '行业不能为空', trigger: 'blur' }
    ],
})

// 表单引用
const dataFormRef = ref(null)

// 定义 emits
const emit = defineEmits(['refreshDataList'])
// 选中行业回调
const tradeChange = (value) => {
    if (value && value.length > 0) {
        dataForm.tradeId = value[value.length - 1]
    } else {
        dataForm.tradeId = ""
    }
}

// 获取行业列表树形
const getTradeTreeList = async () => {
    try {
        tradeTreeList.value = []
        const res = await api.trade.listTree({})
        if (res && res.code === 0) {
            tradeTreeList.value = res.data
            cascaderNum.value++
        }
    } catch (error) {
        console.error('获取行业列表失败:', error)
    }
}

// 获取平台列表
const getPlatformList = async () => {
    try {
        platformList.value = []
        const res = await api.dictdata.list({ limit: -1, typeLogo: "words_platform_type" })
        if (res.code == 0) {
            platformList.value = res.data.list
            platformList.value.forEach(item => {
                item.value = parseInt(item.value)
            })
        }
    } catch (error) {
        console.error('获取平台列表失败:', error)
    }
}

// 获取关键词类型列表
const getTypeList = async () => {
    try {
        typeList.value = []
        const res = await api.dictdata.list({ limit: -1, typeLogo: "crux_words_type" })
        if (res.code == 0) {
            typeList.value = res.data.list
            typeList.value.forEach(item => {
                item.value = parseInt(item.value)
            })
        }
    } catch (error) {
        console.error('获取关键词类型列表失败:', error)
    }
}
// 初始化
const init = (tradeIdArr) => {
    getTradeTreeList()
    getTypeList()
    getPlatformList()
    dataForm.platformTypeList = []
    visible.value = true
    nextTick(() => {
        dataFormRef.value?.resetFields()
        dataForm.tradeIdArr = tradeIdArr
        if (tradeIdArr && tradeIdArr.length > 0) {
            dataForm.tradeId = tradeIdArr[tradeIdArr.length - 1]
        }
    })
}

// 表单提交
const dataFormSubmit = () => {
    dataFormRef.value?.validate(async (valid) => {
        if (valid) {
            try {
                let requestData = JSON.parse(JSON.stringify(dataForm))
                requestData.platformTypeList = [requestData.platformType]

                requestData.words = requestData.words.replaceAll(" ", "")
                if (!requestData.words) {
                    ElMessage.error("内容不能为空")
                    return
                }
                requestData.wordsList = []
                // 分割行
                let rowArr = requestData.words.split("\n")
                if (!rowArr || rowArr.length < 1) {
                    ElMessage.error("内容不能为空")
                    return
                }
                rowArr.forEach((item) => {
                    // 分割列
                    let colArr = item.split("_")
                    if (!colArr || colArr.length < 1) {
                        ElMessage.error("内容格式异常")
                        return
                    }

                    // 添加关键词
                    let obj = {
                        words: colArr[0],
                        similarWords: [],
                    }

                    // 添加相似词
                    if (colArr.length > 1) {
                        for (let i = 1; i < colArr.length; i++) {
                            obj.similarWords.push(colArr[i])
                        }
                    }

                    requestData.wordsList.push(obj)
                })
                requestData.tradeIdArr = JSON.stringify(requestData.tradeIdArr)

                const res = await api.sensitivewords.saveBatch(requestData)
                if (res && res.code === 0) {
                    if (res.data && res.data.length > 0) {
                        ElMessage({
                            message: JSON.stringify(res.data),
                            type: "success",
                            duration: 60000,
                            showClose: true,
                        })
                    } else {
                        ElMessage({
                            message: "添加成功",
                            type: "success",
                            duration: 1500,
                            showClose: true,
                        })
                    }
                    visible.value = false
                    emit("refreshDataList")
                } else {
                    ElMessage.error(res.msg)
                }
            } catch (error) {
                console.error('提交失败:', error)
                 ElMessage.error('提交失败，请重试')
             }
         }
     })
 }

 // 暴露组件方法
defineExpose({
    init
})
</script>

<style scoped>
.el-select {
    width: 100%;
}

.el-tag+.el-tag {
    margin-left: 10px;
}

.button-new-tag {
    margin-left: 10px;
    height: 32px;
    line-height: 30px;
    padding-top: 0;
    padding-bottom: 0;
}

.input-new-tag {
    width: 90px;
    margin-left: 10px;
    vertical-align: bottom;
}
</style>