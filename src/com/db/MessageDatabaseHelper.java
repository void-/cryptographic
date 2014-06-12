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

  String CREATE_QUERY = "CREATE TABLE " + Names.TABLE_NAME + "("+
    Names.MESSAGE_NO+" "+ Names.MESSAGE_NO_TYPE+", "+Names.CONVERSATION_ID +" "
    + Names.CONVERSATION_ID_TYPE+", "+Names.SENDER_NAME+" "+ Names.SENDER_TYPE+
    ", "+ Names.RECEIPT_DATE+" "+Names.RECEIPT_TYPE+", "+ Names.MESSAGE+" "+
    Names.MESSAGE_TYPE+");";
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
   *  @param db SQLiteDatabase for which to create the database.
   */
  @Override
  public void onCreate(SQLiteDatabase db)
  {
    db.execSQL(CREATE_QUERY);
    Log.d(Names.TAG, CREATE_QUERY);
    //db.execSQL("CREATE TABLE " + Names.TABLE_NAME + "("+ Names.MESSAGE_NO+" "+
    //  Names.MESSAGE_NO_TYPE+", "+Names.CONVERSATION_ID +" " +
    //  Names.CONVERSATION_ID_TYPE+", "+Names.SENDER_NAME+" "+ Names.SENDER_TYPE+
    //  ", "+ Names.RECEIPT_DATE+" "+Names.RECEIPT_TYPE+", "+ Names.MESSAGE+" "+
    //  Names.MESSAGE_TYPE+");");
  }

  /**
   *  Do nothing; no upgrading this database.
   */
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
}
