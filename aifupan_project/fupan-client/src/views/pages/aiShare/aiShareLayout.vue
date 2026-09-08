<template>
    <div class="common-bg">
        <shareLogin v-if="isLogin" @changeType="onChangeType"></shareLogin>
        <transition v-else>
            <router-view @changeType="onChangeType" />
        </transition>
    </div>
</template>


<script>
import shareLogin from './../common/shareLogin.vue';
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
    },
    watch: {

    },
    mounted() {
    },
    methods: {
        onChangeType(type) {
            this.type = type;
            // if(type !== 'login' && type !== 'lose'){
            //     // location.reload();
            // }
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