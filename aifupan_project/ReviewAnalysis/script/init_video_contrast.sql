DROP TABLE IF EXISTS "video_contrast";
CREATE TABLE "video_contrast" (
  "id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "video_one_id" text,
  "video_two_id" text,
  "contrast_time" text NOT NULL,
  "anchor_one_id" text,
  "anchor_two_id" text,
  "file_one_id" text,
  "file_two_id" text,
  "user_id" text NOT NULL DEFAULT 0,
  "contrast_id" text NOT NULL DEFAULT 0,
  "is_shard" int NOT NULL DEFAULT 0,
  "share_url" TEXT,
  "delete_status" integer NOT NULL DEFAULT 0,
  "tenant_id" INTEGER NOT NULL DEFAULT 0
);
