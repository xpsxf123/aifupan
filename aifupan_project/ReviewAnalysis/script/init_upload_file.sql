DROP TABLE IF EXISTS "upload_file";
CREATE TABLE "upload_file" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "file_id" text NOT NULL,
  "file_name" TEXT NOT NULL,
  "file_type" integer NOT NULL,
  "original_path" TEXT NOT NULL,
  "now_path" TEXT NOT NULL,
  "analysis_status" integer NOT NULL,
  "analysis_time" text,
  "upload_time" text NOT NULL,
  "file_size" text,
  "file_duration" integer,
  "file_word_num" integer,
  "error_reason" TEXT,
  "trade_id" text NOT NULL DEFAULT 1,
  "platform_type" text NOT NULL DEFAULT 0,
  "user_id" text NOT NULL DEFAULT 0,
  "upload_status" integer NOT NULL DEFAULT 0,
  "is_mark" integer NOT NULL DEFAULT 0,
  "share_url" TEXT,
  "play_url" TEXT,
  "cloud_store" integer DEFAULT 0
);

