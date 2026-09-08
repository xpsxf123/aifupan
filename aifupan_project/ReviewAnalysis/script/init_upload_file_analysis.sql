DROP TABLE IF EXISTS "upload_file_analysis";
CREATE TABLE "upload_file_analysis" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "file_id" text NOT NULL,
  "status" integer NOT NULL,
  "data_json" text,
  "paragraph" integer NOT NULL,
  "trade_id" text NOT NULL DEFAULT 1
);

