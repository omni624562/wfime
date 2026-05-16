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

package net.toload.main.hd.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

import net.toload.main.hd.Lime;
import net.toload.main.hd.R;
import net.toload.main.hd.data.Im;
import net.toload.main.hd.data.Related;
import net.toload.main.hd.data.Word;
import net.toload.main.hd.limedb.LimeDB;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ImportDialog extends DialogFragment {

    LimeDB datasource;
    Activity activity;
    View view;

    Button btnImportCancel;

    // Button btnImportCustom;
    // Button btnImportCj;
    // Button btnImportCj5;
    Button btnImportDayi;
    // Button btnImportEcj;
    Button btnImportPhonetic;
    // Button btnImportScj;

    Button btnImportRelated;

    ImportDialog importdialog;

    String importtext;

    public static ImportDialog newInstance(String importtext) {
        ImportDialog btd = new ImportDialog();
        Bundle args = new Bundle();
        args.putString(Lime.IMPORT_TEXT, importtext);
        btd.setArguments(args);
        btd.setCancelable(true);
        return btd;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
        importtext = getArguments().getString(Lime.IMPORT_TEXT);
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

        getDialog().setOnKeyListener((dialog, keyCode, event) -> {
            if ((keyCode == android.view.KeyEvent.KEYCODE_BACK)) {
                // To dismiss the fragment when the back-button is pressed.
                dismiss();
                return true;
            }
            // Otherwise, do nothing else
            else
                return false;
        });
    }

    public void cancelDialog() {
        this.dismiss();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {

        getDialog().getWindow().setTitle(getResources().getString(R.string.import_dialog_title));
        datasource = new LimeDB(getActivity());
        importdialog = this;

        activity = getActivity();
        view = inflater.inflate(R.layout.fragment_dialog_import, container, false);

        btnImportDayi = view.findViewById(R.id.btnImportDayi);
        btnImportPhonetic = view.findViewById(R.id.btnImportPhonetic);
        btnImportRelated = view.findViewById(R.id.btnImportRelated);
        btnImportCancel = view.findViewById(R.id.btnImportCancel);

        btnImportCancel.setOnClickListener(v -> dismiss());

        HashMap<String, String> check = new HashMap<String, String>();

        List<Im> imlist = datasource.getIm(null, Lime.IM_TYPE_NAME);
        for (int i = 0; i < imlist.size(); i++) {
            check.put(imlist.get(i).getCode(), imlist.get(i).getDesc());
        }

        if (check.get(Lime.DB_TABLE_PHONETIC) == null) {
            btnImportPhonetic.setAlpha(Lime.HALF_ALPHA_VALUE);
            btnImportPhonetic.setTypeface(null, Typeface.ITALIC);
            btnImportPhonetic.setEnabled(false);
        } else {
            btnImportPhonetic.setAlpha(Lime.NORMAL_ALPHA_VALUE);
            btnImportPhonetic.setTypeface(null, Typeface.BOLD);

            btnImportPhonetic.setOnClickListener(v -> confirmimportdialog(Lime.IM_PHONETIC));
        }

        if (check.get(Lime.DB_TABLE_DAYI) == null) {
            btnImportDayi.setAlpha(Lime.HALF_ALPHA_VALUE);
            btnImportDayi.setTypeface(null, Typeface.ITALIC);
            btnImportDayi.setEnabled(false);
        } else {
            btnImportDayi.setAlpha(Lime.NORMAL_ALPHA_VALUE);
            btnImportDayi.setTypeface(null, Typeface.BOLD);

            btnImportDayi.setOnClickListener(v -> confirmimportdialog(Lime.IM_DAYI));
        }

        if (importtext.length() > 1) {
            btnImportRelated.setOnClickListener(v -> confirmimportdialog(Lime.DB_RELATED));
        } else {
            btnImportRelated.setAlpha(Lime.HALF_ALPHA_VALUE);
            btnImportRelated.setTypeface(null, Typeface.ITALIC);
            btnImportRelated.setEnabled(false);
        }

        return view;
    }

    public void confirmimportdialog(final String imtype) {

        final EditText input = new EditText(activity);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);

        if (imtype.equalsIgnoreCase(Lime.DB_RELATED)) {
            builder.setTitle(activity.getResources().getString(R.string.import_dialog_related_title))
                    .setMessage(importtext);
        } else {
            builder.setTitle(activity.getResources().getString(R.string.import_dialog_title))
                    .setMessage(importtext + getResources().getString(R.string.import_code_hint));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);
            builder.setView(input);
        }

        builder.setPositiveButton(activity.getResources().getString(R.string.dialog_confirm),
                (dialog, which) -> {
                    if (imtype.equals(Lime.DB_RELATED)) {
                        importToRelatedTable();
                        dismiss();
                        importdialog.dismiss();
                    } else {
                        if (input.getText() != null && !input.getText().toString().isEmpty()) {
                            importToImTable(imtype, input.getText().toString());
                            dismiss();
                            importdialog.dismiss();
                        } else {
                            Toast.makeText(activity, getResources().getString(R.string.import_code_empty),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(activity.getResources().getString(R.string.dialog_cancel),
                        (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
    }

    private void importToRelatedTable() {

        String pword = importtext.substring(0, 1);
        String cword = importtext.substring(1);

        Related obj = new Related();
        obj.setPword(pword);
        obj.setCword(cword);
        obj.setBasescore(0);
        obj.setUserscore(1);

        datasource.add(Related.getInsertQuery(obj));
        Toast.makeText(activity, getResources().getString(R.string.import_related_success), Toast.LENGTH_SHORT).show();

    }

    private void importToImTable(String imtype, String addcode) {
        Word obj = new Word();
        obj.setCode(addcode);
        obj.setWord(importtext);
        obj.setScore(1);
        obj.setBasescore(0);
        datasource.add(Word.getInsertQuery(imtype, obj));

        Toast.makeText(activity, getResources().getString(R.string.import_word_success), Toast.LENGTH_SHORT).show();

    }

}
