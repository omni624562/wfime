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
 *  */

package nan.toload.main.hd.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Simple RecyclerView adapter for displaying a list of strings.
 * Replaces ArrayAdapter<String> used with ListView.
 */
public class SimpleListAdapter extends RecyclerView.Adapter<SimpleListAdapter.ViewHolder> {

    private String[] mItems;
    private final OnItemClickListener mClickListener;

    /**
     * Interface for handling item click events.
     */
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    /**
     * ViewHolder for simple list items.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView textView;

        public ViewHolder(View view) {
            super(view);
            this.textView = view.findViewById(android.R.id.text1);
        }
    }

    /**
     * Creates a new SimpleListAdapter.
     *
     * @param items         Array of string items to display
     * @param clickListener Callback for item click events
     */
    public SimpleListAdapter(String[] items, OnItemClickListener clickListener) {
        this.mItems = items;
        this.mClickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mItems != null && position < mItems.length) {
            holder.textView.setText(mItems[position]);
            // Set text color based on theme for dark mode support
            int textColor = androidx.core.content.ContextCompat.getColor(
                    holder.itemView.getContext(),
                    com.google.android.material.R.color.m3_default_color_primary_text);
            holder.textView.setTextColor(textColor);
        }

        holder.itemView.setOnClickListener(v -> {
            if (mClickListener != null) {
                mClickListener.onItemClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems != null ? mItems.length : 0;
    }

    /**
     * Updates the data set and refreshes the list.
     *
     * @param items New array of items to display
     */
    public void updateItems(String[] items) {
        this.mItems = items;
        notifyDataSetChanged();
    }
}
