<template>
</template>

<script>
import Shepherd from 'shepherd.js';
import HomeStep from '@/assets/imgs/home-step.png'
import { shift,offset } from '@floating-ui/vue';
export default {
    components: {},
    props: {
        userInfo: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {
            tour: null,
            steps: [

            ],
            options: {
            },
            callbacks: {
            },
            stepBtns: {}
        };
    },
    computed: {},
    watch: {},
    methods: {
        createTour() {
            this.tour = new Shepherd.Tour({
                useModalOverlay: true,
                defaultStepOptions: {
                    modalOverlayOpeningPadding: 10,
                    canClickTarget:false,
                    floatingUIOptions: {
                        middleware: [shift({ padding: 32 }), offset(40)]
                    },
                }
            });
            
            this.tour.addSteps([
                {
                    text: `<div class="shepherd-step-box">
                        <img src="${HomeStep}"/>
                        <h4>你好，${this.userInfo.nickName}</h4>
                        <p class="step-text">现在让我带你来快速了解下如何使用爱复盘吧！</p>
                    </div>`,
                    classes: 'example-step-extra-class',
                    buttons: [
                        {
                            text: '不用了',
                            action: this.tour.cancel,
                            classes: 'left-example-step-bt'
                        },
                        {
                            text: '开始吧',
                            action: () => {
                                this.tour.next();
                                this.addClose();
                            },
                            classes: 'left-example-step-bt'
                        }
                    ]
                },
                {

                    text: `<div  class="shepherd-step-box">
                        <div class="step-title"><p>添加主播</p><i class="step-title-icon-close el-icon-close"></i></div>
                        <div class="step-text">轻松关注您喜爱的主播</div>
                    </div>`,
                    classes: 'example-step-extra-class',
                    attachTo: {
                        element: this.stepBtns.el1, // 目标元素
                        on: "top", // 指导窗的位置，auto 会自动计算
                    },
                    
                    buttons: [
                        {
                            text: '1/3',
                            classes: 'bt-text'
                        },
                        {
                            text: '上一页',
                            action: this.tour.back,
                            classes: 'left-example-step-bt'
                        },
                        {
                            text: '下一页',
                            action: () => {
                                this.tour.next();
                                this.addClose();
                            }
                        }
                    ]
                },
                {

                    text: `<div  class="shepherd-step-box">
                        <div class="step-title"><p>点击开始录制分析</p><i class="step-title-icon-close el-icon-close"></i></div>
                        <div class="step-text">关注主播后，请开启自动录制分析，点击开始录制</div>
                    </div>`,
                    classes: 'example-step-extra-class',
                    attachTo: {
                        element: this.stepBtns.el2, // 目标元素
                        on: "left", // 指导窗的位置，auto 会自动计算
                    },
                    buttons: [
                        {
                            text: '2/3',
                            classes: 'bt-text'
                        },
                        {
                            text: '上一页',
                            action: this.tour.back,
                            classes: 'left-example-step-bt'
                        },
                        {
                            text: '下一页',
                            action: ()=>{
                                this.tour.next();
                            }
                        }
                    ]
                },
                {

                    text: `<div  class="shepherd-step-box">
                        <div class="step-title"><p>查看智能复盘分析</p></div>
                        <div class="step-text">点击智能复盘，去查看录制好的直播复盘分析吧！</div>
                    </div>`,
                    classes: 'example-step-extra-class',
                    attachTo: {
                        element: this.stepBtns.el3, // 目标元素
                        on: "auto", // 指导窗的位置，auto 会自动计算
                    },
                    buttons: [
                        {
                            text: '3/3',
                            classes: 'bt-text'
                        },
                        {
                            text: '上一页',
                            action: this.tour.back,
                            classes: 'left-example-step-bt'
                        },
                        {
                            text: '立即体验',
                            action: this.tour.cancel
                        }
                    ]
                }
            ]);
        },
        // 添加关闭按钮
        addClose() {
            this.$nextTick(() => {
                const icons = document.querySelectorAll('.step-title-icon-close');
                icons.forEach(el => {
                    el.addEventListener('click', () => {
                        this.tour.cancel();
                    })
                })
            })
        },
        ifBeginner(len) {
            // 判断是否执行过新手引导
            let beginners = localStorage.getItem('beginners')?.split(',') || [];
            // 首先判断用户iD是否已经出现过新手引导
            if(beginners.includes(this.userInfo?.id)){
                return true;
            }
            // 记录表中没有的ID 会进入表之后查询主播数量，如果主播数量也有，则会再次添加如用户表，表示不再是新手。
            beginners.push(this.userInfo?.id)
            let bsStr = beginners.join(',')
            // 在判断是否添加有主播。如果没有进入新手引导，但是主播列表有数据 默认为非新手用户。记录ID并且不弹出引导功能。
            if(len>0){
                localStorage.setItem('beginners',bsStr);
                return true;
            }
            // 对新手引导加锁，只有没出现过的用户才会出现
            localStorage.setItem('beginners',bsStr);
        },
        // 开始新手引导
        //len参数为主播列表长度
        startTour(len) {
            return new Promise((resolve, reject) => {
                // 判断是否进入新手引导
                if(this.ifBeginner(len)){
                    reject(false)
                    return
                }
                this.$nextTick(() => {
                    setTimeout(()=>{
                        this.stepBtns = {
                            el1:document.querySelector("#to-add-compere"), 
                            el2:document.querySelector("#click-cecord"), 
                            el3:document.querySelector("#replay-nav"), 
                        }
                        this.createTour();
                        this.tour.start();
                        setTimeout(()=>{
                            resolve(true)
                        },300)
                    },500)
                })
            });
        }
    },
    created() {
        // this.startTour();
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