package com.key;

import com.key.NumberKeyPair;
import com.key.KeyAlreadyExistsException;

import android.util.Log;
import android.telephony.TelephonyManager; //For storing self public key
import android.telephony.PhoneNumberUtils;
import android.app.Activity;
import android.content.Context;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;

/**
 *  Fetcher class provides an interface for retrieving public keys. Methods are
 *  provided for handling the user's public key and encryption. Keys are stored
 *  in (Phone number: Public key) pairs.
 *
 *  Terminology:
 *    The term, "user", refers to the owner of the mobile phone this
 *    application executes on. The phrase, "user's public key", refers to the
 *    public key in the (Phone number: Public key), pair with the phone number
 *    of the mobile phone this application executes on. There is only one user
 *    per mobile phone.
 *
 *  Example usage:
 *    Fetcher f = new Fetcher();
 *    f.storeSelfKey(new PublicKey(--arguments--));
 *    NumberKeyPair myKey = f.shareKey();
 *    NumberKeyPair nkp = f.fetchKey("+1 555 555 5555");
 *    Fetcher.encrypt("Hello, World!".getBytes(), nkp.getKey());
 *
 *  This entails general method usage, but excludes exception handling and the
 *  use of the Key singleton. For actual usage in the rest of the program, get
 *  a static instance of the Fetcher class via calling Key.getFetcher().
 *
 *  The Fetcher class has a particular way to be initialized with respect to
 *  the Storer class. The method, Storer.generateKeyPair() must be called prior
 *  to any calls to Fetcher methods. This need only happen once in time, *not*
 *  each time the program executes. Storer.generateKeyPair() generates a public
 *  key for the user and requests that a static Fetcher instance store this
 *  key. Once any instance of the Fetcher class has stored a public key
 *  corresponding to the user, then all methods provided in this class achieve
 *  full functionality. Attempting to store a key corresponding to the same
 *  phone number will result in a KeyAlreadyExistsException. A means to test if
 *  the user's public key has been stored on disk is to check if shareKey()
 *  returns null.
 *
 *  The means by which Fetcher stores
 */
public class Fetcher
{
  /**
   *  Class Variables.
   *
   *  TAG constant string representing the tag to use when logging exceptions
   *    and debug statements that originate from calls to Fetcher methods.
   *  NUMBER_STORE string representing the file name used to store the user's
   *    phone number. This is necessary because depending on the particular
   *    mobile phone model, there might be no programmatic way to access the
   *    phone's phone number. The alternative means is to ask the user to enter
   *    their phone number and save it on disk.
   */
  private static final String TAG = "FETCHER";
  private static final String NUMBER_STORE = ".myNumber";

  /**
   *  Member Variables.
   *
   *  context Context under which the application operates. This is used for
   *    access to the filesystem: for key storage.
   */
  private Context context;

  /**
   *  Fetcher() given a Context, constructs a new Fetcher instance.
   *
   *  Nothing special is done. Instantiating a Fetcher does not guarantee that
   *  this class's methods will have full functionality. This property requires
   *  that the user's public key has been stored on disk. A means to test for
   *  this is checking if calling shareKey() returns null.
   */
  Fetcher(Context context)
  {
    this.context = context;
  }

  /**
   *  enumerateKeys() returns all phone numbers in the (Phone number: Public
   *  key) pairs stored by this class.
   *
   *  To retrieve the public key that corresponds to a phone number, call
   *  fetchKey() with an argument of the phone number String.
   *
   *  @return array of Strings containing all stored phone numbers.
   */
  public String[] enumerateKeys()
  {
    return context.fileList();
  }

  /**
   *  fetchKey() given a phone number, returns a NumberKeyPair representing the
   *  given phone number and the corresponding public key.
   *
   *  If the given number does not have a corresponding public key, null is
   *  returned.
   *
   *  @param number number to find the public key for.
   *  @return (number: number's public key) NumberKeyPair instance or null.
   */
  public NumberKeyPair fetchKey(String number)
  {
    //unify the number to a standard format
    String numberFormatted = PhoneNumberUtils.formatNumber(number);
    FileInputStream f = null;
    //read the public key from disk
    try
    {
      f = context.openFileInput(numberFormatted);
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      int nRead;
      byte[] data = new byte[Storer.KEYBITS];

      while((nRead = f.read(data, 0, data.length)) != -1)
      {
        buffer.write(data, 0, nRead);
      }
      //decode the public key
      PublicKey k = (KeyFactory.getInstance(Storer.ALGORITHM)).generatePublic(
        new X509EncodedKeySpec((buffer.toByteArray())));
      f.close();
      return new NumberKeyPair(numberFormatted, k);
    }
    catch(IOException e)
    {
      Log.e(TAG, "Couldn't find number", e);
    }
    catch(NoSuchAlgorithmException e)
    {
      Log.e(TAG, "Bad algorithm.", e);
    }
    catch(InvalidKeySpecException e)
    {
      Log.e(TAG, "Problem decoding key; might have been modified.", e);
    }
    //failure to find/read/decode public key
    return null;
  }

  /**
   *  shareKey() fetches the user's public key.
   *
   *  @return user's public key in a NumberKeyPair or null if not found.
   */
  public NumberKeyPair shareKey()
  {
    //load user's phone number from disk
    ByteArrayOutputStream buffer = null;
    try
    {
      FileInputStream f = new FileInputStream(
        new File(
          context.getDir(
            Storer.DIRECTORY, Context.MODE_PRIVATE),
          Fetcher.NUMBER_STORE));
      buffer = new ByteArrayOutputStream();

      int nRead;
      byte[] data = new byte[16]; //at most 15 for international number

      while((nRead = f.read(data, 0, data.length)) != -1)
      {
        buffer.write(data, 0, nRead);
      }
      f.close();
    }
    catch(IOException e)
    {
      Log.e(TAG, "Couldn't find number", e);
      return null;
    }
    return fetchKey(new String(buffer.toByteArray()));
  }

  /**
   *  newKey() given a phone number and public key will persistently store it
   *  on disk.
   *
   *  To store the user's public key, use storeSelfKey() instead.
   *
   *  If the number already has a corresponding public key, regardless of
   *  whether the given public key is different, a KeyAlreadyExistsException is
   *  thrown.
   *
   *  @param number phone number corresponding to the public key to insert.
   *  @param key public key to insert.
   */
  public void newKey(String number, PublicKey key) throws
      KeyAlreadyExistsException
  {
    //if the number is known, i.e. it already has a key, throw an exception
    if(fetchKey(number) != null)
    {
      throw new KeyAlreadyExistsException();
    }
    //unify the number to a standard format
    String numberFormatted = PhoneNumberUtils.formatNumber(number);
    FileOutputStream f = null;
    try
    {
      f = context.openFileOutput(numberFormatted, Context.MODE_PRIVATE);
      f.write(new X509EncodedKeySpec(key.getEncoded()).getEncoded());
      f.close();
    }
    catch(IOException e)
    {
      Log.e(TAG, "Strange io exception", e);
    }
  }

  /**
   *  storeSelfKey() given the user's phone number and public key will
   *  persistently store the public key and phone number (the user's phone
   *  number will appear twice on disk).
   *
   *  If the user's public key is already an entry, a KeyAlreadyExistsException
   *  is thrown.
   *
   *  @param number String representing the user's mobile phone number.
   *  @param key PublicKey representing the user's public key.
   */
  void storeSelfKey(String number, PublicKey key) throws
    KeyAlreadyExistsException
  {
    //store the key like normal; newKey will properly format the number
    this.newKey(number, key);
    //write phone number to file
    try
    {
      FileOutputStream f = new FileOutputStream(
        new File(
          context.getDir(
            Storer.DIRECTORY, Context.MODE_PRIVATE),
          Fetcher.NUMBER_STORE));
      f.write(number.getBytes());
      f.close();
    }
    catch(IOException e)
    {
      Log.e(TAG, "Couldn't save phone number", e);
    }
  }

  /**
   *  encrypt() given a plaintext, produces the corresponding ciphertext
   *  encrypted under a given public key.
   *
   *  null is returned if the encryption was unsuccessful. However, an
   *  unchecked exception is probably raised if the given plaintext byte array
   *  exceeds the amount of data the encryption algorithm can accept.
   *
   *  @param plaintext plaintext byte array to encrypt.
   *  @param k public key to encrypt under.
   *  @return ciphertext of plaintext encrypted under public key k or null.
   */
  public static byte[] encrypt(byte[] plaintext, PublicKey k)
  {
    try
    {
      Cipher c = Cipher.getInstance(Storer.ENCRYPTION_MODE);
      c.init(Cipher.ENCRYPT_MODE, k);

      byte[] cipherText = c.doFinal(plaintext);
      Log.d(TAG, "len:"+cipherText.length);
      return cipherText;
    }
    catch(InvalidKeyException e) {Log.e(Fetcher.TAG, "exception", e); }
    catch(NoSuchAlgorithmException e) {Log.e(Fetcher.TAG, "exception", e); }
    catch(javax.crypto.NoSuchPaddingException e)
    {Log.e(Fetcher.TAG, "exception", e); }
    catch(javax.crypto.IllegalBlockSizeException e)
    {Log.e(Fetcher.TAG, "exception", e); }
    catch(javax.crypto.BadPaddingException e)
    {Log.e(Fetcher.TAG, "exception", e); }
    return null;
  }
}
