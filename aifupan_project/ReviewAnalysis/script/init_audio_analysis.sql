DROP TABLE IF EXISTS "audio_analysis";
CREATE TABLE "audio_analysis" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "video_id" text NOT NULL,
  "status" integer NOT NULL,
  "data_json" text,
  "paragraph" integer NOT NULL,
  "trade_id" text NOT NULL DEFAULT 1
);

