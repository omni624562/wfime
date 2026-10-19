
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

package net.toload.main.hd.candidate;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import net.toload.main.hd.R;

public class CandidateInInputViewContainer extends LinearLayout {

    private static final boolean DEBUG = false;
    private static final String TAG = "CandiInputViewContainer";
    Context ctx;
    private CandidateView mCandidateView;

    public CandidateInInputViewContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (DEBUG)
            Log.i(TAG, "CandidateInInputViewContainer() constructor");

        ctx = context;

    }

    public void initViews() {
        if (DEBUG)
            Log.i(TAG, "initViews()");
        if (mCandidateView == null) {
            mCandidateView = findViewById(R.id.candidatesView);

            mCandidateView.setBackgroundColor(mCandidateView.mColorBackground);
            this.setBackgroundColor(mCandidateView.mColorBackground);
        }
    }

    /** 平板候選列左側的輸入法簡稱（由 CandidateView 自己畫，手機不顯示）。 */
    public void setImeName(String name) {
        if (mCandidateView != null) {
            mCandidateView.setImeName(name);
        }
    }
}
