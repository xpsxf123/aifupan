<template>
    <dialog-box :visible.sync="dialogVisible" title="添加批注" class="annotation-dialog-box"
        :close-on-click-modal="false" width="500px" @closed="onClosed">
        <div>
            <!-- <div class="pd-b10">批注内容:</div> -->
            <annotation ref="annotation" :text="text" :textTime="textTime" type="add"></annotation>
            <div class="flex-jc-e pd-t20">
                <afp-button type="primary"  @click="saveHandler">添加批注</afp-button>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog';
import annotation from './index.vue';
import myUtils from '@/utils/utils';
import { getSourceData } from '@/utils/common';
export default {
    components: {
        DialogBox,
        annotation
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
            text:'',
            textTime: '',
            params:{}
        };
    },
    computed: {
    },
    watch: {},
    methods: {
        async showCallback() {
            this.text = this.dialogData?.text;
            const { contenxtData } = this.dialogData;
            const { data } = contenxtData;
            const { startIndex, endIndex, startOffset, endOffset, items } = data;
            // const { sentenceMarkList } = this.dialogData.sentenceMarkData;
            // const item = sentenceMarkList[startIndex];
            const { startItem } = items;
            // const { startTimeSecond = 0 } =item;
            const { startTime } = startItem;
            // const t =startTimeSecond? ((startTimeSecond || 0) *1000) + (endTime-startTime) : 0;
            this.textTime = startTime && myUtils.toformatTime(startTime) || '';
            const { sourceType, sourceId } = getSourceData(this.dialogData.sentenceMarkData);
            this.params = {
                sourceType: sourceType,
                sourceId,
                paraphStartNo:Number(startIndex),
                paraphEndNo:Number(endIndex),
                markStartIndex: Number(startOffset),
                markEndIndex: Number(endOffset) - 1,
            }
        },
        onClosed(){
            this.$refs?.annotation?.initForm();
        },
        saveHandler(){
            this.$nextTick(()=>{
                if(Object.values(this.params)?.some(d=>typeof d === 'undefined')){
                    this.$message.error('请重新选择批注位置，当前选区数据错误。');
                    return;
                }
                this.$refs.annotation.addHttp(this.params).then(()=>{
                    this.$emit('getList');
                    this.hide();
                })
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
</style>