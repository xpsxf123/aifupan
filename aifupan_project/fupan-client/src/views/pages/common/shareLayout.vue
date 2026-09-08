<template>
    <div>
        <shareLogin v-if="isLogin" @changeType="onChangeType"></shareLogin>
        <div v-if="isLose" class="not-found">
            <img src="@/assets/imgs/not_found.png" style="width: 354px;height: 266px;">
            <span>本复盘已经被删除或失效，请联系链接分享者哦</span>
        </div>
        <transition v-else name="el-fade-in-linear">
            <router-view @changeType="onChangeType" />
        </transition>
    </div>
</template>


<script>
import shareLogin from './shareLogin.vue';
export default {
    components: {
        shareLogin
    },

    data() {
        return {
            type: '',
        }
    },
    computed: {
        isLogin(){
            return this.type === 'login';
        },
        isLose(){
            return this.type === 'lose';
        }
    },
    watch: {

    },
    mounted() {
    },
    methods: {
        onChangeType(type) {
            this.type = type;
            if(type !== 'login' && type !== 'lose'){
                location.reload();
            }
        }
    }
}
</script>

<style lang="scss">
.not-found {
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: column;
    color: #151719;
    font-size: 16px;
    height: 100vh;
}
</style>