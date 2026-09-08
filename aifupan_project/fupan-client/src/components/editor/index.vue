<template>
  <div class="h100">
    <div ref="editor" class="editor-box h100"></div>
  </div>
</template>

<script>
import E from "wangeditor";
import myUtils from "@/utils/utils";
export default {
  name: "editor",
  data() {
    return {
      editor: null,
      changeDebounce: myUtils.debounce(500,true)
    };
  },
  props: {
    value: {
      type: String,
      default: "",
    },
    editorHttp:{
      type: Function,
      default: null
    },
    notMenus: {
      type: Array,
      default: ()=>{return []}
    },
    notAllMenus: {
      type: Boolean,
      default: false
    },
    notLoadHtml: {
      type: Boolean,
      default: false
    },
    editStatus:{
      type: Boolean,
      default: false
    }
  },
  mounted(){
    this.$nextTick(()=>{
      this.init(this.value)
    })
  },
  methods: {
    init(value) {
      let editor = new E(this.$refs.editor);
      editor.config.menus = [
        "head",
        "bold", // 粗体
        "fontSize", // 字号
        "fontName", // 字体
        "italic", // 斜体
        "underline", // 下划线
        "strikeThrough", // 删除线
        "indent", // 缩进
        "lineHeight", // 行高
        "foreColor", // 文字颜色
        "backColor", // 背景颜色
        "link", // 插入链接
        "list", // 列表
        // "todo", //待办事项
        "justify", // 对齐方式
        "quote", // 引用
        "emoticon", // 表情
        "image", // 插入图片
        "video", //视频
        "table", // 表格
        // "code", //代码
        "splitLine",
        "undo", // 撤销
        "redo", // 恢复
      ]?.filter(d=>{
        if(this.notAllMenus){
          return false
        }
        return !this.notMenus.includes(d);
      });
      editor.config.customMenus = []
      let isFirstLoad = true
      editor.config.onchange = (html) => {
        this.$emit("input", html);
        // 判断是否是首次加载
        
        if(!this.editStatus && isFirstLoad){
          isFirstLoad = false
        } else if(!isFirstLoad){
          // 判断是否已经首次加载完成
          this.changeDebounce(()=>{
            this.$emit('change', html);
          })
        }
      };
      editor.config.uploadImgServer = this.editorHttp || this.$httpCommon?.upload || null;
      editor.config.uploadImgMaxSize = 10 * 1024 * 1024; // 将图片大小限制为 10M
      editor.config.uploadFileName = "file"; //后端接受上传文件的参数名
      editor.config.uploadImgMaxLength = 1; // 限制一次最多上传 1 张图片
      editor.config.showLinkImg = false; //隐藏网络图片上传
      editor.config.uploadImgHeaders = {
        Token: this.$store.state.token,
      };
      editor.config.uploadImgHooks = {
        fail: (xhr, editor, result) => {
          // 插入图片失败回调
          console.log(xhr, editor, result);
        },
        success: (xhr, editor, result) => {
          // 图片上传成功回调
          console.log(xhr, editor, result);
        },
        timeout: (xhr, editor) => {
          // 网络超时的回调
          console.log("网络超时", xhr, editor);
        },
        error: (xhr, editor) => {
          // 图片上传错误的回调
          console.log("上传错误", xhr, editor);
        },
        //回显
        customInsert: (insertImg, result) => {
          let url = this.$http.common.pic + result.data;
          insertImg(url);
        },
      };
      editor.create();
      // 判断是否不加载html,需要手动加载，这种情况需要手动加载。
      if(!this.notLoadHtml){
        editor.txt.html(value);
      }
      this.editor = editor;
    },
    setEditrHtml(html){
      this.$nextTick(()=>{
        this.editor.txt.html(html);
      })
    },

    destroy() {
      if (this.editor) {
        this.editor.destroy();
        this.editor = null;
      }
    },
  },
};
</script>

<style scoped lang="scss">
.editor-box {
  display: flex;
  flex-direction: column;
  ::v-deep(.w-e-text-container){
    z-index: 1000 !important;
    flex: 1;
    .w-e-text{
      min-height: initial !important;
      p{
        font-size: 14px !important;
      }
    }
    
  }
  ::v-deep(.w-e-toolbar){
    z-index: 1001 !important;
    .w-e-menu{
      width: 29px !important;
      height: 29px !important;
    }
  }

  ::v-deep(.w-e-menu) {
    i{
      font-family: "font_family" !important; /* 使用 Iconfont 字体 */
      font-style: normal;
      -webkit-font-smoothing: antialiased;
    }
    &[data-title="背景色"],&[data-title="文字颜色"]{
      .w-e-block{
        .w-e-item:nth-of-type(2){
          background: rgba(0,0,0,.05);
        }
      }
    }
    /* 针对不同按钮设置图标 */
    &[data-title="标题"] i::before {
      content: "\e668" !important;
    }
    &[data-title="加粗"] i::before {
      content: "\e65d" !important;
    }
    &[data-title="字号"] i::before {
      content: "\e657" !important;
    }
    &[data-title="斜体"] i::before {
      content: "\e661" !important;
    }
    &[data-title="下划线"] i::before {
      content: "\e667" !important;
    }
    &[data-title="删除线"] i::before {
      content: "\e658" !important;
    }
    &[data-title="缩进"] i::before {
      content: "\e65e" !important;
    }
    &[data-title="行高"] i::before {
      content: "\e65a" !important;
    }
    &[data-title="文字颜色"] i::before {
      content: "\e666" !important;
    }
    &[data-title="背景色"] i::before {
      content: "\e655" !important;
    }
    &[data-title="序列"] i::before {
      content: "\e665" !important;
    }
    &[data-title="对齐"] i::before {
      content: "\e664" !important;
    }
    &[data-title="引用"] i::before {
      content: "\e662" !important;
    }
    &[data-title="分割线"] i::before {
      content: "\e659" !important;
    }
    &[data-title="撤销"] i::before {
      content: "\e663" !important;
    }
    &[data-title="恢复"] i::before {
      content: "\e65c" !important;
    }
    &[data-title="全屏"] i::before {
      content: "\e65b" !important;
    }
    &[data-title="取消全屏"] i::before {
      content: "\e65b" !important;
    }
  }

}

</style>
