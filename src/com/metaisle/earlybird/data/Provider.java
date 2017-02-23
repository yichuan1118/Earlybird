package com.metaisle.earlybird.data;

import java.util.Map.Entry;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.metaisle.util.Util;

public class Provider extends ContentProvider {
	private static final String AUTHORITY = "com.metaisle.earlybird.data.Provider";

	private static final int MATCH_TIMELINE = 111;
	private static final int MATCH_TIMELINE_ID = 112;

	private static final int MATCH_USER = 201;
	private static final int MATCH_USER_ID = 202;

	private static final int MATCH_MESSAGE = 301;
	private static final int MATCH_MESSAGE_ID = 302;

	private static final int MATCH_FOLLOWER = 501;
	private static final int MATCH_FRIEND = 601;

	public static final Uri TIMELINE_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + TimelineTable.TIMELINE_TABLE);
	public static final Uri USER_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + UserTable.USER_TABLE);
	public static final Uri MESSAGE_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + MessageTable.MESSAGE_TABLE);

	public static final Uri FOLLOWER_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + "FOLLOWER");

	public static final Uri FRIEND_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + "FRIEND");

	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, TimelineTable.TIMELINE_TABLE,
				MATCH_TIMELINE);
		sURIMatcher.addURI(AUTHORITY, TimelineTable.TIMELINE_TABLE + "/#",
				MATCH_TIMELINE_ID);

		sURIMatcher.addURI(AUTHORITY, UserTable.USER_TABLE, MATCH_USER);
		sURIMatcher.addURI(AUTHORITY, UserTable.USER_TABLE + "/#",
				MATCH_USER_ID);

		sURIMatcher
				.addURI(AUTHORITY, MessageTable.MESSAGE_TABLE, MATCH_MESSAGE);
		sURIMatcher.addURI(AUTHORITY, MessageTable.MESSAGE_TABLE + "/#",
				MATCH_MESSAGE_ID);

		sURIMatcher.addURI(AUTHORITY, "FOLLOWER", MATCH_FOLLOWER);
		sURIMatcher.addURI(AUTHORITY, "FRIEND", MATCH_FRIEND);
	}

	private Database database;

	@Override
	public boolean onCreate() {
		database = new Database(getContext());
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		String id = uri.getLastPathSegment();
		switch (uriType) {

		case MATCH_TIMELINE:
			rowsDeleted = sqlDB.delete(TimelineTable.TIMELINE_TABLE, selection,
					selectionArgs);
			break;
		case MATCH_TIMELINE_ID:
			if (TextUtils.isEmpty(selection))
				selection = TimelineTable.STATUS_ID + "=" + id;
			else
				selection = selection + " AND " + TimelineTable.STATUS_ID + "="
						+ id;
			rowsDeleted = sqlDB.delete(TimelineTable.TIMELINE_TABLE, selection,
					selectionArgs);
			break;

		case MATCH_USER:
			rowsDeleted = sqlDB.delete(UserTable.USER_TABLE, selection,
					selectionArgs);
			break;
		case MATCH_USER_ID:
			if (TextUtils.isEmpty(selection))
				selection = UserTable.USER_ID + "=" + id;
			else
				selection = selection + " AND " + UserTable.USER_ID + "=" + id;
			rowsDeleted = sqlDB.delete(UserTable.USER_TABLE, selection,
					selectionArgs);
			break;

		case MATCH_MESSAGE:
			rowsDeleted = sqlDB.delete(MessageTable.MESSAGE_TABLE, selection,
					selectionArgs);
			break;
		case MATCH_MESSAGE_ID:
			if (TextUtils.isEmpty(selection))
				selection = MessageTable.STATUS_ID + "=" + id;
			else
				selection = selection + " AND " + MessageTable.STATUS_ID + "="
						+ id;
			rowsDeleted = sqlDB.delete(MessageTable.MESSAGE_TABLE, selection,
					selectionArgs);
			break;

		case MATCH_FOLLOWER:
			rowsDeleted = sqlDB.delete(RelationshipTable.RELATIONSHIP_TABLE,
					selection, selectionArgs);
			break;
		case MATCH_FRIEND:
			rowsDeleted = sqlDB.delete(RelationshipTable.RELATIONSHIP_TABLE,
					selection, selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int match = sURIMatcher.match(uri);
		SQLiteDatabase db = database.getWritableDatabase();
		long id = -1;
		switch (match) {

		case MATCH_TIMELINE:
			// String sql = upsertSql(TimelineTable.TIMELINE_TABLE, values);
			//
			// db.execSQL(sql);
			String status_id = values.getAsString(TimelineTable.STATUS_ID);
			id = db.insertWithOnConflict(TimelineTable.TIMELINE_TABLE, null,
					values, SQLiteDatabase.CONFLICT_IGNORE);
			db.updateWithOnConflict(TimelineTable.TIMELINE_TABLE, values,
					TimelineTable.STATUS_ID + "=?", new String[] { status_id },
					SQLiteDatabase.CONFLICT_IGNORE);

			getContext().getContentResolver().notifyChange(uri, null);
			return Uri.parse(TimelineTable.TIMELINE_TABLE + "/" + id);

		case MATCH_USER:
			String user_id = values.getAsString(UserTable.USER_ID);
			id = db.insertWithOnConflict(UserTable.USER_TABLE, null, values,
					SQLiteDatabase.CONFLICT_IGNORE);
			db.updateWithOnConflict(UserTable.USER_TABLE, values,
					UserTable.USER_ID + "=?", new String[] { user_id },
					SQLiteDatabase.CONFLICT_IGNORE);

			getContext().getContentResolver().notifyChange(uri, null);
			return Uri.parse(UserTable.USER_TABLE + "/" + id);

		case MATCH_MESSAGE:
			id = db.insertWithOnConflict(MessageTable.MESSAGE_TABLE, null,
					values, SQLiteDatabase.CONFLICT_IGNORE);
			getContext().getContentResolver().notifyChange(uri, null);
			return Uri.parse(MessageTable.MESSAGE_TABLE + "/" + id);

		case MATCH_FOLLOWER:
			id = db.insertWithOnConflict(RelationshipTable.RELATIONSHIP_TABLE,
					null, values, SQLiteDatabase.CONFLICT_IGNORE);
			getContext().getContentResolver().notifyChange(uri, null);
			return Uri.parse(RelationshipTable.RELATIONSHIP_TABLE + "/" + id);

		case MATCH_FRIEND:
			id = db.insertWithOnConflict(RelationshipTable.RELATIONSHIP_TABLE,
					null, values, SQLiteDatabase.CONFLICT_IGNORE);
			getContext().getContentResolver().notifyChange(uri, null);
			return Uri.parse(RelationshipTable.RELATIONSHIP_TABLE + "/" + id);

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	public String upsertSql(String table, ContentValues values) {
		String sql = "INSERT OR REPLACE INTO " + table + " (";

		String value = " ) VALUES ( ";

		long status_id = values.getAsLong(TimelineTable.STATUS_ID);

		// ---
		sql += TimelineTable.CACHED_AT;
		value = value + "(select " + TimelineTable.CACHED_AT + " from " + table
				+ " where " + TimelineTable.STATUS_ID + " = " + status_id + ")";

		for (Entry<String, Object> v : values.valueSet()) {
			sql = sql + "," + v.getKey();
			if (v.getValue() instanceof Boolean) {
				if ((Boolean) v.getValue() == false) {
					value = value + "," + 0 + " ";
				} else {
					value = value + "," + 1 + " ";
				}
			} else {
				value = value
						+ ","
						+ DatabaseUtils.sqlEscapeString(String.valueOf(v
								.getValue()));
			}
		}

		Util.log(sql + value + ");");
		return sql + value + ");";
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		int match = sURIMatcher.match(uri);
		SQLiteDatabase db = null;
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		Cursor cursor = null;
		String id = uri.getLastPathSegment();

		switch (match) {

		case MATCH_TIMELINE:
			queryBuilder.setTables(TimelineTable.TIMELINE_TABLE + " JOIN "
					+ UserTable.USER_TABLE + " ON ("
					+ TimelineTable.TIMELINE_TABLE + "."
					+ TimelineTable.FROM_ID + "=" + UserTable.USER_TABLE + "."
					+ UserTable.USER_ID + ")");
			break;
		case MATCH_TIMELINE_ID:
			Util.log("MATCH_HOME_ID " + id);
			queryBuilder.setTables(TimelineTable.TIMELINE_TABLE + " JOIN "
					+ UserTable.USER_TABLE + " ON ("
					+ TimelineTable.TIMELINE_TABLE + "."
					+ TimelineTable.FROM_ID + "=" + UserTable.USER_TABLE + "."
					+ UserTable.USER_ID + ")");
			queryBuilder.appendWhere(TimelineTable.STATUS_ID + "=" + id);
			break;

		case MATCH_USER:
			queryBuilder.setTables(UserTable.USER_TABLE);
			break;
		case MATCH_USER_ID:
			queryBuilder.setTables(UserTable.USER_TABLE);
			queryBuilder.appendWhere(UserTable.USER_ID + "=" + id);
			break;

		case MATCH_MESSAGE:
			queryBuilder.setTables(MessageTable.MESSAGE_TABLE + " JOIN "
					+ UserTable.USER_TABLE + " ON ("
					+ MessageTable.MESSAGE_TABLE + "." + MessageTable.SENDER_ID
					+ "=" + UserTable.USER_TABLE + "." + UserTable.USER_ID
					+ ")");
			break;
		case MATCH_MESSAGE_ID:
			queryBuilder.setTables(MessageTable.MESSAGE_TABLE + " JOIN "
					+ UserTable.USER_TABLE + " ON ("
					+ MessageTable.MESSAGE_TABLE + "." + MessageTable.SENDER_ID
					+ "=" + UserTable.USER_TABLE + "." + UserTable.USER_ID
					+ ")");
			queryBuilder.appendWhere(MessageTable.STATUS_ID + "=" + id);
			break;

		case MATCH_FOLLOWER:
			queryBuilder.setTables(RelationshipTable.RELATIONSHIP_TABLE
					+ " LEFT JOIN " + UserTable.USER_TABLE + " ON ("
					+ UserTable.USER_TABLE + "." + UserTable.USER_ID + "="
					+ RelationshipTable.RELATIONSHIP_TABLE + "."
					+ RelationshipTable.FOLLOWER + ")");
			break;

		case MATCH_FRIEND:
			queryBuilder.setTables(RelationshipTable.RELATIONSHIP_TABLE
					+ " LEFT JOIN " + UserTable.USER_TABLE + " ON ("
					+ UserTable.USER_TABLE + "." + UserTable.USER_ID + "="
					+ RelationshipTable.RELATIONSHIP_TABLE + "."
					+ RelationshipTable.FRIEND + ")");
			break;

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		db = database.getWritableDatabase();
		cursor = queryBuilder.query(db, projection, selection, selectionArgs,
				null, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int match = sURIMatcher.match(uri);
		SQLiteDatabase db = database.getWritableDatabase();
		String id = uri.getLastPathSegment();
		int rowsUpdated = 0;

		// Util.log("match " + match + " id " + id + " selection " + selection
		// + " seletionArgs " + Arrays.toString(selectionArgs));

		switch (match) {
		case MATCH_TIMELINE_ID:
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = db.update(TimelineTable.TIMELINE_TABLE, values,
						TimelineTable.STATUS_ID + "=?",
						new String[] { String.valueOf(id) });
			} else {
				rowsUpdated = db.update(TimelineTable.TIMELINE_TABLE, values,
						TimelineTable.STATUS_ID + "=" + id + " and "
								+ selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

}
