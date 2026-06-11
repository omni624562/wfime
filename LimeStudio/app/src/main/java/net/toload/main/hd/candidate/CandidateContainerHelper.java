/*
 * Copyright 2026 The WFIME Open Source Project
 *
 * Licensed under the GNU General Public License v3.
 */

package net.toload.main.hd.candidate;

import android.widget.TextView;

import net.toload.main.hd.R;

/**
 * Shared logic for the two candidate containers
 * (CandidateViewContainer / CandidateInInputViewContainer):
 * tablet-only IME-name label setup and height sync.
 */
final class CandidateContainerHelper {

    private CandidateContainerHelper() {
    }

    /** Show/hide the IME-name label and set its text color (tablet only). */
    static void setupImeNameView(TextView imeNameView, CandidateView candidateView) {
        if (imeNameView == null)
            return;
        if (net.toload.main.hd.BuildConfig.IS_TABLET) {
            imeNameView.setVisibility(android.view.View.VISIBLE);
            int textColor = candidateView.mColorComposingText;
            if (textColor == 0 || textColor == android.graphics.Color.TRANSPARENT) {
                textColor = imeNameView.getContext().getResources()
                        .getColor(R.color.second_foreground_light);
            }
            imeNameView.setTextColor(textColor);
        } else {
            imeNameView.setVisibility(android.view.View.GONE);
        }
    }

    /** Keep the IME-name label the same height as the candidate row (tablet only). */
    static void syncImeNameHeight(TextView imeNameView, CandidateView candidateView) {
        if (candidateView == null || imeNameView == null || !net.toload.main.hd.BuildConfig.IS_TABLET)
            return;
        int candidateHeight = candidateView.getMeasuredHeight();
        if (candidateHeight > 0) {
            android.view.ViewGroup.LayoutParams lp = imeNameView.getLayoutParams();
            if (lp.height != candidateHeight) {
                lp.height = candidateHeight;
                imeNameView.setLayoutParams(lp);
            }
        }
    }
}
