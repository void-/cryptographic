package com.db;

import com.db.Names;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 *  MessageDatabaseHelper helps create the database.
 *
 *  All sms's are stored unencrypted.
 *
 *  Schema: sender, receipt date, message.
 */
class MessageDatabaseHelper extends SQLiteOpenHelper
{
  /**
   *  MessageDatabaseHelper() constructs a new MessageDatabaseHelper
   *  instance.
   *
   *  This constructor is not responsible for creating the database for the
   *  first time.
   *
   *  A default cursor is requested.
   */
  MessageDatabaseHelper(Context context)
  {
    super(context, Names.DATABASE_NAME, null, Names.VERSION);
  }

  /**
   *  onCreate() creates the database for the first time.
   *
   *  Uses a big prepared statement.
   *
   *  @param db SQLiteDatabase for which to create the database.
   */
  @Override
  public void onCreate(SQLiteDatabase db)
  {
    SQLiteStatement s = db.compileStatement(
      "CREATE TABLE ? (? ?, ? ?, ? ?);");
    s.bindString(1, Names.TABLE_NAME);
    s.bindString(2, Names.SENDER_NAME);
    s.bindString(3, Names.SENDER_TYPE);
    s.bindString(4, Names.RECEIPT_DATE);
    s.bindString(5, Names.RECEIPT_TYPE);
    s.bindString(6, Names.MESSAGE);
    s.bindString(7, Names.MESSAGE_TYPE);
    s.execute();
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
}
