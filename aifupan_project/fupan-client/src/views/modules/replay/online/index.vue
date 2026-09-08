<template>
    <div>
        <online-video v-if="ifTabsIndex(0)" @updateProperty="getUserProperty" :space="getSpace" ></online-video>
        <online-contrast v-if="ifTabsIndex(1)" @updateProperty="getUserProperty"  :space="getSpace"></online-contrast>
    </div>
</template>

<script>
import myUtils from '../../../../utils/utils';
import tabs from '@/mixins/tabs.js';
import OnlineVideo from "./online-video.vue";
import OnlineContrast from './online-contrast.vue';
export default {
    mixins: [tabs],
    components: {
        OnlineVideo,
        OnlineContrast
    },
    inject: ['appVnode'],
    data() {
        return {
            tabs: [
                {label:'单场复盘',name: 'video'},
                {label:'对比分析',name: 'contrast'}
            ],
        };
    },
    computed: {
        userProperty(){
            return this.$store.getters.getUserproperty
        },
        getSpace(){
            if(this.userProperty?.totalStorageNum === undefined || this.userProperty?.storageNum === undefined){
                return '0/0 G';
            }
            let a = (this.userProperty?.totalStorageNum - this.userProperty?.storageNum) / 1024 / 1024;
            let b = this.userProperty?.totalStorageNum / 1024 / 1024;
            return `${this.retainDecimals(a)} / ${this.retainDecimals(b)} G`
        }
    },
    mounted() {
    },

    methods: {  
        retainDecimals(val) {
            return myUtils.retainDecimals(val);
        },
        getUserProperty(){
            this.appVnode.getUserproperty();
        }
    },
    activated(){
    }
};
</script>

<style scoped lang="less">
</style>