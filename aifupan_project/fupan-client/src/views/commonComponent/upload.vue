<template>
  <div>
    <el-upload
      :action="uploadUrl"
      :data="uploadData"
      :headers="uploadHeader"
      :file-list="fileListInner"
      :limit="limit"
      list-type="picture-card"
      :on-success="uploadSuccessHandle"
      :on-remove="uploadRemoveHandle"
      :on-preview="handlePictureCardPreview"
    >
      <i class="el-icon-plus avatar-uploader-icon"></i>
    </el-upload>

    <el-dialog :visible.sync="bigImgVisible" append-to-body>
      <img width="100%" :src="bigImgUrl" alt="" />
    </el-dialog>
  </div>
</template>

<script>
export default {
  data() {
    return {
      bigImgVisible: false,
      bigImgUrl: "",
      fileListInner: [],
    };
  },
  watch: {
    fileList(newVal, oldVal) {
      this.fileListInner = newVal;
    },
  },
  props: {
    fileList: Array,
    limit: {
      type: Number,
      default: 9,
    },
    flag: {
      type: Number,
      default: 0,
    },
  },
  methods: {
    // 图片上传成功的回调
    uploadSuccessHandle(response) {
      this.fileListInner.push({
        id: response.data.imgId,
        name: response.data.imgName,
        url: response.data.showImgUrl,
      });
      this.$emit("imgChange", this.fileListInner);
    },
    uploadRemoveHandle(file, fileList) {
      this.fileListInner = fileList;
      this.$emit("imgChange", this.fileListInner);
    },
    handlePictureCardPreview(file) {
      this.bigImgUrl = file.url;
      this.bigImgVisible = true;
    },
  },
  computed: {
    // 上传地址
    uploadUrl() {
      return this.$http.common.upload;
    },
    // 上传带的参数
    uploadData() {
      return {
        flag: this.flag,
      };
    },
    // 上传的请求头
    uploadHeader() {
      return {
        token: this.$store.state.token,
      };
    },
  },
};
</script>

<style>
</style>