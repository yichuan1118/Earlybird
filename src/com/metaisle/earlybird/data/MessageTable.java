package com.metaisle.earlybird.data;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.metaisle.util.Util;

public class MessageTable implements BaseColumns {
	public static final String MESSAGE_TABLE = "message_table";

	public static final String STATUS_ID = "status_id";
	public static final String STATUS_TEXT = "status_text";
	public static final String CREATED_AT = "created_at";
	public static final String RECIPIENT_NAME = "recipient";
	public static final String RECIPIENT_ID = "recipient_id";
	public static final String SENDER_NAME = "sender";
	public static final String SENDER_ID = "sender_id";
	
	
	public static void onCreate(SQLiteDatabase db){
		Util.log("Create table " + MESSAGE_TABLE);
		db.execSQL("CREATE TABLE " + MESSAGE_TABLE + " (" 
				+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ STATUS_ID + " INTEGER UNIQUE NOT NULL," 
				+ STATUS_TEXT + " TEXT NOT NULL," 
				+ CREATED_AT + " INTEGER NOT NULL," 
				+ RECIPIENT_NAME + " TEXT NOT NULL,"
				+ RECIPIENT_ID + " INTEGER NOT NULL,"
				+ SENDER_NAME + " TEXT NOT NULL,"
				+ SENDER_ID + " INTEGER NOT NULL"
				+ ");");
	}
	
	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Util.log("onUpgrade");
		db.execSQL("DROP TABLE IF EXISTS " + MESSAGE_TABLE);
		onCreate(db);
	}
}
