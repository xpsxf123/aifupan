package com.jiuyu.replay.words.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记内容响应
 *
 * @author HeHui
 * @date 2025-06-09 15:47
 */
@Getter
@Setter
public class NotesContentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 3964040300490182757L;


    /** 最新版本 */
    private Integer lastVersion;


    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;


    /** 内容 */
    private String content;

    /** 编辑人 */
    private List<Editor> editors;


    @Getter
    @Setter
    public static class Editor implements Serializable {
        @Serial
        private static final long serialVersionUID = 3996752083523554754L;

        /** 用户id */
        private Long userId;

        /** 用户名 */
        private String userName;

        /** 创建时间 */
        private LocalDateTime editTime;

        /** 版本号 */
        private Integer version;

    }
}
