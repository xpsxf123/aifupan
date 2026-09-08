import env from '/src/env'

//env.dev 判断当前环境 如果是开发环境将取消按钮禁用
let open = env.dev || env.test;
function windLoad(Vue,options = {
    which:[61,82,107,109,173,187,189],
    keys: ['F12',"F5"],
}){
    const {which, keys} = options;



    // 设置时间定时key
    const ___timeKey =`#${location.hostname}_timekey_`;
    const TimeStorage = {
        ___timeKey
    }
    /**
     * 设置定时数据
     * @param {String} name 名称
     * @param {Object|Array|String|Number} data 储存数据
     * @param {Number} time 时间 
     */
    TimeStorage.setTimeItem = function(name,data,time){
        localStorage.setItem(TimeStorage.___timeKey+"_"+name,JSON.stringify({
            data: data,
            countdown: time,
            time: Date.now()
        }))
    }
    /**
     * 获取定时数据
     * @param {String} name 
     * @returns {Object|String}
     */
    TimeStorage.getTimeItem = function(name){
        let o = localStorage.getItem(TimeStorage.___timeKey+"_"+name);
        if(o){
            o = JSON.parse(o);
            let t =(o.countdown * 1000) - (Date.now() - o.time);
            if(t>0){
                return o.data;
            }else{
                localStorage.removeItem(TimeStorage.___timeKey+"_"+name)
                return ''
            }
        }else{
            localStorage.removeItem(TimeStorage.___timeKey+"_"+name)
            return ''
        }
    }
    window.TimeStorage = TimeStorage;
    // 禁止ctrl+滚轮
    window.addEventListener('mousewheel',function(event){
        if(event.ctrlKey === true || event.metaKey){
            event.preventDefault();
        }
    },{passive: false});
    // 禁止ctrl+滚轮
    window.addEventListener('DOMMouseScroll',function(event){
        if(event.ctrlKey === true || event.metaKey){
            event.preventDefault();
        }
    },{passive: false})

    window.notKeyDown = {};
    window.disableKeyDown = function(event){
        if(sessionStorage.getItem('iframe')){
            return;
        }
        if(window.notKeyDown[event.key]){
            // 99 表示ctrlKey 是否被按下。
            if(window.notKeyDown[event.key] === 99){
                return
            }
        }
        // if(open){return}
        // let code = event.keyCode || event.which;
        // 禁止用户强刷
        if((event.ctrlKey === true && which.includes(event.which))){
            event.preventDefault()
        }
        // 禁止用户按F5和F12
        if (keys.includes(event.key)) {
            event.preventDefault()
        }
    }
    // 添加禁止按键事件。
    window.addDisableKeyDown = function(){
        document.addEventListener('keydown',disableKeyDown,false)
    }
    // 清楚禁止案件事件
    window.delDisableKeyDown = function(){
        document.removeEventListener('keydown',disableKeyDown,false)
    }
    // 禁止案件
    window.onload = function(){
        addDisableKeyDown();
        // 主题颜色设置
        let root = document.documentElement;
        if(localStorage.getItem('main-color')){
            root.style.setProperty('--color-main',localStorage.getItem('main-color'));
        }
    }
    console.devLog = env.devLog
    
}

windLoad.install = windLoad;

export default windLoad