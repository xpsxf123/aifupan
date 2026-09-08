# -*- coding: utf-8 -*-
# @Time    : 2026/5/29 17:05
# @Author  : shark
# @File    : 直播明细.py


import re
import json
import time
import random
import uuid
import math
import requests
import jmespath

from utils.buyin_tool import BuyinUtils

class Spider(object):
    def __init__(self):
        self.cookies = {
            # https://compass.jinritemai.com/passport/sso/aff/login/callback
            # "ucas_c0_buyin": "CkIKBTEuMC4wEK6Ii8zin7qOahi9LyD7o9CUpsyVBSiPETDzjrDFzcz9AkCA0vPQBkiAhrDTBlC2vN3YwpGH22lYgQESFPZAZ1iOmkGjQ60xPk85ngd5F8vQ",
            # "ucas_c0_ss_buyin": "CkIKBTEuMC4wEK6Ii8zin7qOahi9LyD7o9CUpsyVBSiPETDzjrDFzcz9AkCA0vPQBkiAhrDTBlC2vN3YwpGH22lYgQESFPZAZ1iOmkGjQ60xPk85ngd5F8vQ",
            # "LUOPAN_DT": "session_7646242234983153929"

            # 'SASID': 'SID2_7646246588236103986',
            # 'BUYIN_SASID': 'SID2_7646246588236103986',

            'ucas_c0_buyin': 'CkIKBTEuMC4wELSIj-iI7Nmlahi9LyD5lLDn3s2pBCiPETDzjrDFzcz9AkDjzq3SBkjjgurUBlCKvMWSgq7spmlYgQESFIzN52GCnrohaEiMaX61BTI1Yqn4',
            'ucas_c0_ss_buyin': 'CkIKBTEuMC4wELSIj-iI7Nmlahi9LyD5lLDn3s2pBCiPETDzjrDFzcz9AkDjzq3SBkjjgurUBlCKvMWSgq7spmlYgQESFIzN52GCnrohaEiMaX61BTI1Yqn4',

        }


    # 直播明细 - 直播间明细
    def history_live(self):
        '''
        // 子账号无权限
        <Response [200]>
        {"st":10008,"msg":"子账号无权限","data":null}

        // cookie错误
        <Response [200]>
        {"st":10012,"msg":"当前视角与页面不匹配","data":null}

        :return:
        '''
        headers = {
            "accept": "application/json, text/plain, */*",
            "accept-language": "zh-CN,zh;q=0.9,en;q=0.8",
            "cache-control": "no-cache",
            "pragma": "no-cache",
            "priority": "u=1, i",
            "referer": f"https://buyin.jinritemai.com/dashboard/compass-home/live-list?universal_page_params_id={str(uuid.uuid4())}",
            "sec-ch-ua": "\"Chromium\";v=\"148\", \"Google Chrome\";v=\"148\", \"Not/A)Brand\";v=\"99\"",
            "sec-ch-ua-mobile": "?0",
            "sec-ch-ua-platform": "\"Windows\"",
            "sec-fetch-dest": "empty",
            "sec-fetch-mode": "cors",
            "sec-fetch-site": "same-origin",
            "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"
        }

        url = "https://buyin.jinritemai.com/compass_api/content_live/author/live_detail/history_live"

        index_groups = [

            {"index_name": "watch_ucnt", "index_display": "直播间观看人数", "index_tips": "",
             "group_display": "直播指标"},
            {"index_name": "avg_hour_watch_ucnt", "index_display": "单小时观看人数", "index_tips": "",
             "group_display": "直播指标"},
            {"index_name": "watch_cnt", "index_display": "直播间观看人次", "index_tips": "",
             "group_display": "直播指标"},
            {"index_name": "pcu", "index_display": "最高在线人数", "index_tips": "", "group_display": "直播指标"},
            {"index_name": "acu", "index_display": "平均在线人数", "index_tips": "", "group_display": "直播指标"},
            {"index_name": "avg_watch_duration", "index_display": "人均观看时长", "index_tips": "",
             "group_display": "直播指标"},
            {"index_name": "comment_cnt", "index_display": "评论次数", "index_tips": "", "group_display": "直播指标"},
            {"index_name": "fans_club_ucnt", "index_display": "新加直播团人数",
             "index_tips": "在直播间内通过头像加入粉丝团的用户人数，不含通过福袋加入的用户，不去除取消的用户",
             "group_display": "直播指标"},
            {"index_name": "incr_fans_cnt", "index_display": "新增粉丝数",
             "index_tips": "在直播间内点击关注主播的用户人数，不去除取消关注的用户", "group_display": "直播指标"},
            {"index_name": "decr_fans_cnt", "index_display": "取关粉丝数",
             "index_tips": "在直播间内取消关注主播的用户人数，不去除重新关注的用户", "group_display": "直播指标"},
            {"index_name": "fans_rate", "index_display": "看播粉丝占比",
             "index_tips": "直播间看播的用户中，看播前一天已经是主播粉丝的用户占比", "group_display": "直播指标"},
            {"index_name": "pay_order_fans_rate", "index_display": "成交粉丝占比",
             "index_tips": "直播间全部成交人数中看播前一天已经关注主播的人数占比", "group_display": "电商指标"},
            {"index_name": "popular_product_cnt", "index_display": "带货商品数", "index_tips": "",
             "group_display": "电商指标"},
            {"index_name": "product_show_ucnt", "index_display": "直播间商品曝光人数",
             "index_tips": "在直播间内看到商品展示的人数，包含讲解商品卡展示+闪购商品展示+点击购物车后的商品列表的商品展示",
             "group_display": "电商指标"},
            {"index_name": "product_click_ucnt", "index_display": "直播间商品点击人数",
             "index_tips": "在直播间内点击商品的人数，包含点击讲解商品卡、点击闪购商品卡、点击购物车后的商品列表中的商品",
             "group_display": "电商指标"},
            {"index_name": "product_click_rate", "index_display": "商品点击率(人数)",
             "index_tips": "商品点击人数与商品曝光人数的比值", "group_display": "电商指标"},
            {"index_name": "product_conversation_rate", "index_display": "点击成交转化率(人数)",
             "index_tips": "直播间总成交次数/直播间商品在直播间内的总点击次数", "group_display": "电商指标"},
            {"index_name": "pay_order_cnt", "index_display": "直播间成交订单数",
             "index_tips": "用户从直播间下单的全部成交订单数", "group_display": "电商指标"},
            {"index_name": "pay_gmv", "index_display": "直播间成交金额",
             "index_tips": "用户从直播间下单的全部成交订单金额", "group_display": "电商指标"},
            {"index_name": "avg_hour_pay_amt", "index_display": "单小时GMV", "index_tips": "",
             "group_display": "电商指标"},
            {"index_name": "pay_product_cnt", "index_display": "直播间成交件数",
             "index_tips": "用户从直播间下单的全部成交件数（仅包含直播间下单，不包含直播期间短视频、橱窗等其他渠道下单）",
             "group_display": "电商指标"},
            {"index_name": "pay_ucnt", "index_display": "直播间成交人数", "index_tips": "从直播间下单的全部用户数去重",
             "group_display": "电商指标"},
            {"index_name": "refund_gmv", "index_display": "直播间退款金额",
             "index_tips": "直播间全部成交订单中退款金额", "group_display": "电商指标"},
            {"index_name": "refund_cnt", "index_display": "直播间退款订单数",
             "index_tips": "直播间全部成交订单中退款订单数", "group_display": "电商指标"},
            {"index_name": "refund_ucnt", "index_display": "直播间订单退款人数",
             "index_tips": "直播间全部成交订单中退款人数", "group_display": "电商指标"},
            {"index_name": "predict_commission", "index_display": "预估佣金收入",
             "index_tips": "主播带货的全部成交订单预估带来的佣金收入，数据仅供参考，与实际佣金收入可能不一致",
             "group_display": "电商指标"},
            {"index_name": "product_show_cnt", "index_display": "直播间商品曝光次数", "index_tips": "",
             "group_display": "电商指标"},
            {"index_name": "product_click_cnt", "index_display": "直播间商品点击次数", "index_tips": "",
             "group_display": "电商指标"},
            {"index_name": "prod_click_cnt_rate", "index_display": "商品点击率(次数)",
             "index_tips": "直播间商品点击次数/直播间商品曝光次数", "group_display": "电商指标"},
            {"index_name": "prod_click_to_pay_cnt_rate", "index_display": "点击成交转化率(次数)",
             "index_tips": "直播间总成交次数/直播间商品在直播间内的总点击次数", "group_display": "电商指标"},
            {"index_name": "price_per_pay_combo_in_live", "index_display": "成交件单价",
             "index_tips": "直播间总成交金额/直播间商品总成交件数", "group_display": "电商指标"},
            {"index_name": "watch_cnt_to_pay_rate_in_live", "index_display": "看播成交转化率(次数)",
             "index_tips": "直播间商品在直播间总成交次数/直播间总观看次数", "group_display": "电商指标"},
            {"index_name": "watch_ucnt_to_pay_rate_in_live", "index_display": "看播成交转化率(人数)",
             "index_tips": "直播间商品在直播间总成交人数/直播间总观看人数", "group_display": "电商指标"},
            {"index_name": "pre_sell_order_cnt", "index_display": "预售订单数",
             "index_tips": "预售订单数仅对参加定金预售的商品有效", "group_display": "电商指标"},
            {"index_name": "presale_depay_deamt", "index_display": "预售定金金额", "index_tips": "",
             "group_display": "电商指标"},
            {"index_name": "pre_sell_order_amt", "index_display": "预售全款金额", "index_tips": "",
             "group_display": "电商指标"},
            {"index_name": "ecom_live_ecf_joinclub_ucnt_td", "index_display": "新加购物团人数", "index_tips": "",
             "group_display": "电商指标"},
            {"index_name": "achv_ship_ord_amt", "index_display": "发货金额", "index_tips": "",
             "group_display": "电商指标"},
            {"index_name": "achv_ship_prod_cnt", "index_display": "发货件数", "index_tips": "",
             "group_display": "电商指标"},
            {"index_name": "t7payin8d_predstl_ord_amt", "index_display": "结算有效成交金额", "index_tips": "",
             "group_display": "电商指标"},
            {"index_name": "t7payin8d_predstl_ord_cnt", "index_display": "结算有效成交订单量", "index_tips": "",
             "group_display": "电商指标"},
        ]

        key_index_item = {i['index_name']: i['index_display'] for i in index_groups}

        index_item = key_index_item | {
            "live_room": "直播标题",
            "live_id": "直播id",
            "cover_img_uri": "直播封面",
            "start_time": "直播开始时间",
            "live_duration": "直播时长"
        }

        time_info = BuyinUtils.get_last_7_days_range(7)
        print(time_info)

        index_selected = ','.join(list(key_index_item.keys()))

        # 5个排序字段
        sort_field_item = {
            "start_time": "开播时间",
            "watch_ucnt": "直播间观看人数",
            "avg_watch_duration": "人均观看时长",
            "acu": "平均在线人数",
            "product_click_rate": "商品点击率(人数)",
        }

        params = {
            "is_asc": "false", # 是否升序
            "page_no": "1",
            "page_size": "10", # 可以改为20
            "date_type": time_info['date_type'],
            "begin_date": time_info['history_live_begin_date_timestamp'],
            "begin_date_format": time_info['history_live_begin_date_iso_format'],
            "index_selected": index_selected,
            "sort_field": "", # 排序字段 默认是空
        }
        response = BuyinUtils.request_handler(method="GET", url=url, headers=headers, cookies=self.cookies, params=params)

        json_data = response.json()
        if json_data.get("st") == 0:
            # 选择的指标
            index_selected = json_data['data']['index_selected']

            page_no = jmespath.search('data.page_result.page_no', json_data)
            page_size = jmespath.search('data.page_result.page_size', json_data)
            total = jmespath.search('data.page_result.total', json_data)
            # 计算需要的页数
            total_pages = math.ceil(int(total) / int(page_size))
            print(f"当前页：{page_no}/{total_pages}, 每页{page_size}条， 共{total}条数据")

            for i in json_data['data']['data_result']:
                live_id = jmespath.search('operation.live_id', i)
                live_duration = jmespath.search('start_time.live_duration', i)
                start_time = jmespath.search('start_time.start_time', i)
                item = {
                    "live_room": i.get('live_room'),
                    "live_id": live_id,
                    "cover_img_uri": i.get('cover_img_uri'),
                    "start_time": start_time,
                    "live_duration": live_duration,
                }

                for k, v in i.items():
                    if k in index_selected:
                        item[k] = v

                item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
                print(item)
                print(item_zh)

        else:
            print(response)
            print(response.text)

    def main(self):
        self.history_live()


if __name__ == '__main__':
    s = Spider()
    s.main()
