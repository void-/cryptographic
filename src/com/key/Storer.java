package src.com.key;

import src.com.key.Fetcher;
import android.util.Log;
import android.app.Activity;
import android.content.Context;
import java.security.*;
import javax.crypto.Cipher;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
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
 *  Design notes:
 *    The private key managed by Storer never leaves Storer. This decreases the
 *      size of the TCB.
 *    A KeyStore is used for storing the private key on disk for convenient
 *      serialization. No password or cert chain is used because both of these
 *      seem irrelevant. Android OS file permission security is relied upon to
 *      avoid other applications exfiltrating the private key.
 */
public class Storer extends Activity
{
  /**
   *  Class Variables.
   *
   *  KEYBITS constant int representing the size of keys to use.
   *  CIPHER constant string representing which public key algorithm to use.
   *  KEYSTORENAME constant string representing the file name used for storing
   *    the private key on disk.
   *  PRIVKEYALIAS constant string representing the alias used for the private
   *    key in the keystore. A constant is used because only one private key
   *    needs to be stored.
   */
  public static final int KEYBITS = 1024;
  public static final String CIPHER = "RSA";
  private static final String KEYSTORENAME = ".privKeystore";
  private static final String PRIVKEYALIAS = "self";
  private static final String TAG = "STORER";

  /**
   *  Member Variables.
   *
   *  ks KeyStore for accessing private key.
   */
  private KeyStore ks;

  /**
   *  Storer() constructs a new storer instance.
   *
   *  If no private key currently exists, generate a public+private key pair
   *  and store it. This will only happen once. If a private key already
   *  exists, extract it from the key store.
   *
   *  Use null for password.
   *  Use null for cert chain:certs aren't relevant
   */
  Storer()
  {
    try
    {
      this.ks = KeyStore.getInstance(KeyStore.getDefaultType());
    }
    catch(KeyStoreException e) {Log.e(Storer.TAG, "exception", e);}
    try
    {
      //try to load private key from disk
      FileInputStream f = openFileInput(Storer.KEYSTORENAME);
      ks.load(f, null);
      f.close();
    }
    catch(FileNotFoundException e)
    {
      //generate a new keypair for the user
      KeyPairGenerator kgen = null;
      try
      {
        kgen = KeyPairGenerator.getInstance(Storer.CIPHER);
      }
      catch(NoSuchAlgorithmException e1) { Log.e(Storer.TAG, "exception", e1);}
      kgen.initialize(Storer.KEYBITS);
      KeyPair keyPair = kgen.generateKeyPair();

      //store private key into keystore
      try
      {
        ks.setKeyEntry(Storer.PRIVKEYALIAS, keyPair.getPrivate(), null, null);
      }
      catch(KeyStoreException e1) {Log.e(Storer.TAG, "exception", e1); }
      //store public key using Fetcher
      try
      {
        (new Fetcher()).storeSelfKey(keyPair.getPublic());
      }
      catch(Fetcher.KeyAlreadyExistsException e1)
      { Log.e(Storer.TAG, "exception", e1); }

      //write keystore to disk
      try
      {
        FileOutputStream f = openFileOutput(Storer.KEYSTORENAME,
          Context.MODE_PRIVATE);
        ks.store(f, null);
        f.close();
      }
      catch(FileNotFoundException e1) {Log.e(Storer.TAG, "exception", e1); }
      catch(KeyStoreException e1) {Log.e(Storer.TAG, "exception", e1); }
      catch(IOException e1) {Log.e(Storer.TAG, "exception", e1); }
      catch(NoSuchAlgorithmException e1) {Log.e(Storer.TAG, "exception", e1); }
      catch(java.security.cert.CertificateException e1)
      { Log.e(Storer.TAG, "exception", e1); }
    }
    catch(IOException e) {Log.e(Storer.TAG, "exception", e); }
    catch(NoSuchAlgorithmException e) {Log.e(Storer.TAG, "exception", e); }
    catch(java.security.cert.CertificateException e)
    { Log.e(Storer.TAG, "exception", e); }
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
    Cipher c = null;
    try
    {
      c = Cipher.getInstance(Storer.CIPHER);
      c.init(Cipher.DECRYPT_MODE, (PrivateKey) ks.getKey(Storer.PRIVKEYALIAS,
        null));
      return c.doFinal(cipherText);
    }
    catch(NoSuchAlgorithmException e) {Log.e(Storer.TAG, "exception", e); }
    catch(javax.crypto.NoSuchPaddingException e)
    {Log.e(Storer.TAG, "exception", e); }
    catch(KeyStoreException e) {Log.e(Storer.TAG, "exception", e); }
    catch(UnrecoverableKeyException e) {Log.e(Storer.TAG, "exception", e); }
    catch(InvalidKeyException e) {Log.e(Storer.TAG, "exception", e); }
    catch(javax.crypto.IllegalBlockSizeException e)
    {Log.e(Storer.TAG, "exception", e); }
    catch(javax.crypto.BadPaddingException e)
    {Log.e(Storer.TAG, "exception", e); }
    return null;
  }
}
