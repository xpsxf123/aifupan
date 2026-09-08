# -*- coding: utf-8 -*-
# @Time    : 2025/12/18 10:41
# @Author  : shark
# @File    : 直播大屏.py


import re
import json
import time
import random
import math
import requests
import jmespath

from loguru import logger
from utils.buyin_tool import BuyinUtils


class Spider(object):
    def __init__(self):
        self.room_id = "7658483066440026926"

        self.product_id = "3767121942352626067"

        self.cookies = {
            'LUOPAN_DT': 'session_7659358143452889396',

        }
        self.headers = {
            'accept': 'application/json, text/plain, */*',
            'accept-language': 'zh-CN,zh;q=0.9',
            'cache-control': 'no-cache',
            'pragma': 'no-cache',
            'priority': 'u=1, i',
            'referer': f'https://compass.jinritemai.com/screen/live/talent?live_room_id={self.room_id}',
            'sec-ch-ua': '"Microsoft Edge";v="143", "Chromium";v="143", "Not A(Brand";v="24"',
            'sec-ch-ua-mobile': '?0',
            'sec-ch-ua-platform': '"Windows"',
            'sec-fetch-dest': 'empty',
            'sec-fetch-mode': 'cors',
            'sec-fetch-site': 'same-origin',
            'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0',
        }


    # 直播后 - 直播流地址
    def live_screen_review_video_link(self, room_id):
        index_item = {
            "end_ts": "直播结束时间",
            "expire_ts": "直播流过期时间（相对于请求时间）",
            "start_ts": "直播开始时间",
            "url": "m3u8直播流地址",
        }

        params = {
            'room_id': room_id,
        }

        url = 'https://compass.jinritemai.com/compass_api/content_live/author/live_screen/review_video_link'
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)
        print(response)
        print(response.json())

        json_data = response.json()
        if json_data.get("st") == 0:
            item = {
                'end_ts': BuyinUtils.convert_timestamp(jmespath.search('data.end_ts', json_data)),
                'expire_ts': BuyinUtils.convert_timestamp(jmespath.search('data.expire_ts', json_data)),
                'start_ts': BuyinUtils.convert_timestamp(jmespath.search('data.start_ts', json_data)),
                'url': jmespath.search('data.url', json_data)
            }
            item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
            print(json.dumps(item_zh, indent=2, ensure_ascii=False))
        else:
            print(response)
            print(response.text)


    def main(self):

        self.live_screen_review_video_link(self.room_id)

        # self.live_screen_product_overall_trend(self.room_id, self.product_id)


if __name__ == '__main__':
    s = Spider()
    s.main()
