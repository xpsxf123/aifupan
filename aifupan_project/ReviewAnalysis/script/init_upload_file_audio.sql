DROP TABLE IF EXISTS "upload_file_audio";
CREATE TABLE "upload_file_audio" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "file_id" text NOT NULL,
  "audio_name" integer NOT NULL,
  "paragraph" integer NOT NULL,
  "audio_type" integer NOT NULL
);
