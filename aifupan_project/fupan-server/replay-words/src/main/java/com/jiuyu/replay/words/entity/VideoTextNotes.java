package com.jiuyu.replay.words.entity;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 视频原文全文笔记
 *
 * @author HeHui
 * @date 2025-06-07 14:23
 */
@Getter
@Setter
@Document(collection = "replay_video_text_notes")
@CompoundIndexes({
    @CompoundIndex(name = "text_notes_sourceId_type_index", def = "{ 'sourceId': 1,  'sourceType': 1, 'notesType': 1 }")
})
public class VideoTextNotes implements Serializable {


    @Serial
    private static final long serialVersionUID = 7956285024663295816L;

    @Id
    private Long id;

    /** 所属租户id */
    private Long tenantId;

    /** 所属用户id */
    private Long userId;

    /**
     * 视频id
     */
    private String sourceId;

    /**
     * 类型 0视频，1文件，2对比分析
     */
    private Integer sourceType;

    /**
     * 笔记类型
     * 1: 原文笔记,2：复盘小结，3分段笔记
     */
    private Integer notesType;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;

    /** 删除标识 */
    private Boolean isDeleted;

    /** 创建用户id */
    private Long createUserId;

    /** 修改用户id */
    private Long updateUserId;

    /**
     * 内容md5摘要值
     * 用于计算对比内容是否有更改
     */
    private String contentHash;

    /** 最新版本号 */
    private Integer lastVersion;

    /**
     * 内容文件path路径规则
     * 使用format(last_version) 得到可访问地址
     */
    @Indexed(unique = true)
    private String contentFilePath;


    /** 笔记编辑人 */
    private List<Editor> editors;


    /**
     * 升级视频文本笔记的版本信息
     * 此方法用于在内容变更时更新笔记的版本号和相关用户信息
     *
     * @param contentHash 新内容的哈希值，用于标识笔记内容的变更
     * @param user        用户缓存对象，包含用户的ID等信息，用于记录更新者信息
     */
    public void upgrade(String contentHash, UserCacheVo user) {

        // 设置新的内容哈希值
        this.setContentHash(contentHash);
        int currentVersion = this.lastVersion;
        // 更新版本号，表示笔记内容的变更
        this.setLastVersion(lastVersion + 1);
        // 设置最后一次更新的用户ID
        this.setUpdateUserId(user.getId());
        // 设置最后一次更新的时间
        this.setUpdateTime(LocalDateTime.now());
        Editor editor = new Editor();
        editor.setUserId(user.getId());
        editor.setUserName(user.getNickName());
        editor.setEditTime(this.getUpdateTime());
        editor.setVersion(currentVersion);
        if (CollUtil.isEmpty(this.editors)) {
            this.editors = List.of(editor);
        } else {
            this.editors = new ArrayList<>(this.editors);
            this.editors.add(editor);
        }
    }


    /**
     * 获取内容文件的路径
     *
     * @return Optional<String> 包装了内容文件路径的Optional对象，如果路径不存在，则为Optional.empty()
     */
    public Optional<String> contentFilePath() {
        if (StrUtil.isBlank(this.contentFilePath) || this.lastVersion == null) {
            return Optional.empty();
        }
        return Optional.of(String.format(this.contentFilePath, this.lastVersion));
    }


    /**
     * 根据版本号返回历史文件路径
     * 此方法用于获取特定版本的内容文件路径，确保请求的版本在有效范围内
     *
     * @param version 请求的版本号
     *
     * @return 如果版本号有效，则返回包含文件路径的Optional对象；否则返回Optional.empty()
     */
    public Optional<String> historyFilePath(int version) {
        // 检查版本号是否有效，如果版本号小于等于0或lastVersion未初始化，则返回空值
        if (version <= 0 || this.lastVersion == null) {
            return Optional.empty();
        }
        // 如果请求的版本号大于或等于最新版本号，则返回空值，表示请求的版本无效或不存在
        if (version >= this.lastVersion) {
            return Optional.empty();
        }
        // 请求的版本号有效，根据版本号格式化并返回对应的文件路径
        return Optional.of(String.format(this.contentFilePath, version));
    }


    /**
     * 获取内容文件的历史版本路径列表
     * <p>
     * 此方法根据当前笔记的 contentFilePath 模板和 lastVersion 版本号生成对应的历史文件路径。
     * 路径格式通过 String.format 使用版本号进行填充。
     * 如果 contentFilePath 为空或 lastVersion 为 null，则返回空列表。
     *
     * <p><strong>版本规则说明：</strong>
     * <ul>
     *   <li>当 lastVersion 小于等于 1 时，仅返回一个版本路径：版本号为 1</li>
     *   <li>当 lastVersion 大于 1 时，返回从版本号 1 到 lastVersion 的所有路径</li>
     * </ul>
     *
     * <p><strong>示例：</strong>
     * 假设 contentFilePath = "/path/to/file_v%s.html"，lastVersion = 3，则输出为：
     * <pre>
     * [
     *   "/path/to/file_v1.html",
     *   "/path/to/file_v2.html",
     *   "/path/to/file_v3.html"
     * ]
     * </pre>
     *
     * @return 返回一个包含历史文件路径的列表，类型为 List<String>
     */
    public List<String> historyFilePaths() {
        if (StrUtil.isBlank(this.contentFilePath) || this.lastVersion == null) {
            return List.of();
        }
        if (lastVersion <= 1) {
            return List.of(String.format(this.contentFilePath, 1));
        }
        return IntStream.range(1, this.lastVersion + 1)
            .mapToObj(i -> String.format(this.contentFilePath, i))
            .collect(Collectors.toList());
    }






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


        public static Editor of(Long userId, String userName, LocalDateTime editTime, Integer version) {
            Editor editor = new Editor();
            editor.setUserId(userId);
            editor.setUserName(userName);
            editor.setEditTime(editTime);
            editor.setVersion(version);
            return editor;
        }


        public static Editor first(Long userId, String userName) {
            return Editor.of(userId, userName, LocalDateTime.now(), 1);
        }
    }
}
