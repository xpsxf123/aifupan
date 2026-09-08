<template>
    <div class="cu-upload">
        <el-upload
          :action="this.$http.common.upload"
          :on-success="uploadHandler"
        >

        </el-upload>
    </div>
</template>

<script>
export default {
    data() {
        return {

        }
    },
    methods: {
        // 上传文件成功后
        uploadHandler() {
            
        }
    }
}
</script>