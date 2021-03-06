package ctxt.ctxt;

import ctxt.ctxt.KeyGenerationDialogFragment;
import ctxt.share.KeyShare;
import ctxt.key.Key;
import ctxt.key.Storer;
import ctxt.key.Fetcher;
import ctxt.key.NumberKeyPair;
import ctxt.key.KeyAlreadyExistsException;

import ctxt.db.Base128;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.util.Log;

public class MainActivity extends Activity implements
  AdapterView.OnItemClickListener
{
  private static String TAG = "MAIN_ACTIVITY:";
  protected String[] numbers;
  private GridView gv;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    gv = (GridView) findViewById(R.id.grid_main);
    //enumerate keys should be safe even if no key was generated yet
    gv.setOnItemClickListener(this);
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
    //set the adapter in onResume() incase any new numbers are added
    this.numbers = Key.getFetcher(getApplicationContext()).enumerateKeys();
    gv.setAdapter(new ArrayAdapter<String>(this,
      android.R.layout.simple_list_item_1, numbers));
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
   *  onItemClick() listens for clicks in the grid view.
   */
  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id)
  {
    enterConversation(this.numbers[position]);
  }

  /**
   *  enterConversation() enter a phone number, enter the conversation thread
   *  with that phone number.
   */
  //public void enterConversation(View view)
  public void enterConversation(String no)
  {
    //String no = (((EditText) findViewById(R.id.number)).getText()).toString();
    Bundle b = new Bundle(1);
    b.putString(ConversationActivity.NUMBER, no);
    Intent i = new Intent(this, ConversationActivity.class);
    i.putExtras(b);
    startActivity(i);
  }
}
