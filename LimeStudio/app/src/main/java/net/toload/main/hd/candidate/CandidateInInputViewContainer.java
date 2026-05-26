
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
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.toload.main.hd.R;

public class CandidateInInputViewContainer extends LinearLayout implements View.OnClickListener {

    private static final boolean DEBUG = false;
    private static final String TAG = "CandiInputViewContainer";
    Context ctx;
    private ImageButton mRightButton;
    private View mButtonRightExpand;
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
            mButtonRightExpand = findViewById(R.id.candidate_right);
            mRightButton = findViewById(R.id.candidate_right);

            if (mRightButton != null) {
                mRightButton.setOnClickListener(this);
            }
            mCandidateView = findViewById(R.id.candidatesView);

            mCandidateView.setBackgroundColor(mCandidateView.mColorBackground);
            mRightButton.setBackgroundColor(mCandidateView.mColorBackground);
            this.setBackgroundColor(mCandidateView.mColorBackground);

            mImeNameView = findViewById(R.id.candidate_ime_name);
            if (mImeNameView != null) {
                if (net.toload.main.hd.BuildConfig.IS_TABLET) {
                    mImeNameView.setVisibility(VISIBLE);
                    int textColor = mCandidateView.mColorComposingText;
                    if (textColor == 0 || textColor == android.graphics.Color.TRANSPARENT) {
                        textColor = getContext().getResources().getColor(R.color.second_foreground_light);
                    }
                    mImeNameView.setTextColor(textColor);
                } else {
                    mImeNameView.setVisibility(GONE);
                }
            }
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
        if (mCandidateView != null && mImeNameView != null && net.toload.main.hd.BuildConfig.IS_TABLET) {
            int candidateHeight = mCandidateView.getMeasuredHeight();
            if (candidateHeight > 0) {
                android.view.ViewGroup.LayoutParams lp = mImeNameView.getLayoutParams();
                if (lp.height != candidateHeight) {
                    lp.height = candidateHeight;
                    mImeNameView.setLayoutParams(lp);
                }
            }
        }
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

            // Jeremy '24,1,6: Remove expand button entirely as requested by user ("looks
            // bad")
            boolean showExpandButton = false;

            // Jeremy '24,1,6 Remove top-right emoji button as requested (redundant with
            // bottom row)
            boolean showSymbolInputButton = false;

            // Logic below naturally hides it if both are false
            if (mCandidateView.isCandidateExpanded())
                showExpandButton = false; // Ensure it stays hidden even if expanded

            if (mRightButton != null) {
                mRightButton.setImageDrawable(null); // Clear drawable
            }

            if (mButtonRightExpand != null) {
                // Should be GONE since both flags are false
                mButtonRightExpand.setVisibility(GONE);
            }
        }
        super.requestLayout();
    }

    @Override
    public void onClick(View v) {

        if (mCandidateView.isEmpty())
            mCandidateView.startSymbolInput();
        else
            mCandidateView.showCandidatePopup();

    }
}
