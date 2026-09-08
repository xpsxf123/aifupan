-- ============================================================================
-- SEO 内容管理（文章 / 分类 / 标签 + 批量导入）
-- 面向官网 https://www.ifupan.com，与 tb_article（客户端 H5 协议页）无关
-- 分支 feature/seo-article ｜ openspec: .claude/runs/Change__2026-08-12_19-11-17/
--
-- 说明：
--   1. 全部为新建表（isolated additive DDL），无外键回指既有表，回滚只需删除这 5 张新表
--   2. 不设 tenant_id —— SEO 内容是平台级运营数据（面向官网访客），
--      与 tb_dict_data / tb_login_rotate_image 等平台配置表一致
--   3. slug / name 采用「墓碑值」实现逻辑删除后释放唯一值：
--      删除时改写为 {原值}__del_{id}，故字段长度预留 25 字符后缀空间
--      （__del_ 6 位 + 雪花 ID 19 位）
-- ============================================================================

-- ---------------------------------------------------------------- 分类
CREATE TABLE `tb_seo_category` (
  `id`          BIGINT       NOT NULL             COMMENT '主键，雪花ID',
  `category_name` VARCHAR(120) NOT NULL           COMMENT '分类名称，业务限 1-20 字；删除时改写墓碑值。不用 name 是因为它是 MySQL 非保留关键字',
  `slug`        VARCHAR(140) NOT NULL             COMMENT '别名，小写字母/数字/连字符，官网 /category/{slug}；删除时改写墓碑值',
  `description` VARCHAR(300) NOT NULL DEFAULT ''  COMMENT '分类描述，作为官网聚合页 SEO 描述',
  `sort`        INT          NOT NULL DEFAULT 0   COMMENT '排序，越小越靠前',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0   COMMENT '软删除标记：0 正常 1 已删除',
  `create_date` DATETIME     NOT NULL             COMMENT '创建时间',
  `update_date` DATETIME     NOT NULL             COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seo_category_slug` (`slug`),
  UNIQUE KEY `uk_seo_category_name` (`category_name`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SEO 分类（官网栏目）';

-- ---------------------------------------------------------------- 标签
CREATE TABLE `tb_seo_tag` (
  `id`          BIGINT       NOT NULL             COMMENT '主键，雪花ID',
  `tag_name`    VARCHAR(120) NOT NULL             COMMENT '标签名称，业务限 1-20 字；删除时改写墓碑值。不用 name 是因为它是 MySQL 非保留关键字',
  `slug`        VARCHAR(140) NOT NULL             COMMENT '拼音别名，官网 /tag/{slug}；删除时改写墓碑值',
  `tag_status`  TINYINT      NOT NULL DEFAULT 1   COMMENT '状态：1 启用 0 禁用（禁用后聚合页不可访问，关联关系保留）。不用 status 是因为它是 MySQL 非保留关键字',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0   COMMENT '软删除标记：0 正常 1 已删除',
  `create_date` DATETIME     NOT NULL             COMMENT '创建时间',
  `update_date` DATETIME     NOT NULL             COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seo_tag_slug` (`slug`),
  UNIQUE KEY `uk_seo_tag_name` (`tag_name`),
  KEY `idx_status` (`tag_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SEO 标签（官网长尾聚合页）';

-- ---------------------------------------------------------------- 文章
CREATE TABLE `tb_seo_article` (
  `id`              BIGINT       NOT NULL             COMMENT '主键，雪花ID',
  `title`           VARCHAR(200) NOT NULL             COMMENT '文章标题，业务限 1-100 字',
  `slug`            VARCHAR(180) NOT NULL             COMMENT 'URL 别名，官网 /article/{slug}；重复时自动追加 4 位随机数；删除时改写墓碑值',
  `category_id`     BIGINT       NOT NULL             COMMENT '所属分类 tb_seo_category.id，单选必填',
  `content`         LONGTEXT     NOT NULL             COMMENT '正文 HTML，入库前已做 XSS 过滤且 h1 已降级为 h2',
  `summary`         VARCHAR(300) NOT NULL DEFAULT ''  COMMENT '摘要，业务限 200 字；留空时前台取正文前 120 字',
  `cover_url`       VARCHAR(500) NOT NULL             COMMENT '封面图 COS 地址，必填',
  `article_status`  TINYINT      NOT NULL DEFAULT 1   COMMENT '状态：1 已发布 0 已下架（导入落地一律为 0）。不用 status 是因为它是 MySQL 非保留关键字',
  `seo_title`       VARCHAR(200) NOT NULL             COMMENT 'SEO 标题，建议 ≤60 字符；留空时取 title',
  `seo_description` VARCHAR(300) NOT NULL             COMMENT 'SEO 描述，建议 80-160 字符',
  `seo_keywords`    VARCHAR(300) NOT NULL DEFAULT ''  COMMENT 'SEO 关键词，英文逗号分隔，建议 ≤5 个',
  `view_count`      INT          NOT NULL DEFAULT 0   COMMENT '浏览量：本期不做，字段预留，暂不写入不查询',
  `publish_time`    DATETIME              DEFAULT NULL COMMENT '首次发布时间；下架不清空，重新发布不覆盖',
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0   COMMENT '软删除标记：0 正常 1 已删除',
  `create_date`     DATETIME     NOT NULL             COMMENT '创建时间',
  `update_date`     DATETIME     NOT NULL             COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seo_article_slug` (`slug`),
  KEY `idx_category` (`category_id`, `is_deleted`),
  KEY `idx_status_publish` (`article_status`, `publish_time`),
  KEY `idx_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SEO 文章（官网内容）';

-- ---------------------------------------------------------------- 文章-标签关联
CREATE TABLE `tb_seo_article_tag` (
  `id`          BIGINT   NOT NULL COMMENT '主键，雪花ID',
  `article_id`  BIGINT   NOT NULL COMMENT '文章 tb_seo_article.id',
  `tag_id`      BIGINT   NOT NULL COMMENT '标签 tb_seo_tag.id',
  `create_date` DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`),
  KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SEO 文章标签关联（多对多）';

-- ---------------------------------------------------------------- 导入任务
CREATE TABLE `tb_seo_import_task` (
  `id`             BIGINT       NOT NULL             COMMENT '主键，雪花ID',
  `task_id`        VARCHAR(64)  NOT NULL             COMMENT '对外任务ID（UUID），前端据此轮询进度',
  `task_status`    TINYINT      NOT NULL DEFAULT 0   COMMENT '任务状态：0 处理中 1 已完成 2 失败。不用 status 是因为它是 MySQL 非保留关键字',
  `total`          INT          NOT NULL DEFAULT 0   COMMENT '待处理文件总数',
  `processed`      INT          NOT NULL DEFAULT 0   COMMENT '已处理数',
  `success_count`  INT          NOT NULL DEFAULT 0   COMMENT '成功篇数',
  `fail_count`     INT          NOT NULL DEFAULT 0   COMMENT '失败篇数',
  `result_json`    LONGTEXT              DEFAULT NULL COMMENT '逐篇结果 JSON：[{file,title,success,notes[]}]',
  `error_msg`      VARCHAR(500) NOT NULL DEFAULT ''  COMMENT '任务级失败原因（status=2 时）',
  `create_user_id` BIGINT       NOT NULL DEFAULT 0   COMMENT '发起导入的后台用户ID',
  `create_date`    DATETIME     NOT NULL             COMMENT '创建时间',
  `finish_date`    DATETIME              DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_user_date` (`create_user_id`, `create_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SEO 文章批量导入任务（异步，结果落库供刷新后查回）';

-- ---------------------------------------------------------------- 初始分类
-- 运营已确认的初始分类清单（id 为示例雪花值，执行时可替换）
INSERT INTO `tb_seo_category` (`id`, `category_name`, `slug`, `description`, `sort`, `is_deleted`, `create_date`, `update_date`) VALUES
  (1955000000000000001, '直播运营', 'zhibo-yunying',  '直播间搭建、场控节奏、流量承接的实操方法', 1, 0, NOW(), NOW()),
  (1955000000000000002, '主播培训', 'zhubo-peixun',   '话术、表达、镜头感与新人上手路径',       2, 0, NOW(), NOW()),
  (1955000000000000003, '行业洞察', 'hangye-dongcha', '直播电商的行业数据、政策与趋势解读',     3, 0, NOW(), NOW());

-- ---------------------------------------------------------------- 后台菜单
-- 前端路由由菜单驱动：loadComponent 按 url 映射到 `/src/views{url}/index.vue`，
-- 因此 url 必须与前端目录严格对应，写错不会报错、只会白屏。
--   /seo/article  -> src/views/seo/article/index.vue
--   /seo/category -> src/views/seo/category/index.vue
--   /seo/tag      -> src/views/seo/tag/index.vue
-- type：0 菜单 / 1 功能 / 2 目录。img 取 element-plus 图标名（去掉 el-icon- 前缀）。
--
-- ⚠️ 【菜单名必须全局唯一】permission/index.js 把菜单 name 直接当 Vue Router 的路由 name：
--       router.addRoute('main', { name: ele.name, path: ele.url, ... })
--     而 Vue Router 4 遇到同名路由会**先移除已存在的那条**。所以两条菜单同名时，
--     只有后注册的 path 活下来，另一条彻底消失，访问即 404。
--     这里刻意不叫「文章管理」——`/client/article`（客户端 H5 协议页）已经占用了那个名字，
--     撞名的表现是：菜单能显示、能点，type/url/组件文件全都正确，但就是 404，极难排查。
INSERT INTO `tb_menu` (`id`, `parent_id`, `name`, `url`, `type`, `sort`, `img`, `create_date`, `update_date`, `is_deleted`) VALUES
  (1955000000000001000, 0,                   'SEO内容管理', '/seo',          2, 50, 'document',     NOW(), NOW(), 0),
  (1955000000000001001, 1955000000000001000, 'SEO文章',     '/seo/article',  0, 0,  'tickets',      NOW(), NOW(), 0),
  (1955000000000001002, 1955000000000001000, 'SEO分类',     '/seo/category', 0, 1,  'folder-opened',NOW(), NOW(), 0),
  (1955000000000001003, 1955000000000001000, 'SEO标签',     '/seo/tag',      0, 2,  'price-tag',    NOW(), NOW(), 0);

-- 已经用「文章管理」等名字插过的环境，执行这三条修正（可重复执行）：
-- UPDATE `tb_menu` SET `name` = 'SEO文章' WHERE `url` = '/seo/article';
-- UPDATE `tb_menu` SET `name` = 'SEO分类' WHERE `url` = '/seo/category';
-- UPDATE `tb_menu` SET `name` = 'SEO标签' WHERE `url` = '/seo/tag';
-- 改完必须退出重新登录：menuList 是登录时拉进 store 的，刷新页面不会重新注册路由。

-- 菜单要挂到角色上才可见（listByUserId 走 tb_menu_role 关联）。
-- 下面把四条菜单授予 id=1 的角色；实际执行前请把 role_id 换成贵司的运营/管理员角色 ID，
-- 或改从后台「角色管理」里勾选。不做这一步的话菜单不会出现在左侧导航。
INSERT INTO `tb_menu_role` (`id`, `role_id`, `menu_id`, `create_date`, `update_date`, `is_deleted`) VALUES
  (1955000000000002000, 1, 1955000000000001000, NOW(), NOW(), 0),
  (1955000000000002001, 1, 1955000000000001001, NOW(), NOW(), 0),
  (1955000000000002002, 1, 1955000000000001002, NOW(), NOW(), 0),
  (1955000000000002003, 1, 1955000000000001003, NOW(), NOW(), 0);
