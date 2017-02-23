package com.metaisle.earlybird.data;

import android.database.sqlite.SQLiteDatabase;

import com.metaisle.util.Util;

public final class TimelineTable extends BaseTable{
	
	public static final String TIMELINE_TABLE = "TIMELINE_TABLE";
	
	public static final String STATUS_ID = "status_id";
	public static final String STATUS_TEXT = "status_text";
	public static final String STATUS_TEXT_EXP = "status_text_exp";
	public static final String CREATED_AT = "created_at";
	public static final String IS_RETWEET = "is_retweet";
	public static final String IS_RETWEETED_BY_ME = "is_retweeted_by_me";
	public static final String RT_STATUS_ID = "rt_status_id";
	public static final String RT_USER_ID = "rt_user_id";
	public static final String RT_USER_NAME = "rt_user_name";
	public static final String IS_FAVORITED = "is_favorited";
	public static final String FROM_ID = "from_id";
	
//	public static final String MENTION_ENTITY_ID = "MENTION_ENTITY_ID";
//	public static final String MENTION_ENTITY_NAME = "MENTION_ENTITY_NAME";
//	public static final String MENTION_ENTITY_SCREEN_NAME = "MENTION_ENTITY_SCREEN_NAME";
//	public static final String MENTION_ENTITY_START = "MENTION_ENTITY_START";
//	public static final String MENTION_ENTITY_END = "MENTION_ENTITY_END";
//	
//	public static final String URL_ENTITY_DISPLAY_URL = "URL_ENTITY_DISPLAY_URL";
//	public static final String URL_ENTITY_EXPENDED_URL = "URL_ENTITY_EXPENDED_URL";
//	public static final String URL_ENTITY_URL = "URL_ENTITY_URL";
//	public static final String URL_ENTITY_START = "URL_ENTITY_START";
//	public static final String URL_ENTITY_END = "URL_ENTITY_END";
//	
//	public static final String HASHTAG_ENTITY_TEXT = "HASHTAG_ENTITY_TEXT";
//	public static final String HASHTAG_ENTITY_START = "HASHTAG_ENTITY_START";
//	public static final String HASHTAG_ENTITY_END = "HASHTAG_ENTITY_END";
//	
//	public static final String MEDIA_ENTITY_ID = "MEDIA_ENTITY_ID";
//	public static final String MEDIA_ENTITY_URL = "MEDIA_ENTITY_URL";
//	public static final String MEDIA_ENTITY_URL_HTTPS = "MEDIA_ENTITY_URL_HTTPS";
//	public static final String MEDIA_ENTITY_SIZES = "MEDIA_ENTITY_SIZES";
//	public static final String MEDIA_ENTITY_TYPE = "MEDIA_ENTITY_TYPE";
	
	public static final String CACHED_AT = "cached_at";
	public static final String OFFLINED_AT = "offlined_at";
	public static final String USER_TIMELINE = "user_timeline";
	public static final String IS_HOME = "is_home";
	public static final String IS_MENTION = "is_mention";
	
	
	public static void onCreate(SQLiteDatabase db){
		
			Util.log("Create table " + TIMELINE_TABLE);
			db.execSQL("CREATE TABLE " + TIMELINE_TABLE + " (" 
					+ _ID				+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ STATUS_ID 		+ " INTEGER UNIQUE NOT NULL," 
					+ STATUS_TEXT	 	+ " TEXT NOT NULL," 
					+ STATUS_TEXT_EXP 	+ " TEXT," 
					+ CREATED_AT 		+ " INTEGER NOT NULL," 
					+ IS_RETWEET 			+ " BOOLEAN NOT NULL,"
					+ IS_RETWEETED_BY_ME + " BOOLEAN NOT NULL,"
					+ RT_STATUS_ID 		+ " INTEGER,"
					+ RT_USER_ID 		+ " INTEGER,"
					+ RT_USER_NAME 		+ " TEXT,"
					+ IS_FAVORITED 		+ " BOOLEAN NOT NULL,"
					+ FROM_ID 			+ " INTEGER NOT NULL,"
					
//					+ MENTION_ENTITY_ID + " INTEGER,"
//					+ MENTION_ENTITY_NAME + " TEXT,"
//					+ MENTION_ENTITY_SCREEN_NAME + " TEXT,"
//					+ MENTION_ENTITY_START + " INTEGER,"
//					+ MENTION_ENTITY_END + " INTEGER,"
//					
//					+ URL_ENTITY_DISPLAY_URL + " TEXT,"
//					+ URL_ENTITY_EXPENDED_URL + " TEXT,"
//					+ URL_ENTITY_URL + " TEXT,"
//					+ URL_ENTITY_START + " INTEGER,"
//					+ URL_ENTITY_END + " INTEGER,"
//					
//					+ HASHTAG_ENTITY_TEXT  + " TEXT,"
//					+ HASHTAG_ENTITY_START + " INTEGER,"
//					+ HASHTAG_ENTITY_END   + " INTEGER,"
//					
//					+ MEDIA_ENTITY_ID       + " INTEGER,"
//					+ MEDIA_ENTITY_URL      + " TEXT,"
//					+ MEDIA_ENTITY_URL_HTTPS+ " TEXT,"
//					+ MEDIA_ENTITY_SIZES    + " INTEGER,"
//					+ MEDIA_ENTITY_TYPE     + " TEXT,"
					
					+ CACHED_AT + " INTEGER,"
					+ OFFLINED_AT+ " INTEGER,"
					+ USER_TIMELINE + " INTEGER,"
					+ IS_HOME + " BOOLEAN,"
					+ IS_MENTION + " BOOLEAN"
					+ ");");
	}
	
	
	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Util.log("onUpgrade");
		db.execSQL("DROP TABLE IF EXISTS " + TIMELINE_TABLE);
		onCreate(db);
	}
}
