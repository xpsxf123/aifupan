<template>
    <div class="searchContainer">
        <div class="search-box flex-ai-c">
            <el-form :inline="true">
                <el-form-item>
                    <el-input placeholder="请输入直播间名称" size="default" v-model="localAnchorName"
                              @input="anchorNameChange" clearable></el-input>
                </el-form-item>
                <el-form-item>
                    <afp-button @click="keywordChange" size="default">查询</afp-button>
                </el-form-item>
                <!--                    <el-form-item>-->
                <!--                        <afp-button @click="recordHistory" type="primary" plain size="small">录制历史</afp-button>-->
                <!--                    </el-form-item>-->

            </el-form>
<!--            <polish class="mg-l24">-->
<!--                <img v-if="$store.getters.getPackageLevel === 0" src="@/assets/imgs/mflz.png" alt=""-->
<!--                     style="max-height: 24px;">-->
<!--            </polish>-->
        </div>
    </div>
</template>

<script>
import polish from "@/components/polish/index.vue";

export default {
    components: {polish},

    props: {
        anchorName: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            localAnchorName: this.anchorName
        };
    },
    computed: {},
    watch: {},
    methods: {
        keywordChange() {
            this.$emit('keywordChange')
        },
        anchorNameChange() {
            this.$emit('anchorNameChange', this.localAnchorName)
        }
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() {
    }, //生命周期 - 创建之前
    beforeMount() {
    }, //生命周期 - 挂载之前
    beforeUpdate() {
    }, //生命周期 - 更新之前
    updated() {
    }, //生命周期 - 更新之后
    beforeDestroy() {
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.searchContainer{
    .el-form-item{
        margin-bottom:0;
    }
}
</style>