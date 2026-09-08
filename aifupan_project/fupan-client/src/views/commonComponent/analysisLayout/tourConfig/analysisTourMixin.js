import tourObj from "/src/utils/tour";
import allConfig from "./allConfig.js";
export default {
    components: {},
    props: {
    },
    data() {
        return {
            tourObjFun: null,
        };
    },
    computed: {},
    watch: {},
    methods: {
        // 开始引导
        startTour(){
            this.$nextTick(()=>{
                this.tourObjFun.startTour();
            })
        },
        complete(){
            this.$nextTick(()=>{
                this.initSetTour();
            })
        },
        // 初始化设置引导
        initSetTour(){
            this.tourObjFun = tourObj(this,{});
            const { tour } = this.tourObjFun;
            let list = allConfig.filter(d=>{
                if(this.isWebOnline){
                    return d.name != 'share';
                }else{
                    return true
                }
            }).map(item=>{
                if(item.name && !item.step.buttons){
                    item.step.buttons = {};
                }
                switch(item.name){
                    case 'wordsBack':
                        item.step.buttons.back = ()=>{
                            this.$refs?.ControlTabs?.selectActiveName(0);
                        }
                        break;
                    case 'wordsNext':
                        item.step.buttons.next = ()=>{
                            this.$refs?.ControlTabs?.selectActiveName(0);
                        }
                        break;
                    case "compass":
                        item.step.buttons.next = ()=>{
                            this.$refs?.ControlTabs?.selectActiveName(1);
                        }
                        item.step.buttons.back = ()=>{
                            this.$refs?.analysis?.onPlayerParagraphIndex(1);
                        }
                        break;
                    case "text1": 
                        item.step.buttons.next = ()=>{
                            this.$refs?.analysis?.onPlayerParagraphIndex(0);
                        }
                        break;
                    // case 'model':
                    //     item.next = ()=>{
                    //         this.getTextVnode?.setModel(2);
                    //     }

                    //     break;
                    // case "chat":
                    //     item.next = ()=>{
                    //         this.getTextVnode?.setModel(3);
                    //     }
                    //     break;
                }

                return item
            });
            this.tourObjFun.addSteps(list);
        },
    },
    created() {
    },
    mounted() {
        this.$nextTick(() => {
            this.initSetTour();
        });
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}