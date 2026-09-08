export default {
    props:{
    },
    data() {
        return {
        };
    },
    computed: {
        // 选中段落
        selectIndex(){
            return this.getTextVnode?.selectIndex
        },
        // 选中数据
        selectMark() {
            return  this.getTextVnode?.selectMark;
        }
    },
    methods: {
        // 右键点击
        rightTickContextMenu(event) {
            if(this.readonly){return;};
            this.$emit('rightTickContextMenu', event)
        },
        selectedTextHandler(v){
            this.$emit('selectedText', v);
        },
        // 选中事件
        updateSelectedText(v) {
            this.$emit('update:selectedText', v)
        },
        onParagraph(index){
            this.$emit('onParagraph',index)
        },
        // 行业变更
        treeChange(id) {
            this.$emit('treeChange', id)
        },
        // 行业加载完成
        tradeTreeLoad() {
            this.$emit('treeLoad')
        },
        // 设置关键词
        markwords(obj) {
            this.$emit('markwords', obj)
        },
        setMarkwords(obj){
            this.$emit('updateMarkwords',obj)
        },
        playerReadied(time, type){
            this.$emit('playerReadied',time, type)
        },
        playerPause(){
            this.$emit('playerPause')
        },
    }
}