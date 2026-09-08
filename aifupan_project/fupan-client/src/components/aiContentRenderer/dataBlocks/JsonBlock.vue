<template>
    <div class="aiDataBlockJson" v-html="html"></div>
</template>

<script>
import renderJsonResponseContent from '../renderers/jsonRenderParser'

export default {
    props: {
        value: {
            type: [Object, Array, String, Number, Boolean],
            default: null
        }
    },
    computed: {
        html() {
            if (this.value === null || this.value === undefined) return ''
            const text = typeof this.value === 'string' ? this.value : JSON.stringify(this.value)
            const res = renderJsonResponseContent(text, {})
            return res?.html || ''
        }
    }
}
</script>

