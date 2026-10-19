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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

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
    public void onAttach(Context context) {
        super.onAttach(context);
        importtext = getArguments().getString(Lime.IMPORT_TEXT);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {

        getDialog().getWindow().setTitle(getResources().getString(R.string.import_dialog_title));
        // 返回鍵／返回手勢可關閉（預測返回不會送 KEYCODE_BACK 給 OnKeyListener），點對話框外面不關
        getDialog().setCanceledOnTouchOutside(false);
        datasource = new LimeDB(getActivity());
        importdialog = this;

        activity = getActivity();

        HashMap<String, String> check = new HashMap<String, String>();

        List<Im> imlist = datasource.getIm(null, Lime.IM_TYPE_NAME);
        for (int i = 0; i < imlist.size(); i++) {
            check.put(imlist.get(i).getCode(), imlist.get(i).getDesc());
        }

        // 按鈕由 Compose 畫（ImportDialogContent）：沒載入的輸入法、只有一個字時的關聯字庫不能按
        return ImportDialogContent.createView(activity,
                check.get(Lime.DB_TABLE_DAYI) != null,
                check.get(Lime.DB_TABLE_PHONETIC) != null,
                importtext.length() > 1,
                () -> confirmimportdialog(Lime.IM_DAYI),
                () -> confirmimportdialog(Lime.IM_PHONETIC),
                () -> confirmimportdialog(Lime.DB_RELATED),
                this::dismiss);
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

        datasource.insert(Lime.DB_RELATED, Related.getContentValues(obj));
        Toast.makeText(activity, getResources().getString(R.string.import_related_success), Toast.LENGTH_SHORT).show();

    }

    private void importToImTable(String imtype, String addcode) {
        Word obj = new Word();
        obj.setCode(addcode);
        obj.setWord(importtext);
        obj.setScore(1);
        obj.setBasescore(0);
        datasource.insert(imtype, Word.getContentValues(imtype, obj));

        Toast.makeText(activity, getResources().getString(R.string.import_word_success), Toast.LENGTH_SHORT).show();

    }

}
