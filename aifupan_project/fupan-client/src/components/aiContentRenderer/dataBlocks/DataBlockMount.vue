<template>
    <component :is="is" v-bind="bindProps" />
</template>

<script>
export default {
    props: {
        is: {
            type: [String, Object, Function],
            default: null
        },
        bindProps: {
            type: Object,
            default: () => ({})
        }
    }
}
</script>

