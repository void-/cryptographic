package com.key;

import com.key.Storer;

import android.util.Log;

import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.io.Serializable;
import java.io.IOException;

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
   *  Class Variables.
   *
   *  TAG constant string representing the tag to use when logging exceptions
   *    and debug statements that originate from calls to NumberKeyPair
   *    methods.
   *
   */
  private static final String TAG = "NumberKeyPair";
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

  /**
   *  writeObject() serializes this NumberKeyPair.
   *
   *  The order in which data is serialized:
   *  Phone number as a UTF-8 string.
   *  Int representing the length of the encoded public key in bytes.
   *  The encoded public key as a byte array.
   *
   *  @param out ObjectOutputStream to write to.
   */
  //@Override
  private void writeObject(java.io.ObjectOutputStream out) throws IOException
  {
    //write the phone number
    out.writeUTF(this.number);
    byte[] buffer = (new X509EncodedKeySpec(key.getEncoded()).getEncoded());
    out.writeInt(buffer.length);
    out.write(buffer, 0, buffer.length);
  }

  /**
   *  readObject() deserializes this NumberKeyPair.
   *
   *  The order in which data is deserialized:
   *  Phone number read as a UTF-8 string.
   *  Int representing the length of the encoded public key in bytes.
   *  Allocate a byte array this length.
   *  Read the public key from the stream into the byte array.
   *  Decode the public key from the byte array.
   *
   *  @param in ObjectInputStream to read from.
   */
  //@Override
  private void readObject(java.io.ObjectInputStream in) throws IOException,
      ClassNotFoundException
  {
    //read back the phone number
    this.number = in.readUTF();
    //read the length of the public key
    int len = in.readInt();
    byte[] buffer = new byte[len];
    //read the public key and decode it
    in.read(buffer, 0, len);
    try
    {
      this.key = (KeyFactory.getInstance(Storer.ALGORITHM)).generatePublic(
        new X509EncodedKeySpec((buffer)));
    }
    catch(NoSuchAlgorithmException e)
    {
      Log.e(TAG, "Bad algorithm.", e);
    }
    catch(InvalidKeySpecException e)
    {
      Log.e(TAG, "Problem decoding key; might have been modified.", e);
    }
  }
}
