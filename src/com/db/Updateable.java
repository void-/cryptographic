package com.db;

/**
 *  Updateable interface with the sole purpose of ensuring that the
 *  ConversationActivity can update its ListView when a new sms is received.
 */
public interface Updateable
{
  /**
   *  update() method called to update the instance for whatever reason.
   */
  public void update();
}
