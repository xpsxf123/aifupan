export default {
    data(){
        return {
            shareStatus: false
        }
    },
    methods: {
        shareHandler(type, id){
            switch(type){
                case 'all':
                    this.setSelectCode(true);
                    break;
                case 'notAll':
                    this.setSelectCode(false);
                    break;
                case 'exit':
                    this.setElesShare(false);
                    this.$nextTick(()=>{
                        this.shareStatus = false;
                    })
                    break;
                case 'img': 
                    this.shareTask(type);
                    break;
                case 'url':
                    this.shareTask(type);
                    break;
                default:
                    this.shareStatus = !this.shareStatus;
                    this.setElesShare(this.shareStatus,{
                        selectCode: true,
                        id: type
                    })
            }
        },
        selectEles(){
            this.setElesShare(true)
        },
        exitEles(){
            this.setElesShare(false)
        },
        setSelectCode(bl,d){
            if(typeof d === 'undefined'){
                this.getEls.forEach((item) => {
                    if(item.el === 'aiCustom'){
                        this.setSelectCode(bl,item);
                    }
                });
            }else{
                this.$set(d.otherOption, 'selectCode', bl || false);
            }
        },
        setElesShare(bl, opt={}){
            const {selectCode, id} = opt;
            let qieCodeIndex = null;
            if(id){
                qieCodeIndex = this.getEls.findIndex(d=>d.id === id);
            }
            this.getEls.forEach((d,index)=>{
                 if(d.el==='aiCustom'){
                    if(typeof d.otherOption === 'undefined'){
                        this.$set(d, 'otherOption', {});
                    }
                    // 初始化数据
                    this.$set(d.otherOption, 'isShare', bl);
                }
                // 勾选指定问答。
                if(id && qieCodeIndex !== null){
                    if(index === qieCodeIndex || index === qieCodeIndex - 1){
                        this.$set(d.otherOption, 'selectCode', selectCode);
                        this.$set(d.otherOption, 'isShare', bl);
                    }
                }else if(typeof selectCode !== 'undefined'){
                    // 默认全选问答
                    this.$set(d.otherOption, 'selectCode', selectCode);
                }else if(typeof bl === 'boolean' && !!d.otherOption){
                    // 如果是boolean类型，且otherOption存在，并且id，selectCode都不存在 ，那么就将bl设置给selectCode。 主要用于取消勾选文旦
                    this.$set(d.otherOption, 'selectCode', bl);
                }
            });
        },
        getShareEls(){
            return this.getEls?.filter(d=>d?.otherOption?.selectCode)
        },
        shareTask(type){
            let els = this.getShareEls();
            if(type && els?.length){
                this.trackAiAnalysisEvent?.('P003_A044');
                if(type === 'img'){
                    this.shareIamge(els);
                }else if(type === 'url'){
                    this.shareUrl(els);
                }
            }
        },
        shareIamge(list){
            this.$emit('shareIamge', list);
        },
        shareUrl(list){
            let ids = list?.map(item=>item.id);
            if(typeof this.shareUrlCallback === 'function'){
                this.shareUrlCallback(ids);
            }
            this.$emit('shareUrl', ids)
        },
    }
}
