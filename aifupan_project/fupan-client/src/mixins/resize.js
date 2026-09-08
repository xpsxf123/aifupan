import myUtils from "../utils/utils"
// myUtils.monitorScreen
export default {
    data(){
        return {
            monitorScreenNames: []
        }
    },
    methods: {
        addResizeFns(name,fn,sort){
            this.monitorScreenNames.push(name)
            myUtils.monitorScreen(fn,name, sort)
        },
        delResizeFns(){
            this.monitorScreenNames.forEach((name)=>{
                myUtils.delMonitorScreen(name)
            })
        }
    },
    mounted(){
        
    },
    beforeDestroy(){
        this.delResizeFns()
    }
}


