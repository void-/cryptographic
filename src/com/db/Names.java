package com.db;



/**
 *  Names static class defines constant database names and schema.
 *
 *  All sms's are stored unencrypted.
 *
 *  Schema: message number, conversation, sender, send time, message body.
 *
 *  Conversation: Identifies with whom the user is communicating with; a
 *    particular conversation thread.
 *  Message number: A means to order who sent what message in which order.
 *  Sender: Identifies who sent the message.
 *  Send time: When the message was sent.
 *  Message body: The unencrypted contents of the message.
 */
final class Names
{
  /**
   *  Class Variables.
   *
   *  DATABASE_NAME
   *  VERSION
   *  TAG
   *
   *  MESSAGE_NO
   *  MESSAGE_NO_TYPE this autoincrements, but on a database-wide level; not
   *    specific to a particular conversation.
   *  CONVERSATION_ID phone number of the other individual in the conversation;
   *    international phone numbers are gaurenteed to be 15 characters or less.
   *  CONVERSATION_ID_TYPE
   *  TABLE_NAME
   *  SENDER_NAME boolean that answers the question: Did I send this message?
   *  SENDER_TYPE type representing whether the user sent the message.
   *  RECEIPT_DATE
   *  RECEIPT_TYPE
   *  MESSAGE
   *  MESSAGE_TYPE
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
