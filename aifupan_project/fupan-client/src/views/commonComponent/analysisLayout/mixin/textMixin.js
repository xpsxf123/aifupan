export default {
    props:{
    },
    data() {
        return {
        };
    },
    computed: {
    },
    methods: {
        // 设置关键词数据
        setWordsInfo(o) {
            this.getTextVnode?.setWordsInfo(o);
        },
        // 选中单词
        selectMarkHandler(name, type) {
            this.getTextVnode?.selectMarkHandler(name, type)
        },
        // 设置选中行业
        setTrade(id) {
            this.getTextVnode?.setTrade(id)
        },
        // 刷新段落
        refreshHandler(type) {
            if(type === 'init'){
                // 初始化判断是否存在setData函数
                if(typeof this.setSentenceMarkData === 'function'){
                    this.setSentenceMarkData(this.sentenceMarkData);
                }
            }
            this.getTextVnode?.refreshHandler()
        },
        // 关闭行业弹窗
        dropDown() {
            this.getTextVnode?.dropDown();
        },
        marksTitle(...arg){
            this.getTextVnode.marksTitle(...arg);
        },
        setVideoCurrentTime(...arg){
            this.getTextVnode.setVideoCurrentTime(...arg)
        },
        setCurrentParagraphIndex(index){
            this.getTextVnode?.setCurrentParagraphIndex(index)
        },
        selectDomeScrollIntoView(...arg){
            this.getTextVnode?.selectDomeScrollIntoView(...arg)
        },
        countWords(){
            if(this.getTextVnode?.countWords){
                this.getTextVnode?.countWords()
            }else{
                this.getTextVnode?.tidyCountWords();
            }
        }
    }
}