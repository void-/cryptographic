package com.share;

import com.key.Key;
import com.ctxt.R;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.AlertDialog;

import android.util.Log;

import android.text.InputType;

import android.telephony.TelephonyManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneNumberFormattingTextWatcher;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.BitmapFactory;

/**
 *  ShareConfirmationDialogFragment manages dialog creation for key generation.
 *
 *  TODO: put an image in here representing the user's key and the new key.
 *    make a blob out of both keys and use it as an RGB image
 */
public class ShareConfirmationDialogFragment extends DialogFragment
{
  /**
   *  Class Variables.
   *
   *  FRAG_TAG unique tag used to track this fragment.
   *  TAG constant string representing the tag to use when logging events that
   *    originate from calls to this class's methods.
   *  PHONE_NUMBER static identifier to identify the phone number argument
   *    passed when creating a ShareConfirmationDialogFragment.
   *  IMAGE static identifier to identify an image bytearray passed via bundle.
   */
  static final String FRAG_TAG = "com.share.confirmFragment";
  static final String TAG = "SHARE_CONFIRM";
  static String PHONE_NUMBER = "com.share.phoneNumber";
  static String IMAGE = "com.share.image";

  /**
   *  Member Variables.
   *
   *  number String representing the phone number in question. This will be
   *    displayed in the created dialog.
   *  buttonListener ShareConfirmationListener to callback when a button is
   *    pressed.
   *  keyIcon Drawable object to display as the dialog's icon.
   */
  private String number;
  private ShareConfirmationListener buttonListener;
  private Drawable keyIcon;

  /**
   *  onCreate() is called when instantiating a new
   *  ShareConfirmationDialogFragment.
   *
   *  Prior to calling this method, the caller needs to set the arguments to
   *  contain a String representing the phone number to display in dialogs
   *  created by this fragment. The phone number needs to be associated with
   *  the key, ShareConfirmationDialogFragment.PHONE_NUMBER.
   *
   *  set arguments for IMAGE too.
   *
   *  To authenticate the connection, a byte array representing the server and
   *  client's serialized NumberKeyPairs is needed. This byte array is
   *  converted into a bitmap which is displayed to the user.
   *
   *  Both users need to compare the icons on each of their devices to ensure
   *  that they did indeed communicate with each other.
   *
   *  @param savedInstanceState Bundle with any data from a previous instance.
   */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    Bundle args = getArguments();
    this.number = args.getString(ShareConfirmationDialogFragment.PHONE_NUMBER);
    byte[] img = args.getByteArray(ShareConfirmationDialogFragment.IMAGE);
    //turn img into a useable bitmap
    keyIcon = new BitmapDrawable(getResources(),
      BitmapFactory.decodeByteArray(img, 0, img.length));
  }

  /**
   *  onCreateDialog() is called when creating a dialog from this fragment.
   *
   *  @param savedInstanceState Bundle with any data from a previous instance.
   */
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState)
  {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

    builder.setTitle(R.string.confirm_title);
    builder.setMessage(number);
    builder.setIcon(keyIcon);
    builder.setPositiveButton(R.string.button_number_accept,
      new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
          Log.d(TAG, "posotive button:" + which);
          buttonListener.receiveDialogResult(true);
        }
      });
    builder.setNegativeButton(R.string.button_number_deny,
      new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
          Log.d(TAG, "negative button:" + which);
          buttonListener.receiveDialogResult(false);
        }
      });

    return builder.create();
  }

  /**
   *  onAttach() given a calling activity will save an instance of that
   *  activity to callback later.
   *  The given Activity must implement the interface
   *  ShareConfirmationDialogFragment.ShareConfirmationListener.
   *  The Activity's setDialogResult() method will be called when a button is
   *  clicked in this dialog.
   *
   *  @param activity Activity to register for calling back.
   */
  @Override
  public void onAttach(Activity activity)
  {
    super.onAttach(activity);
    this.buttonListener = (ShareConfirmationListener) activity;
  }

  /**
   *  ShareConfirmationListener interface allows for
   *  ShareConfirmationDialogFragment to communicate the results on the dialog
   *  back to the calling Activity.
   */
  interface ShareConfirmationListener
  {
    /**
     *  receiveDialogResult() is called whenever a button is pressed in the
     *  dialog.
     *
     *  @param confirmed boolean indicating if the posotive button was pressed.
     */
    public void receiveDialogResult(boolean confirmed);
  }
}
