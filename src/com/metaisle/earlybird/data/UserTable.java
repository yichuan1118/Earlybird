package com.metaisle.earlybird.data;

import android.database.sqlite.SQLiteDatabase;

import com.metaisle.util.Util;

public final class UserTable extends BaseTable{
	public static final String USER_TABLE = "user_table";
	
	public static final String USER_ID = "user_id";
	public static final String CREATED_AT = "created_at";
	public static final String DESCRIPTION= "description";
	public static final String FOLLOWERS_COUNT= "followers_count";
	public static final String FRIENDS_COUNT= "friends_count";
	public static final String USER_NAME = "user_name";
	public static final String SCREEN_NAME = "screen_name";
	public static final String PROFILE_IMAGE_URL = "profile_image_url";
	public static final String STATUS_COUNT = "status_count";

	
	public static void onCreate(SQLiteDatabase db){
		Util.log("Create table " + USER_TABLE);
		db.execSQL("CREATE TABLE " + USER_TABLE + " (" 
				+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ USER_ID + " INTEGER UNIQUE NOT NULL," 
				+ CREATED_AT + " INTEGER NOT NULL," 
				+ DESCRIPTION + " TEXT,"
				+ FOLLOWERS_COUNT + " INTEGER NOT NULL,"
				+ FRIENDS_COUNT + " INTEGER NOT NULL,"
				+ USER_NAME + " TEXT NOT NULL,"
				+ SCREEN_NAME + " TEXT NOT NULL,"
				+ PROFILE_IMAGE_URL + " TEXT NOT NULL,"
				+ STATUS_COUNT + " INTEGER NOT NULL"
				+ ");");
	}
	
	
	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Util.log("onUpgrade");
		db.execSQL("DROP TABLE IF EXISTS " + USER_TABLE);
		
		onCreate(db);
	}
}
