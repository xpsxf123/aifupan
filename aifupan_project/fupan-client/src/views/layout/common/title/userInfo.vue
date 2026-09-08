<template>
     <!-- 名字/头像 -->
    <div class="user-info-box">
        <div class="user-info-content">
            <img :src="userInfo.avatar" class="avatarImg" v-if="userInfo.avatar">
            <img src="@/assets/imgs/avatar.png" class="avatarImg" v-else>
            <div class="nameText single-line">{{ userInfo.nickName||'匿名用户' }}</div>
        </div>
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        
    },
    data() {
        return {
            userInfo: {},
            count: 0
        };
    },
    computed: {},
    watch: {},
    methods: {
        // 获取用户信息
        getUserInfo() {
            this.$httpBack.user.infoByClient({}).then(res => {
                if (res.code == 0 && res.data) {
                    this.userInfo = res.data;
                    this.$store.commit("saveUserInfo", res.data);
                }
            });
        },
        getUserInfoTime(){
            if(!this.$store.getters?.getToken){return}
            this.userInfo = this.$store.state.userInfo;
            // 表示没有用户数据
            if(Object.keys(this.userInfo).length > 0){return}
            if(this.count>3){
                return
            }
            this.getUserInfo() //获取用户信息
            setTimeout(()=>{
                this.count++;
                this.getUserInfoTime(); //防漏用户数据
            },3000)
        }
    },
    created() {
        this.getUserInfoTime();
    },
    mounted() {
        
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.user-info-box{
    display: inline-block;
    height: 100%;
    .user-info-content{
        height: 100%;
        display: inline-block;
        align-items: center;
        display: flex;
    }
}
.avatarImg{
    max-height: calc(100% - 8px);
    border-radius: 50%;
    aspect-ratio: 1 / 1;
    object-fit: cover;
}
.nameText{
    margin-left: 10px;
    max-width: 120px;
    display: inline-block;
}
</style>