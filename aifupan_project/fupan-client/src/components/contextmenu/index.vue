<template>
</template>

<script>
// import ContextmenuDialog from './contextmenuDialog.vue';
// import addAiDialog from './addAiDialog.vue';
export default {
    components: {  },
    props: {
    },
    data() {
        return {
            contenxtData: {},
            contextMenuData: [],
        }
    },
    computed:{
    },
    methods: {
        getContextMenuData(notModel){
            return this.contextMenuData?.filter(d=>{
                if(this.notLexicon && d.type === 'lexicon'){
                    return false
                }else{
                    return !(notModel.includes(d.type))
                }
            })
        },
        // 鼠标右键事件
        rightContextMenu(event, data) {
            this.contenxtData = data;
            if (this.selectedText.trim()) {
                this.$contextmenu({
                    items: this.getContextMenuData(notModel),
                    event, // 鼠标事件信息
                    customClass: 'custom-class', //自定义菜单 class
                    zIndex: 9999, // 菜单样式 
                    minWidth: 100 // 主菜单最小宽度
                });
                return false;
            }
        },
    }
}

</script>

<style scoped>
/deep/ .el-dialog__body {
    padding: 0;
}

/deep/ .el-dialog__header {
    display: block;
}
</style>