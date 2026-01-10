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

package nan.toload.main.hd;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView adapter for the navigation drawer menu items.
 * Replaces the legacy ArrayAdapter used with ListView.
 */
public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.ViewHolder> {

    private final String[] mMenuItems;
    private final OnItemClickListener mClickListener;
    private int mSelectedPosition = 0;

    /**
     * Interface for handling item click events.
     */
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    /**
     * ViewHolder for navigation drawer menu items.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView textView;
        public final View itemView;

        public ViewHolder(View view) {
            super(view);
            this.itemView = view;
            this.textView = view.findViewById(android.R.id.text1);
        }
    }

    /**
     * Creates a new NavigationDrawerAdapter.
     *
     * @param menuItems     Array of menu item titles
     * @param clickListener Callback for item click events
     */
    public NavigationDrawerAdapter(String[] menuItems, OnItemClickListener clickListener) {
        this.mMenuItems = menuItems;
        this.mClickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_activated_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(mMenuItems[position]);
        holder.itemView.setActivated(position == mSelectedPosition);

        holder.itemView.setOnClickListener(v -> {
            if (mClickListener != null) {
                mClickListener.onItemClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mMenuItems != null ? mMenuItems.length : 0;
    }

    /**
     * Sets the currently selected position and updates the UI.
     *
     * @param position The position to select
     */
    public void setSelectedPosition(int position) {
        int oldPosition = mSelectedPosition;
        mSelectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(position);
    }

    /**
     * Gets the currently selected position.
     *
     * @return The selected position
     */
    public int getSelectedPosition() {
        return mSelectedPosition;
    }
}
