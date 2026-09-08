CREATE TABLE "video_record" (
  "id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "user_id" text,
  "video_id" text,
  "trade_id" text,
  "store_file_path" text,
  "tenant_id" integer DEFAULT 0,
  "create_date" text
);