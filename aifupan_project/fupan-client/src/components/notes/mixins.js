export default {
    data(){
        return {
            isNotes: false
        }
    },
    methods:{
        startNotes(){
            this.isNotes = true;
        },
        quitNotes(){
            this.isNotes = false;
        }
    }
}