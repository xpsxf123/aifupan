CREATE TABLE "upload_file_record" (
  "id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "user_id" text,
  "file_id" text,
  "trade_id" text,
  "store_file_path" text,
  "tenant_id" INTEGER DEFAULT 0,
  "create_date" text
);