/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 *
 * https://www.renren.io
 *
 * 版权所有，侵权必究！
 */

package com.jiuyu.replay.common.utils;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.generic.vo.common.R;

/**
 * 自定义异常
 *
 * @author Mark sunlightcs@gmail.com
 */
public class RRException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
    private String msg;
    private int code = 500;
    
    public RRException(String msg) {
		super(msg);
		this.msg = msg;
	}
	
	public RRException(String msg, Throwable e) {
		super(msg, e);
		this.msg = msg;
	}
	
	public RRException(String msg, int code) {
		super(msg);
		this.msg = msg;
		this.code = code;
	}
	
	public RRException(String msg, int code, Throwable e) {
		super(msg, e);
		this.msg = msg;
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
	
	public static void create(String msg) {
		throw new RRException(msg, 3001);
	}

	public static void create(int code, String msg) {
		throw new RRException(msg, code);
	}

	public static void create(R r){
		if (r.getCode() != 0){
			throw new RRException(r.getMsg(), r.getCode());
		}
	}

	/**
	 * 判断是否为空，为空则抛出异常
	 * @param o
	 * @param errorStr
	 */
	public static void isNotEmpty(Object o, String errorStr){
		if (ObjectUtil.isEmpty(o)) create(errorStr);
	}

	/**
	 * 判断是否为空，为空则抛出异常
	 * @param o
	 * @param errorStr
	 * @param code
	 */
	public static void isNotEmpty(Object o, String errorStr, int code){
		if (ObjectUtil.isEmpty(o)) create(code, errorStr);
	}

	/**
	 * 判断是否为真，不为真则抛出异常
	 * @param flag
	 * @param error
	 */
	public static void isTrue(boolean flag, String error){
		if (!flag) create(error);
	}
}
