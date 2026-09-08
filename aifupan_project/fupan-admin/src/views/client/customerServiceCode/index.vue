<template>
  <div class="customer-service-code">
    <el-form label-suffix=":" label-width="110px">
      <el-form-item label="客服二维码">
        <upload-img
            :fileList="customerImgList"
            :limit="1"
            @imgChange="imgChange"
        ></upload-img>
      </el-form-item>
      <el-form-item label="销售二维码">
        <upload-img
            :fileList="saleImgList"
            :limit="1"
            @imgChange="imgChange2"
        ></upload-img>
      </el-form-item>
    </el-form>
    <el-button style="margin-top: 10px" type="primary" @click="submit"
    >提交更改
    </el-button
    >
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/request-api'
import uploadImg from '@/components/commonComponent/uploadImg.vue'

const customerImgList = ref([])
const saleImgList = ref([])

const imgChange = (imgList) => {
  customerImgList.value = imgList
}

const imgChange2 = (imgList) => {
  saleImgList.value = imgList
}

const getCodeImg = async () => {
  customerImgList.value = []
  const [res, res2] = await Promise.all([
    api.file.showOne({resourceType: 5}),
    api.file.showOne({resourceType: 30})
  ])
  if (res.code == 0) {
    customerImgList.value = [res.data]
  }
  if (res2.code == 0) {
    saleImgList.value = [res2.data]
  }
}

const submit = async () => {
  if (!customerImgList.value || customerImgList.value.length < 1) {
    ElMessage.error('客服二维码不能为空')
    return
  }
  if (!saleImgList.value || saleImgList.value.length < 1) {
    ElMessage.error('销售二维码不能为空')
    return
  }

  let requestData = customerImgList.value[0]
  requestData.resourceType = 5
  requestData.remarks = '客服二维码图片'
  const res = await api.file.updateFile(requestData)
  if (res.code != 0) {
    ElMessage.error(res.msg)
  }

  let requestData1 = saleImgList.value[0]
  requestData1.resourceType = 30
  requestData1.remarks = '销售二维码图片'
  const res1 = await api.file.updateFile(requestData1)
  if (res1.code != 0) {
    ElMessage.error(res1.msg)
  }

  ElMessage.success('修改成功')
}

onMounted(async () => {
  await getCodeImg()
})
</script>
<style lang="less" scoped>
.customer-service-code {
  padding: 15px;
}
</style>
