DROP TABLE IF EXISTS "online_num";
CREATE TABLE "online_num" (
  "id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "user_id" text,
  "sec_uid" text,
  "batch_number" text,
  "record_date" text,
  "people_num" text,
  "video_id" text,
  "people_num_data" text
);