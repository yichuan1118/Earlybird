package com.metaisle.earlybird.data;

import android.database.sqlite.SQLiteDatabase;

import com.metaisle.util.Util;

public class RelationshipTable extends BaseTable {
	public static final String RELATIONSHIP_TABLE = "RELATIONSHIP_TABLE";

	public static final String FOLLOWER = "FOLLOWER";
	public static final String FRIEND = "FRIEND";
	
	public static void onCreate(SQLiteDatabase db){
		
		Util.log("Create table " + RELATIONSHIP_TABLE);
		db.execSQL("CREATE TABLE " + RELATIONSHIP_TABLE + " (" 
				+ _ID			+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ FOLLOWER 		+ " INTEGER NOT NULL," 
				+ FRIEND 		+ " INTEGER NOT NULL" 
				+ ");");
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		Util.log("onUpgrade");
		db.execSQL("DROP TABLE IF EXISTS " + RELATIONSHIP_TABLE);
		onCreate(db);
	}
}
