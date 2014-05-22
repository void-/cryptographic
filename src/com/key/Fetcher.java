package key;

/**
 *  Fetcher class given someone's contact number, will return their public key.
 *
 *  (prospective) General useage steps:
 *    Initialize a Fetcher; it will open up the database or whatever it needs
 *    Ask the Fetcher to enumerate all contacts (people who's pub keys it has)
 *    Ask the Fetcher for someone's public key; raise exception if WTF
 *    Ask someone(?) to encrypt a message under the public key
 *    ...other things that dont concern the Fetcher
 *
 *    or
 *    Init a Fetcher
 *    Inform the Fetcher to store a new public key(why's this a fetcher then?)
 *    Ask the Fetcher for own public key(combine with regular lookup?)
 *
 *    TODO: determine the object types to be used-replace all CAPs
 *
 */
public class Fetcher
{
  public Fetcher()
  {
    //setup connection to database/ unpickle things or whatever
  }

  public PUBKEY[] enumerateKeys()
  {
  }

  public PUBKEY fetchKey(CONTACT number) throws WTFEXCEPTION
  {
  }

  public PUBKEY shareKey() throws NOKEYYETEXCEPTION
  {
  }

  public void newKey(CONTACT number, PUBKEY key) throws ALREADYINTHEREEXCEPTION
  {
  }
}
