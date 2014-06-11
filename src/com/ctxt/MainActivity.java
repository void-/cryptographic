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
   *  onShareKey() provides a dedicated button to switch to the KeyShare
   *  activity.
   */
  public void onShareKey(View view)
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
