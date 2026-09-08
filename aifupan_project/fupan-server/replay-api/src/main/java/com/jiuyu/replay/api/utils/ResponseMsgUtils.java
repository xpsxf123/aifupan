package com.jiuyu.replay.api.utils;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

public class ResponseMsgUtils {

    public static void returnMsp(HttpServletResponse response, Integer code, String msg) {
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        try {
            response.setContentType("application/json");
            writer = response.getWriter();
            String s = "{\"msg\":\""+ msg+"\", \"code\":"+ code +"}";
            writer.print(s);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null)
                writer.close();
        }

    }
}
