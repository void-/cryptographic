package ctxt.db;

/**
 *  Message class simply packages together the data in an sms.
 *  This class has no invariants; all fields are public. Due to this, there are
 *  no getters or setters.
 */
public class Message
{
  /**
   *  Member Variables.
   *
   *  sender boolean representing whether the user sent this message or not.
   *  date long representing the number of seconds since the epoch at which
   *    this message was sent.
   *  message String containing the plaintext, human readable message body in
   *    the sms.
   */
  public boolean sender;
  public long date;
  public String message;

  /**
   *  Message() given a sender, date, and message constructs a new message.
   *  This constructor does nothing more than set all the fields of a Message
   *  instance.
   *
   *  @param sender boolean representing whether the user sent this message or
   *    not.
   *  @param date long representing the number of seconds since the epoch at
   *    which this message was sent.
   *  @param message String containing the plaintext, human readable message
   *    body in the sms.
   */
  public Message(boolean sender, long date, String message)
  {
    this.sender = sender;
    this.date = date;
    this.message = message;
  }
}
