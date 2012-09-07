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
import android.app.Activity;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.RemoteException;
import org.freeril.i9100oemservice.IPhoneService;
import org.freeril.i9100oemservice.IEventHandler;
import java.util.List;
import android.widget.ListView;
import android.widget.AdapterView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.util.Log;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.os.Handler;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.app.DialogFragment;

public class ServiceModeActivity extends Activity {
    private IPhoneService mPhoneService = null;
    private boolean mInServiceMode = false;
    private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private final Handler mHandler = new Handler();
    static final private Pattern mKeyPattern = Pattern.compile("^ *\\[(.)\\] ");
    static final private Pattern mInputPattern = Pattern.compile("^ *Input ");
    static final private int DIALOG_SERVICE_UNAVAILABLE = 1;
    static final private int DIALOG_INPUT = 2;

    private final IEventHandler mEventHandler = new IEventHandler.Stub() {

        public void handleServiceCompleted() {
            finish();
        }

        public void handleServiceDisplay(final List<String> lines) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.clear();
                    mAdapter.addAll(lines);
                }
            });
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        setContentView(R.layout.main);
        mListView = (ListView) findViewById(R.id.displayList);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);

                Matcher keyMatcher = mKeyPattern.matcher(item);
                Matcher inputMatcher = mInputPattern.matcher(item);

                if(keyMatcher.find()) {
                    char key = keyMatcher.group(1).charAt(0);
                    boolean successful;

                    try {
                        successful = mPhoneService.sendServiceKeyCode(key);
                    } catch(RemoteException e) {
                        successful = false;
                    }

                    if(!successful) {
                        finish();
                    }
                } else if(inputMatcher.find()) {
                    DialogFragment frag = InputDialogFragment.newInstance();
                    frag.show(getFragmentManager(), "inputDialog");
                } else {
                    Log.d("ServiceModeActivity", String.format("Clicked '%s'", item));
                }
            }
        });
        mAdapter = new ArrayAdapter<String>(this, R.layout.list_item);
        mListView.setAdapter(mAdapter);

        mPhoneService = IPhoneService.Stub.asInterface(ServiceManager.getService("org.freeril.i9100oemservice.PhoneService"));
        if(mPhoneService == null) {
            DialogFragment frag = ServiceUnavailableDialogFragment.newInstance();
            frag.show(getFragmentManager(), "serviceUnavailableDialog");
        } else {
            try {
                mPhoneService.registerEventHandler(mEventHandler);

                mInServiceMode = mPhoneService.enterServiceMode(ServiceModeCommands.SM_TYPE_TEST_MANUAL,
                                                                ServiceModeCommands.SM_TYPE_SUB_ENTER);


                if(!mInServiceMode) {
                    mPhoneService.unregisterEventHandler(mEventHandler);
                    mPhoneService = null;

                    DialogFragment frag = ServiceUnavailableDialogFragment.newInstance();
                    frag.show(getFragmentManager(), "serviceUnavailableDialog");
                }

            } catch(RemoteException e) {
                DialogFragment frag = ServiceUnavailableDialogFragment.newInstance();
                frag.show(getFragmentManager(), "serviceUnavailableDialog");

                mPhoneService = null;
            }
        }
    }

    @Override
    protected void onStop() {
        if(mInServiceMode) {
            try {
                mPhoneService.exitServiceMode(ServiceModeCommands.SM_TYPE_TEST_MANUAL);
                mPhoneService.unregisterEventHandler(mEventHandler);
            } catch(RemoteException e) {

            }

            mInServiceMode = false;
            mPhoneService = null;
        }

        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if(mInServiceMode) {
            boolean successful;

            try {
                successful = mPhoneService.sendServiceKeyCode('\\');
            } catch(RemoteException e) {
                successful = false;
            }

            if(!successful)
                finish();
        } else {
            finish();
        }
    }

    public void sendString(String input) {
        boolean successful = false;

        try {
            for(char chr: input.toCharArray()) {
                successful = mPhoneService.sendServiceKeyCode(chr);

                if(!successful)
                    break;
            }

            if(successful)
                successful = mPhoneService.sendServiceKeyCode('S');

        } catch(RemoteException e) {
            successful = false;
        }

        if(!successful)
            finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_input:
            DialogFragment frag = InputDialogFragment.newInstance();
            frag.show(getFragmentManager(), "inputDialog");

            break;

        case R.id.menu_quit:
            finish();
            break;
        }

        return true;
    }
}
