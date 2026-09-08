<template>
    <div>
        <ContextmenuDialog v-if="keywordDialogVisible" :keywordSelectedText="selectedText" ref="contextmenuDialog"></ContextmenuDialog>
        <addAiDialog ref="addAiDialog" v-if="aiAddState" :targetType="targetType" @submit="onSubmit"></addAiDialog>
    </div>
</template>

<script>

import ContextmenuDialog from './contextmenuDialog.vue';
import addAiDialog from './addAiDialog.vue';
import copyHandler from '@/utils/copy.js';
import {VERSION_TYPE} from '@/enum';
export default {
    components: { ContextmenuDialog, addAiDialog },
    props: {
        // 父组件选中的文本
        selectedText: {
            type: String,
            required: true
        },
        notLexicon: {
            type: Boolean,
            default: false
        },
        targetType: {
            type: String,
            default: ''
        },
        sentenceMarkData: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            keywordDialogVisible: false,
            aiAddState: false,
            contenxtData: {},
            contextMenuData: [
                {
                    label: '批注',
                    type: 'annotation',
                    onClick:()=>{
                        this.annotation();
                    },
                    hide: () => {
                        return this.versionType === VERSION_TYPE.PURE
                    }
                },
                {
                    label: '添加到词库',
                    type: 'lexicon',
                    onClick: () => {
                        this.onRightCode();
                    }
                },
                // {
                //     label: 'AI运营分析',
                //     type: 'ai1',
                //     onClick: () => {
                //         this.taskEmit('assistant')
                //     }
                // },
                // {
                //     label: 'AI违规分析',
                //     type: 'ai2',
                //     onClick: () => {
                //         this.taskEmit('violation')
                //     },
                //     hide: () => {
                //         return this.versionType === VERSION_TYPE.PURE
                //     }
                // },
                {
                    label: '切片',
                    type: 'slice',
                    onClick: () => {
                        this.taskSlice()
                    },
                    hide: () => {
                        return this.versionType === VERSION_TYPE.PURE
                    }
                },
                {
                    label: '复制',
                    type: 'copy',
                    onClick: () => {
                        this.copyTask()
                    }
                },
            ],
        }
    },
    computed:{
        getSelectText(){
            return this.contenxtData?.data?.txt || this.selectedText?.trim()
        },
        versionType(){
            return this.$store.getters.getVersionType
        },
    },
    methods: {
        getContextMenuData(notModel, annotation){
            return this.contextMenuData?.filter(a=>!a.hide?.())?.filter(d=>{
                // 'assistant'
                // 'violation'
                if(this.contenxtData.aiType ==='textAssistant'){
                    if(d.type === 'ai1' || d.type === 'ai2'){
                        return false
                    }
                }
                if(d.type === 'annotation'){
                    return annotation
                }
                if(this.notLexicon && ['lexicon'].includes(d.type)){
                    return false
                }else{
                    return !(notModel.includes(d.type))
                }
            })
        },
        // 鼠标右键事件
        rightContextMenu(event, data) {
            let notModel = data?.notModel || [];
            delete data.notModel;
            this.contenxtData = data;
            // 如果选区重叠批注，则不显示批注选项
            const annotation = !data?.data?.ifAnnotation && data?.annotation;
            if (this.getSelectText) {
                this.$contextmenu({
                    items: this.getContextMenuData(notModel, annotation),
                    event:event, // 鼠标事件信息
                    customClass: 'custom-class', //自定义菜单 class
                    zIndex: 9999, // 菜单样式 
                    minWidth: 100 // 主菜单最小宽度
                });
                this.$nextTick(()=>{
                    let dom = document.getElementsByClassName('custom-class')[0];
                    let closeDom = document.createElement('div');
                    closeDom.className = 'close-dom font_family icon-a-Frame959';
                    dom.appendChild(closeDom)
                    closeDom.addEventListener('click',()=>{
                        this.$contextmenu.destroy();
                    })
                })
                return false;
            }
        },
        taskEmit(type){
            this.aiAddState = true;
            if (this.selectedText.trim()) {
                this.$nextTick(() => {
                    this.$refs.addAiDialog.show({
                        data: {
                            text: this.selectedText,
                            type,
                            contenxtData: this.contenxtData
                        }
                    })
                })
            }
        },
        onSubmit(formData){
            this.$emit('task',{
                ...formData
            })
        },
        annotation(){
            this.$emit('annotation', {
                text: this.getSelectText,
                type: 'annotation',
                contenxtData: this.contenxtData
            })
        },
        onRightCode() {
            this.keywordDialogVisible = true
            if (this.getSelectText) {
                this.$nextTick(() => {
                    this.$refs.contextmenuDialog.init()
                })
            }
        },
        taskSlice(){
            if (this.getSelectText) {
                this.$emit('taskSlice', {
                    pureText: this.getSelectText,
                    contenxtData: this.contenxtData
                })
            }
        },
        copyTask(){
            if (this.getSelectText) {
                this.$nextTick(() => {
                    copyHandler(this.getSelectText);
                })
            }
        }
    }
}

</script>

<style lang="less" scoped>
/deep/ .el-dialog__body {
    padding: 0;
}

/deep/ .el-dialog__header {
    display: block;
}
</style>