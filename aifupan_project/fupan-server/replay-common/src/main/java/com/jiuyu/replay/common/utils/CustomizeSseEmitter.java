package com.jiuyu.replay.common.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/3/23 下午5:06
 */
@Slf4j
public class CustomizeSseEmitter extends SseEmitter {

    private boolean isClosed = false;

    public CustomizeSseEmitter() {
        super();
    }

    public CustomizeSseEmitter(Long timeout) {
        super(timeout);
    }

    public void sendMessage(String message, String eventName) {
        try {
            if (!this.isClosed){
                String format = StrUtil.format("event: {}\ndata: {}\n\n", eventName, message);
                Set<ResponseBodyEmitter.DataWithMediaType> items = new HashSet<>();
                items.add(new ResponseBodyEmitter.DataWithMediaType(format, MediaType.TEXT_PLAIN));
                super.send(items);
            }
        } catch (Exception e){
            this.isClosed = true;
            log.warn("sendMessage error: {}", e.getMessage());
        }
    }

    public void sendStop(){
        try {
            if (!this.isClosed){
                sendMessage("stop", "stop");
                super.complete();
            }
        } catch (Exception e){
            log.warn("sendMessage error: {}", e.getMessage());
        }finally {
            this.isClosed = true;
        }
    }
    public void sendStop2(){
        try {
            if (!this.isClosed){
                super.complete();
            }
        } catch (Exception e){
            log.warn("sendMessage error: {}", e.getMessage());
        }finally {
            this.isClosed = true;
        }
    }

}
