/*
 * Free RIL implementation for Samsung Android-based smartphones.
 * Copyright (C) 2012  Sergey Gridasov <grindars@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.freeril.servicemode;
import android.app.DialogFragment;
import android.widget.EditText;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.os.Bundle;
import android.widget.TextView;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.WindowManager;

public class InputDialogFragment extends DialogFragment implements TextView.OnEditorActionListener {
    private EditText mInputText;

    public static InputDialogFragment newInstance() {
        return new InputDialogFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_input_dialog, container);
        mInputText = (EditText) view.findViewById(R.id.txt_input);
        getDialog().setTitle(R.string.input);

        mInputText.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mInputText.setOnEditorActionListener(this);

        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(actionId == EditorInfo.IME_ACTION_DONE) {

            ServiceModeActivity activity = (ServiceModeActivity) getActivity();
            activity.sendString(mInputText.getText().toString());
            dismiss();

            return true;
        }

        return false;
    }
}