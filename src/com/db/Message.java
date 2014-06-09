package com.db;

/**
 *  Message class simply for packaging data together.
 *  This class has no invariants; all fields are public.
 *
 */
public class Message
{
  public boolean sender;
  public long date;
  public String message;

  public Message(boolean sender, long date, String message)
  {
    this.sender = sender;
    this.date = date;
    this.message = message ;
  }
}
