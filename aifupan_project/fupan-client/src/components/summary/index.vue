<template>
    <dialog-box :visible.sync="dialogVisible" title="本场小结" append-to-body
        :close-on-click-modal="false" width="600px" @closed="closed">
        <div style="height: 500px;">
            <div v-if="!isEdit"  style="height: calc(500px - 40px);" class="ai-window-box overflow_hidden overflow_auto_y">
                <div class="edit-time font-s14 pd-b12">最近编辑时间: {{ editDate }}</div>
                <div v-html="summary"></div>
            </div>
            <div v-else style="height: calc(500px - 40px);">
                <div class="p-r w100" style="height: calc(500px - 40px);">
                    <div class="edit-time font-s14 pd-b12" style="height: 30px;">最近编辑时间: {{ editDate }}</div>
                    <div class="edit-content-box" style="height: calc(500px - 70px);">
                        <editor v-model="summaryEdit" class="summary-review-box h100" ref="summary"
                        :editStatus="editStatus" @change="onChange" notLoadHtml
                        :notMenus="['image', 'video', 'emoticon', 'link','fontName']"></editor>
                    </div>
                </div>
            </div>
            <div class="flex-jc-e pd-6" v-auth="selfIds" style="height: 40px;">
                <afp-button v-if="!isEdit" type="primary" @click="editSummary" >编辑</afp-button>
                <template v-else>
                    <afp-button class="mg-r10" type="" @click="notEdit" >退出编辑</afp-button>
                    <el-popconfirm
                        title="确定修改本场小结吗？"
                        @confirm="saveSummary"
                        > 
                        <afp-button slot="reference" v-loading="isSaveLoading" :disabled="!editStatus" type="primary" >保存</afp-button>
                    </el-popconfirm>
                </template>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog';
import editor from '@/components/editor/index.vue';
import auth from '@/mixins/auth';
export default {
    components: {
        DialogBox,
        editor
    },
    mixins: [dialogMixin,auth],
    props: {
    },
    data() {
        return {

            summaryEdit: '',
            summary: '',
            // 查看还是编辑
            isEdit: false,
            // 编辑状态
            editStatus: false,
            isSaveLoading: false,
            editDate:''
        };
    },
    computed: {
    },
    watch: {},
    methods: {
        onChange(){
            this.editStatus = true;
        },
        notEdit(){
            this.isEdit = false;
            this.editStatus = false;
            this.summaryEdit = '';
        },
        onCancel() {
            this.hide();
        },
        closed(){
            this.isSaveLoading = false;
            this.isEdit = false;
            this.editStatus = false;
            this.summary = '';
            this.summaryEdit = '';
        },
        editSummary(){
            this.summaryEdit = this.summary;
            this.isEdit = true;
            this.editStatus = false;
            this.$nextTick(() => {
                this.$refs?.summary?.setEditrHtml(this.summaryEdit);
            })
        },
        saveSummary(){
            this.isEdit = false;
            this.isSaveLoading = true;
            this.$httpBack.notes.saveNotes({
                sourceId: this.dialogData.videoId || this.dialogData.fileId, 
                sourceType: this.getSourceType(),
                notesType: 2,
                content: this.summaryEdit
            }).then(async res => {
                this.summary = this.summaryEdit;
                this.isSaveLoading = false;
                await this.getSummaryInfo();
            });
        },
        async showCallback() {
            this.authInfo = this.dialogData;
            await this.getSummaryInfo();
        },
        // 获取源类型
        getSourceType(){
            if(this.dialogData.videoId){
                return 0
            }else{
                return 1
            }
        },
        async getSummaryInfo(){
            const { videoId, fileId } = this.dialogData;
            this.summary = await this.$httpBack.notes.getNotes({
                sourceId: videoId || fileId, 
                sourceType: this.getSourceType(),
                notesType: 2
            }).then(res => {
                let list = res?.data?.editors || [];
                if(list?.length){
                    let pop = list[list?.length - 1];
                    let timeDate = pop?.editTime?.replace('T', ' ')?.split('.')[0];
                    this.$set(this,'editDate',timeDate);
                }
                return res?.data?.content || '';
            }).catch(()=>{
                return '';
            });
            
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
.edit-content-box{

}
</style>