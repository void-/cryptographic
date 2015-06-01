package ctxt.db;

/**
 *  Names static class defines database constants and schema.
 *
 *  All sms's are stored unencrypted.
 *
 *  Schema: message number, conversation, sender, send time, message body.
 *
 *  Message number: A means to order who sent what message in which order.
 *  Conversation: Identifies with whom the user is communicating with; a
 *    particular conversation thread.
 *  Sender: Identifies who sent the message.
 *  Send time: When the message was sent.
 *  Message body: The unencrypted contents of the message.
 *
 *  The message number is an auto incrementing integer. Contrary to intuition,
 *  this number does not increment per each successive message in a
 *  conversation, but rather on a database-wide level. This is more efficient
 *  and still equally useful for determining the order in which messages were
 *  sent in time.
 *
 *  A sequence of messages exchanged between the user and another individual is
 *  referred to as a conversation. In this database, the value held in the
 *  conversation field is the other individual's phone number. This is true
 *  regardless of whether or not the message was sent by the user or by the
 *  other individual; to determine this, the sender field is used. The way in
 *  which the conversation field is formatted as a string is in an
 *  international format without any delimiters. For example, "15555215556" is
 *  an example of a conversation number that would be in the database.
 */
final class Names
{
  /**
   *  Class Variables.
   *
   *  DATABASE_NAME String containing the name of the database.
   *  VERSION int representing the current version of the database in case of
   *    upgrades.
   *  TAG String used for the tag when logging debug statements and exceptions
   *  that originate from any class in the db package.
   *
   *  TABLE_NAME the name of the table in the database used to store messages.
   *
   *  MESSAGE_NO the name of a message number in the database.
   *  MESSAGE_NO_TYPE MESSAGE_NO's type in the database. This integer auto
   *    increments, but on a database-wide level; not in the scope of a
   *    particular conversation.
   *  CONVERSATION_ID phone number of the other individual in the conversation;
   *    
   *  CONVERSATION_ID_TYPE international phone numbers are guaranteed to be 15
   *    characters or less excluding the delimiters.
   *  SENDER_NAME field that answers the question: Did I send this message?
   *  SENDER_TYPE boolean type representing whether the user sent the message.
   *  RECEIPT_DATE time since the epoch that the message was sent by the
   *    sender.
   *  RECEIPT_TYPE long integer for storing the number of seconds from the
   *    epoch.
   *  MESSAGE the actual message of an sms that was either sent or received.
   *  MESSAGE_TYPE size unlimited text blob.
   */
  static final String DATABASE_NAME = ".smsDb";
  static final int VERSION = 1;
  static final String TAG = "SMS_DATABASE";

  static final String TABLE_NAME = "message";

  static final String MESSAGE_NO = "_id";
  static final String MESSAGE_NO_TYPE = "INTEGER PRIMARY KEY";
  static final String CONVERSATION_ID = "conv";
  static final String CONVERSATION_ID_TYPE = "char(15)";
  static final String SENDER_NAME = "sender";
  static final String SENDER_TYPE = "TINYINT";
  static final String RECEIPT_DATE = "receipt";
  static final String RECEIPT_TYPE = "INT8";
  static final String MESSAGE = "msg";
  static final String MESSAGE_TYPE = "TEXT";
}
