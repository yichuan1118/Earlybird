package com.metaisle.earlybird.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Database extends SQLiteOpenHelper {

	public static final String DB_NAME = "earlybird.db";
	public static final int DB_VERSION = 4;

	public Database(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		TimelineTable.onCreate(db);
		UserTable.onCreate(db);
		MessageTable.onCreate(db);
		RelationshipTable.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		TimelineTable.onUpgrade(db, oldVersion, newVersion);
		UserTable.onUpgrade(db, oldVersion, newVersion);
		MessageTable.onUpgrade(db, oldVersion, newVersion);
		RelationshipTable.onUpgrade(db, oldVersion, newVersion);
	}

}
