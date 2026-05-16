/*
 * Copyright 2026, The LimeIME Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.toload.main.hd.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import net.toload.main.hd.R;
import net.toload.main.hd.data.Word;

/**
 * RecyclerView Adapter for ManageImFragment, replacing ManageImAdapter.
 */
public class ManageImRecyclerAdapter extends RecyclerView.Adapter<ManageImRecyclerAdapter.ViewHolder> {

    private final Context context;
    private List<Word> wordlist;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Word word);
    }

    public ManageImRecyclerAdapter(Context context, List<Word> wordlist, OnItemClickListener listener) {
        this.context = context;
        this.wordlist = wordlist;
        this.listener = listener;
    }

    public void setList(List<Word> list) {
        this.wordlist = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.word, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Word w = wordlist.get(position);
        if (w != null) {
            String wordtext = w.getWord();
            if (wordtext.length() > 12) {
                wordtext = wordtext.substring(0, 10) + "...";
            }
            holder.txtCode.setText(w.getCode());
            holder.txtWord.setText(wordtext);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(w);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return wordlist != null ? wordlist.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtWord;
        TextView txtCode;

        ViewHolder(View itemView) {
            super(itemView);
            txtWord = itemView.findViewById(R.id.txtWord);
            txtCode = itemView.findViewById(R.id.txtCode);
        }
    }
}
