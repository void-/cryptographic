package ctxt.key;

import ctxt.key.Storer;
import ctxt.key.Fetcher;

import android.content.Context; //Needed for constructing Storer and Fetcher

/**
 *  Key class for keeping static instances of Storer and Fetcher; a singleton.
 *
 *  There does not appear to be much use in keeping separate instances of
 *  both Fetcher and Storer when their purpose focuses on providing an
 *  interface to a single, collective set of keys that is specific to this
 *  application. It is true that a singleton increases the difficulty of
 *  testing its static classes, but the extra memory allocation and the
 *  additional required synchronization is reduced.
 *
 *  Call getFetcher() and getStorer() passing in a Context as a parameter to
 *  get Fetcher and Storer instances respectively.
 *
 *  The instances are lazily loaded upon first need.
 */
public class Key
{
  /**
   *  Class Variables.
   *
   *  FETCHER static reference to a Fetcher instance.
   *  STORER static reference to a Storer instance.
   */
  static Fetcher _FETCHER = null;
  static Storer _STORER = null;

  /**
   *  Key() private constructor does not permit any construction and also does
   *  nothing.
   */
  private Key() { }

  /**
   *  getFetcher() returns a static reference to a Fetcher instance. This
   *  method does nothing to initialize the Fetcher instance.
   *
   *  @param Context that the Fetcher instance will be constructed.
   *  @return static Fetcher reference.
   */
  public static Fetcher getFetcher(Context context)
  {
    if(_FETCHER == null)
    {
      _FETCHER = new Fetcher(context);
    }
    return _FETCHER;
  }

  /**
   *  getStorer() returns a static reference to a Storer instance. This method
   *  does nothing to initialize the Storer instance.
   *
   *  @param Context that the Storer instance will be constructed.
   *  @return static Storer reference.
   */
  public static Storer getStorer(Context context)
  {
    if(_STORER == null)
    {
      _STORER = new Storer(context);
    }
    return _STORER;
  }
}
