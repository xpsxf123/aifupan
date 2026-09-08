package com.jiuyu.replay.video.common.utils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @program:
 * @description:
 * @author: RayChou
 * @create: 2020-08-03 11:21
 **/
public class LambdaUtil {

    /**
     * 转换forEach index item
     *
     * @param consumer
     * @param <T>
     * @return
     */
    public static <T> Consumer<T> consumerWithIndex(BiConsumer<T, Integer> consumer) {
        class Obj {
            int i;
        }
        Obj obj = new Obj();
        return t -> {
            int index = obj.i++;
            consumer.accept(t, index);
        };
    }

}
