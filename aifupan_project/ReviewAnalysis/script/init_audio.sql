DROP TABLE IF EXISTS "audio";
CREATE TABLE "audio" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "video_id" string NOT NULL,
  "audio_name" integer NOT NULL,
  "paragraph" integer NOT NULL,
  "audio_type" integer NOT NULL
);
