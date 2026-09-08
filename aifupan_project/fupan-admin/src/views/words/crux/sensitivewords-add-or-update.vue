<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataFormData.id ? '新增' : '修改'"
      :width="800"
  >
    <el-form
        :key="reRender"
        ref="dataForm"
        :model="dataFormData"
        :rules="dataRule"
        label-width="100px"
    >
      <el-form-item label="分组名称" prop="groupStr">
        <el-input
            v-model="dataFormData.groupStr"
            placeholder="请输入分组名称"
        ></el-input>
      </el-form-item>
      <el-form-item label="示例词" prop="name">
        <el-input
            v-model="dataFormData.name"
            placeholder="请输入敏感词"
        ></el-input>
      </el-form-item>
      <el-form-item
          v-if="dataFormData.wordsType == 1"
          label="关键词分类"
          prop="cruxTypeId"
      >
        <el-cascader
            v-model="cruxTypeIdArr"
            :options="cruxTypeTreeList"
            :props="{ checkStrictly: true, value: 'id', label: 'name' }"
            style="width: 100%"
            @change="parentCruxTypeChange"
        ></el-cascader>
      </el-form-item>
      <el-form-item
          v-if="dataFormData.wordsType == 0"
          label="词类型"
          prop="type"
      >
        <el-select v-model="dataFormData.type" placeholder="请选择">
          <el-option
              v-for="item in typeList"
              :key="item.id"
              :label="item.label"
              :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <!-- <el-form-item label="平台类型" prop="platformTypeList" v-if="!dataForm.id">
                <el-checkbox-group v-model="dataForm.platformTypeList">
                    <el-checkbox v-for="item in platformList" :key="item.id" :label="item.value">{{ item.label
                        }}</el-checkbox>
                </el-checkbox-group>
            </el-form-item> -->
      <el-form-item label="平台类型" prop="platformType">
        <el-select v-model="dataFormData.platformType" placeholder="请选择">
          <el-option
              v-for="item in platformList"
              :key="item.id"
              :label="item.label"
              :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item
          v-if="dataFormData.wordsType == 0"
          label="示例词等级"
          prop="level"
      >
        <el-select v-model="dataFormData.level" placeholder="请选择">
          <el-option
              v-for="item in LevelList"
              :key="item.id"
              :label="item.label"
              :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="行业" prop="tradeId">
        <el-cascader
            :key="cascaderNum"
            v-model="dataFormData.tradeIdArr"
            :options="tradeTreeList"
            :props="{ checkStrictly: true, value: 'id', label: 'name' }"
            clearable
            filterable
            placeholder="请选择"
            style="width: 100%"
            @change="tradeChange"
        >
        </el-cascader>
      </el-form-item>
      <el-form-item label="概览" prop="overView">
        <el-input
            v-model="dataFormData.overView"
            placeholder="请输入概览"
        ></el-input>
      </el-form-item>
      <el-form-item label="描述" prop="remarks">
        <el-input
            v-model="dataFormData.remarks"
            placeholder="请输入描述"
        ></el-input>
      </el-form-item>
      <el-form-item label="示例词规则">
        <el-button
            size="small"
            type="primary"
            @click="
            showRuleList(dataFormData.restrictRuleList, 2, dataFormData.name)
          "
        >限定词{{
            '(' +
            (dataFormData.restrictRuleList
                ? dataFormData.restrictRuleList.length
                : 0) +
            ')'
          }}
        </el-button>
        <el-button
            size="small"
            type="primary"
            @click="
            showRuleList(dataFormData.blackRuleList, 0, dataFormData.name)
          "
        >黑名单{{
            '(' +
            (dataFormData.blackRuleList
                ? dataFormData.blackRuleList.length
                : 0) +
            ')'
          }}
        </el-button>
        <el-button
            size="small"
            type="primary"
            @click="
            showRuleList(dataFormData.whiteRuleList, 1, dataFormData.name)
          "
        >白名单{{
            '(' +
            (dataFormData.whiteRuleList
                ? dataFormData.whiteRuleList.length
                : 0) +
            ')'
          }}
        </el-button>
      </el-form-item>
      <el-form-item :label="dataFormData.wordsType == 0 ? '敏感词' : '关键词'">
        <div
            v-if="
            dataFormData.similarWordList &&
            dataFormData.similarWordList.length > 0
          "
        >
          <div class="similarWordTitleContainer">
            <div class="similarWordTitleItem1">词语</div>
            <div
                v-if="dataFormData.wordsType == 0"
                class="similarWordTitleItem1"
            >
              等级
            </div>
            <div class="similarWordTitleItem2">描述</div>
            <div class="similarWordTitleItem3">操作</div>
          </div>
          <div
              v-for="(item, index) in dataFormData.similarWordList"
              :key="`similar-${index}`"
              class="similarWordTrContainer"
          >
            <div class="similarWordTd1">
              <el-input
                  v-model="dataFormData.similarWordList[index].name"
              ></el-input>
            </div>
            <div v-if="dataFormData.wordsType == 0" class="similarWordTd1">
              <el-select
                  v-model="dataFormData.similarWordList[index].level"
                  placeholder="请选择"
              >
                <el-option
                    v-for="item in LevelList"
                    :key="item.id"
                    :label="item.label"
                    :value="item.value"
                >
                </el-option>
              </el-select>
            </div>
            <div class="similarWordTd2">
              <el-input
                  v-model="dataFormData.similarWordList[index].remarks"
              ></el-input>
            </div>
            <div class="similarWordTd3">
              <span
                  style="color: green; margin-right: 10px; cursor: pointer"
                  @click="
                  showRuleList(
                    dataFormData.similarWordList[index].restrictRuleList,
                    2,
                    dataFormData.similarWordList[index].name
                  )
                "
              >限定词{{
                  '(' +
                  dataFormData.similarWordList[index].restrictRuleList.length +
                  ')'
                }}</span
              >
              <span
                  style="color: green; margin-right: 10px; cursor: pointer"
                  @click="
                  showRuleList(
                    dataFormData.similarWordList[index].blackRuleList,
                    0,
                    dataFormData.similarWordList[index].name
                  )
                "
              >黑名单{{
                  '(' +
                  dataFormData.similarWordList[index].blackRuleList.length +
                  ')'
                }}</span
              >
              <span
                  style="color: green; margin-right: 10px; cursor: pointer"
                  @click="
                  showRuleList(
                    dataFormData.similarWordList[index].whiteRuleList,
                    1,
                    dataFormData.similarWordList[index].name
                  )
                "
              >白名单{{
                  '(' +
                  dataFormData.similarWordList[index].whiteRuleList.length +
                  ')'
                }}</span
              >
              <!-- v-if="index == dataFormData.similarWordList.length - 1">添加</span> -->
              <span
                  style="color: blue; cursor: pointer"
                  @click="addSimilar(index)"
              >添加</span
              >
              <span
                  style="color: red; margin-left: 10px; cursor: pointer"
                  @click="removeSimilar(index)"
              >删除</span
              >
            </div>
          </div>
        </div>
        <div v-else>
          <el-button size="small" type="primary" @click="addSimilar"
          >添加相似词
          </el-button>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
      </span>
    </template>

    <rule-list
        v-if="ruleListVisible"
        ref="ruleListDialog"
        @reRender="reRender = !reRender"
    ></rule-list>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import api from '@/utils/request-api'
import RuleList from '@/views/words/component/ruleList.vue'

const emit = defineEmits(['refreshDataList'])

const cruxTypeIdArr = ref([]) // 选中的关键词分类id数组
const cruxTypeTreeList = ref([]) // 关键词分类列表（树形）
const reRender = ref(false)
const ruleListVisible = ref(false)
const visible = ref(false)
const tradeTreeList = ref([]) // 行业列表
const platformList = ref([]) // 平台列表
const resourceList = ref([]) // 来源列表
const typeList = ref([]) // 类型列表
const LevelList = ref([]) // 等级列表
const oldName = ref('')
const cascaderNum = ref(0)
const dataForm = ref(null)
const ruleListDialog = ref(null)

const dataFormData = reactive({
  id: 0,
  resourceType: 0,
  type: 0,
  platformType: 1,
  platformTypeList: [],
  ruleList: [],
  blackRuleList: [],
  whiteRuleList: [],
  restrictRuleList: [],
  level: '',
  name: '',
  tradeId: '',
  tradeIdArr: [],
  similarWords: '',
  createDate: '',
  updateDate: '',
  isDeleted: '',
  groupStr: '',
  wordsType: 0,
  similarWordList: [],
  overView: '',
  remarks: '',
  cruxTypeId: 0
})

const dataRule = reactive({
  type: [{required: true, message: '敏感词类型不能为空', trigger: 'blur'}],
  platformTypeList: [
    {required: true, message: '平台类型不能为空', trigger: 'blur'}
  ],
  platformType: [
    {required: true, message: '平台类型不能为空', trigger: 'blur'}
  ],
  level: [{required: true, message: '敏感词等级不能为空', trigger: 'blur'}],
  name: [{required: true, message: '敏感词不能为空', trigger: 'blur'}],
  tradeId: [{required: true, message: '行业不能为空', trigger: 'blur'}],
  cruxTypeId: [
    {required: true, message: '关键词分类不能为空', trigger: 'blur'}
  ],
  overView: [{max: 6, message: '概览不能超过6个字符', trigger: 'blur'}]
})
const parentCruxTypeChange = (cruxTypeIdArrParam) => {
  dataFormData.cruxTypeId = cruxTypeIdArrParam[cruxTypeIdArrParam.length - 1]
}

const getCruxTypeTreeList = () => {
  cruxTypeTreeList.value = []
  api.cruxtype.listTree({childrenNotNull: 0}).then((res) => {
    if (res && res.code === 0 && res.data) {
      cruxTypeTreeList.value = res.data
    }
  })
}
const showRuleList = (ruleList, blackOrWhite, wordName) => {
  ruleListVisible.value = true
  nextTick(() => {
    ruleListDialog.value.init(ruleList, blackOrWhite, wordName)
  })
}

const removeSimilar = (index) => {
  dataFormData.similarWordList.splice(index, 1)
}

const addSimilar = (index) => {
  if (!dataFormData.similarWordList) {
    dataFormData.similarWordList = []
  }
  dataFormData.similarWordList.splice(index + 1, 0, {
    name: '',
    remarks: '',
    ruleList: [],
    blackRuleList: [],
    whiteRuleList: [],
    restrictRuleList: [],
    level: ''
  })
}
const tradeChange = (value) => {
  if (value && value.length > 0) {
    dataFormData.tradeId = value[value.length - 1]
  } else {
    dataFormData.tradeId = ''
  }
}

const getTradeTreeList = () => {
  tradeTreeList.value = []
  api.trade.listTree({}).then((res) => {
    if (res && res.code === 0) {
      tradeTreeList.value = res.data
      cascaderNum.value++
    }
  })
}

const getPlatformList = () => {
  platformList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'words_platform_type'})
      .then((res) => {
        if (res.code == 0) {
          platformList.value = res.data.list
          platformList.value.forEach((item) => {
            item.value = parseInt(item.value)
          })
        }
      })
}
const getResourceList = () => {
  resourceList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'words_resource_type'})
      .then((res) => {
        if (res.code == 0) {
          resourceList.value = res.data.list
        }
      })
}

const getTypeList = () => {
  typeList.value = []
  let typeLogo = ''
  if (dataFormData.wordsType === 0) {
    // 敏感词
    typeLogo = 'sensitive_words_type'
  } else {
    // 关键词
    typeLogo = 'crux_words_type'
  }
  api.dictdata.list({limit: -1, typeLogo}).then((res) => {
    if (res.code == 0) {
      typeList.value = res.data.list
      typeList.value.forEach((item) => {
        item.value = parseInt(item.value)
      })
    }
  })
}

const getLevelList = () => {
  LevelList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'sensitive_words_level_type'})
      .then((res) => {
        if (res.code == 0) {
          LevelList.value = res.data.list
          LevelList.value.forEach((item) => {
            item.value = parseInt(item.value)
          })
        }
      })
}
const init = (id, tradeIdArr, wordsType) => {
  dataFormData.wordsType = wordsType
  getCruxTypeTreeList()
  getTradeTreeList()
  getPlatformList()
  getResourceList()
  getTypeList()
  getLevelList()
  dataFormData.id = id || 0
  cascaderNum.value = 0
  dataFormData.platformTypeList = []
  dataFormData.similarWordList = []
  dataFormData.tradeIdArr = []
  dataFormData.blackRuleList = []
  dataFormData.whiteRuleList = []
  dataFormData.restrictRuleList = []
  cruxTypeIdArr.value = []
  visible.value = true
  nextTick(() => {
    console.log('dataForm', dataForm)

    dataForm.value.resetFields()

    dataFormData.tradeIdArr = tradeIdArr
    if (tradeIdArr && tradeIdArr.length > 0) {
      dataFormData.tradeId = tradeIdArr[tradeIdArr.length - 1]
    }

    if (dataFormData.id) {
      api.sensitivewords.info({id: dataFormData.id}).then((res) => {
        if (res && res.code === 0) {
          Object.assign(dataFormData, res.data)
          oldName.value = dataFormData.name
          if (dataFormData.tradeIdArr) {
            dataFormData.tradeIdArr = JSON.parse(dataFormData.tradeIdArr)
          }
          if (res.data.cruxTypeIdArr && res.data.cruxTypeIdArr.length > 0) {
            cruxTypeIdArr.value = res.data.cruxTypeIdArr
          }
          dataFormData.platformTypeList = [dataFormData.platformType]
          // 拆解黑/白名单规则
          if (!dataFormData.ruleList || dataFormData.ruleList.length < 1) {
            dataFormData.ruleList = []
            dataFormData.blackRuleList = []
            dataFormData.whiteRuleList = []
            dataFormData.restrictRuleList = []
          } else {
            dataFormData.blackRuleList = []
            dataFormData.whiteRuleList = []
            dataFormData.restrictRuleList = []
            dataFormData.ruleList.forEach((item) => {
              if (item.blackOrWhite === 0) {
                dataFormData.blackRuleList.push(item)
              } else if (item.blackOrWhite === 1) {
                dataFormData.whiteRuleList.push(item)
              } else if (item.blackOrWhite === 2) {
                dataFormData.restrictRuleList.push(item)
              }
            })
          }
          // 拆解相似词黑/白名单规则
          if (
              dataFormData.similarWordList &&
              dataFormData.similarWordList.length > 0
          ) {
            dataFormData.similarWordList.forEach((item) => {
              item.blackRuleList = []
              item.whiteRuleList = []
              item.restrictRuleList = []
              item.ruleList.forEach((ruleItem) => {
                if (ruleItem.blackOrWhite === 0) {
                  item.blackRuleList.push(ruleItem)
                } else if (ruleItem.blackOrWhite === 1) {
                  item.whiteRuleList.push(ruleItem)
                } else if (ruleItem.blackOrWhite === 2) {
                  item.restrictRuleList.push(ruleItem)
                }
              })
            })
          }
        }
      })
    }
  })
}

const dataFormSubmit = () => {
  dataForm.value.validate((valid) => {
    if (valid) {
      let requestDate = JSON.parse(JSON.stringify(dataFormData))

      if (requestDate.similarWordList) {
        let submitFlag = true
        requestDate.similarWordList.forEach((item) => {
          if (!item.name) {
            submitFlag = false
          }
        })
        if (!submitFlag) {
          ElMessage.error('相似词不能为空')
          return
        }
      }

      // 组装规则
      requestDate.ruleList = []
      if (requestDate.blackRuleList && requestDate.blackRuleList.length > 0) {
        requestDate.ruleList = requestDate.ruleList.concat(
            requestDate.blackRuleList
        )
      }
      if (requestDate.whiteRuleList && requestDate.whiteRuleList.length > 0) {
        requestDate.ruleList = requestDate.ruleList.concat(
            requestDate.whiteRuleList
        )
      }
      if (
          requestDate.restrictRuleList &&
          requestDate.restrictRuleList.length > 0
      ) {
        requestDate.ruleList = requestDate.ruleList.concat(
            requestDate.restrictRuleList
        )
      }
      if (
          requestDate.similarWordList &&
          requestDate.similarWordList.length > 0
      ) {
        requestDate.similarWordList.forEach((item) => {
          item.ruleList = []
          if (item.blackRuleList && item.blackRuleList.length > 0) {
            item.ruleList = item.ruleList.concat(item.blackRuleList)
          }
          if (item.whiteRuleList && item.whiteRuleList.length > 0) {
            item.ruleList = item.ruleList.concat(item.whiteRuleList)
          }
          if (item.restrictRuleList && item.restrictRuleList.length > 0) {
            item.ruleList = item.ruleList.concat(item.restrictRuleList)
          }
        })
      }

      requestDate.tradeIdArr = JSON.stringify(requestDate.tradeIdArr)
      requestDate.platformTypeList = [requestDate.platformType]

      if (dataFormData.id) {
        // 修改
        requestDate.oldName = oldName.value
        api.sensitivewords.update(requestDate).then((res) => {
          if (res && res.code === 0) {
            if (res.msg && res.msg.length < 5) {
              ElMessage({
                message: res.msg,
                type: 'success'
              })
            } else {
              ElMessage({
                message: res.msg,
                type: 'success'
              })
            }

            visible.value = false
            emit('refreshDataList')
          }
        })
      } else {
        // 新增
        requestDate.id = ''
        api.sensitivewords.save(requestDate).then((res) => {
          if (res && res.code === 0) {
            if (res.data && res.data.length > 0) {
              ElMessage({
                message: JSON.stringify(res.data),
                type: 'success'
              })
            } else {
              ElMessage({
                message: '添加成功',
                type: 'success'
              })
            }
            visible.value = false
            emit('refreshDataList')
          }
        })
      }
    }
  })
}

defineExpose({
  init
})
</script>

<style scoped>
.similarWordTd3 {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1.5;
  box-sizing: border-box;
  border: 0.5px solid #ccc;
}

.similarWordTd2 {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1.5;
  box-sizing: border-box;
  border: 0.5px solid #ccc;
}

.similarWordTd1 {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  box-sizing: border-box;
  border: 0.5px solid #ccc;
}

.similarWordTrContainer {
  display: flex;
  align-items: center;
  width: 100%;
}

.similarWordTitleItem3 {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1.5;
  background-color: rgb(177, 250, 177);
  font-size: 16px;
}

.similarWordTitleItem2 {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1.5;
  background-color: rgb(177, 250, 177);
  font-size: 16px;
  box-sizing: border-box;
  border-right: 0.5px solid #ccc;
}

.similarWordTitleItem1 {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  background-color: rgb(177, 250, 177);
  font-size: 16px;
  box-sizing: border-box;
  border-right: 0.5px solid #ccc;
}

.similarWordTitleContainer {
  display: flex;
  align-items: center;
  width: 100%;
}

.el-select {
  width: 100%;
}

.el-tag + .el-tag {
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
