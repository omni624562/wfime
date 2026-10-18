package net.toload.main.hd.keyboard;

import java.util.LinkedList;

/**
 * 按著的手指依先後排隊（多指時先放開較早按下的非修飾鍵）。原本是 LIMEKeyboardBaseView 的內部類別，
 * A4-7② 刪除該類別時原樣搬出。
 */
class PointerQueue {
    private LinkedList<PointerTracker> mQueue = new LinkedList<>();

    public void add(PointerTracker tracker) {
        mQueue.add(tracker);
    }

    public int lastIndexOf(PointerTracker tracker) {
        LinkedList<PointerTracker> queue = mQueue;
        for (int index = queue.size() - 1; index >= 0; index--) {
            PointerTracker t = queue.get(index);
            if (t == tracker)
                return index;
        }
        return -1;
    }

    public void releaseAllPointersOlderThan(PointerTracker tracker, long eventTime) {
        LinkedList<PointerTracker> queue = mQueue;
        int oldestPos = 0;
        for (PointerTracker t = queue.get(oldestPos); t != tracker; t = queue.get(oldestPos)) {
            if (t.isModifier()) {
                oldestPos++;
            } else {
                t.onUpEvent(t.getLastX(), t.getLastY(), eventTime);
                t.setAlreadyProcessed();
                queue.remove(oldestPos);
            }
        }
    }

    public void releaseAllPointersExcept(PointerTracker tracker, long eventTime) {
        for (PointerTracker t : mQueue) {
            if (t == tracker)
                continue;
            t.onUpEvent(t.getLastX(), t.getLastY(), eventTime);
            t.setAlreadyProcessed();
        }
        mQueue.clear();
        if (tracker != null)
            mQueue.add(tracker);
    }

    public void remove(PointerTracker tracker) {
        mQueue.remove(tracker);
    }

    public boolean isInSlidingKeyInput() {
        for (final PointerTracker tracker : mQueue) {
            if (tracker.isInSlidingKeyInput())
                return true;
        }
        return false;
    }
}
