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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import nan.toload.main.hd.R;
import nan.toload.main.hd.data.Related;

/**
 * RecyclerView Adapter for displaying Related items.
 * Converted from BaseAdapter to RecyclerView.Adapter for better performance
 * and proper height handling.
 */
public class ManageRelatedAdapter extends RecyclerView.Adapter<ManageRelatedAdapter.ViewHolder> {

    private List<Related> relatedlist;
    private OnItemClickListener onItemClickListener;

    /**
     * Interface for handling item click events.
     */
    public interface OnItemClickListener {
        void onItemClick(Related item, int position);
    }

    public ManageRelatedAdapter(List<Related> relatedlist) {
        this.relatedlist = relatedlist != null ? relatedlist : new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.related, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Related r = relatedlist.get(position);
        if (r != null) {
            String pword = r.getPword();
            String cword = r.getCword();
            String text = pword + cword;
            int freq = r.getBasescore();

            if (text.length() > 12) {
                text = text.substring(0, 10) + "...";
            }

            holder.txtWord.setText(text);
            holder.txtFreq.setText(String.valueOf(freq));

            holder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemClickListener.onItemClick(r, pos);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return relatedlist.size();
    }

    public Related getItem(int position) {
        if (position >= 0 && position < relatedlist.size()) {
            return relatedlist.get(position);
        }
        return null;
    }

    public long getItemId(int position) {
        if (position >= 0 && position < relatedlist.size()) {
            return relatedlist.get(position).getId();
        }
        return -1;
    }

    public List<Related> getList() {
        return relatedlist;
    }

    public void setList(List<Related> list) {
        this.relatedlist = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for Related items.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtWord;
        TextView txtFreq;

        ViewHolder(View itemView) {
            super(itemView);
            txtWord = itemView.findViewById(R.id.txtWord);
            txtFreq = itemView.findViewById(R.id.txtFreq);
        }
    }
}
