
export default (keyName)=>{
    let name = 'click_hinde_' + keyName
    return {
        data:()=>{
            return {
                clickHiden_3Num: parseInt(localStorage.getItem(name) || '0')
            }
        },
        computed:{
            isClick3Hiden(){
                return this.clickHiden_3Num >= 3
            }
        },
        methods: {
            clickHiden_3(){
                this.clickHiden_3Num++;
                localStorage.setItem(name, this.clickHiden_3Num);
            },
            clickHiden_3_forever(){
                this.clickHiden_3Num = 3;
                localStorage.setItem(name, this.clickHiden_3Num);
            }
        },
        
    }
}