package key;
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
  Storer()
  {
    //setup a connection to the database/ pull out key from keystore
  }

  public PLAINTEXT decrypt(CIPHERTEXT c)
  {
  }
}
