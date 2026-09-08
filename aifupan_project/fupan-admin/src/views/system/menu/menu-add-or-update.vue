<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataForm.id ? '新增' : '修改'"
      :width="550"
      custom-class="my-dialog-class"
      @close="handelClose"
  >
    <el-form
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRule"
        label-width="80px"
    >
      <el-form-item label="类型" prop="type">
        <el-radio-group
            v-model="dataForm.type"
            size="default"
            @change="handleChange"
        >
          <el-radio :label="0" border>📜菜单</el-radio>
          <el-radio :label="1" border>📌功能</el-radio>
          <el-radio :label="2" border>📁目录</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="mainName" prop="name">
        <el-input v-model="dataForm.name" :placeholder="mainName"></el-input>
      </el-form-item>
      <el-form-item :label="mainUrlName" prop="url">
        <el-input v-model="dataForm.url" :placeholder="mainUrlName"></el-input>
      </el-form-item>
      <el-form-item v-if="dataForm.type !== 1" label="排序" prop="sort">
        <el-input v-model="dataForm.sort" placeholder="排序"></el-input>
      </el-form-item>
      <el-form-item v-if="dataForm.type !== 1" label="图标" prop="img">
        <el-popover
            v-model:visible="iconListVisible"
            :show-arrow="false"
            placement="bottom-start"
            popper-class="icon-popper"
            trigger="click"
        >
          <template #reference>
            <el-input
                v-model="dataForm.img"
                clearable
                placeholder="请选择图标"
                readonly
            >
              <template #suffix>
                <i
                    v-if="dataForm.img"
                    class="el-icon-circle-close el-input__icon"
                    @click.stop="clearIconHandler()"
                ></i>
              </template>
            </el-input>
          </template>
          <div class="iconContainer">
            <div
                v-for="(item, index) in iconList"
                :key="index"
                class="iconItem"
                @click="iconListCurrentChangeHandle(item)"
            >
              <SvgIcon :name="item"/>
            </div>
          </div>
        </el-popover>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="dataFormSubmit">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { customIcons } from '@/utils/icon.js'
import api from '@/utils/request-api'

const emit = defineEmits(['refreshDataList'])

const visible = ref(false)
const iconListVisible = ref(false)
const iconList = ref([])
const dataFormRef = ref(null)

const dataForm = ref({
  id: 0,
  parentId: '',
  name: '',
  url: '',
  type: 0,
  sort: '',
  img: '',
  createDate: '',
  updateDate: '',
  isDeleted: ''
})

const dataRule = ref({
  name: [{required: true, message: '值不能为空', trigger: 'blur'}],
  url: [{required: true, message: '值不能为空', trigger: 'blur'}],
  type: [
    {
      required: true,
      message: '类型不能为空',
      trigger: 'blur'
    }
  ],
  sort: [{required: true, message: '值不能为空', trigger: 'blur'}]
})

const mainNameOrNameMap = ref({
  0: {
    label: '菜单名',
    urlName: '菜单URL'
  },
  1: {
    label: '功能名',
    urlName: '接口API'
  },
  2: {
    label: '目录名',
    urlName: '目录URL'
  }
})

const mainName = computed(() => {
  return mainNameOrNameMap.value[dataForm.value.type].label
})

const mainUrlName = computed(() => {
  return mainNameOrNameMap.value[dataForm.value.type].urlName
})
const iconListCurrentChangeHandle = (icon) => {
  dataForm.value.img = icon
  iconListVisible.value = false
}

const clearIconHandler = () => {
  dataForm.value.img = ''
}

const init = (id, parentId) => {
  iconList.value = customIcons
  dataForm.value.parentId = parentId
  dataForm.value.id = id || 0
  visible.value = true
  nextTick(() => {
    dataFormRef.value.resetFields()
    if (dataForm.value.id) {
      api.menu.info({id: dataForm.value.id}).then((data) => {
        if (data && data.code === 0) {
          dataForm.value = data.data
        }
      })
    }
  })
}

const dataFormSubmit = () => {
  dataFormRef.value.validate((valid) => {
    if (valid) {
      let requestDate = JSON.parse(JSON.stringify(dataForm.value))
      // 如果是功能把排序置为1
      if (requestDate.type === 1) {
        requestDate.sort = 1
      }
      if (dataForm.value.id) {
        // 修改
        api.menu.update(requestDate).then((res) => {
          if (res && res.code === 0) {
            visible.value = false
            emit('refreshDataList')
            /*            ElMessage({
                          message: '修改成功，需重新登录以生效。',
                          type: 'success'
                        })*/
            ElNotification({
              title: '操作成功',
              type: 'warning',
              message: '修改已完成，请重新登录以使更改生效。',
              duration: 0
            })
          }
        })
      } else {
        // 新增
        requestDate.id = ''
        api.menu.save(requestDate).then((res) => {
          if (res && res.code === 0) {
            visible.value = false
            emit('refreshDataList')
            /*            ElMessage({
                          message: '新增成功，需重新登录以生效。',
                          type: 'success'
                        })*/
            ElNotification({
              title: '操作成功',
              type: 'warning',
              message: '新增已完成，请重新登录以使更改生效。',
              duration: 0
            })
          }
        })
      }
    }
  })
}

const handleChange = (val) => {
  nextTick(() => {
    dataFormRef.value.resetFields()
    dataForm.value.img = null
    dataForm.value.sort = null
    dataForm.value.type = val
  })
}
const handelClose = () => {
  dataFormRef.value.resetFields()
}
defineExpose({
  init
})
</script>
<style lang="scss" scoped>
.iconContainer {
  width: 460px;
  max-height: 260px;
  overflow-x: hidden;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
}

.iconItem {
  font-size: 32px;
  margin-right: 8px;
  cursor: pointer;
  padding: 5px;

  &:hover {
    background-color: #4e73df;
  }
}

:deep(.my-dialog-class) {
  min-width: 510px;
}
</style>
