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
 *  MessageReader provides a readonly interface for sms's stored in the
 *  database.
 *
 *  All data stored in the database is unencrypted.
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
   *  db SQLiteDatabase connection used for reading from the database.
   *  context Context under which the application operates and under which the
   *    database is opened.
   */
  private SQLiteDatabase db;
  private Context context;

  /**
   *  MessageReader() constructs a new MessageReader purposed for reading
   *  sms's from the database.
   *
   *  @param context Context under which the database should be opened.
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
   *  getConversationCursor() given a phone number will return a cursor over
   *  all messages sent and received between the user and the phone number.
   *
   *  The given phone number must be an international number including country
   *  code and area code. For example, "5215554" is *not* acceptable, but
   *  "15555215554" is. It does not matter if the given number contains
   *  delimiters or not, getConversationCursor() removes them regardless.
   *
   *  @param String representing the phone number of the conversation to get.
   *  @return Cursor over the conversation.
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
   *  The given phone number must be an international number including country
   *  code and area code. For example, "5215554" is *not* acceptable, but
   *  "15555215554" is. It does not matter if the given number contains
   *  delimiters or not, getConversationCursor() removes them regardless.
   *
   *  @param context for cursor adapter.
   *  @param number String for phone number.
   *  @return SimpleCursorAdapter for conversation data.
   */
  public SimpleCursorAdapter getAdapter(Context context, String number)
  {
    return new MessageCursorAdapter(context, R.layout.message,
      getConversationCursor(number), fromColumns, toViews, 0);
  }

  /**
   *  updateAdapter() given an adapter will swap its cursor for a new one and
   *  notify the adapter that its dataset has changed.
   *
   *  This method should be called when the database has changed and the user
   *  of the Adapter needs fresh data.
   *
   *  @param a SimpleCursorAdapter with an old cursor, this will be mutated.
   *  @param number phone number for which to get a new cursor.
   */
  public void updateAdapter(SimpleCursorAdapter a, String number)
  {
    a.swapCursor(getConversationCursor(number)).close();
    a.notifyDataSetChanged();
  }

  /**
   *  This method is deprecated, use getAdapter() instead to get a cursor.
   *
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
    /**
     *  Class Variables.
     *
     *  COLOR_SENT int representing the color to set the view of a sent sms to.
     *  COLOR_RECEIVED color to set the view of a received sms to.
     */
    private static final int COLOR_SENT = android.R.color.black;
    private static final int COLOR_RECEIVED = android.R.color.holo_orange_dark;

    /**
     *  MessageCursorAdapter() calls the super constructor.
     *
     *  For usage, refer to SimpleCursorAdapter's constructor.
     */
    MessageCursorAdapter(Context co, int i, Cursor c, String[] s, int[] j,
        int k)
    {
      super(co, i, c, s, j, k);
    }

    /**
     *  ViewWrapper container class to be passed around from calls to newView()
     *  and bindView().
     *
     *  This class has no getter for member variable base, but use getLabel()
     *  to get the label field.
     */
    private class ViewWrapper
    {
      /**
       *  Member Variables.
       *
       *  base View object representing the base View in a conversation.
       *  label TextView for an actual text box in a conversation.
       */
      View base;
      TextView label = null;

      /**
       *  ViewWrapper() given a base constructs a new ViewWrapper. The label
       *  field will be lazily created.
       *
       *  @param base View object to store.
       */
      ViewWrapper(View base)
      {
        this.base = base;
      }

      /**
       *  getLabel() returns the label TextView object stored in this
       *  ViewWrapper.
       *
       *  label is lazily created upon calling this method for the first time.
       *
       *  @return label TextView object.
       */
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
     *  newView() is called to create a new View for the adapter.
     *
     *  newView() instantiates a new View object, but does not populate its
     *  data. This is because doing so would be pointless because the super
     *  class immediately calls bindView() after this method.
     *
     *  @param context Context under which the View is created.
     *  @param cursor cursor that contains data to create the View with.
     *  @param parent ViewGroup that will be a parent to the new View.
     *  @return new View created.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
      LayoutInflater inflater = LayoutInflater.from(context);
      View row = inflater.inflate(R.layout.message, null);
      ViewWrapper w = new ViewWrapper(row);
      //set tag to pass data to the View for use in bindView()
      row.setTag(w);
      return row;
    }

    /**
     *  bindView() is called to conditionally populate the data in a newly
     *  created View.
     *
     *  bindView(), depending on the sender of the current message in the
     *  cursor, will set the gravity of the text and the background color.
     *
     *  @param row View that was recently created via newView().
     *  @param context Context under which the View is created.
     *  @param cursor cursor that contains data to create the View with.
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
  @Deprecated
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
