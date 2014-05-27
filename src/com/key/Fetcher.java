package key;
import android.telephony.TelephonyManager; //For storing self public key
import android.content.Context;
import java.security.*;

/**
 *  Fetcher class given someone's contact number, will return their public key.
 *
 *  Example usage:
 *    Fetcher f = new Fetcher();
 *    f.storeSelfKey(new PublicKey());
 *    Storer.NumberKeyPair myKey = f.shareKey();
 *    Storer.NumberKeyPair nkp = f.fetchKey("+1 555 555 5555");
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
public class Fetcher
{
  /**
   *  Class Variables.
   *
   *  KEYSTORENAME constant string representing the file name used for storing
   *    the public keys on disk.
   */
  private static final String KEYSTORENAME = ".pubKeyStore"

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
    this.ks = KeyStore.getInstance(KeyStore.getDefaultType());

    //invalidate cache for enumerateKeys()
    this.savedPairs = null;
    try
    {
      //load all public keys from disk
      FileInputStream f = openFileInput(Fetcher.KEYSTORENAME);
      ks.load(f);
      f.close();
    }
    catch(FileNotFoundException e)
    {
      //touch key store
      FileOutputStream f = openFileOutput(Fetcher.KEYSTORENAME,
        Context.MODE_PRIVATE);
      f.close();
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
   *
   *  @return array of NumberKeyPair objects representing (number : key) pairs.
   */
  public NumberKeyPair[] enumerateKeys()
  {
    if(savedPairs != null)
    {
      return savedPairs;
    }
    NumberKeyPair[] pairs = new NumberKeyPair[ks.size()]();
    int i = 0;
    for(String number : ks.aliases())
    {
      pairs[i++] = fetchKey(number);
    }
    savedPairs = pairs;
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
    PublicKey p = (PublicKey) ks.getKey(number, null);
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
    return fetchKey((new TelephonyManager()).getLine1Number());
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
    ks.setKeyEntry(number, key, null, null);

    //rewrite the ENTIRE file: this is not preferable
    FileOutputStream f = openFileOutput(Storer.KEYSTORENAME,
      Context.MODE_PRIVATE);
    ks.store(f, null);
    f.close();
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
    this.newKey(((new TelephonyManager()).getLine1Number()), key);
  }

  /**
   *  NumberKeyPair nested class defines a key-value pair for storing
   *  (phone number: public key) pairs.
   *
   *  NumberKeyPair objects are immutable once instantiated.
   */
  public class NumberKeyPair implements serializable
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
    final String getNumber()
    {
      return this.number;
    }

    /**
     *  getKey() returns the key in the (phone number: public key) pair.
     *
     *  @return key.
     */
    final PublicKey getKey()
    {
      return this.number;
    }

    /**
     *  writeObject() serializes this NumberKeyPair.
     *
     *  @param out ObjectOutputStream to write to.
     */
    @Override
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
      //This is cheating
      out.writeObject(this);
    }

    /**
     *  readObject() deserializes this NumberKeyPair.
     *
     *  @param in ObjectInputStream to read from.
     */
    @Override
    private void readObject(java.io.ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
      //still cheating
      NumberKeyPair pair = (NumberKeyPair) in.readObject();
      this.number = pair.number;
      this.key = pair.key;
    }
  }

  /**
   *  KeyAlreadyExistsException represents the exception when a public key for
   *  the same phone number is attempted to be set more than once.
   *
   *  This class has no constructor because the offending phone number should
   *  be quite obvious.
   */
  public class KeyAlreadyExistsException extends Exception
  {
  }
}
