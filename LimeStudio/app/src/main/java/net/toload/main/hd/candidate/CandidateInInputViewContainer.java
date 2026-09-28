
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.toload.main.hd.R;

public class CandidateInInputViewContainer extends LinearLayout {

    private static final boolean DEBUG = false;
    private static final String TAG = "CandiInputViewContainer";
    Context ctx;
    private ImageButton mRightButton;
    private CandidateView mCandidateView;

    public CandidateInInputViewContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (DEBUG)
            Log.i(TAG, "CandidateInInputViewContainer() constructor");

        ctx = context;

    }

    private TextView mImeNameView;

    public void initViews() {
        if (DEBUG)
            Log.i(TAG, "initViews()");
        if (mCandidateView == null) {
            mRightButton = findViewById(R.id.candidate_right);
            mCandidateView = findViewById(R.id.candidatesView);

            mCandidateView.setBackgroundColor(mCandidateView.mColorBackground);
            mRightButton.setBackgroundColor(mCandidateView.mColorBackground);
            this.setBackgroundColor(mCandidateView.mColorBackground);

            mImeNameView = findViewById(R.id.candidate_ime_name);
            CandidateContainerHelper.setupImeNameView(mImeNameView, mCandidateView);
        }
    }

    public void setImeName(String name) {
        if (mImeNameView != null) {
            mImeNameView.setText(name);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        CandidateContainerHelper.syncImeNameHeight(mImeNameView, mCandidateView);
    }

    @Override
    public void requestLayout() {
        if (DEBUG)
            Log.i(TAG, "requestLayout()");

        if (mCandidateView != null) {
            int availableWidth = mCandidateView.getWidth();
            int neededWidth = mCandidateView.computeHorizontalScrollRange();

            if (DEBUG)
                if (DEBUG)
                    Log.i(TAG, "requestLayout() availableWidth:" + availableWidth + " neededWidth:" + neededWidth);

            // Jeremy '24,1,6: Remove expand button and top-right emoji button entirely as requested by user
            // (右側鈕一律隱藏)
            if (mRightButton != null) {
                mRightButton.setImageDrawable(null); // Clear drawable
            }

            if (mRightButton != null) {
                mRightButton.setVisibility(GONE);
            }
        }
        super.requestLayout();
    }
}
