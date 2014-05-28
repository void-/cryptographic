package src.com.key;

import src.com.key.Storer;
import src.com.key.Fetcher;

/**
 *  Key class for keeping static instances of Storer and Fetcher; a singleton.
 *
 *  Design notes:
 *
 *  There does not appear to be much use in keeping separate instances of
 *  both Fetcher and Storer when their purpose focuses on providing an
 *  interface to a single database that will be held in main memory for the
 *  lifetime of the app.
 *
 *  Staging stored keys from disk into main memory is an expensive operation
 *  and only permits itself to occur only once in the lifetime of the app. The
 *  collective size of all keys ought not to exceed a few kibi. The prospected
 *  size does not merit the use of a proper database and it seems that low
 *  latency should be desired over extra main memory useage.
 */
public class Key
{
  /**
   *  Class Variables.
   *
   *  FETCHER static reference to a Fetcher instance.
   *  STORER static reference to a Storer instance.
   */
  static final Fetcher _FETCHER = new Fetcher();
  static final Storer _STORER = new Storer();

  /**
   *  Key() private constructor does not permit any construction and also does
   *  nothing.
   */
  private Key() { }

  /**
   *  getFetcher() returns a static reference to a Fetcher instance.
   *
   *  @return static Fetcher reference.
   */
  public static Fetcher getFetcher()
  {
    return _FETCHER;
  }

  /**
   *  getStorer() returns a static reference to a Storer instance.
   *
   *  @return static Storer reference.
   */
  public static Storer getStorer()
  {
    return _STORER;
  }
}
