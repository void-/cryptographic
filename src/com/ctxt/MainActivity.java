package com.ctxt;

import com.ctxt.KeyGenerationDialogFragment;
import com.share.KeyShare;
import com.key.Key;
import com.key.Storer;
import com.key.Fetcher;
import com.key.NumberKeyPair;
import com.key.KeyAlreadyExistsException;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.widget.EditText;
import android.util.Log;

public class MainActivity extends Activity
{
  private static String TAG = "MAIN_ACTIVITY:";

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    //test ciphers
    //Key.getFetcher(getApplicationContext());
    //test that decryption is encryption inverse
    //Log.d(TAG, "Hello, World ?= " + new
    //  String((Key.getStorer(getApplicationContext())).decrypt(
    //  Fetcher.encrypt("Hello, World!".getBytes(),
    //  (Key.getFetcher(getApplicationContext())).shareKey().getKey()))));
  }

  /**
   *  onResume() if no key pair has been generate will launch a dialog to
   *  generate a public+private key pair.
   */
  @Override
  public void onResume()
  {
    super.onResume();
    //no key generated: generate a key via a KeyGenerationDialogFragment
    if(!(Key.getStorer(getApplicationContext())).isKeyAvailable())
    {
      (new KeyGenerationDialogFragment()).show(getFragmentManager(), 
        KeyGenerationDialogFragment.FRAG_TAG);
    }
  }

  /**
   *  OnCreateOptionsMenu() is called when creating the action bar and options
   *  menu at the top of the application.
   *
   *  Inflates the action bar and overflow options menu.
   *
   *  @return true.
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.layout.main_menu, menu);
    return true;
  }

  /**
   *  onShareKey() when the proper menu item is clicked, starts an activity for
   *  sharing public keys.
   *
   *  @param item MenuItem that was clicked; this will always be for share.
   */
  public void onShareKey(MenuItem item)
  {
    startActivity(new Intent(this, KeyShare.class));
  }

  /**
   *  enterConversation() enter a phone number, enter the conversation thread
   *  with that phone number.
   */
  public void enterConversation(View view)
  {
    String no = (((EditText) findViewById(R.id.number)).getText()).toString();
    Bundle b = new Bundle(1);
    b.putString(ConversationActivity.NUMBER, no);
    Intent i = new Intent(this, ConversationActivity.class);
    i.putExtras(b);
    startActivity(i);
  }
}
