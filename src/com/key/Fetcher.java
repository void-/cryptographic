package src.com.key;

import src.com.key.NumberKeyPair;
import src.com.key.KeyAlreadyExistsException;

import android.util.Log;
import android.telephony.TelephonyManager; //For storing self public key
import android.app.Activity;
import android.content.Context;
import java.security.*;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Enumeration;

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
 *    Use null for keystore password.
 *    Use null for cert chain:certs aren't relevant.
 *
 */
public class Fetcher extends Activity //extend just to get internal file access
{
  /**
   *  Class Variables.
   *
   *  KEYSTORENAME constant string representing the file name used for storing
   *    the public keys on disk.
   *  TAG constant string representing the tag to use when logging events that
   *    originate from calls to Fetcher methods.
   */
  private static final String KEYSTORENAME = ".pubKeyStore";
  private static final String TAG = "FETCHER";

  /**
   *  Member Variables.
   *
   *  ks KeyStore to hold public keys.
   *  savedPairs NumberKeyPair array to cache the output of enumerateKeys().
   */
  private KeyStore ks;
  private NumberKeyPair[] savedPairs;

  /**
   *  Fetcher() constructs a new Fetcher instance.
   *
   *  If the keystore file does not already exist, a new one will be created.
   *  Otherwise, the keystore will be loaded from disk.
   */
  public Fetcher()
  {
    //setup connection to database/ unpickle things or whatever
    try
    {
      this.ks = KeyStore.getInstance(KeyStore.getDefaultType());
    }
    catch(KeyStoreException e) {}

    //invalidate cache for enumerateKeys()
    this.savedPairs = null;
    try
    {
      //load all public keys from disk
      FileInputStream fi = openFileInput(Fetcher.KEYSTORENAME);
      try
      {
        ks.load(fi, null);
        fi.close();
      }
      catch(IOException e) { Log.e(Fetcher.TAG, "exception", e); }
      catch(NoSuchAlgorithmException e) { Log.e(Fetcher.TAG, "exception", e); }
      catch(java.security.cert.CertificateException e)
      { Log.e(Fetcher.TAG, "exception", e); }
    }
    catch(FileNotFoundException e)
    {
      //touch key store
      try
      {
        FileOutputStream fo = openFileOutput(Fetcher.KEYSTORENAME,
          Context.MODE_PRIVATE);
        fo.close();
      }
      catch(FileNotFoundException e1) { Log.e(Fetcher.TAG, "exception", e1); }
      catch(IOException e1) { Log.e(Fetcher.TAG, "exception", e1); }
    }
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
  public NumberKeyPair[] enumerateKeys()
  {
    //savedPairs cache still valid
    if(savedPairs != null)
    {
      return savedPairs;
    }
    NumberKeyPair[] pairs = null;
    try
    {
      pairs = new NumberKeyPair[ks.size()];
    }
    catch(KeyStoreException e) { Log.e(Fetcher.TAG, "exception", e); }
    //Iterate through the keystore aliases, lookup its key, add to output array
    int i = 0;
    try
    {
      for(Enumeration<String> number = ks.aliases(); number.hasMoreElements();)
      {
        pairs[i++] = fetchKey(number.nextElement());
      }
    }
    catch(KeyStoreException e) { Log.e(Fetcher.TAG, "exception", e); }
    this.savedPairs = pairs;
    return pairs;
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
    PublicKey p = null;
    try
    {
      p = (PublicKey) ks.getKey(number, null);
    }
    catch(KeyStoreException e) { Log.e(Fetcher.TAG, "exception", e); }
    catch(NoSuchAlgorithmException e) { Log.e(Fetcher.TAG, "exception", e); }
    catch(UnrecoverableKeyException e) { Log.e(Fetcher.TAG, "exception", e); }
    if(p == null)
    {
      return null;
    }
    return (new NumberKeyPair(number, p));
  }

  /**
   *  shareKey() fetches the user's public key.
   *
   *  @return user's public key.
   */
  public NumberKeyPair shareKey()
  {
    return fetchKey(((TelephonyManager)getSystemService(
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
    //if the number is not known, throw an exception
    if(fetchKey(number) != null)
    {
      throw new KeyAlreadyExistsException();
    }
    //insert the new public key
    try
    {
      ks.setKeyEntry(number, key, null, null);
    }
    catch(KeyStoreException e) { Log.e(Fetcher.TAG, "exception", e); }

    //rewrite the ENTIRE file: this is not preferable
    try
    {
      FileOutputStream f = openFileOutput(Fetcher.KEYSTORENAME,
        Context.MODE_PRIVATE);
      ks.store(f, null);
      f.close();
    }
    catch(FileNotFoundException e) { Log.e(Fetcher.TAG, "exception", e); }
    catch(KeyStoreException e) { Log.e(Fetcher.TAG, "exception", e); }
    catch(NoSuchAlgorithmException e) { Log.e(Fetcher.TAG, "exception", e); }
    catch(java.security.cert.CertificateException e)
    { Log.e(Fetcher.TAG, "exception", e); }
    catch(IOException e) { Log.e(Fetcher.TAG, "exception", e); }
    //invalidate cache for enumerateKeys()
    this.savedPairs = null;
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
    this.newKey(((TelephonyManager)getSystemService(
      Context.TELEPHONY_SERVICE)).getLine1Number(), key);
  }
}
