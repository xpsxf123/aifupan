DROP TABLE IF EXISTS "online_num_new";
CREATE TABLE "online_num_new" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "user_id" text,
  "sec_uid" text,
  "batch_number" text,
  "video_id" text, 
  'people_num_data' text);