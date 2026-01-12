/*
 *
 *  *
 *  **    Copyright 2015, The LimeIME Open Source Project
 *  **
 *  **    Project Url: http://github.com/lime-ime/limeime/
 *  **                 http://android.toload.net/
 *  **
 *  **    This program is free software: you can redistribute it and/or modify
 *  **    it under the terms of the GNU General Public License as published by
 *  **    the Free Software Foundation, either version 3 of the License, or
 *  **    (at your option) any later version.
 *  *
 *  **    This program is distributed in the hope that it will be useful,
 *  **    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  **    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  **    GNU General Public License for more details.
 *  *
 *  **    You should have received a copy of the GNU General Public License
 *  **    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *
 */

package nan.toload.main.hd.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import nan.toload.main.hd.R;
import nan.toload.main.hd.data.Keyboard;
import nan.toload.main.hd.limedb.LimeDB;

public class ManageImKeyboardDialog extends DialogFragment {

    private Activity activity;
    private View view;

    private List<Keyboard> keyboardlist;
    private RecyclerView listSelectKeyboard;

    private LimeDB datasource;
    private String code;
    private ManageImHandler handler;

    public static ManageImKeyboardDialog newInstance() {
        ManageImKeyboardDialog btd = new ManageImKeyboardDialog();
        btd.setCancelable(true);
        return btd;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.setCancelable(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(android.content.DialogInterface dialog,
                    int keyCode, android.view.KeyEvent event) {
                if ((keyCode == android.view.KeyEvent.KEYCODE_BACK)) {
                    // To dismiss the fragment when the back-button is pressed.
                    dismiss();
                    return true;
                }
                // Otherwise, do nothing else
                else
                    return false;
            }
        });
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {

        getDialog().getWindow().setTitle(getResources().getString(R.string.manage_select_keyboard));

        activity = getActivity();
        datasource = new LimeDB(this.activity);

        view = inflater.inflate(R.layout.fragment_dialog_keyboard, container, false);

        listSelectKeyboard = view.findViewById(R.id.listSelectKeyboard);
        listSelectKeyboard.setLayoutManager(new LinearLayoutManager(activity));

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        keyboardlist = datasource.getKeyboard();

        String[] listitems = new String[keyboardlist.size()];
        for (int i = 0; i < keyboardlist.size(); i++) {
            listitems[i] = keyboardlist.get(i).getDesc();
        }

        SimpleListAdapter adapter = new SimpleListAdapter(listitems, position -> {
            Keyboard keyboard = keyboardlist.get(position);
            datasource.setImKeyboard(code, keyboard);
            handler.updateKeyboardButton(keyboard.getCode());
            dismiss();
        });

        listSelectKeyboard.setAdapter(adapter);
    }

    @Override
    public void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
    }

    public void setHandler(ManageImHandler handler, String code) {
        this.code = code;
        this.handler = handler;
    }
}
