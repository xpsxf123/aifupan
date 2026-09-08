export default {
    props: {
        options: {
            type:Array,
            default: ()=>{return []}
        },
        prop: {
            type:Object,
            default: ()=>{
                return {}
            }
        },
        formmater:{
            type:Function,
            default:null,
        },
    },
    data(){

    },
    computed:{
        getValues:{
            get(){
                return this.$attrs.value
            },
            set(v){
                this.$emit('input',v);
            }
        },
        getProps(){
            return {
                label: 'label',
                value: 'value',
                ...this.prop,
            }
        }
    },
    methods: {
        getValue(item){
            return item[this.getProps?.value]
        },
        getLabel(item){
            if(typeof this.formmater === 'function'){
                return this.formmater(item);
            }
            return item[this.getProps?.label]
        },
    }
}