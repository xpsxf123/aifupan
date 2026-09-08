


import re
import json
import jmespath
import requests
from datetime import datetime
from urllib.parse import quote, unquote



# 时间戳转日期
def convert_timestamp(timestamp):
    if timestamp:
        dt_object = datetime.fromtimestamp(timestamp)
        formatted_time = dt_object.strftime('%Y-%m-%d %H:%M:%S')
        return formatted_time
    else:
        return None



# 解析视频详情信息
def get_video_detail(modal_id):
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "Accept-Encoding": "gzip, deflate, br, zstd",
        "sec-ch-ua": "\"Microsoft Edge\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"",
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": "\"Windows\"",
        "upgrade-insecure-requests": "1",
        "sec-fetch-site": "none",
        "sec-fetch-mode": "navigate",
        "sec-fetch-user": "?1",
        "sec-fetch-dest": "document",
        "accept-language": "zh-CN,zh;q=0.9",
        "priority": "u=0, i"
    }
    cookies = {
        "ttwid": "1%7ChhRW0k9lmpA9qAwAPqalLeHta-2cat_GYAX6o6nZ9D0%7C1754307613%7Caa0d166b8fb1f1f60c56d8a2d0015fb00e83a6f798a7f154b2fe8d1dcdeb8750"
    }

    url = f"https://www.douyin.com/jingxuan?modal_id={modal_id}"

    response = requests.get(url, headers=headers)
    response_text = response.text
    print(response)
    response_text = unquote(response_text.replace('\\u0026', '&'))

    videoDetail = re.search(r'"videoDetail":(.*?),"lazyLoadConfig"', response_text)
    if videoDetail:
        videoDetail = json.loads(videoDetail.group(1))

        authorInfo = videoDetail.get('authorInfo', {})
        video = videoDetail.get('video', {})
        stats = videoDetail.get('stats', {})

        video_play_addr = None
        playAddr = video.get('playAddr', [])
        if playAddr:
            for i in playAddr:
                if 'v3-web.douyinvod.com' in i['src']:
                    video_play_addr = i['src']

        dataSize = video.get('dataSize')

        item = {
            "authorInfo": {
                "uid": authorInfo.get('uid'),
                "secUid": authorInfo.get('secUid'),
                "nickname": authorInfo.get('nickname'),
                "avatarUri": authorInfo.get('avatarUri'),
                "followerCount": authorInfo.get('followerCount'),
                "totalFavorited": authorInfo.get('totalFavorited'),
            },


            "awemeId": videoDetail.get('awemeId'),
            "awemeType": videoDetail.get('awemeType'),
            "desc": videoDetail.get('desc'),
            "images": videoDetail.get('images'),
            "createTime": convert_timestamp(videoDetail.get('createTime')),

            "video": {
                "dynamicCover": video.get("dynamicCover"),
                "duration": int(video.get('duration') / 1000) if video.get('duration') > 1000 else video.get('duration'),
                "dataSize": f"{(dataSize/1024/1024):.2f}MB" if dataSize > 1024 else dataSize,
                "video_play_addr": video_play_addr,
            },
            "music": {
                "playUrl": jmespath.search('music.playUrl.uri', videoDetail),
                "musicName": jmespath.search('music.musicName', videoDetail),
                "duration": jmespath.search('music.duration', videoDetail),
            },


            "stats": {
                "commentCount": stats.get('commentCount'),
                "diggCount": stats.get('diggCount'),
                "shareCount": stats.get('shareCount'),
                "collectCount": stats.get('collectCount'),
                "recommendCount": stats.get('recommendCount'),
            }


        }
        print(item)
        # print(json.dumps(item, ensure_ascii=False, indent=4))



if __name__ == '__main__':
    modal_id = '7531331104026201384'
    get_video_detail(modal_id)