export default {
    urlMap: {},
    add(url){
        this.urlMap[url]=true
    },
    del(url){
        this.urlMap[url] = false;
    },
    edit(url,state){
        this.urlMap[url] = state;
    },
    get(url){
        return this.urlMap[url]
    }
}