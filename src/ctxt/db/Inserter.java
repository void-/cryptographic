package ctxt.db;

import ctxt.db.MessageInserter;

import android.content.Context; //for constructing a MessageInserter

/**
 *  Inserter class is a singleton for the MessageInserter class.
 *
 *  This design is less than ideal, but it reduces complexity for
 *  synchronization and registering a callback for when a new sms is received.
 */
public class Inserter
{
  /**
   *  Class Variables.
   *
   *  _INSERTER static MessageInserter instance.
   */
  static MessageInserter _INSERTER = null;

  /**
   *  Inserter() private constructor prevents instantiation; does nothing.
   */
  private Inserter() {}

  /**
   *  getMessageInserter() returns a static reference to a MessageInserter
   *  instance.
   *
   *  The MessageInserter instance is lazily loaded upon first used.
   *
   *  @param context Context under which to construct the MessageInserter.
   *  @return static MessageInserter instance.
   */
  public static MessageInserter getMessageInserter(Context context)
  {
    if(_INSERTER == null)
    {
      _INSERTER = new MessageInserter(context);
    }
    return _INSERTER;
  }
}
