package com.db;

import com.db.Names;
import com.db.MessageDatabaseHelper;

import android.content.Context;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 *  MessageReader provides a readonly database for storing incoming sms's.
 *
 */
public class MessageReader
{
  /**
   *  Member Variables.
   *
   *  db
   *  context
   */
  private SQLiteDatabase db;
  private Context context;

  /**
   *  MessageReader() constructs a new message reader purposed for reading
   *  messages from the database.
   */
  MessageReader(Context context)
  {
    this.context = context;
    db = (new MessageDatabaseHelper(context)).getWritableDatabase();
  }
}
