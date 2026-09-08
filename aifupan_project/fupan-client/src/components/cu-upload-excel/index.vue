<template>
  <el-dialog :visible.sync="visible" title="文件批量上传">
    <el-upload
      drag
      :limit="limitNum"
      :auto-upload="false"
      accept=".xlsx, .xlsm, .xls"
      :action="UploadUrl()"
      :before-upload="beforeUploadFile"
      :on-change="fileChange"
      :on-exceed="exceedFile"
      :on-success="handleSuccess"
      :on-error="handleError"
      :file-list="fileList"
    >
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <div class="el-upload__tip" slot="tip">只能上传xlsx文件，且不超过10M</div>
    </el-upload>

    <afp-button type="primary">上传</afp-button>
    
  </el-dialog>
</template>

<script>
export default {
  data() {
    return {
      visible: false,
      limitNum: 1,
      fileList: [],
    };
  },
  methods: {
    init() {
      this.visible = true;
    },
    UploadUrl() {
      return "";
    },
    // 文件超出个数限制时的钩子
    exceedFile(files, fileList) {
      this.$message.warning(
        `只能选择 ${this.limitNum} 个文件，当前共选择了 ${
          files.length + fileList.length
        } 个`
      );
    },
    // 文件状态改变时的钩子
    fileChange(file, fileList) { 
      this.fileList.push(file.raw);
    },
    // 上传文件之前的钩子, 参数为上传的文件,若返回 false 或者返回 Promise 且被 reject，则停止上传
    beforeUploadFile(file) {
      let extension = file.name.substring(file.name.lastIndexOf(".") + 1);
      let size = file.size / 1024 / 1024;
      //   if (extension !== "xlsx") {
      //     this.$message.warning("只能上传后缀是.xlsx的文件");
      //   }
      if (size > 10) {
        this.$message.warning("文件大小不得超过10M");
      }
    },
    // 文件上传成功时的钩子
    handleSuccess(res, file, fileList) {
      this.$message.success("文件上传成功");
    },
    // 文件上传失败时的钩子
    handleError(err, file, fileList) {
      this.$message.error("文件上传失败");
    },
    uploadFile() {
      if (this.fileList.length === 0) {
        this.$message.warning("请上传文件");
      } else {
        let form = new FormData();
        form.append("file", this.fileList);
      }
    },
  },
};
</script>