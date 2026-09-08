import Shepherd from 'shepherd.js';
import { shift, offset } from '@floating-ui/vue';


export default function(vue,option){
    let tour = null;
    const steps = [];
    const stepBtns = {};
    const AllStepBtns = {};
    const createTour = ()=>{
        tour = new Shepherd.Tour({
            useModalOverlay: true,
            defaultStepOptions: {
                modalOverlayOpeningPadding: 10,
                floatingUIOptions: {
                    middleware: [shift({ padding: 32 }), offset(40)]
                },
            }
        });
    }
    // 加入步骤
    const addSteps=(listOrObj)=>{
        
        /**
            text: `<div  class="home-step-box">
                <div class="step-title"><p></p><i class="step-title-icon-close el-icon-close"></i></div>
                <div class="step-text"></div>
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
        */
        if(Array.isArray(listOrObj)){
            listOrObj.forEach(item=>{
                setStepsOption(item);
            });
        }else{
            setStepsOption(listOrObj);
        }
    }
    const getElDom = function(index){
        // Object.keys(AllStepBtns).forEach(key=>{
        //     stepBtns[key] = document?.querySelector(AllStepBtns[key]) || null;
        // })
    }
    const runNextTick =(callback, time)=>{
        if(vue?.$nextTick){
            vue?.$nextTick(()=>{
                setTimeout(()=>{
                    callback?callback():null;
                },50)
            })
        }else{
            setTimeout(()=>{
                callback?callback():null;
            },time ||300);
        }
    }
    // 获取默认按钮
    const getButtons = function(buttonsOpt={}, index){
        return [
            {
                text: buttonsOpt.closeText || '关闭',
                action: ()=>{
                    if(typeof buttonsOpt?.cancel === 'function'){
                        buttonsOpt?.cancel();
                    }
                    tour.cancel();
                },
                classes: buttonsOpt.closeClasses || 'tour-bt-close left-example-step-bt'
            },
            {
                text: buttonsOpt.prevText || '上一项',
                action: ()=>{
                    if(typeof buttonsOpt?.back === 'function'){
                        buttonsOpt?.back();
                        runNextTick(()=>{
                            getElDom();
                            tour.back();
                        }, buttonsOpt.backTime)
                    }else{
                        runNextTick(()=>{
                            getElDom();
                            tour.back();
                        },buttonsOpt.backTime)
                    }
                },
                classes: buttonsOpt.prevClasses || 'tour-bt-text left-example-step-bt'
            },
            {
                text: buttonsOpt.nextText || '下一项',
                action:  ()=>{
                    if(typeof buttonsOpt?.next === 'function'){
                        buttonsOpt?.next();
                        runNextTick(()=>{
                            getElDom();
                            tour.next();
                        }, buttonsOpt.nextTime)
                    }else{
                        runNextTick(()=>{
                            getElDom();
                            tour.next();
                        }, buttonsOpt.nextTime)
                    }
                },
                classes: buttonsOpt.nextClasses || 'tour-bt-text left-example-step-bt'
            }
        ]
    }
    // 设置步骤配置并储存
    const setStepsOption=(obj={})=>{
        try{
            const {el, step}=obj; // 获取步骤配置
            let dom = document?.querySelector(el);
            let elIndex = steps.length +1; // 储存步骤索引
            AllStepBtns[elIndex] = el;
            if(!dom){
                stepBtns[elIndex] = el;
            }else{
                stepBtns[elIndex] = dom; //储存步骤高亮节点。  
            }
            steps.push({
                    text: typeof step?.text === 'function' ? step?.text() : `
                    <div  class="shepherd-step-box">
                        <div class="step-text">${step.text}</div>
                    </div>`, // 步骤内容
                    classes: step.classes || 'example-step-extra-class', // 样式
                    canClickTarget: step.canClickTarget || false, // 是否可以点击目标元素
                    attachTo: {
                        element: stepBtns[elIndex] || document.body, // 目标元素
                        on: step.on || 'top', // 指导窗的位置，auto 会自动计算
                    },
                    buttons: Array.isArray(step.buttons) ? step.buttons : typeof step.buttons === 'function' ? step.buttons(getButtons()) : getButtons(step.buttons, elIndex)
                }
            )
        }catch(e){
            console.error(e)
        }
    };

    // 设置添加运行步骤
    const setAddSteps = () =>{
        tour.addSteps(steps);
    };
    // 添加关闭按钮
    const addClose = () =>{
        // 添加关闭按钮
        vue?.$nextTick(()=>{
            // 添加关闭事件
            document.body.addEventListener('click',function(event){
                if(event.target && event.target.matches('.step-title-icon-close')){
                    tour.cancel();
                }
            })
        })
    };
    // 运行引导
    const startTour =()=>{
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                addClose(); // 添加关闭按钮
                setAddSteps(); // 添加步骤
                setTimeout(() => {
                    tour.start(); // 开始引导
                    resolve(true);
                }, 50)
            },50)
        });
    }
    const complete = ()=>{
        tour.complete();
    }
    createTour(); // 创建引导
    return {
        addSteps,
        startTour,
        complete,
        steps,
        stepBtns,
        tour,
    }
}
