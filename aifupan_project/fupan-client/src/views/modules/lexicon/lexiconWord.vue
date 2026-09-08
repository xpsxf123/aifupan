<template>
    <div>
        <lexiconWordList v-if="lexiconId" :lexiconId="lexiconId" :wordType="wordType" />
    </div>

</template>

<script>
import lexiconWordList from './lexicon-word-list.vue';
export default {
    components: { lexiconWordList },
    data() {
        return {
            lexiconId: '',
            wordType: 0,
        }
    },
    mounted(){
        this.getQuery();
    },
    activated(){
        this.getQuery();
    },
    methods: {
        getQuery(){
            let query = this.$route.query;
            this.lexiconId = query.lexiconId;
            this.wordType = Number(query.wordType);
        }
    }
}
</script>

<style></style>