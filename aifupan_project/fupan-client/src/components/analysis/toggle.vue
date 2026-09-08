<template>
    <div>
        <afp-button plain class="elBt" size="small" :style="{background:'transparent',...customStyle}" :disabled="readonly" :class="getBtState || readonly ? 'cancelIndiciaBtn' : 'indiciaBtn'" @click="change" >{{
            getBtState ? inactive : active }}</afp-button>
    </div>
</template>

<script>
export default {
    name: "",
    props: {
        inactive: {
            type: String,
            default: ''
        },
        active: {
            type: String,
            default: ''
        },
        default: {
            type: Boolean,
            default: false
        },
        readonly: {
            type: Boolean,
            default: false
        },
        customStyle: {
            type: [String, Object],
            default: () => ({})
        }
    },
    computed: {
        getBtState:{
            get(){
                if(typeof this.$attrs.value !== 'undefined'){
                    return this.$attrs.value
                }else{
                    return this.btState
                }
            },
            set(v){
                this.btState = v;
                this.$emit('input',v);
            }
        }
    },
    data() {
        return {
            btState: false
        };
    },
    mounted() {
        this.btState = this.default;
    },
    created() {

    },
    methods: {
        change() {
            this.getBtState = !this.getBtState;
            this.$emit('change', this.getBtState)
        }
    }

};
</script>

<style scoped>
.elBt {
    padding: 5px;
    margin-left: 6px;
}

.cancelIndiciaBtn {
    color: #2E3742 !important;
    border: none !important;
}

.indiciaBtn {
    color: var(--color-main) !important;
    border: none !important;
}
</style>