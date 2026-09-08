<template>
    <div class="loading" v-if="isLoading">
     <div class="spinner-contet">
        <div class="spinner">
          <img :src="spinImage" style="width: 150px;height: 150px;">
        </div>
        <div style="margin-top: -35px;color: #1997ff;font-size: 14px;">{{ dialogText }}</div>
     </div>
    </div>
</template>
  
<script>

  export default {
    props: {
        dialogText: {
            type: String,
            required: true
        },
        // spinImage:{
        //     type: String,
        //     required:true
        // },
    },
    watch: {
      isLoading:{
        handler(val){
        }
      }
    },
    data(){
        return{
          spinImage:require('@/assets/imgs/loading.gif'),
          isLoading:false,
        }
    },
    methods:{
        showLoading(){
            this.isLoading = true;
        },
        show(){
          this.showLoading()
        },
        hide(){
          this.isLoading = false
        }
    }
    
  };
</script>  

<style scoped>
  .loading {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(38,50,56,.7);
    z-index: 1000000;
  }

  .spinner-contet{
    margin-bottom: 100px;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: column;
  }
  
  .spinner {
    /* background-size: cover;
    background-repeat: no-repeat;
    width: 50px;
    height: 50px; */
    /* animation: spin 1s linear infinite; */
  }

</style>