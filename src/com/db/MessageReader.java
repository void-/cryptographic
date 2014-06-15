package com.db;

import com.ctxt.R;

import com.db.Names;
import com.db.MessageDatabaseHelper;
import com.db.Message;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.database.Cursor;
import android.database.ContentObserver;
import android.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.SimpleCursorAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import android.telephony.PhoneNumberUtils;

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
   *  fromColumns
   *  toView
   */
  protected static final String[] fromColumns = { Names.MESSAGE };
  protected static final int[] toViews = { R.id.message_text };

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
      PhoneNumberUtils.stripSeparators(number)
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
    //return new SimpleCursorAdapter(context, android.R.layout.simple_list_item_1
    //  ,getConversationCursor(number), fromColumns, toViews, 0);
    return new MessageCursorAdapter(context, R.layout.message,
      getConversationCursor(number), fromColumns, toViews, 0);
  }

  /**
   *  updateAdapter() given an adapter will swap its cursor for a new one and
   *  notify the adapter that its dataset has changed.
   */
  public void updateAdapter(SimpleCursorAdapter a, String number)
  {
    a.swapCursor(getConversationCursor(number)).close();
    a.notifyDataSetChanged();
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
  @Deprecated
  public Iterable<Message> getConversationIterator(String number)
  {
    return new MessageIterator(getConversationCursor(number));
  }

  /**
   *  MessageCursorAdapter class extends SimpleCursorAdapter to conditionally
   *  provide different views based upon the results of the query.
   */
  private class MessageCursorAdapter extends SimpleCursorAdapter
  {
    private static final int COLOR_SENT = android.R.color.black;
    private static final int COLOR_RECEIVED = android.R.color.holo_orange_dark;

    /**
     *  MessageCursorAdapter() calls the super constructor.
     */
    MessageCursorAdapter(Context co, int i, Cursor c, String[] s, int[] j,
        int k)
    {
      super(co, i, c, s, j, k);
    }

    /**
     *  ViewWrapper container class.
     */
    private class ViewWrapper
    {
      View base;
      TextView label = null;

      ViewWrapper(View base)
      {
        this.base = base;
      }

      TextView getLabel()
      {
        if(label == null)
        {
          label = (TextView) base.findViewById(R.id.message_text);
        }
        return this.label;
      }
    }

    /**
     *  newView()
     *
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
      LayoutInflater inflater = LayoutInflater.from(context);
      View row = inflater.inflate(R.layout.message, null);
      ViewWrapper w = new ViewWrapper(row);
      row.setTag(w);
      return row;
    }

    /**
     *  bindView()
     *
     */
    @Override
    public void bindView(View row, Context context, Cursor c)
    {
      ViewWrapper w = (ViewWrapper) row.getTag();
      TextView t = w.getLabel();

      //This is where the actual logic goes for picking the view
      if(c.getShort(c.getColumnIndex(Names.SENDER_NAME)) > 0) //user sent it
      {
        t.setGravity(Gravity.LEFT);
        t.setBackgroundResource(COLOR_SENT);
      }
      else //user received it
      {
        t.setGravity(Gravity.RIGHT);
        t.setBackgroundResource(COLOR_RECEIVED);
        //row.setBackgroundResource(COLOR_RECEIVED);
        //t.setTextColor(android.R.color.primary_text_dark);
      }
      t.setText(c.getString(c.getColumnIndex(Names.MESSAGE)));
    }
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
      Log.d(Names.TAG, "senderDex:"+senderIndex+";date:"+dateIndex+";msg"+
        messageIndex);
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
