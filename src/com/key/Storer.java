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
 *    byte[] cipherText = --some method that returns ciphertext--
 *    byte[] plainText = s.decrypt(cipherText);
 *    --do something with plainText--
 *
 *  This entails general method usage, but excludes Storer's use in the Key
 *  singleton.
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
   *  CIPHER constant string representing which public key algorithm to use.
   *  KEYSTORENAME constant string representing the file name used for storing
   *    the private key on disk.
   *  TAG constant string representing the tag to use when logging events that
   *    originate from calls to Storer methods.
   */
  public static final int KEYBITS = 1024;
  public static final String CIPHER = "RSA";
  private static final String KEYSTORENAME = ".privKey";
  private static final String TAG = "STORER";

  /**
   *  Member Variables.
   *
   *  PrivateKey the user's private key for decryption. The algorithm used is
   *   represented by Storer.CIPHER.
   */

  private PrivateKey k;
  private Cipher c;

  /**
   *  Storer() constructs a new storer instance.
   *
   *  If no private key currently exists, generate a public+private key pair
   *  and store it. This will only happen once. If a private key already
   *  exists, extract it from the key store.
   */
  Storer(Context context)
  {
    try
    {
      //try to load private key from disk
      FileInputStream f = context.openFileInput(Storer.KEYSTORENAME);
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      int nRead;
      byte[] data = new byte[KEYBITS]; //make a guess on how big it is

      while((nRead = f.read(data, 0, data.length)) != -1)
      {
        buffer.write(data, 0, nRead);
      }
      //decode the private key
      k = (KeyFactory.getInstance(Storer.CIPHER)).generatePrivate(new
        PKCS8EncodedKeySpec((buffer.toByteArray())));
      f.close();
    }
    catch(FileNotFoundException e)
    {
      KeyPairGenerator kgen = null;
      try
      {
        //kgen = KeyPairGenerator.getInstance(Storer.CIPHER);
        kgen = KeyPairGenerator.getInstance(Storer.CIPHER);
        kgen.initialize(
          KEYBITS);
          //new RSAKeyGenParameterSpec(Storer.KEYBITS,
          //RSAKeyGenParameterSpec.F0));
      }
      catch(NoSuchAlgorithmException e1) { Log.e(Storer.TAG, "exception", e1);}
      //catch(InvalidAlgorithmParameterException e1)
      //  { Log.e(Storer.TAG, "Bad algo param", e1);}
      KeyPair keyPair = kgen.generateKeyPair();
      this.k = keyPair.getPrivate();

      try
      {
        (Key.getFetcher(context)).storeSelfKey(keyPair.getPublic());
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
    }
    catch(IOException e) {Log.e(Storer.TAG, "exception", e); }
    catch(NoSuchAlgorithmException e) {Log.e(Storer.TAG, "exception", e); }
    catch(java.security.spec.InvalidKeySpecException e)
    {Log.e(Storer.TAG, "exception", e); }

    try
    {
      //this.c = Cipher.getInstance(Storer.CIPHER);
      //this.c.init(Cipher.DECRYPT_MODE, this.k, (AlgorithmParameterSpec)null);
      //this.c.init(Cipher.DECRYPT_MODE, this.k);
      this.c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      this.c.init(Cipher.DECRYPT_MODE, k);
    }
    catch(InvalidKeyException e) {Log.e(Storer.TAG, "exception", e); }
    //catch(InvalidAlgorithmParameterException e1)
    //  { Log.e(Storer.TAG, "Bad algo param", e1);}
    catch(NoSuchAlgorithmException e) {Log.e(Storer.TAG, "exception", e); }
    catch(javax.crypto.NoSuchPaddingException e)
    {Log.e(Storer.TAG, "exception", e); }
  }

  /**
   *  decrypt() given a ciphertext will decrypt it with the private key.
   *
   *  Unfortunately RSA is vulnerable to chosen ciphertext(oops), maybe this
   *  shouldn't be a public method but rather package protected. However, the
   *  callers of this method are likely not in this package.
   *
   *  @param cipherText byte array containing ciphertext to decrypt.
   *  @return plaintext of given cipherText using the private key.
   */
  public byte[] decrypt(byte[] cipherText)
  {
    Log.d(TAG, "len:"+cipherText.length);
    //do not decrypt if the length is incorrect
    if(cipherText.length > (Storer.KEYBITS>>3))
    {
      Log.d(TAG, "Aborting decryption, too long");
      //return null;
    }
    try
    {
      return this.c.doFinal(cipherText);
    }
    catch(javax.crypto.IllegalBlockSizeException e)
    {Log.e(Storer.TAG, "exception", e); }
    catch(javax.crypto.BadPaddingException e)
    {Log.e(Storer.TAG, "exception", e); }
    return null; //could not decrypt
  }
}
