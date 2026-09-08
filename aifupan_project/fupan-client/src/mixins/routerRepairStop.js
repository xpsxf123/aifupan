export default {
    data() {
        return {
            routerBack: false,
            isNotesEditer: false,
            routerBackMap: {}
        }
    },
    mounted() {
        this.setRouterBackMap();
    },
    methods: {
        setRouterBackMap(){
            const fns = Object.values(this.routerBackMap)?.filter(d=>!!d);
            this.$store.state.routerPathBack = fns?.length?()=>{
                fns?.forEach(fn=>{
                    if(typeof fn === 'function'){
                        fn();
                    }
                })
            }:null
        },
        setNotesEditerChanges(val){
            if(val){
                this.routerBackMap['2']=()=>{
                    this.$refs?.analysis?.isNotesEditer(()=>{
                        this.isNotesEditer = true;
                        // console.log(this.isNotesEditer);
                        setTimeout(()=>{
                            this.$router.back();
                        },100);
                    });
                }
            }else{
                this.routerBackMap['2'] = null;
            }
            this.setRouterBackMap();
        },
        setHasUnsavedChanges(val){
            if(val){
                this.routerBackMap['1'] = ()=>{
                    this.$refs?.analysis?.isRepairStop((bl)=>{
                        if(bl){
                            this.routerBack = true;
                            setTimeout(()=>{
                                this.$router.back();
                            },100);
                        }
                    })
                }
                // this.$store.state.routerPathBack = () => {
                //     this.$refs?.analysis?.isRepairStop((bl)=>{
                //         if(bl){
                //             this.routerBack = true;
                //             setTimeout(()=>{
                //                 this.$router.back();
                //             },100);
                //         }
                //     })
                // }
            }else{
                // this.$store.state.routerPathBack = null;
                this.routerBackMap['1'] = null;
            }
            this.setRouterBackMap();
        }
    },
    async beforeRouteLeave(to,form,next){
        let status = await this.$refs?.analysis?.isNotesEditer(()=>{next();});
        if(status && !this.isNotesEditer){
            next(false);
        }else{
            if(this.routerBack || this.isNotesEditer){
                return next();
            }
            this.$refs?.analysis?.isRepairStop((bl)=>{
                if(bl){
                    next()
                }else{
                    next(bl);
                }
            })
        }
    }
}