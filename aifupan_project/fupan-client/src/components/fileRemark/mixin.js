import FileRemark from './index.vue';
export default {
    components: {
        FileRemark
    },
    data(){
        return {

        }
    },
    methods: {
        onClickFileRemark(row){
            this.$refs.fileRemark.show({
                data: row,
            });
        }
    }
}