<template>
    <div>
        <div v-if="childData == 'lexiconList'">
            <lexiconList @child-event="switchUpdate" />
        </div>
        <div v-if="childData == 'lexiconWordList'">
            <lexiconWordList :lexiconId="lexiconId" :wordType="wordType" @child-event="switchList" />
        </div>
    </div>

</template>

<script>
import lexiconList from './lexicon-list.vue';
import lexiconWordList from './lexicon-word-list.vue';
export default {
    components: { lexiconList, lexiconWordList },
    data() {
        return {
            lexiconId: '',
            wordType: "",
            childData: 'lexiconList'
        }
    },
    methods: {
        switchUpdate(lexiconId, wordType) {
            this.childData = 'lexiconWordList';
            this.lexiconId = lexiconId;
            this.wordType = wordType;
        },
        switchList() {
            this.childData = 'lexiconList';
        }
    }
}
</script>

<style></style>