<template>
  <div>
    <div ref="editorRoot"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import E from 'wangeditor'
import api from '@/utils/request-api.js'
import { useUserInfoStore } from '@/store/index.js'
import { storeToRefs } from 'pinia'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})
const emit = defineEmits(['update:modelValue'])

const editorRoot = ref()
let editorInstance = null

const userInfoStore = useUserInfoStore()
const {token} = storeToRefs(userInfoStore)

const init = (value) => {
  destroy()
  const editor = new E(editorRoot.value)
  editor.config.menus = [
    'head',
    'bold',
    'fontSize',
    'fontName',
    'italic',
    'underline',
    'strikeThrough',
    'indent',
    'lineHeight',
    'foreColor',
    'backColor',
    'link',
    'list',
    'justify',
    'quote',
    'emoticon',
    'image',
    'table',
    'splitLine',
    'undo',
    'redo'
  ]
  editor.config.onchange = (html) => {
    emit('update:modelValue', html)
  }
  editor.config.uploadImgServer = api.common.uploadImg + '?resourceType=6'
  editor.config.uploadImgMaxSize = 30 * 1024 * 1024
  editor.config.uploadFileName = 'file'
  editor.config.uploadImgMaxLength = 1
  editor.config.showLinkImg = false
  editor.config.uploadImgHeaders = {
    Token: token.value || ''
  }
  editor.config.uploadImgHooks = {
    fail: (xhr, ed, result) => {
      console.log(xhr, ed, result)
    },
    success: (xhr, ed, result) => {
      console.log(xhr, ed, result)
    },
    timeout: (xhr, ed) => {
      console.log('网络超时', xhr, ed)
    },
    error: (xhr, ed) => {
      console.log('上传错误', xhr, ed)
    },
    customInsert: (insertImg, result) => {
      const url = result?.data?.url
      insertImg(url)
    }
  }
  editor.create()
  editor.txt.html(value ?? props.modelValue)
  editorInstance = editor
}

const destroy = () => {
  if (editorInstance) {
    editorInstance.destroy()
    editorInstance = null
  }
}

onMounted(() => {
  init()
})

onBeforeUnmount(() => {
  destroy()
})

defineExpose({init, destroy})
</script>

<style scoped></style>
