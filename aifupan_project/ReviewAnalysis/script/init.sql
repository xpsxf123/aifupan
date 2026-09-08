
DROP TABLE IF EXISTS "anchor_info";
CREATE TABLE "anchor_info" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "anchor_name" TEXT NOT NULL,
  "anchor_avatar" TEXT NOT NULL,
  "anchor_platform" TEXT NOT NULL,
  "home_url" TEXT,
  "live_url" TEXT,
  "app_share_url" TEXT,
  "live_status" integer,
  "record_status" integer,
  "is_auto_record" integer,
  "sec_uid" text,
  "online_number" text,
  "batch_number" text,
  "start_time" text,
  "add_time" text,
  "stream_url" TEXT,
  "trade_id" text NOT NULL DEFAULT 1,
  "anchor_user_id" text,
  "web_socket_id" text,
  "is_remove_record" TEXT,
  "source_url" TEXT,
  "is_barrage_monitoring" integer NOT NULL DEFAULT 0,
  "is_auto_upload_cloud" integer NOT NULL DEFAULT 0,
  "is_top" integer NOT NULL DEFAULT 0,
  "add_top_time" text,
  "last_record_time" text NOT NULL DEFAULT "2020-01-01 00:00:00"
);



DROP TABLE IF EXISTS "anchor_video";
CREATE TABLE "anchor_video" (
  "id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "video_id" text NOT NULL,
  "video_name" TEXT NOT NULL,
  "start_time" text NOT NULL,
  "paragraph" integer NOT NULL,
  "subsection_type" integer,
  "duration" text NOT NULL,
  "vedio_sizie" TEXT,
  "end_time" text,
  "batch_number" INTEGER,
  "video_type" integer,
  "definition" integer,
  "storage_path" TEXT,
  "live_url" TEXT,
  "share_url" TEXT,
  "source_url" TEXT,
  "anchor_id" INTEGER,
  "is_recording" integer,
  "source_type" integer,
  "live_title" TEXT,
  "analysis_status" integer,
  "analysis_time" text,
  "error_reason" TEXT,
  "sec_uid" text,
  "trade_id" text,
  "platform_type" TEXT,
  "user_id" text NOT NULL DEFAULT 0,
  "upload_status" integer NOT NULL DEFAULT 0,
  "is_mark" integer NOT NULL DEFAULT 0,
  "play_url" text,
  "cloud_store" integer DEFAULT 0,
  "delete_status" integer NOT NULL DEFAULT 0,
  "tenant_id" integer NOT NULL DEFAULT 0
);



DROP TABLE IF EXISTS "config";
CREATE TABLE "config" (
  "id" INTEGER NOT NULL,
  "serial_number" text NOT NULL,
  "detection_fre" integer NOT NULL,
  "save_path" TEXT,
  "live_source" integer NOT NULL DEFAULT 0,
  "limit_type" integer NOT NULL,
  "limit_value" text NOT NULL,
  "is_rocord" integer NOT NULL,
  "is_subection" integer NOT NULL,
  "hide_duration" integer NOT NULL,
  "hide_size" integer NOT NULL,
  "live_notice" integer NOT NULL,
  "online_number" integer NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);



INSERT INTO "main"."config" ("id", "serial_number", "detection_fre", "save_path", "live_source", "limit_type", "limit_value", "is_rocord", "is_subection", "hide_duration", "hide_size", "live_notice", "online_number") VALUES (1, '2.5.2.6', 10, '', 0, 2, '240', 0, 0, 0, 0, 0,1);
