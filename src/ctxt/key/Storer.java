package ctxt.key;

import ctxt.key.Key;
import ctxt.key.KeyAlreadyExistsException;
import android.util.Log;
import android.app.Activity;
import android.content.Context;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

/**
 *  Storer class handles storage of the user's single, private key. Methods are
 *  provided for decryption under the user's private key.
 *
 *  Terminology:
 *    The term, "user", refers to the owner of the mobile phone this
 *    application executes on. The phrase, "user's private key", refers to the
 *    private portion of the key pair generated by the Storer class used for
 *    decryption on the mobile phone this application executes on. There is
 *    only one user per mobile phone. There is only one public key per user.
 *
 *  Example usage:
 *    Storer s = new Storer();
 *    s.generateKeyPair();
 *    byte[] cipherText = --some method that returns ciphertext--
 *    byte[] plainText = s.decrypt(cipherText);
 *    --do something with plainText--
 *
 *  This entails general method usage, but excludes exception handling and the
 *  use of the Key singleton. For actual usage in the rest of the program, get
 *  a static instance of the Storer class via calling Key.getStorer(). Storer's
 *  constructor is package protected to prevent non static instances.
 *
 *  Storer is not fully functional until generateKeyPair() has been called
 *  at some point in time. This method generates a key pair and stores both on
 *  disk. To check if the private portion of the key pair has been stored on
 *  disk, check the status of isKeyAvailable().
 *
 *  The private key managed by Storer never leaves Storer. This prevents
 *  accidental exfiltration of the private key and decreases the size of the
 *  TCB. However, the private key is simply stored in a regular file on disk.
 *  If intentionally malicious code executes in the origin of the application's
 *  package, then the private key is at risk. The safety of the private key
 *  from other applications relies on the operating system's security. Physical
 *  compromise of the mobile device guarantees no safety.
 */
public class Storer
{
  /**
   *  Class Variables.
   *
   *  KEYBITS constant int representing the size of keys to use. 1064 is
   *    selected because this is the maximum number of bytes that can fit into
   *    an sms.
   *  ALGORITHM constant string representing which public key algorithm to use
   *    for key generation and storage.
   *  ENCRYPTION_MODE constant string representing how the public and private
   *    keys are used for encryption and decryption.
   *  DIRECTORY constant string representing the subdirectory in which the
   *    user's private key(with Storer) and phone number(with Fetcher) are
   *    stored for the purpose of simplifying Fetcher.enumerateKeys().
   *  KEYSTORENAME constant string representing the file name used for storing
   *    the private key on disk.
   *  TAG constant string representing the tag to use when logging debug
   *    statements and exceptions that originate from calls to Storer methods.
   */
  public static final int KEYBITS = 1064;
  public static final String ALGORITHM = "RSA";
  static final String ENCRYPTION_MODE = "RSA/ECB/PKCS1Padding";
  static final String DIRECTORY = "me";
  private static final String KEYSTORENAME = ".privKey";
  private static final String TAG = "STORER";

  /**
   *  Member Variables.
   *
   *  k user's PrivateKey for decryption. The algorithm used is represented by
   *    Storer.ALGORITHM.
   *  c Cipher object initialized using Storer.ENCRYPTON_MODE and k. This
   *    object exists so that a new cipher does not need to be instantiated each
   *    time decrypt() is called.
   *  context Context under which the application operates. This is used for
   *    access to the filesystem: for key storage.
   *  keyGenerated boolean indicating whether the user's private key is
   *    available.
   */

  private PrivateKey k;
  protected Cipher c;
  private Context context;
  private boolean keyGenerated;

  /**
   *  Storer() constructs a new storer instance.
   *
   *  If a private key is present on disk, read it from disk and store in
   *  member variable k, then initialize c for decryption and set keyGenerated.
   *  If no private key can be found on disk, i.e. one has not been generated,
   *  mark keyGenerated as false and do nothing extra. In this case, the next
   *  step would be to call generateKeyPair() because attempting to call other
   *  methods will result in a lack of useful functionality without a private
   *  key.
   */
  Storer(Context context)
  {
    this.context = context;
    try
    {
      //try to load private key from disk
      //Open an input stream from ./me/.privKey
      FileInputStream f = new FileInputStream(
        new File(
          context.getDir(
            Storer.DIRECTORY, Context.MODE_PRIVATE),
          Storer.KEYSTORENAME));
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      int nRead;
      byte[] data = new byte[KEYBITS]; //make a guess on how big the key is

      while((nRead = f.read(data, 0, data.length)) != -1)
      {
        buffer.write(data, 0, nRead);
      }
      //decode the private key
      k = (KeyFactory.getInstance(Storer.ALGORITHM)).generatePrivate(new
        PKCS8EncodedKeySpec((buffer.toByteArray())));
      f.close();
      this.keyGenerated = true;
      //initialize Cipher c for decrypt()
      initializeCipher();
    }
    catch(FileNotFoundException e)
    {
      //key has not been generated yet
      this.keyGenerated = false;
    }
    catch(IOException e) {Log.e(Storer.TAG, "IO exception?", e); }
    catch(NoSuchAlgorithmException e) {Log.e(Storer.TAG, "no algorithm?", e); }
    catch(java.security.spec.InvalidKeySpecException e)
    {Log.e(Storer.TAG, "bad key spec", e); }
  }

  /**
   *  initializeCipher() will initialize Cipher c to preform decryption.
   *
   *  The caller of this method must ensure that member varialbe k actually has
   *  a meaning value, i.e. generateKeyPair() has been called before in the
   *  past.
   *
   *  The caller of this method must also ensure that this method is called
   *  prior to any called to decrypt().
   *
   *  @throws IllegalStateException is a private key is not available.
   */
  protected void initializeCipher() throws IllegalStateException
  {
    if(!keyGenerated)
    {
      throw new IllegalStateException("No private key to init cipher with.");
    }
    try
    {
      this.c = Cipher.getInstance(Storer.ENCRYPTION_MODE);
      this.c.init(Cipher.DECRYPT_MODE, k);
    }
    catch(InvalidKeyException e) {Log.e(Storer.TAG, "exception", e); }
    catch(NoSuchAlgorithmException e) {Log.e(Storer.TAG, "exception", e); }
    catch(javax.crypto.NoSuchPaddingException e)
    {Log.e(Storer.TAG, "exception", e); }
  }

  /**
   *  generateKeyPair() will generate the user's public+private key pair for
   *  the first time.
   *
   *  If a key pair currently exists, generateKeyPair() does nothing. If a key
   *  pair has been generated before[and the private key entry still exists],
   *  there is no need to call generateKeyPair(). In other words, the key
   *  resulting from this method call are persistent on disk. The existence of
   *  a key pair can be determined via a call to isKeyAvailable().
   *
   *  This method will also initialize Storer for decrypt() to function.
   *  The appropriate call will be made to Fetcher to persistently store the
   *  public key portion when generated. The Fetcher instance is accessed via
   *  Key.getFetcher(); it is a static instance.
   *
   *  The user's phone number is needed as an argument to associate with the
   *  public key for sharing and lookup. This is needed because there might be
   *  no programmatic way to access the mobile phone's phone number.
   *  The alternative means is to ask the user to enter his or her phone number
   *  and store it on disk.
   *
   *  The given number must be an international number. The Fetcher class does
   *  not know how to format numbers that exclude the area code and or country
   *  code. For example, "521-5554" would be an *invalid* input because it
   *  lacks a country code and an area code. On the other hand, "15555215554"
   *  would be a valid phone number; the delimiters are unnecessary, Fetcher
   *  will canonicalize the number to include familiar delimiters.
   *
   *  @param number String representation of the user's phone number.
   */
  public void generateKeyPair(String number)
  {
    //do nothing if a key pair has already been generated.
    if(keyGenerated)
    {
      return;
    }
    KeyPairGenerator kgen = null;
    try
    {
      kgen = KeyPairGenerator.getInstance(Storer.ALGORITHM);
      kgen.initialize(
        KEYBITS);
    }
    catch(NoSuchAlgorithmException e1) { Log.e(Storer.TAG, "exception", e1);}
    KeyPair keyPair = kgen.generateKeyPair();
    this.k = keyPair.getPrivate();

    //store public key onto disk via Fetcher
    try
    {
      (Key.getFetcher(context)).storeSelfKey(number, keyPair.getPublic());
    }
    catch(KeyAlreadyExistsException e1)
    { Log.e(Storer.TAG, "exception", e1); }

    //store private key onto disk
    try
    {
      FileOutputStream f = new FileOutputStream(
        new File(
          context.getDir(
            Storer.DIRECTORY, Context.MODE_PRIVATE),
          Storer.KEYSTORENAME));
      f.write((new PKCS8EncodedKeySpec(k.getEncoded())).getEncoded());
      f.close();
    }
    catch(IOException e1) {Log.e(Storer.TAG, "exception", e1); }
    //initialize Cipher c for decrypt()
    this.keyGenerated = true;
    initializeCipher();
  }

  /**
   *  isKeyAvailable() returns whether or not a key pair has been generated for
   *  this user. This method indicates whether other Storer methods will have
   *  meaningful behaviour.
   *
   *  Specifically, this checks for the existence of the user's private key in
   *  terms of a file; the public key is not checked. The means by which
   *  isKeyAvailable() does this is by relying on the keyGenerated boolean.
   *
   *  @return whether the user's key pair has been generated or not.
   */
  public boolean isKeyAvailable()
  {
    return this.keyGenerated;
  }

  /**
   *  decrypt() given a ciphertext will decrypt it with the user's private key.
   *  If the given ciphertext cannot be decrypted, null is returned.
   *
   *  Unfortunately RSA is vulnerable to chosen ciphertext(oops), maybe this
   *  shouldn't be a public method but rather package protected. However, the
   *  callers of this method are likely not in this package.
   *
   *  Ensure that generateKeyPair() has been called at any time before(to
   *  initialize k), and that initializeCipher() has been called after
   *  instantiation of a new Storer. Calling initializeCipher() should only
   *  concern the internal methods of Storer.
   *
   *  If no private key has been generated yet, decrypt() automatically returns
   *  null.
   *
   *  @param cipherText byte array containing ciphertext to decrypt.
   *  @return plaintext of given cipherText using the private key.
   *  @return null if decryption is unsuccessful (e.g. bad padding).
   */
  public byte[] decrypt(byte[] cipherText)
  {
    if(!keyGenerated)
    {
      Log.d(Storer.TAG, "Cannot decrypt; private key not generated.");
      return null;
    }
    try
    {
      Log.d(TAG, "len:"+cipherText.length);
      return this.c.doFinal(cipherText);
    }
    catch(javax.crypto.IllegalBlockSizeException e)
    {Log.e(Storer.TAG, "exception", e); }
    catch(javax.crypto.BadPaddingException e)
    {Log.e(Storer.TAG, "Bad padding: Probably a plaintext.", e); }
    catch(NullPointerException e) {Log.e(Storer.TAG, "null ciphertext", e);}
    return null; //could not decrypt
  }
}
