DROP TABLE IF EXISTS "total_online_num";
CREATE TABLE "total_online_num" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "user_id" text,
  "sec_uid" text,
  "batch_number" text,
  "record_date" text,
  "people_num" text,
  "video_id" text,
  "start_record_date" text
);
DROP TABLE IF EXISTS "video_viewership_num";
CREATE TABLE "video_viewership_num"(
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "user_id" text, 
  "sec_uid" text, 
  "batch_number" text, 
  "video_id" text, 
  "viewership_num" text, 
  "barrage_num" text,
  "create_date" text, 
  "update_date" text
  );
 