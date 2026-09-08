<template>
    <div class="annotation brs-10">
        <div v-if="!isAdd" class="font-s14 pd-t4 pd-b4" :class="{'b-b1-c1': true && collapse}">
            
            <span class="text-colorErr">批注{{markNo}}：</span>
            <span v-if="!collapse">{{getTime}}</span>
        </div>
        <div v-if="!collapse" class="font-s14">
            <div v-if="!getEdit" class="pd-b8" style="overflow-wrap: break-word;word-wrap: break-word;">{{markContent}}</div>
            <el-input v-else v-model="getValue" ref="textBox" class="text-content-input"
            :class="{errInput: !!errMsg}" type="textarea" placeholder="请输入批注内容"
            @input="inputHandler" :autosize="{ minRows: 4, maxRows: 6}"
            maxlength="200" show-word-limit
            style="width: 100%;" />
            <div v-if="getEdit" style="height: 20px;" class="text-colorErr font-s12">{{ errMsg }}</div>
        </div>
        <div v-if="!collapse" class="common-bg text-color3 font-s14">
            <div class="pd-b4"><span class="text-yw">原文：</span><span v-if="textTime" class="font-s14">{{textTime}}</span></div>
            <div class="text-content" style="max-height: 60px;" v-html="text"></div>
        </div>
        <div v-if="!isAdd" :class="collapse?'pd-t4':'flex-jc-sb pd-t14'">
            <div v-if="collapse && !isPlayUrl" class="font-s12">
                {{isFileText? '上传文本没有视频':'本地查询不到视频'}}
            </div>
            <div :class="collapse?'flex-jc-sb':'flex-ji-c'">
                <template v-if="!isPopover">
                    <el-button v-auth="selfIds" v-if="!getEdit" @click="editHandler" type="text" >编辑</el-button>
                    <el-button v-auth="selfIds" v-else @click="saveHandler" type="text" >保存</el-button>
                </template>
                <template v-else-if="isPlayUrl">
                    <el-button type="text" class="pd-0" @click="playHandler">
                        <img style="width: 30px;" v-if="getPalyTpe === 'play' && getPalyStatus.play" src="@/assets/imgs/pStop.png" alt="" srcset="">
                        <img style="width: 28px;" v-else src="@/assets/imgs/play1.png" alt="" srcset="">
                    </el-button>
                    <el-button type="text" class="pd-0" @click="cycleHandler">
                        <img style="width: 30px;" v-if="getPalyTpe === 'cycle' && getPalyStatus.cycle" src="@/assets/imgs/cStop.png" alt="" srcset="">
                        <img style="width: 28px;" v-else src="@/assets/imgs/cycle.png" alt="" srcset="">
                    </el-button>
                </template>
            </div>
            <div v-if="!collapse">
                <template v-if="!isPopover">
                    <afp-button type="primary"  plain @click="toTextMark">跳转原文</afp-button>
                    <afp-button v-if="!getEdit" v-auth="selfIds" type="danger"  plain @click="deleteHandler">删除</afp-button>
                </template>
            </div>
        </div>
    </div>
</template>

<script>
import auth from '@/mixins/auth';
export default {
    mixins: [auth],
    components: {},
    props:{
        value: {
            type: String,
            default: ''
        },
        item: {
            type: Object,
            default: () => {
                return {}
            }
        },
        markNo:{
            type: [String,Number],
            default: ''
        },
        createTime:{
            type: String,
            default: ''
        },
        updateTime:{
            type: String,
            default: ''
        },
        markContent:{
            type: String,
            default: ''
        },
        // 原文时间
        textTime: {
            type: [String,Number],
            default: ''
        },
        // 原文文本
        text:{
            type: String,
            default: ''
        },
        type:{
            type: String,
            default: ''
        },
        sentenceMarkData:{
            type: Object,
            default:()=>{
                return {}
            }
        },
        isPlayUrl:{
            type: [String,Boolean],
            default: false
        },
        videoCurrentTime:{
            type: [String,Number],
            default: -1
        },
        playData:{
            type: [Object,null],
            default:()=>{
                return {}
            }
        },
        collapse:{
            type: Boolean,
            default: false
        },
        isFileText:{
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            editInfo:'',
            edit: false,
            errMsg: '',
            stopData: null
        };
    },
    computed: {
        getTime(){
            console.log(this.item)
            if(this.isFileText){return ''}
            return this.updateTime || this.createTime
        },
        getValue: {
            get(){
                return this.editInfo;
            },
            set(val){
                this.editInfo = val;
            }
        },
        isAdd(){
            return this.type === 'add'
        },
        isPopover(){
            return this.type === 'popover'
        },
        getEdit(){
            if(this.isAdd){
                return true
            }else{
                return this.edit
            }
        },
        getPalyTpe(){
            return this.playData?.playType || '';
        },
        getPalyStatus(){
            return this.playData?.playStatus || {};
        }
    },
    watch: {
        videoCurrentTime(val){
            if(this.getPalyTpe === 'play'){
                if(this.item.endTime < val){
                    this.setPlayStatus('play',false);
                }
            }
        }
    },
    methods: {
        setPlayStatus(type,status){
            if(typeof status === 'undefined'){return}
            if(this.getPalyTpe !== type){
                this.stopData = null;
            }
            this.$emit('changePlayData', {
                playType: type,
                playStatus: {
                    play: false,
                    cycle: false,
                    [type]: status
                }
            })
        },
        cycleHandler(){
            if(this.getPalyStatus.cycle){
                this.setPlayStatus('cycle',false);
                this.stopData = this.videoCurrentTime
                this.$emit('play',{
                    type: 'stop',
                })
            }else{
                this.setPlayStatus('cycle',true);
                this.$emit('play',{
                    type: 'cycle',
                    data: {
                        ...this.item,
                        stopTime: this.stopData
                    }
                })
            }
        },
        playHandler(){
            if(this.getPalyStatus.play){
                this.setPlayStatus('play',false);
                this.stopData = this.videoCurrentTime;
                this.$emit('play',{
                    type: 'stop'
                })
            }else{
                this.setPlayStatus('play',true);
                this.$emit('play',{
                    type: 'play',
                    data: {
                        ...this.item,
                        stopTime: this.stopData
                    }
                })
            }
        },
        toTextMark(){
            this.$emit('toTextMark', this.item);
        },
        deleteHandler(){
            this.$confirm('确定要永久删除该批注吗?', '友情提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
            }).then(() => {
                this.$httpBack.analysisMark.delAnnotation({
                    id: this.item.id
                }).then(res=>{
                    this.$message.success('删除成功');
                    this.$emit('del', this.item.id);
                })
            }).catch(() => {

            });
        },
        editHandler(){
            this.edit = true;
            this.editInfo = this.markContent;
            this.setError();
        },
        initForm(){
            this.editInfo = '';
            this.setError();
        },
        saveHandler(){
            if(!this.editInfo){
                this.$message.error('请输入批注内容'); 
            }
            this.saveHttp()?.then(()=>{
                this.$set(this.item, 'markContent', this.editInfo);
                this.$set(this,'edit',false);
                this.$emit('save', this.item);
                this.getValue = '';
            });
        },
        setError(err){
            this.errMsg = err ?? ''
        },
        inputHandler(){
            this.setError();
        },
        saveHttp(){
            return this.$httpBack.analysisMark.updateAnnotation({
                ...this.item,
                markContent: this.editInfo
            }).then(res=>{
                this.$message.success('保存成功');
            })
        },
        addHttp(params){
            if(!this.editInfo){
                this.setError('请输入批注内容');
                this.$refs.textBox.focus();
                return Promise.reject();
            }
            let p = {
                ...params,
                markContent: this.getValue
            }
            return this.$httpBack.analysisMark.addAnnotation(p).then(res=>{
                this.getValue = '';
                this.$message.success('添加批注成功');
            });
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
.annotation{
    background: #F2F8FF;
    .text-yw{
        color: #934A0B;
    }
}
.text-content{
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;  /* 限制显示的行数 */
    overflow: hidden;
    text-overflow: ellipsis;
}
.errInput{
    ::v-deep(.el-textarea__inner){
        border-color: var(--color-err) !important;
    }
}
.text-content-input{
    background: rgba(0,119,255,0.05);
    border-radius: 4px 4px 4px 4px;
    border: 1px dashed var(--color-main);
    ::v-deep(.el-textarea__inner){
        background: transparent;
        border:none;
        resize: none;
        padding-bottom: 15px;
        padding-left: 4px;
        padding-right: 4px;
    }
    ::v-deep(.el-input__count){
        background: transparent;
        right: 2px;
        bottom: 2px;
    }
}
</style>