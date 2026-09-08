<template>
  <div class="img-config">
    <el-form label-suffix=":" label-width="210px">
      <el-form-item label="H5兜底销售二维码图片">
        <upload-img
          :fileList="h5ImgList"
          @imgChange="h5ImgChange"
          :limit="1"
        ></upload-img>
      </el-form-item>
      <el-form-item label="客户端销售销售二维码图片">
        <upload-img
          :fileList="clientSaleImgList"
          @imgChange="clientSaleImgChange"
          :limit="1"
        ></upload-img>
      </el-form-item>
    </el-form>
    <el-button type="primary" style="margin-top: 10px" @click="submit"
      >提交更改</el-button
    >
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import uploadImg from '@/components/commonComponent/uploadImg.vue'
import api from '@/utils/request-api'

const h5ImgList = ref([])
const clientSaleImgList = ref([])

const submit = async () => {
  if (!h5ImgList.value || h5ImgList.value.length < 1) {
    ElMessage.error('H5兜底销售二维码图片')
    return
  }
  if (!clientSaleImgList.value || clientSaleImgList.value.length < 1) {
    ElMessage.error('客户端销售销售二维码图片')
    return
  }
  const requestData = {
    h5ImgId: h5ImgList.value[0].id,
    clientSaleImgId: clientSaleImgList.value[0].id,
  }
  const res = await api.systemkv.updateImgConfig(requestData)
  if (res.code != 0) {
    ElMessage.error(res.msg)
    return
  }
  ElMessage.success('修改成功')
}

const h5ImgChange = (imgList) => {
  h5ImgList.value = imgList
}

const clientSaleImgChange = (imgList) => {
  clientSaleImgList.value = imgList
}

const getCodeImg = async () => {
  h5ImgList.value = []
  clientSaleImgList.value = []
  const res = await api.systemkv.getImgConfig({})
  if (res.data?.h5ImgVo) {
    h5ImgList.value.push(res.data.h5ImgVo)
  }
  if (res.data?.clientSaleImgVo) {
    clientSaleImgList.value.push(res.data.clientSaleImgVo)
  }
}

onMounted(() => {
  getCodeImg()
})
</script>

<style lang="scss" scoped>
.img-config {
  padding: 15px;
}
</style>
