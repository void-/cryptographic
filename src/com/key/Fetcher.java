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
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;

/**
 *  Fetcher class given someone's contact number, will return their public key.
 *
 *  Example usage:
 *    Fetcher f = new Fetcher();
 *    f.storeSelfKey(new PublicKey());
 *    Storer.NumberKeyPair myKey = f.shareKey();
 *    Storer.NumberKeyPair nkp = f.fetchKey("+1 555 555 5555");
 *
 *  This entails general method usage, but excludes exception handling and the
 *  use of the Key singleton.
 *
 *  (prospective) General useage steps:
 *    Initialize a Fetcher; it will open up the database or whatever it needs
 *    Ask the Fetcher to enumerate all contacts (people who's pub keys it has)
 *    Ask the Fetcher for someone's public key; raise exception if not found
 *    Ask someone(?) to encrypt a message under the public key
 *    ...other things that dont concern the Fetcher
 *
 *    or
 *    Init a Fetcher
 *    Inform the Fetcher to store a new public key(why's this a fetcher then?)
 *    Ask the Fetcher for own public key(combine with regular lookup?)
 *
 *
 */
public class Fetcher
{
  /**
   *  Class Variables.
   *
   *  TAG constant string representing the tag to use when logging events that
   *    originate from calls to Fetcher methods.
   *  context Context under which this app operates. This is used for access to
   *    the filesystem; for key storage.
   */
  private static final String TAG = "FETCHER";
  private static Context context;

  /**
   *  Fetcher() constructs a new Fetcher instance.
   *
   *  If the keystore file does not already exist, a new one will be created.
   *  Otherwise, the keystore will be loaded from disk.
   */
  Fetcher(Context context)
  {
    this.context = context;
  }

  /**
   *  enumerateKeys() returns all know public keys as an array of NumberKeyPair
   *  objects.
   *
   *  If, for some reason, any number in the keystore does not have a
   *  corresponding public key or does not exist, the entry in the array will
   *  be null.
   *
   *  Caching is done on the output by private member variable 'savedPairs'.
   *  The savedPairs cache is invalidated by setting it to null.
   *
   *  @return array of NumberKeyPair objects representing (number : key) pairs.
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
   *  @return (number: number public key) NumberKeyPair instance or null.
   */
  public NumberKeyPair fetchKey(String number)
  {
    //unify the number to a standard format
    String numberFormatted = PhoneNumberUtils.formatNumber(number);
    FileInputStream f = null;
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
    return null;
  }

  /**
   *  shareKey() fetches the user's public key.
   *
   *  @return user's public key wrapped in a NumberKeyPair.
   */
  public NumberKeyPair shareKey()
  {
    return fetchKey(((TelephonyManager)context.getSystemService(
      Context.TELEPHONY_SERVICE)).getLine1Number());
  }

  /**
   *  newKey() given a phone number and public key will insert it into the
   *  keystore.
   *
   *  If the number already has a corresponding public key, regardless of
   *  whether the given public key is different, a KeyAlreadyExistsException is
   *  raised.
   *
   *  Each call the newKey() rewrites the entire keystore file and invalidates
   *  the enumerateKeys() output cache.
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
   *  storeSelfKey() given the user's public key will create a new entry in the
   *  public key keystore under the user's phone number.
   *
   *  If the user's public key is already an entry, a KeyAlreadyExistsException
   *  is thrown.
   *
   *  @param key user's public key.
   */
  void storeSelfKey(PublicKey key) throws KeyAlreadyExistsException
  {
    this.newKey(((TelephonyManager)context.getSystemService(
      Context.TELEPHONY_SERVICE)).getLine1Number(), key);
  }

  /**
   *  given plaintext, produce ciphertext.
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
