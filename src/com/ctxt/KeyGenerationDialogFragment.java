package com.ctxt;

import com.key.Key;

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

/**
 *  KeyGenerationDialogFragment manages dialog creation for key generation.
 */
public class KeyGenerationDialogFragment extends DialogFragment implements
    DialogInterface.OnClickListener
{
  /**
   *  Class Variables.
   *
   *  FRAG_TAG unique tag used to track this fragment.
   */
  static final String FRAG_TAG = "KEYGEN";

  /**
   *  Member Variables.
   *
   *  number String containing user's phone number.
   */
  protected String number;
  protected EditText input;

  /**
   *  Called when creating the dialog.
   *
   */
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState)
  {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    //attempt to extract number
    number = ((TelephonyManager)((getActivity()).getSystemService(
      Context.TELEPHONY_SERVICE))).getLine1Number();

    //create edit text view
    input = new EditText(getActivity());
    input.setHint(R.string.phone_number_hint);
    input.setInputType(InputType.TYPE_CLASS_PHONE);
    input.addTextChangedListener(
      new PhoneNumberFormattingTextWatcher());
    //show edit text if phone number is unavailable
    input.setVisibility((number == null) ? View.VISIBLE : View.GONE);

    builder.setView(input);
    builder.setTitle(R.string.generate_title);
    builder.setPositiveButton(R.string.generate_button, this);

    return builder.create();
  }

  /**
   *  onClick() is called when the DialogFragment's posotive button is clicked.
   *
   *  This preforms key generation.
   */
  public void onClick(DialogInterface dialog, int which)
  {
    if(number == null)
    {
      number = (input.getText()).toString();
    }
    //not a well-formatted number
    if(!PhoneNumberUtils.isGlobalPhoneNumber(number))
    {
      return;
    }

    //preform key generation
    (Key.getStorer(getActivity())).generateKeyPair(
      PhoneNumberUtils.stripSeparators(number));

    //all done
    dismiss();
  }
}
