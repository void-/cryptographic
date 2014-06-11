package com.key;

import com.key.Key;
import com.key.KeyAlreadyExistsException;
import android.util.Log;
import android.app.Activity;
import android.content.Context;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

/**
 *  Storer class handles storage of a single private key and decryption.
 *
 *  Example usage:
 *    Storer s = new Storer();
 *    s.generateKeyPair();
 *    byte[] cipherText = --some method that returns ciphertext--
 *    byte[] plainText = s.decrypt(cipherText);
 *    --do something with plainText--
 *
 *  This entails general method usage, but excludes Storer's use in the Key
 *  singleton.
 *
 *  Storer is not automatically ready until generateKeyPair() has been called
 *  at some point in the past. This method generates a public+private key pair
 *  and stores it on disk.
 *
 *  Design notes:
 *    The private key managed by Storer never leaves Storer. This decreases the
 *      size of the TCB.
 *    The private key is stored in a regular file on disk. The assumption is
 *      made that if the device is compromised, all security is lost regardless
 *      of any form of disk encryption. Due to this assumption, no effort is
 *      made to encrypt or authenticate assets such as private or public keys.
 */
public class Storer
{
  /**
   *  Class Variables.
   *
   *  KEYBITS constant int representing the size of keys to use.
   *  ALGORITHM constant string representing which public key algorithm to use.
   *  KEYSTORENAME constant string representing the file name used for storing
   *    the private key on disk.
   *  TAG constant string representing the tag to use when logging events that
   *    originate from calls to Storer methods.
   */
  public static final int KEYBITS = 1064;
  public static final String ALGORITHM = "RSA";
  static final String ENCRYPTION_MODE = "RSA/ECB/PKCS1Padding";
  private static final String KEYSTORENAME = ".privKey";
  private static final String TAG = "STORER";

  /**
   *  Member Variables.
   *
   *  PrivateKey the user's private key for decryption. The algorithm used is
   *   represented by Storer.ALGORITHM.
   */

  private PrivateKey k;
  private Cipher c;
  private Context context;
  private boolean keyGenerated;

  /**
   *  Storer() constructs a new storer instance.
   *
   *  If no private key currently exists, generate a public+private key pair
   *  and store it. This will only happen once. If a private key already
   *  exists, extract it from the key store.
   */
  Storer(Context context)
  {
    this.context = context;
    try
    {
      //try to load private key from disk
      FileInputStream f = context.openFileInput(Storer.KEYSTORENAME);
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
   *  The caller of this method must ensure that PrivateKey k actually has a
   *  meaning value, i.e. generateKeyPair() has been called before.
   *
   *  The caller of this method must also ensure that this method is called
   *  prior to any called to decrypt().
   */
  protected void initializeCipher()
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
   *  resulting from this method call are persistent. The existence of a key
   *  pair can be determined via a call to isKeyAvailable().
   *
   *  This method will also initialize Storer for decryption to function.
   *  The appropriate call will be made to Fetcher to persistently store the
   *  public key portion.
   *
   *  The user's phone number is needed as an argument to associate with the
   *  public key for sharing and lookup.
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
      FileOutputStream f = context.openFileOutput(Storer.KEYSTORENAME,
        Context.MODE_PRIVATE);
      f.write((new PKCS8EncodedKeySpec(k.getEncoded())).getEncoded());
      f.close();
    }
    catch(IOException e1) {Log.e(Storer.TAG, "exception", e1); }
    //initialize Cipher c for decrypt()
    initializeCipher();
  }

  /**
   *  isKeyAvailable() returns whether or not a key pair has been generated for
   *  this user.
   *
   *  Specifically, this checks for the existence of the user's private key in
   *  terms of a file; the public key is not checked.
   *
   *  @return whether the user's key pair has been generated or not.
   */
  public boolean isKeyAvailable()
  {
    return this.keyGenerated;
  }

  /**
   *  decrypt() given a ciphertext will decrypt it with the private key.
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
    Log.d(TAG, "len:"+cipherText.length);
    try
    {
      return this.c.doFinal(cipherText);
    }
    catch(javax.crypto.IllegalBlockSizeException e)
    {Log.e(Storer.TAG, "exception", e); }
    catch(javax.crypto.BadPaddingException e)
    {Log.e(Storer.TAG, "Bad padding: Probably a plaintext.", e); }
    return null; //could not decrypt
  }
}
