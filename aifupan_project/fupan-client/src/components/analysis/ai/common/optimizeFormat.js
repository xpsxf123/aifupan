export default {
    data(){
        return {

        }
    },
    methods: {
        optimizeFormat(callback) {
            this.trackReplayListEvent?.('P003_A0043');
            this.$confirm(`
                <div style="text-align: center">
                    <div>本次输出格式您是否满意？</div>
                    <div style="padding-block:5px">如果您不满意，是否需要我帮您优化输出格式？</div>
                    <div class="font-bold">（优化格式不需要消耗您的额外算力！请放心使用）</div>
                </div>
            `, '友情提示', {
                confirmButtonText: '点我优化格式',
                cancelButtonText: '不需要',
                closeOnClickModal: false,
                closeOnPressEscape: false,
                showClose: false,
                dangerouslyUseHTMLString: true,
                customClass: 'format-front',
            }).then(() => {
                if(callback){
                    callback?.()
                }else {
                    this.aiCorrectFormat()
                }
            }).catch(() => {
            });
        },
    }
}