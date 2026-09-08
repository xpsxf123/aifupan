import Summary from './index.vue';
export default {
    components: {
        Summary
    },
    data(){
        return {
            
        }
    },
    methods: {
        onClickSummary(row){
            this.$refs.summary.show({
                data: row,
            });
        }
    }
}