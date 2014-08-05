package com.share;

import com.ctxt.R;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import android.util.Log;

/**
 *  Discover bluetooth devices, ask the user to select one, return the device
 *  back to the calling activity.
 */
public class SelectDeviceActivity extends Activity
{
  /**
   *  Class Variables.
   */
  private static final String TAG = "SelectDeviceActivity";
  static final String EXTRA_MAC = "extra_device";

  /**
   *  Member Variables.
   *
   *  deviceNames ArrayAdapter to display all the discovered devices.
   */
  private ArrayAdapter<String> deviceNamesAdapter;
  private BluetoothAdapter bluetoothAdapter;

  /**
   *  onCreate() is called when instantiating a new SelectDeviceActivity
   */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.select_device);

    setResult(Activity.RESULT_CANCELED);

    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    //show the device mac address as a title
    ((TextView) findViewById(R.id.device_mac)).setText(
      bluetoothAdapter.getAddress());

    //register a receiver for discovering devices
    registerReceiver(discoveredReceiver,
      new IntentFilter(BluetoothDevice.ACTION_FOUND));

    deviceNamesAdapter = new ArrayAdapter<String>(this,
      android.R.layout.simple_list_item_1);
    //setup the list view for displaying devices
    ListView l = (ListView) findViewById(R.id.device_list);
    l.setAdapter(deviceNamesAdapter);
    l.setOnItemClickListener(deviceClickListener);
  }

  /**
   *  onPause() stops discovering bluetooth devices.
   *
   *  Its most likely that if this activity goes into onPause(), discovery
   *  should stop.
   */
  @Override
  public void onPause()
  {
    super.onPause();
    stopDiscovery();
  }

  /**
   *  onDestroy()
   *
   *  stop discovering devices.
   */
  @Override
  public void onDestroy()
  {
    super.onDestroy();

    unregisterReceiver(discoveredReceiver);
    stopDiscovery();
  }

  /**
   *  discoveredReceiver adds newly discovered bluetooth devices to the array
   *  adapter.
   */
  private BroadcastReceiver discoveredReceiver = new BroadcastReceiver()
  {
    /**
     *  This should already be filtered for discovering a new bluetooth device.
     */
    @Override
    public void onReceive(Context context, Intent intent)
    {
      BluetoothDevice device =
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
      deviceNamesAdapter.add(device.getName() + "-" + device.getAddress());
      Log.d(TAG, "found an new device:" + device.getName());
    }
  };

  /**
   *  deviceClickListener responds to clicks on devices listed in
   *  deviceNamesAdapter.
   *  
   *  Give the bluetooth device back to the calling activity.
   */
  private OnItemClickListener deviceClickListener = new OnItemClickListener()
  {

    /**
     *  onItemClick() callback when a bluetooth device is clicked.
     *
     *  @param a the AdapterView where the click happened.
     *  @param v the View that was clicked.
     *  @param position index of v in a.
     *  @param id the row id of v.
     */
    @Override
    public void onItemClick(AdapterView<?> a, View v, int position, long id)
    {
      String nameAndMac = (((TextView) v).getText()).toString();
      setResult(
        Activity.RESULT_OK,
        (new Intent()).putExtra(
          SelectDeviceActivity.EXTRA_MAC,
          nameAndMac.substring(nameAndMac.lastIndexOf('-')+1)));

      stopDiscovery();
      //quit the activity because a device was selected
      finish();
    }
  };

  /**
   *  startDiscovery() makes the device look for other bluetooth devices.
   */
  private void startDiscovery()
  {
    Log.d(TAG, "starting discovery");
    bluetoothAdapter.startDiscovery();
  }

  /**
   *  stopDiscovery() stops looking for bluetooth devices.
   */
  private void stopDiscovery()
  {
    Log.d(TAG, "stopping discovery");
    bluetoothAdapter.cancelDiscovery();
  }
}
