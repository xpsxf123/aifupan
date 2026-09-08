<template>
    
    <!-- <div class="onlineItemContainer" style="padding: 0px; display: flex; align-items: center;">
        <div class="onlineItemLabel">查看人群：</div>
        <div style="display: flex;" class="onlineItemText">
            <el-radio-group v-model="formData.crowd">
                <el-radio :label="0" :disabled="!isUpload">任何人可查看</el-radio>
                <el-radio :label="1" :disabled="!isUpload">仅限机构内账号可查看</el-radio>
            </el-radio-group>
            <div style="padding-left: 5px;margin-top: -2px;">
                <el-tooltip class="item" effect="dark" content="机构内成员指，同属于同一个主账号的成员可查看，其他人不可查看"
                    placement="bottom">
                    <i class="font_family icon-bangzhuzhongxinoff"></i>
                </el-tooltip>
            </div>
        </div>
    </div>
    <div class="onlineItemContainer" style="padding: 0px;">
        <div class="onlineItemLabel">查看人群：</div>
        <div style="display: flex;" class="onlineItemText">
            <div>
                <el-checkbox-group v-model="formData.shareTypes" >
                    <el-checkbox label="m" :disabled="!isUpload">可查看敏感词</el-checkbox>
                    <el-checkbox label="g" :disabled="!isUpload">可查看关键词</el-checkbox>
                </el-checkbox-group>
            </div>
        </div>
    </div>  -->
</template>

<script>

export default {
    components: {},
    props:{
        
    },
    data() {
        return {
        
        };
    },
    computed: {},
    watch: {},
    methods: {
        
    },
    created() {
        
    },
    mounted() {
        
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>

</style>