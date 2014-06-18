package com.key;

import java.security.PublicKey;
import java.io.Serializable;

/**
 *  NumberKeyPair class defines a key-value pair for storing
 *  (Phone number: Public key) pairs. Use getter methods, getNumber() and
 *  getKey() to get the phone number String and PublicKey respectively.
 *
 *  NumberKeyPair objects are immutable once instantiated.
 */
public class NumberKeyPair implements Serializable
{
  /**
   *  Member Variables.
   *
   *  number String representing the phone number that corresponds to the
   *    held public key.
   *  key PublicKey representing the public key.
   */
  private String number;
  private PublicKey key;

  /**
   *  NumberKeyPair constructs a new NumberKeyPair given a string
   *  representing the phone number and a PublicKey representing the key. It is
   *  preferred if the given phone number is in a standard format for
   *  consistency.
   *
   *  Once instantiated, this object is immutable.
   *  @param number String representing the phone number key.
   *  @param key PublicKey representing the PublicKey value.
   */
  NumberKeyPair(String number, PublicKey key)
  {
    this.number = number;
    this.key = key;
  }

  /**
   *  getNumber() returns the number in the (phone number: public key) pair.
   *
   *  @return String containing the phone number.
   */
  public final String getNumber()
  {
    return this.number;
  }

  /**
   *  getKey() returns the public key in the (phone number: public key) pair.
   *
   *  @return PublicKey.
   */
  public final PublicKey getKey()
  {
    return this.key;
  }

  //actually implementing the Serializable methods may be unnecessary

//  /**
//   *  writeObject() serializes this NumberKeyPair.
//   *
//   *  @param out ObjectOutputStream to write to.
//   */
//  @Override
//  private void writeObject(ObjectOutputStream out) throws IOException
//  {
//    out.writeObject(this);
//  }

//  /**
//   *  readObject() deserializes this NumberKeyPair.
//   *
//   *  @param in ObjectInputStream to read from.
//   */
//  @Override
//  private void readObject(java.io.ObjectInputStream in) throws IOException,
//      ClassNotFoundException
//  {
//    //still cheating
//    NumberKeyPair pair = (NumberKeyPair) in.readObject();
//    this.number = pair.number;
//    this.key = pair.key;
//  }
}
