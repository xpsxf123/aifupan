const channel = (datas,opt={})=>{
    const {version = '1'} = opt;
    // 分配版本
    let findIndex = versions.findIndex(d=>d === version);
    if(findIndex === -1){return datas};
    let versionData = null;
    while(findIndex < versions.length){
        versionData = versionChannel[version[findIndex]](versionData || datas,opt);
        findIndex++;
    }
    return versionData;
} 
// 渲染函数
const r = (item,html,render)=>{
    return render && render(item, html) || html;
}
const channelFunc = (datas,callback, opt)=>{
    // 自定义渲染
    const {render = null} = opt;
    let data = {
        ...datas,
        html: ''
    }
    // 处理数据
    datas?.forEach(item=>{
        data.html+= r(item,callback && callback(item) || item, render) 
    })
    return data
}
// 数据版本1清洗通道，第一版清洗 后续清洗将会沿用此通道经版本递增清洗
const version_1_Channel = (datas,opt={})=>{
    const { sentenceMarkList } = datas;
    let data = channelFunc(sentenceMarkList,(item)=>{
        return `<p>${item?.content}</p>`||'';
    }, opt);
    return data;
}

const versionChannel = {
    '1': version_1_Channel,
}

const versions = Object.keys(versionChannel);



export default channel;