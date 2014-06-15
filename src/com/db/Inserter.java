package com.db;

import com.db.MessageInserter;

import android.content.Context;

/**
 *  Singleton for message inserter.
 *
 */
public class Inserter
{
  static MessageInserter _INSERTER = null;

  /**
   *  Inserter() private constructor prevents instantiation.
   */
  private Inserter() {}

  /**
   *  getMessageInserter() returns a static reference to a MessageInserter
   *  instance.
   *
   *  Lazily loads.
   *
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
