package com.key;

import com.key.Storer;
import com.key.Fetcher;

import android.content.Context;

/**
 *  Key class for keeping static instances of Storer and Fetcher; a singleton.
 *
 *  Design notes:
 *
 *  There does not appear to be much use in keeping separate instances of
 *  both Fetcher and Storer when their purpose focuses on providing an
 *  interface to a single, collective set of keys that is specific to this app.
 *
 *  Call getFetcher() and getStorer() passing in a context as a parameter to
 *  get Fetcher and Storer instances respectively.
 *
 *  The instances are lazily loaded upon first need. Due to key generation
 *  order, Key ensures that an instance of Storer is always ready prior to and
 *  instance of Fetcher.
 */
public class Key
{
  /**
   *  Class Variables.
   *
   *  FETCHER static reference to a Fetcher instance.
   *  STORER static reference to a Storer instance.
   *  STORER_READY static boolean indicating whether _STORER is initialized.
   *    This exists to avoid infinite recursion when first calling getFetcher()
   *    because the Storer constructor calls getFetcher() within itself.
   */
  static Fetcher _FETCHER = null;
  static Storer _STORER = null;
  private static boolean STORER_READY = false;

  /**
   *  Key() private constructor does not permit any construction and also does
   *  nothing.
   */
  private Key() { }

  /**
   *  getFetcher() returns a static reference to a Fetcher instance.
   *
   *  It is ensured that a Storer instance is always created before a Fetcher
   *  instance. Sets the STORER_READY boolean to true.
   *
   *  @return static Fetcher reference.
   */
  public static Fetcher getFetcher(Context context)
  {
    if(!STORER_READY)
    {
      STORER_READY = true;
      _STORER = new Storer(context);
    }
    if(_FETCHER == null)
    {
      _FETCHER = new Fetcher(context);
    }
    return _FETCHER;
  }

  /**
   *  getStorer() returns a static reference to a Storer instance.
   *  Sets the STORER_READY boolean to true.
   *
   *  @return static Storer reference.
   */
  public static Storer getStorer(Context context)
  {
    if(_STORER == null)
    {
      STORER_READY = true;
      _STORER = new Storer(context);
    }
    return _STORER;
  }
}
