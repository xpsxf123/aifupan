<template>
    <dialog-box :visible.sync="dialogVisible" title="导出弹幕" class="ai-window-dialog-box"
        :close-on-click-modal="false" width="500px">
        <div>
            <el-form  ref="form" label-width="90px" label-position="left" class="demo-ruleForm">
                <el-form-item label="本地文件名">
                    {{ formData.fileName }}
                </el-form-item>
                <el-form-item label="弹幕共计">
                    {{ formData.totalCount }}条
                </el-form-item>
            </el-form>
            <div class="flex-ji-c">
                <afp-button type="primary" :plain="false" @click="exportHandler">导出</afp-button>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog';
export default {
    components: {
        DialogBox,
    },
    mixins: [dialogMixin],
    props: {
    },
    provide() {
        return {
        }
    },
    data() {
        return {
            formData: {}
        };
    },
    computed: {
    },
    watch: {},
    methods: {
        onCancel() {
            this.hide();
        },
        async showCallback() {
           this.formData = await this.getExportInfo(this.dialogData);
        },
        async getExportInfo(params){
            return await this.$httpBack.v2100.queryDanMuCount(params).then(res=>{
                return res.data;
            })
        },
        exportHandler(){
            let http = null;
            const params = {
                ...this.dialogData,
                ...this.formData,
                token: this.$store.getters.getToken || '',
            }
            if(this.$isAifupan){
                http = this.$httpClient.anchorvideo.queryDanMuExport(params);
            }else{
                const base = window.SITE_CONFIG && window.SITE_CONFIG['backApiURL'] ? window.SITE_CONFIG['backApiURL'] : ''
                const qs = Object.keys(params || {}).filter(k => params[k] !== undefined && params[k] !== null).map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`).join('&')
                const url = `${base}/openapi/v2100/queryDanMuExportGet${qs ? `?${qs}` : ''}`
                window.open(url, '_blank')
            }
            // http.then(()=>{
            //     this.hide();
            //     this.$message.success("导出成功");
            // })
        }
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss'>
</style>