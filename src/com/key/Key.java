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
 *  interface to a single database that will be held in main memory for the
 *  lifetime of the app.
 *
 *  Staging stored keys from disk into main memory is an expensive operation
 *  and only permits itself to occur only once in the lifetime of the app. The
 *  collective size of all keys ought not to exceed a few kibi. The prospected
 *  size does not merit the use of a proper database and it seems that low
 *  latency should be desired over extra main memory useage.
 *
 *  TODO: redocument to indicate lazy loading.
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
  static boolean STORER_READY = false;

  /**
   *  Key() private constructor does not permit any construction and also does
   *  nothing.
   */
  private Key() { }

  /**
   *  getFetcher() returns a static reference to a Fetcher instance.
   *
   *  getStorer() is always called to ensure that a Storer instance is always
   *  created before a Fetcher instance.
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
