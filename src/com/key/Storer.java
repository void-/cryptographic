package key;

import android.content.Context;
import java.security.*;

/**
 *  Storer class handles storage and access of a single private key.
 *
 *  (prospective) General useage steps:
 *    Initialize a Storer ; it will get out the private key
 *    Ask the Storer for the private key OR ask the Storer to decrypt something
 *
 *  Design thoughts:
 *    It is probably preferable if the private key never left the Storer:
 *    decrease the size of the TCB
 *    This implies decryption must be bundled up with the Storer-bad for
 *    abstraction/refactoring for different algorithms: but this is later
 *
 *    TODO: determine the object types to be used-replace all CAPs
 *
 */
public class Storer
{
  /**
   *  Class Variables.
   *
   *  KEYBITS constant int representing the size of keys to use.
   *  CIPHER constant string representing which public key algorithm to use.
   */
  public static final int KEYBITS = 1024;
  public static final String CIPHER = "RSA";
  private static final String KEYSTORENAME = ".privKeystore";

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
   *
   */
  Storer()
  {
    this.ks = KeyStore.getInstance(KeyStore.getDefaultType());
    try
    {
      //try to load private key from disk
      FileInputStream f = openFileInput(Storer.KEYSTORENAME);
      ks.load(f);
      f.close();
    }
    catch(FileNotFoundException e)
    {
      //generate a new keypair for the user
      KeyPairGenerator kgen = new KeyPairGenerator.getInstance(Storer.CIPHER);
      kgen.initialize(Storer.KEYBITS);
      KeyPair keyPair = kpg.generateKeyPair();

      //store private key into keystore
      ks.setKeyEntry(USERNUMBER, keyPair.getPrivate(), null, null);
      //TODO:Go store KeyPair.public for my own number with fetcher
        //what is my own number?

      //write keystore to disk
      FileOutputStream f = openFileOutput(Storer.KEYSTORENAME,
        Context.MODE_PRIVATE);
      ks.store(f, null);
      f.close();
    }
  }

  /**
   *  decrypt() given a ciphertext will decrypt it with the private key.
   *
   *  Unfortunately RSA is vulnerable to chosen ciphertext(oops), maybe this
   *  shouldn't be a public method but rather package protected.
   */
  public PLAINTEXT decrypt(CIPHERTEXT c)
  {
  }
}
