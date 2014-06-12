package com.db;

import com.ctxt.R;

import com.db.Names;
import com.db.MessageDatabaseHelper;
import com.db.Message;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.database.Cursor;
import android.util.Log;

import android.widget.SimpleCursorAdapter;

import java.util.Iterator;

/**
 *  MessageReader provides a readonly database for storing incoming sms's.
 *
 */
public class MessageReader
{
  /**
   *  Class Variables
   *
   */
  protected static final String[] fromColumns = { Names.MESSAGE };
  protected static final int[] toViews =
  {
    android.R.id.text1
  };

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
  public MessageReader(Context context)
  {
    db = (new MessageDatabaseHelper(context)).getReadableDatabase();
    this.context = context;
  }

  /**
   *  close() closes the connection to the database.
   */
  public void close()
  {
    db.close();
  }

  /**
   *  getConversationCursor() given a phone number will a cursor over all
   *  messages sent and received between the user and the phone number.
   *
   *  @return Cursor over conversation.
   */
  protected Cursor getConversationCursor(String number)
  {
    Log.d(Names.TAG, "no:"+number);
    return db.query(Names.TABLE_NAME,
    new String[]
    {
      Names.MESSAGE_NO, Names.SENDER_NAME, Names.RECEIPT_DATE, Names.MESSAGE
      //message_no NOT necessary for iterator; only for list view
    },
    Names.CONVERSATION_ID+"=?",
    new String[]
    {
      number
    },
    null, //no grouping
    null, //no having
    Names.MESSAGE_NO,
    null //no limit
    );
  }

  /**
   *  getAdapter() given a context will return a SimpleCursorAdapter over the
   *  data for displaying the coversation.
   *
   *  @param context for cursor adapter.
   *  @param number String for phone number.
   *  @return SimpleCursorAdapter for conversation data.
   */
  public SimpleCursorAdapter getAdapter(Context context, String number)
  {
    return new SimpleCursorAdapter(context, android.R.layout.simple_list_item_1
      ,getConversationCursor(number), fromColumns, toViews, 0);
  }

  /**
   *  getConversationIterator() given a phone number will return an iterator
   *  over a conversation held between the user and that phone number.
   *
   *  The order in which Messages are iterated over is the order in which they
   *  were sent and received.
   *
   *  @return Iterator over conversation messages.
   */
  public Iterable<Message> getConversationIterator(String number)
  {
    return new MessageIterator(getConversationCursor(number));
  }

  /**
   *  Convenient iterator over message objects from data in a cursor.
   */
  private class MessageIterator implements Iterator<Message>, Iterable<Message>
  {
    Cursor c;
    int senderIndex;
    int dateIndex;
    int messageIndex;

    /**
     *  MessageIterator() given a cursor constructs an iterator over Message
     *  objects from data in the cursor.
     */
    MessageIterator(Cursor c)
    {
      this.c = c;
      senderIndex = c.getColumnIndex(Names.SENDER_NAME);
      dateIndex = c.getColumnIndex(Names.RECEIPT_DATE);
      messageIndex = c.getColumnIndex(Names.MESSAGE);
      Log.d(Names.TAG, "senderDex:"+senderIndex+";date:"+dateIndex+";msg"+messageIndex);
    }

    /**
     *  iterator()
     *
     *  @return this.
     */
    @Override
    public MessageIterator iterator()
    {
      return this;
    }

    /**
     *  hasNext()
     *
     *  @return whether this iterator has at least 1 more element.
     */
    @Override
    public boolean hasNext()
    {
      return c.moveToNext();
    }

    /**
     *  next() returns the next message from the cursor. The iteration order is
     *  over the order in which messages were sent and received.
     *
     *  @return next message
     */
    @Override
    public Message next()
    {
      return new Message((c.getShort(senderIndex) > 0),
        c.getLong(dateIndex),
        c.getString(messageIndex));
    }

    /**
     *  remove() does nothing. Cannot remove messages from the database via the
     *  MessageReader class.
     */
    @Override
    public void remove() throws UnsupportedOperationException { }
  }
}
