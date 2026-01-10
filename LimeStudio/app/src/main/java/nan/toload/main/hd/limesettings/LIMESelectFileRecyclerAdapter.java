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

package nan.toload.main.hd.limesettings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import nan.toload.main.hd.R;

/**
 * RecyclerView adapter for file selection.
 * Replaces LIMESelectFileAdapter (BaseAdapter) for use with RecyclerView.
 */
public class LIMESelectFileRecyclerAdapter extends RecyclerView.Adapter<LIMESelectFileRecyclerAdapter.ViewHolder> {

    private List<File> list;
    private final LayoutInflater mInflater;
    private final OnItemClickListener mClickListener;

    /**
     * Interface for handling item click events.
     */
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    /**
     * ViewHolder for file items.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView image;
        public final TextView filename;

        public ViewHolder(View view) {
            super(view);
            this.image = view.findViewById(R.id.img_function_icon);
            this.filename = view.findViewById(R.id.txt_function_name);
        }
    }

    /**
     * Creates a new LIMESelectFileRecyclerAdapter.
     *
     * @param context       The context
     * @param ls            List of files to display
     * @param clickListener Callback for item click events
     */
    public LIMESelectFileRecyclerAdapter(Context context, List<File> ls, OnItemClickListener clickListener) {
        this.list = ls;
        this.mInflater = LayoutInflater.from(context);
        this.mClickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.imgstring, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = list.get(position);

        if (file.isDirectory()) {
            holder.image.setImageResource(R.drawable.folder);
        } else {
            holder.image.setImageResource(R.drawable.scolling_holder);
        }
        holder.filename.setText(file.getName());

        holder.itemView.setOnClickListener(v -> {
            if (mClickListener != null) {
                mClickListener.onItemClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    /**
     * Updates the data set and refreshes the list.
     *
     * @param newList New list of files to display
     */
    public void updateItems(List<File> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }
}
