<template>
    <dialog-box :visible.sync="dialogVisible" :close-on-click-modal="false" width="500px">
        <div class="pd-10">
            <el-form ref="formNode" :model="formData" :rules="dataRule" label-width="100px" >
                <el-form-item label="分析类型" prop="type">
                    <el-select v-model="formData.type" style="width: 100%;" placeholder="请选择加入分析类型">
                            <el-option label="AI运营助手" value="assistant"></el-option>
                            <el-option label="AI违规助手" value="violation"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="分析标题" prop="title">
                    <el-input v-model="formData.title" placeholder="请输入分析标题"></el-input>
                </el-form-item>
                <el-form-item label="分析内容" prop="content">
                    <el-input v-model="formData.content" disabled placeholder="请输入分析内容" type="textarea" resize="none" :autosize="{ minRows: 4, maxRows: 6}"></el-input>
                </el-form-item>
                <!-- <el-form-item label="分析违规" v-if="formData.type === 'violation'" prop="reason">
                    <el-input v-model="formData.reason"  placeholder="请输入违规信息" type="textarea" resize="none" :autosize="{ minRows: 4, maxRows: 6}"></el-input>
                </el-form-item> -->
                <div class="text-right">
                    <afp-button @click="onCancel" >重新选择</afp-button>
                    <afp-button type="primary" @click="submit" >添加分段</afp-button>
                </div>
            </el-form>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog'
export default {
    components: {
        DialogBox
    },
    mixins: [dialogMixin],
    props:{
        targetType: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            formData: {},
            dataRule:{
                title:[
                    {required:true,message:'请输入标题',trigger:'blur'}
                ],
                type:[
                    {required:true,message:'请选择分析类型',trigger:'change'}
                ],
                content:[
                    {required:true,message:'请输入分析内容',trigger:'blur'}
                ],
                reason: [
                    { message:'请输入违规信息',trigger:'blur'}
                ]
            }
        };
    },
    computed: {
        isUseBack(){
            return this.targetType === 'online' || this.targetType === 'webOnline'
        },
        httpAddHistoryParagraph(){
            if(this.isUseBack){
                return this.$httpBack.aiRelated.addHistoryParagraph;
            }else{
                return this.$httpClient?.aiRelated?.addHistoryParagraph
            }
        }
    },
    watch: {},
    methods: {
        submit(){
            this.$refs.formNode?.validate((valid)=>{
                if(valid){
                    this.httpAddHistoryParagraph({
                        type: this.formData?.type === 'assistant' ? 0 : 1,
                        alias: this.formData?.title,
                        content: this.formData?.content,
                        sourceId: this.dialogData?.contenxtData?.id,
                        sourceType: this.dialogData?.contenxtData?.sourceType,
                    }).then(res=>{
                        const { data } = res;
                        this.$emit('submit', {
                            ...data,
                            type: this.formData.type,
                            title: data?.alias,
                            value: data?.code,
                        });
                        this.$nextTick(()=>{
                            this.hide();
                            this.formData={};
                        });
                    })
                }
            })
        },
        onCancel(){
            this.hide();
        },
        async showCallback(){
            const id = this.dialogData?.contenxtData?.id;
            const sourceType = this.dialogData?.contenxtData.sourceType;
            const type = this.dialogData.type === 'assistant' ? 0 : 1;
            // 查询高度;
            let sort = localStorage.getItem(`${id}_${this.dialogData.type}_itemsLen`);
            if(!sort || sort === '0'){
                sort =(await this.getItems(type,id,sourceType) || 0);
                localStorage.setItem(`${id}_${this.dialogData.type}_itemsLen`, sort);
            }
            sort = parseInt(sort) + 1;
            this.$set(this.formData,'content', this.dialogData.text);
            this.$set(this.formData,'type',this.dialogData.type);
            this.$set(this.formData,'title',`段落${sort}`);
            this.$set(this.formData,'sort',sort);
            this.$set(this.formData,'value',`${sort}`);
        },
        getItems(type,id, sourceType){
            // 获取全部历史分类
            return this.$httpClient.aiRelated.getHistoryParagraphList({
                type,
                sourceId: id,
                sourceType, // 对比还是单个
            }).then(res=>{
                return res.data?.length;
            })
        }
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