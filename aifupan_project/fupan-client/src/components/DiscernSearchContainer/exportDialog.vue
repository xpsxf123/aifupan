<template>
    <dialog-box :visible.sync="dialogVisible" :close-on-click-modal="false" width="600px">
        
        <div slot="title">
            <h3 class="mg-0 font-s16">导出原文</h3>
            <div class="font-s12 text-color3">将本场视频的原文话术导出到本地</div>
        </div>
        <div>
            <el-form label-width="100px" label-position="left">
                <el-form-item label="本地文件名:">
                   <div class="slh" style="line-height: 20px;width: 260px;">
                        <span class="font-s14 slh" >
                            <el-tooltip class="item" effect="dark" :content="dialogData.fileName" placement="top">
                                <span class="slh">{{ dialogData.fileName }}</span>
                            </el-tooltip>
                        </span><br>
                        <span class="font-s12">{{ getFileSize }}</span>
                   </div>
                </el-form-item>
                <el-form-item label="导出内容:">
                    <div class="w100 mg-t6" style="height: 40px; width: 80%;">
                        <el-radio-group v-model="checkData" class="el-radio-group-export flex-jc-sb flex-ai-c">
                        <el-radio v-if="readonly && dialogData.isRecording" :label="0">导出文中段落</el-radio>
                        <el-radio v-else :label="0">导出分钟段落</el-radio>
                        <template v-if="versionType===VERSION_TYPE.AGENT">
                            <!-- AI脚本拆解 -->
                            <el-tooltip v-if="getAiShardingStatus == 2 && dialogData.isRecording && !readonly" class="item" effect="dark" :disabled="!readonly" content="免费提取话术不能导出AI脚本拆解" placement="top-start">
                                <el-radio :label="1" :disabled="readonly">导出AI脚本拆解</el-radio>
                            </el-tooltip>
                            <el-tooltip v-else-if="dialogData.isRecording && !readonly" class="item" effect="dark"  :content="selfUId?'请先生成AI脚本拆解，才可下载AI脚本拆解':'没有生成权限,请联系所属人生成后下载'" placement="top-start">
                                <afp-button v-if="isReplay.a || getAiShardingStatus==0 && $isAifupan" :disabled="!selfUId" @click="createText(1)">生成AI脚本拆解</afp-button>
                                <afp-button icon="el-icon-loading"  v-if="getAiShardingStatus==1">AI脚本拆解生成中</afp-button>
                            </el-tooltip>
                            <!-- 优化原文 -->
                            <el-tooltip v-if="getAiOptimalStatus == 2 && dialogData.isRecording && !readonly" class="item" effect="dark" :disabled="!readonly" content="免费提取话术不能导出优化原文" placement="top-start">
                                <el-radio :label="2" :disabled="readonly">导出优化原文</el-radio>
                            </el-tooltip>
                            <el-tooltip v-else-if="dialogData.isRecording && !readonly" class="item" effect="dark" :content="selfUId?'请先生成优化原文，才可下载优化原文':'没有生成权限,请联系所属人生成后下载'" placement="top-start">
                                <afp-button v-if="isReplay.b || getAiOptimalStatus==0  && $isAifupan" :disabled="!selfUId" @click="createText(2)">生成优化原文</afp-button>
                                <afp-button icon="el-icon-loading" v-if="getAiOptimalStatus==1">优化原文生成中</afp-button>
                            </el-tooltip>
                        </template>
                    </el-radio-group>
                    </div>
                </el-form-item>
                <el-form-item v-if="isWord" label="导出格式:">
                    <el-radio-group v-model="fileType">
                        <el-radio :label="0">txt文档</el-radio>
                        <el-radio :label="1">word文档</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <div class="text-center pd-t10">
                <afp-button type="primary" style="width: 105px;" :plain="false" @click="exportClick" size="medium">导出</afp-button>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import myUtils from '../../utils/utils';
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog';
import auth from '@/mixins/auth';
import {VERSION_TYPE} from "@/enum";
export default {
    components: {
        DialogBox
    },
    mixins: [dialogMixin,auth],
    props: {
        textTypeConfig:{
            type: Object,
            default: () => {
                return {}
            }
        },
        isWord:{
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            VERSION_TYPE,
            checkData: 0,
            fileType: 0,
            fileNameMap: {
                0: '分钟段落',
                1: 'AI脚本拆解',
                2: '优化原文'
            }
        };
    },
    computed: {
        getAiOptimalStatus(){
            return this.dialogData?.aiOptimalStatus ?? this.textTypeConfig?.aiOptimalStatus
        },
        getAiShardingStatus(){
            return this.dialogData?.aiShardingStatus ?? this.textTypeConfig?.aiShardingStatus
        },
        isReplay() {
            return {
                a:this.dialogData?.isReplay && this.getAiShardingStatus == 3,
                b:this.dialogData?.isReplay && this.getAiOptimalStatus == 3
            }
        },
        getFileSize(){
            if(this.dialogData.fileSize==0){
                return '0KB'
            }
            return myUtils.formatFileSize(this.dialogData.fileSize);
        },
        readonly(){
            return this.dialogData.readonly;
        },
        getFileName(){
            return `${this.dialogData.fileName}_${this.checkData !== 0? this.fileNameMap[this.checkData] : this.readonly?'文中段落':'分钟段落'}`
        },
        versionType() {
            return this.$store.getters.getVersionType
        }
    },
    watch: {},
    methods: {
        async showCallback() {
            let fs = this.dialogData.fileName.split('_');
            if(fs.length>1){
                fs.pop();
            }
            this.dialogData.fileName = fs.join('_');
        },
        exportClick(){
            let params = {
                sourceId: this.dialogData.sourceId,
                sourceType: this.dialogData.sourceType,
                type: this.checkData
            }
            if(this.fileType === 1){
                this.$emit('exportWord',{type: this.checkData, name: this.getFileName})
                return;
            }
            let p = null;
            if(this.$isAifupan){
                p = this.clientExport(params);
            }else{
                p = this.backExport(params);
            }
            p.then((res)=>{
                this.$nextTick(()=>{
                    if(res.code == 0){
                        this.$message.success("导出成功");
                    }
                })
            }).finally(()=>{
                this.hide();
            });
            this.$emit('export', this.checkData)
        },
        clientExport(params){
            return this.$httpClient.anchorvideo.exportVideoContent(params).then(res=>{
                return res;
            })
        },
        createText(val){
            if(!this.selfUId){
                return;
            }
            this.$emit('createText',val,this.dialogData?.sourceId)
        },
        backExport(params){
            return this.$httpBack.words.exportVideoContent(params, {
                config: {
                    responseType: 'blob',
                    headers: {
                        'Content-Type': 'application/octet-stream'
                    }
                }
            }).then(res=>{
                const url = window.URL.createObjectURL(new Blob([res.data]),{type: res.headers['content-type']});
                const link = document.createElement('a');
                link.href = url;
                link.setAttribute('download', `${this.getFileName}.txt`); // 设置下载文件名
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                return res;
            })
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
.el-radio-group-export{

}
</style>