package com.ctxt;

import com.share.KeyShare;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;

public class MainActivity extends Activity
{
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
  }

  /**
   *  onShareKey() provides a dedicated button to switch to the KeyShare
   *  activity.
   */
  public void onShareKey(View view)
  {
    startActivity(new Intent(this, KeyShare.class));
  }
}
