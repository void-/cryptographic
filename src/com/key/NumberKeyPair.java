package src.com.key;

import java.security.PublicKey;
import java.io.Serializable;

/**
 *  NumberKeyPair class defines a key-value pair for storing
 *  (phone number: public key) pairs.
 *
 *  NumberKeyPair objects are immutable once instantiated.
 */
public class NumberKeyPair implements Serializable
{
  /**
   *  Member Variables.
   *
   *  number String representing the phone number that corresponds with the
   *    held public key.
   *  key PublicKey representing the public key.
   */
  private String number;
  private PublicKey key;

  /**
   *  NumberKeyPair constructs a new NumberKeyPair given a string
   *  representing the phone number and a PublicKey representing the key.
   *
   *  Once instantiated, this object is immutable.
   */
  NumberKeyPair(String number, PublicKey key)
  {
    this.number = number;
    this.key = key;
  }

  /**
   *  getNumber() returns the number in the (phone number: public key) pair.
   *
   *  @return number.
   */
  public final String getNumber()
  {
    return this.number;
  }

  /**
   *  getKey() returns the key in the (phone number: public key) pair.
   *
   *  @return key.
   */
  public final PublicKey getKey()
  {
    return this.key;
  }

//  /**
//   *  writeObject() serializes this NumberKeyPair.
//   *
//   *  @param out ObjectOutputStream to write to.
//   */
//  @Override
//  private void writeObject(ObjectOutputStream out) throws IOException
//  {
//    //This is cheating
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
