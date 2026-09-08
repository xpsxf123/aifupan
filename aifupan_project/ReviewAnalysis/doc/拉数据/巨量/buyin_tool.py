

import json
import time
import random
import secrets
from datetime import datetime, timedelta, date
import requests
from urllib.parse import urlencode
from .get_douyin_params import get_fp, get_ms_token, get_ab_use_js, get_ab


class BuyinUtils:
    # 保存json文件
    @staticmethod
    def save_to_json(file_path, json_data):
        with open(file_path, 'w', encoding='utf-8') as file:
            file.write(json.dumps(json_data, indent=2, ensure_ascii=False))

    # 读取json文件
    @staticmethod
    def read_to_json(file_path):
        with open(file_path, 'r', encoding='utf-8') as file:
            json_data = json.loads(file.read())
        return json_data

    @staticmethod
    def gen_ewid():
        return secrets.token_hex(16)

    @staticmethod
    def gen_lid():
        return str(int(time.time() * 1000))[:5] + str(random.random())[2:6]

    @staticmethod
    def format_seconds(seconds):
        '''
        将秒数格式化成 xx天xx小时xx分钟xx秒
        超出24小时自动显示天，单位为0时不显示
        :param seconds: 秒数（int/float）
        :return: 格式化后的时间字符串
        '''
        seconds = int(seconds)
        # 计算天、小时、分钟、秒
        days, remainder = divmod(seconds, 86400)  # 1天 = 86400秒
        hours, remainder = divmod(remainder, 3600)  # 1小时 = 3600秒
        minutes, seconds = divmod(remainder, 60)  # 1分钟 = 60秒

        result = ""
        if days > 0:
            result += f"{days}天"
        if hours > 0:
            result += f"{hours}小时"
        if minutes > 0:
            result += f"{minutes}分钟"
        if seconds > 0:
            result += f"{seconds}秒"

        # 所有单位都为0时返回 0秒
        return result or "0秒"

    @staticmethod
    def format_unit_value(value, unit=None):
        try:
            if value is None:
                return "-"
            if isinstance(value, dict):
                unit = value.get("unit")
                value = value.get("value")

            if unit is None:
                return f"¥{int(value/100)}"
            elif unit == "time":
                return BuyinUtils.format_seconds(value)
            elif unit == "number":
                return f"{value:,}"
            elif unit == "price":
                price = value / 100
                return f"¥{price:.2f}"
            elif unit == "ratio":
                ratio = value * 100
                return f"{ratio:.2f}%"
            else:
                return "-"
        except:
            return value

    @staticmethod
    def convert_timestamp(timestamp):
        if timestamp:
            if len(str(timestamp)) == 13:
                _timestamp = int(timestamp) / 1000
            else:
                _timestamp = int(timestamp)
            dt_object = datetime.fromtimestamp(_timestamp)
            return dt_object.strftime('%Y-%m-%d %H:%M:%S')
        else:
            return None

    @staticmethod
    def request_handler(method, url, headers, cookies, params=None, data=None, need_ab=False, timeout=5, with_ewid=False, ab_use_js=True, **kwargs):
        params = params or {}
        if with_ewid == True:
            params["ewid"] = BuyinUtils.gen_ewid()
        else:
            params["_lid"] = BuyinUtils.gen_lid()

        if need_ab:
            fp = get_fp()
            msToken = get_ms_token(length=184)
            params["verifyFp"] = fp
            params["fp"] = fp
            params["msToken"] = msToken

            ua = headers.get('user-agent', headers.get('User-Agent'))
            if method.upper() == "GET":
                if ab_use_js:
                    a_bogus = get_ab_use_js(ua, params)
                else:
                    a_bogus = get_ab(ua, params)
            else:
                if ab_use_js:
                    a_bogus = get_ab_use_js(ua, params, data)
                else:
                    a_bogus = get_ab(ua, params, data)

            params['a_bogus'] = a_bogus


        response = requests.request(
            method=method,
            url=f"{url}?{urlencode(params)}",
            headers=headers,
            cookies=cookies,
            data=json.dumps(data, separators=(',', ':'), ensure_ascii=False) if data is not None else None,
            timeout=timeout,
            **kwargs
        )
        return response


    # 近7天 近30天
    @staticmethod
    def get_last_7_days_range(days_range=7):
        days_range = int(days_range)
        today = date.today()
        end_date_obj = today - timedelta(days=1)
        begin_date_obj = end_date_obj - timedelta(days=days_range - 1)

        date_format = "%Y/%m/%d"
        time_suffix = " 00:00:00"

        begin_date_str = begin_date_obj.strftime(date_format) + time_suffix
        end_date_str = end_date_obj.strftime(date_format) + time_suffix

        # 转换成 datetime 对象
        dt = datetime.strptime(end_date_str, "%Y/%m/%d %H:%M:%S")
        p_date = str(int(dt.timestamp()))

        date_type_item = {
            7: "21",  # 近7天
            30: "23",  # 近30天
            "d": "2",  # 自然日
            "w": "3",  # 自然周
            "m": "4",  # 自然月
        }

        begin_date_dt = datetime.strptime(begin_date_str, "%Y/%m/%d %H:%M:%S")
        begin_date_timestamp = str(int(begin_date_dt.timestamp()))
        begin_date_iso_format = f"{begin_date_str.split(' ')[0]}T{begin_date_str.split(' ')[1]}+08:00".replace('/', '-')

        end_date_dt = datetime.strptime(end_date_str, "%Y/%m/%d %H:%M:%S")
        end_date_timestamp = str(int(end_date_dt.timestamp()))
        end_date_iso_format = f"{end_date_str.split(' ')[0]}T{end_date_str.split(' ')[1]}+08:00".replace('/', '-')

        # ===================== 新增：历史直播开始时间（原开始时间 +1天）=====================
        history_live_begin_date_obj = begin_date_obj + timedelta(days=1)
        history_live_begin_date_str = history_live_begin_date_obj.strftime(date_format) + time_suffix

        history_live_begin_date_dt = datetime.strptime(history_live_begin_date_str, "%Y/%m/%d %H:%M:%S")
        history_live_begin_date_timestamp = str(int(history_live_begin_date_dt.timestamp()))
        history_live_begin_date_iso_format = f"{history_live_begin_date_str.split(' ')[0]}T{history_live_begin_date_str.split(' ')[1]}+08:00".replace(
            '/', '-')

        return {
            "begin_date": begin_date_str,
            "begin_date_timestamp": begin_date_timestamp,
            "begin_date_iso_format": begin_date_iso_format,
            "end_date": end_date_str,
            "end_date_timestamp": end_date_timestamp,
            "end_date_iso_format": end_date_iso_format,
            "date_type": date_type_item.get(days_range),
            "p_date": p_date,

            # 新增三个字段
            "history_live_begin_date": history_live_begin_date_str,
            "history_live_begin_date_timestamp": history_live_begin_date_timestamp,
            "history_live_begin_date_iso_format": history_live_begin_date_iso_format
        }


def format_sale_range(sale_low, sale_high):
    """
    根据销售价格范围生成格式化字符串

    参数:
        sale_low (int): 最低销售价格
        sale_high (int): 最高销售价格

    返回:
        str: 格式化后的销售范围字符串
    """

    def format_price(price):
        """格式化单个价格数值，避免显示不必要的小数点和零"""
        if price >= 10000:
            # 转换为万单位
            price_in_wan = price / 10000
            # 检查是否为整数
            if price_in_wan.is_integer():
                return f"{int(price_in_wan)}万"
            else:
                # 保留一位小数
                return f"{price_in_wan:.1f}万"
        else:
            return f"{price:}"

    # 格式化最低价格
    formatted_low = format_price(sale_low)

    # 特殊处理：当sale_high为0时显示加号格式
    if sale_high == 0:
        return f"¥{formatted_low}+"

    # 如果最高价格存在且与最低价格不同，则格式化并添加到结果中
    if sale_high is not None and sale_high != sale_low:
        formatted_high = format_price(sale_high)
        return f"¥{formatted_low}-{formatted_high}"
    else:
        return f"¥{formatted_low}"


def get_sale_display(sale_status, live_total_sales_low, live_total_sales_high):
    """
    根据销售状态和销售范围获取显示文本

    参数:
        sale_status (int): 销售状态 (0=有数据, 1=未授权, 2=无数据)
        live_total_sales_low (int): 最低销售额
        live_total_sales_high (int): 最高销售额

    返回:
        str: 格式化后的显示文本
    """
    if sale_status == 0:
        # 正常状态，格式化销售范围
        return format_sale_range(live_total_sales_low, live_total_sales_high)
    elif sale_status == 1:
        # 未授权状态
        return '达人未授权'
    elif sale_status == 2:
        # 不可用状态
        return '-'


def format_sale(item, sale_status_key='sale_status', sale_low_key='sale_low', sale_high_key='sale_high'):
    sale_status = item[sale_status_key]
    sale_low = item[sale_low_key]
    sale_high = item[sale_high_key]
    return get_sale_display(sale_status, sale_low, sale_high)


# 示例使用
# print(get_sale_display(0, 5000, 7500))     # 输出: ¥5,000-7,500
# print(get_sale_display(0, 100000, 250000))  # 输出: ¥10万-25万
# print(get_sale_display(0, 7500, 10000))     # 输出: ¥7,500-1万
# print(get_sale_display(0, 10000, 15000))     # 输出: ¥1万-1.5万
# print(get_sale_display(0, 100000, 0))       # 输出: ¥10万+
# print(get_sale_display(0, 5000, 0))         # 输出: ¥5,000+
# print(get_sale_display(1, 5000, 7500))      # 输出: 达人未授权
# print(get_sale_display(2, 5000, 7500))      # 输出: -

def format_number(num):
    """
    将数值转换为万/亿单位格式，保留两位小数

    参数:
        num (int): 需要格式化的数值

    返回:
        str: 格式化后的字符串
    """
    num = int(num)
    if num >= 100000000:
        value = num // 1000000  # 先转为整万（保留两位小数用）
        formatted = f"{value / 100:.2f}亿"  # 实际显示为两位小数
    elif num >= 10000:
        value = num // 100  # 先转为整百（保留两位小数用）
        formatted = f"{value / 100:.2f}万"
    else:
        return str(num)

    return formatted


# 示例使用
# print(format_number(1126451))  # 输出: 112.65万
# print(format_number(648664))   # 输出: 64.87万
# print(format_number(5000))     # 输出: 5000

def format_number_round(num):
    """
    将数值转换为万/亿单位格式，保留两位小数，并进行四舍五入

    参数:
        num (int): 需要格式化的数值

    返回:
        str: 格式化后的字符串
    """
    num = int(num)
    if num >= 100000000:
        value = round(num / 100000000 * 100) / 100  # 转换为亿，并四舍五入到两位小数
        formatted = f"{value:.2f}亿"
    elif num >= 10000:
        value = round(num / 10000 * 100) / 100  # 转换为万，并四舍五入到两位小数
        formatted = f"{value:.2f}万"
    else:
        formatted = str(num)

    return formatted


def seconds_to_hms(seconds):
    seconds = int(seconds)
    hours, remainder = divmod(seconds, 3600)
    minutes, seconds = divmod(remainder, 60)

    result = ""
    if hours > 0:
        result += f"{hours}小时"
    if minutes > 0:
        result += f"{minutes}分钟"
    if seconds > 0:
        result += f"{seconds}秒"

    return result or "0秒"


# 示例：61210 秒
# total_seconds = 61210
# formatted_time = seconds_to_hms(total_seconds)

# print(formatted_time)  # 输出：17小时10秒


def format_percent(value):
    return f"{value}%" if value == 0 else f"{value / 100:.2f}%"


def format_money(value):
    return f"¥{value}" if value == 0 else f"¥{value / 100:.2f}"
