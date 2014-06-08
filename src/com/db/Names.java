package com.db;

/**
 *  Names static class defines constant database names and schema.
 *
 *  All sms's are stored unencrypted.
 *
 *  Schema: sender, receipt date, message.
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
   *  TABLE_NAME
   *  SENDER_NAME 
   *  SENDER_TYPE 
   *  RECEIPT_DATE
   *  RECEIPT_TYPE
   *  MESSAGE
   *  MESSAGE_TYPE
   */
  static final String DATABASE_NAME = ".smsDb";
  static final int VERSION = 0;
  static final String TAG = "SMS_DATABASE";

  static final String TABLE_NAME = "message";

  static final String SENDER_NAME = "sender";
  static final String SENDER_TYPE = "TEXT";
  static final String RECEIPT_DATE = "date";
  static final String RECEIPT_TYPE = "LONG INTEGER";
  static final String MESSAGE = "msg";
  static final String MESSAGE_TYPE = "TEXT";
}
