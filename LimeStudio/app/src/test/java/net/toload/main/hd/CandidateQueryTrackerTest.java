package net.toload.main.hd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.toload.main.hd.data.Mapping;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * 候選查詢的先後順序:過去的時序 bug 都寫成案例
 * (過期候選、選字鍵搶在結果之前、電話鍵盤自動上字等結果)。純邏輯,不需要 Android。
 */
public class CandidateQueryTrackerTest {

    private CandidateQueryTracker tracker;

    private CandidateQueryTracker.Pending pending(int seq, String code) {
        return new CandidateQueryTracker.Pending(seq, code, Collections.<Mapping>emptyList(), false, "");
    }

    @Before
    public void setup() {
        tracker = new CandidateQueryTracker();
    }

    @Test
    public void initiallyNothingPending() {
        assertTrue(tracker.isLatestApplied());
        assertNull(tracker.latestPending());
    }

    @Test
    public void startQuery_IncrementsAndMarksUnapplied() {
        int first = tracker.startQuery();
        int second = tracker.startQuery();
        assertEquals(first + 1, second);
        assertEquals(second, tracker.getLatestSeq());
        assertFalse(tracker.isLatestApplied());
    }

    @Test
    public void staleResultIsDropped() {
        // 打 d 再打 a:d 的結果比較慢到,不能蓋掉 da 的候選
        int d = tracker.startQuery();
        int da = tracker.startQuery();
        assertFalse(tracker.acceptResult(d, "d", "da"));
        assertFalse(tracker.isLatestApplied());
        assertTrue(tracker.acceptResult(da, "da", "da"));
        assertTrue(tracker.isLatestApplied());
    }

    @Test
    public void resultForChangedComposingIsDropped() {
        // 結果到的時候組字已經被刪掉(序號雖然是最新的,碼不一樣)
        int seq = tracker.startQuery();
        assertFalse(tracker.acceptResult(seq, "da", ""));
        assertFalse(tracker.isLatestApplied());
    }

    @Test
    public void selkeyBeforeResult_UsesLatestPendingOnly() {
        // 選字鍵搶在結果套用之前:只能用最新一次查詢已算好的結果
        int d = tracker.startQuery();
        tracker.publish(pending(d, "d"));
        int da = tracker.startQuery();
        assertNull(tracker.latestPending()); // 排隊中的是舊查詢的
        CandidateQueryTracker.Pending latest = pending(da, "da");
        tracker.publish(latest);
        assertSame(latest, tracker.latestPending());
        tracker.markApplied(latest.seq);
        assertTrue(tracker.isLatestApplied());
    }

    @Test
    public void autoCommitWaitsForThatResult() {
        // 電話鍵盤自動上字:這一碼的結果還沒到,結果到了才上字,而且只上一次
        int seq = tracker.startQuery();
        tracker.requestAutoCommitOnArrival();
        assertFalse(tracker.takeAutoCommit(seq - 1));
        assertTrue(tracker.takeAutoCommit(seq));
        assertFalse(tracker.takeAutoCommit(seq));
    }

    @Test
    public void autoCommitForOlderQueryIsNotFiredByNewer() {
        // 等待上字之後又打了一碼:新查詢的結果不能觸發舊的等待
        int first = tracker.startQuery();
        tracker.requestAutoCommitOnArrival();
        int second = tracker.startQuery();
        assertFalse(tracker.takeAutoCommit(second));
        assertTrue(tracker.takeAutoCommit(first));
    }
}
