<template>
    <div class="listColContainer1">
        <img :src="item.AnchorAvatar" class="compereImg">
        <div class="compereInfoContainer">
            <div class="compereName">{{ item.AnchorName }}</div>
            <div class="compereStatusContainer">
            <div v-if="item.LiveStatus == 0" class="compereNotPlayStatus">未检测</div>
            <div v-if="item.LiveStatus == -1" class="compereNotPlayStatus">检测中</div>
            <div v-if="item.LiveStatus == 4" class="compereNotPlayStatus">未开播</div>
            <div v-if="item.LiveStatus == 2" class="comperePlayStatus">
                <img src="@/assets/imgs/play.png" class="comperePlayStatusImg">
                <div class="comperePlayStatusText">直播中</div>
            </div>
            <div class="compereResourceType"
                v-if="item.platform === PLATFORM_ENUM.douyin">
                <img src="@/assets/imgs/icon/douyin.png" class="compereResourceTypeImg">
            </div>
            <div class="compereResourceType" v-if="item.platform == PLATFORM_ENUM.kuaishou">快手</div>
            <!-- <div class="compereResourceType" v-if="item.RecordStatus">
              <div v-if="item.RecordStatus == 0">未录制</div>
              <div v-if="item.RecordStatus == 1">录制中</div>
              <div v-if="item.RecordStatus == 2">手动停止</div>
              <div v-if="item.RecordStatus == 3">录制完成</div>
              <div v-if="item.RecordStatus == 4">手动开启中</div>
            </div> -->
              <!-- <i class="recordStatus danger-color font_family icon-luzhi"></i> -->
          </div>
      </div>
</div>
</template>

<script>
import { PLATFORM_ENUM } from '@/enum'
export default {
    components: {},
    props:{
        item:{
            type:Object,
            default: ()=>{
                return {}
            }
        }
    },
    data() {
        return {
        
        };
    },
    computed: {},
    watch: {},
    methods: {
        
    },
    created() {
        
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
.listColContainer1 {
  flex: 2;
  display: flex;
  align-items: center;
}
.compereImg {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}
.compereInfoContainer {
  margin-left: 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.compereName {
  font-size: 14px;
  color: #2E3742;
}
.compereStatusContainer {
  display: flex;
  align-items: center;
}
.compereNotPlayStatus {
  font-size: 13px;
  color: #95A1AF;
}
.comperePlayStatus {
  display: flex;
  align-items: center;
}
.comperePlayStatusImg {
  width: 18px;
}
.comperePlayStatusText {
  margin-left: 4px;
  font-weight: 500;
  font-size: 13px;
  color: #FF3270;
}
.compereResourceType {
  border-radius: 8px;
  font-size: 10px;
  color: #FFFFFF;
  margin-left: 5px;
  display: flex;
  align-items: center;
}
.compereResourceTypeImg {
  width: 16px;
  height: 16px;
}
.compereResourceType {
  border-radius: 8px;
  font-size: 10px;
  color: #FFFFFF;
  margin-left: 5px;
  display: flex;
  align-items: center;
}

</style>