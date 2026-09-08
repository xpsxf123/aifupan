<template>
    <dialog-box :visible.sync="dialogVisible" class="violation-dialog-box" title="AI违规助手" @close="onCancel" :close-on-click-modal="false" width="550px">
        <div>
            <el-form ref="formNode" :model="formData" :rules="dataRule" size="mini">
                <p class="mg-0 pd-b10">为了方便更好的解决您的问题，请您告诉我们以下几个问题：</p>
                <el-form-item class="form-reason-item" prop="reason">
                    <template #label>
                        <div>违规原因:<span class="text-colorErr">（必填）</span></div>
                    </template>
                    <div>
                        <el-input v-model="formData.reason"  placeholder="请输入违规原因" type="textarea" resize="none" :autosize="{ minRows: 4, maxRows: 6}"></el-input>
                    </div>
                </el-form-item>
                <el-form-item v-if="!getParagraphCode" label="违规时间:(非必填)" prop="type">
                    <div class="w100">
                        <br>
                        <el-radio-group v-model="formData.timeType" @change="timeTypeChange">
                            <el-radio label="nature" v-if="!isUoload">北京时间</el-radio>
                            <el-radio label="video">视频时间</el-radio>
                        </el-radio-group>
                        <div v-show="formData.timeType === 'nature'" class="pd-t8">
                            选择时间:
                            <el-date-picker
                                v-model="nature_startTime"
                                type="datetime"
                                value-format="timestamp"
                                format="yyyy-MM-dd HH:mm:ss"
                                :disabled = "!formData.timeType"
                                placeholder="任意开始时间点"
                                style="width: 180px;"
                                :clearable="false"
                                :picker-options="getNatureStart"
                                @change="timeChange('start')"
                                @blur="timeChange('start')"
                            >
                            </el-date-picker>
                            <span class="mg-l6 mg-r6">至</span>
                            <el-date-picker
                                v-model="nature_endTime"
                                type="datetime"
                                value-format="timestamp"
                                format="yyyy-MM-dd HH:mm:ss"
                                :disabled = "!formData.timeType"
                                placeholder="任意结束时间点"
                                style="width: 180px;"
                                :clearable="false"
                                :picker-options="getNatureEnd"
                                @change="timeChange('end')"
                                @blur="timeChange('end')"
                            >
                            </el-date-picker>
                        </div>
                        <div v-show="formData.timeType === 'video'" class="pd-t8">
                            选择时间:
                            <el-time-picker
                                v-model="video_startTime"
                                :disabled = "!formData.timeType"
                                value-format="yyyy-MM-dd HH:mm:ss"
                                placeholder="任意开始时间点"
                                style="width: 160px;"
                                :picker-options="{
                                    selectableRange:`${video_minTime}-${video_maxTime}`,
                                    format: 'HH:mm:ss'
                                }"
                                @change="timeChange('start')"
                                >
                            </el-time-picker>
                            <span class="mg-l6 mg-r6">至</span>
                            <el-time-picker
                                v-model="video_endTime"
                                :disabled = "!formData.timeType"
                                value-format="yyyy-MM-dd HH:mm:ss"
                                placeholder="任意结束时间点"
                                style="width: 160px;"
                                :picker-options="{
                                    selectableRange: getVideoSelectableRange,
                                    format: 'HH:mm:ss'
                                }"
                                @change="timeChange('end')"
                                >
                            </el-time-picker>
                        </div>
                    </div>
                </el-form-item>
                <el-form-item v-if="!getParagraphCode" label="违规句子/段落" prop="paragraphContent">
                    <el-input v-model="formData.paragraphContent"  placeholder="请输入违规句子或段落，如果没有的话可不填" type="textarea" resize="none" :autosize="{ minRows: 4, maxRows: 6}"></el-input>
                </el-form-item>
                <div class="text-right">
                    <afp-button type="primary" @click="submit" >点我分析</afp-button>
                </div>
            </el-form>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog';
import myUtils from '/src/utils/utils';
export default {
    components: {
        DialogBox
    },
    mixins: [dialogMixin],
    props:{
        sentenceMarkData: {
            type: Object,
            default:()=>{return {}}
        },
        scene: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            formData: {
            },
            dataRule:{
                reason:[
                    {required:true,message:'请输入原因',trigger:'blur'}
                ]
            },
            nature_startTime: null,
            nature_endTime: null,
            nature_minTime: '',
            nature_maxTime: '',

            video_startTime: null,
            video_endTime: null,
            video_minTime: '',
            video_maxTime: '',
            textDebounce:myUtils.debounce(500)
        };

    },
    computed: {
        
        isUoload(){
            return !!this.sentenceMarkData?.uploadFile
        },
        getInfo(){
            let o = this.sentenceMarkData?.videoInfo || this.sentenceMarkData?.uploadFile;
            if(this.isUoload){
                return {
                    ...o,
                    Duration: o.fileDuration,
                    StartTime: o.analysisTime
                }
            }else{
                return o
            }
        },
        getParagraphCode(){
            return this.dialogData?.paragraphCode
        },
        isParagraph(){
            return this.scene === 'paragraph';
        },
        getVideoSelectableRange(){
            return `${this.video_startTime && this.getToFormatDateSplit(this.video_startTime) || this.video_minTime}-${this.video_maxTime}`
        },
        getNatureSelectableRange(){
            return `${this.nature_startTime || this.video_minTime}-${this.video_maxTime}`
        },
        getIsCrossOneDay(){
            return this.isCrossOneDay(this.getInfo?.StartTime, this.getInfo?.EndTime);
        },
        getNatureStart(){
            let s = this.getInfo?.StartTime?.split(' ')[1];
            let e = this.getIsCrossOneDay ?'23:59:59' : this.getInfo?.EndTime?.split(' ')[1];
            return {
                disabledDate:(time)=>{
                    return !(this.nature_minTime <= time.getTime() && time.getTime() < (this.nature_maxTime + 1000));
                },
                selectableRange:`${s} - ${e}`
            }
        },
        getNatureEnd(){
            let t = this.nature_startTime || this.nature_minTime;
            // 时间是否过夜
            let c = this.isCrossOneDay(t, this.nature_maxTime);
            let sT = c ? this.nature_minTime : this.nature_minTime;
            const d = myUtils.toFormatDate(new Date(t))?.split(' ')[1];
            const o = this.getIsCrossOneDay ? c ? `${d} - 23:59:59`: `00:00:00 - ${this.getInfo?.EndTime?.split(' ')[1]}` : `${d} - ${this.getInfo?.EndTime?.split(' ')[1]}`;
            return  {
                disabledDate:(time)=>{
                    return !(sT <= time.getTime() && time.getTime() < (this.nature_maxTime + 1000));
                },
                selectableRange: o
            }
        }
    },
    watch: {},
    methods: {
        isCrossOneDay(time1,time2){
            let time1d = new Date(time1);
            let time2d = new Date(time2);
            return time1d.getUTCDay() !== time2d.getUTCDay();
        },
        getToFormatDateSplit(time){
            if(typeof time === 'string'){
                return time.split(' ')[1];
            }else{
                return myUtils.toFormatDate(time)?.split(' ')[1]
            }
        },
        timeTypeChange(val){
            this.$nextTick(()=>{
                if(val === 'nature'){
                    let s = this.getInfo?.StartTime?.split(' ')[0] + ' 00:00:00';
                    let e = this.getInfo?.EndTime?.split(' ')[0] + ' 23:59:59';
                    this.$set(this,'nature_startTime',(new Date(this.getInfo.StartTime)).getTime());
                    this.$set(this,'nature_endTime', (new Date(this.getInfo.StartTime)).getTime());
                    this.$set(this,'nature_minTime',(new Date(s))?.getTime());
                    this.$set(this,'nature_maxTime',(new Date(e))?.getTime());
                }else if(val === 'video'){
                    let y = this.getInfo.StartTime?.split(' ')[0];
                    let s ='00:00:00';
                    let e =this.getInfo.Duration;
                    this.$set(this,'video_startTime',new Date(y+ " " +s));
                    this.$set(this,'video_endTime',undefined);
                    this.$set(this,'video_minTime',s);
                    this.$set(this,'video_maxTime',e);
                }
            })
        },
        submit(){
            this.$refs.formNode?.validate((valid)=>{
                if(valid){
                    this.$emit('submit', {
                        ...this.dialogData,
                        ...JSON.parse(JSON.stringify(this.formData))
                    });
                    this.$nextTick(()=>{
                        this.hide();
                    });
                }
            })
        },
        onCancel(){
            this.$refs.formNode.clearValidate();
            this.hide();
            this.formData = {};
        },
        hideCallback(){
            this.formData = {};
            // this.$nextTick(()=>{
            //     ['nature','video'].map(d=>{
            //         this.$set(this,`${d}_startTime`,'');
            //         this.$set(this,`${d}_endTime`,'');
            //         this.$set(this,`${d}_minTime`,'');
            //         this.$set(this,`${d}_maxTime`,'');
            //     })
            // });
        },
        timeChange(type){
            // if(type ==='start'){
            //     this.formData.paragraphContent = '';
            // }
            this.textDebounce(()=>{
                this.getArrayText();
            })
        },
        getArrayText(){
            let textAry = [];
            // 自然时间
            let nature = this.formData.timeType === 'nature';
            let sTime,eTime;
            if(nature){
                if(!this.nature_startTime || !this.nature_endTime){return}
                // 自然时间计算（秒级）
                sTime = this.nature_startTime / 1000;
                eTime = this.nature_endTime / 1000;
            }else{
                if(!this.video_startTime || !this.video_endTime){return}
                // 视频时间计算（秒级）
                let vs = typeof this.video_startTime === 'string'? this.video_startTime : myUtils.toFormatDate(this.video_startTime);
                let ve = typeof this.video_endTime === 'string'? this.video_endTime :  myUtils.toFormatDate(this.video_endTime);
                sTime = myUtils.toSecond(vs?.split(' ')?.[1]) * 1000;
                eTime = myUtils.toSecond(ve?.split(' ')?.[1])* 1000;
            }
            textAry = this.sentenceMarkData?.sentenceMarkList?.filter(d=>{
                const {startTime,endTime,startTimeSecond,endTimeSecond} = d;
                if(nature){
                    return sTime <= endTimeSecond && eTime >= startTimeSecond;
                }else{
                    return sTime <= endTime && eTime >= startTime;
                }
            })?.map(d=>{
                return d?.content;
            });
            this.$set(this.formData,'paragraphContent', textAry?.join('\n') || '');
        },
        showCallback(){
            this.formData = {};
        },
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
.violation-dialog-box{
    ::v-deep(.el-dialog){
        background-image: url('~@/assets/imgs/aiVolationBg.png');
        background-size: cover;
    }
}
.form-reason-item{
    ::v-deep(.el-form-item__label){
        &::before{
            display: none !important;
        }
    }
}
</style>