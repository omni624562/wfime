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

package net.toload.main.hd;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import net.toload.main.hd.R;
import net.toload.main.hd.candidate.CandidateInInputViewContainer;
import net.toload.main.hd.candidate.CandidateView;
import net.toload.main.hd.candidate.CandidateViewContainer;
import net.toload.main.hd.candidate.ComposingTextPopup;
import net.toload.main.hd.data.ChineseSymbol;
import net.toload.main.hd.data.Mapping;
import net.toload.main.hd.global.LIMEPreferenceManager;
import net.toload.main.hd.global.LIMEUtilities;
import net.toload.main.hd.global.RootMapper;
import net.toload.main.hd.keyboard.LIMEBaseKeyboard;
import net.toload.main.hd.keyboard.LIMEKeyboard;
import net.toload.main.hd.keyboard.LIMEKeyboardBaseView;
import net.toload.main.hd.keyboard.LIMEKeyboardView;
import net.toload.main.hd.keyboard.LIMEMetaKeyKeyListener;


import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LIMEService extends InputMethodService implements
        LIMEKeyboardBaseView.OnKeyboardActionListener {

    static final int KEYCODE_SWITCH_TO_SYMBOL_MODE = -2;
    static final int KEYCODE_SWITCH_TO_ENGLISH_MODE = -9;
    static final int KEYCODE_SWITCH_TO_IM_MODE = -10;
    static final int KEYCODE_SWITCH_SYMBOL_KEYBOARD = -15;
    // Replace Keycode.KEYCODE_CTRL_LEFT/RIGHT, ESC on android 3.x
    // for backward compatibility of 2.x
    static final int MY_KEYCODE_ESC = 111;
    static final int MY_KEYCODE_CTRL_LEFT = 113;
    static final int MY_KEYCODE_CTRL_RIGHT = 114;
    static final int MY_KEYCODE_ENTER = 10;
    static final int MY_KEYCODE_SPACE = 32;
    static final int MY_KEYCODE_SWITCH_CHARSET = 95;
    static final int MY_KEYCODE_WINDOWS_START = 117; // Jeremy '12,4,29 windows start key
    static final boolean DEBUG = true;
    static final String TAG = "LIMEService";
    private static final String CHANNEL_ID = "lime_ime_service";
    private static final int FOREGROUND_NOTIFICATION_ID = 1001;
    // Jeremy '16,7,22 To control delayed hiding candidate view and avoid hide and
    // show candidate view in short time.
    private static final int DELAY_BEFORE_HIDE_CANDIDATE_VIEW = 200;
    private static final long SHIFT_LOCK_TIMEOUT = 500; // Jeremy '24,1,7: Double-tap timeout for caps lock
    private static final int POS_SETTINGS = 0;
    private static final int POS_HANCONVERT = 1; // Jeremy '11,9,17
    private static final int POS_KEYBOARD = 2;
    private static final int POS_METHOD = 3;
    private static final int POS_SPLIT_KEYBOARD = 4;
    private static final KeyboardTheme[] KEYBOARD_THEMES = {
            new KeyboardTheme("Light", 0, R.style.LIMETheme_Light),
            new KeyboardTheme("Dark", 1, R.style.LIMETheme_Dark),
            new KeyboardTheme("Pink", 2, R.style.LIMETheme_Pink),
            new KeyboardTheme("TechBlue", 3, R.style.LIMETheme_TechBlue),
            new KeyboardTheme("FashionPurple", 4, R.style.LIMETheme_FashionPurple),
            new KeyboardTheme("RelaxGreen", 5, R.style.LIMETheme_RelaxGreen),
            new KeyboardTheme("Material3", 6, R.style.LIMETheme_Material3),
    };
    private java.util.concurrent.ExecutorService queryExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private volatile java.util.concurrent.Future<?> queryFuture;
    final android.os.Handler mMainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    final CandidateViewHandler mCandidateViewHandler = new CandidateViewHandler(this);
    public boolean hasMappingList = false;
    public String activeIM; // Jeremy '12,4,30 renamed from keyboardSelection
    LIMEKeyboardSwitcher mKeyboardSwitcher;
    // Keep keydown event
    KeyEvent mKeydownEvent = null;
    LIMEKeyboardView mInputView = null;

    // 即時翻譯模式狀態 (用於 Phase 2)
    public boolean isTranslationModeActive = false;
    public StringBuilder translateQuery = new StringBuilder();
    public String translatedResult = "";

    // Compose 可以無縫訂閱與反應式渲染的 Live 狀態
    public static final androidx.compose.runtime.MutableState<Boolean> isTranslationModeState =
            androidx.compose.runtime.SnapshotStateKt.mutableStateOf(false, androidx.compose.runtime.SnapshotStateKt.structuralEqualityPolicy());
    public static final androidx.compose.runtime.MutableState<String> translateQueryState =
            androidx.compose.runtime.SnapshotStateKt.mutableStateOf("", androidx.compose.runtime.SnapshotStateKt.structuralEqualityPolicy());
    public static final androidx.compose.runtime.MutableState<String> translatedResultState =
            androidx.compose.runtime.SnapshotStateKt.mutableStateOf("", androidx.compose.runtime.SnapshotStateKt.structuralEqualityPolicy());
    
    public int translateCursorPosition = 0;
    public static final androidx.compose.runtime.MutableState<Integer> translateCursorPositionState =
            androidx.compose.runtime.SnapshotStateKt.mutableStateOf(0, androidx.compose.runtime.SnapshotStateKt.structuralEqualityPolicy());

    private CandidateInInputViewContainer mCandidateInInputView = null;// Jeremy'12,5,3
    boolean mFixedCandidateViewOn; // Jeremy'12,5,3
    CandidateView mCandidateView = null;
    CandidateView mCandidateViewInInputView = null;
    CandidateView mCandidateViewStandAlone = null;
    private CandidateViewContainer mCandidateViewContainer = null;
    private ComposingTextPopup mComposingPopup = null;
    CompletionInfo[] mCompletions;
    StringBuilder mComposing = new StringBuilder();
    private EditorInfo mLastInitializedEditorInfo = null;
    boolean mPredictionOn;
    boolean mCompletionOn;
    private boolean mCapsLock;
    private boolean mAutoCap;

    // private String mWordSeparators;
    // private String misMatched; //Removed by Jeremy '13,1,10
    private boolean mHasShift;
    boolean mEnglishOnly;
    private boolean mEnglishFlagShift;
    private boolean mPersistentLanguageMode; // Jeremy '12,5,1
    private int mShowArrowKeys; // Jeremy '12,5,22 force recreate keyboard if show arrow keys mode changes.
    private int mSplitKeyboard; // Jeremy '12,5,26 force recreate keyboard if split keyboard settings changes;
                                // 6/19 changed to int
    long mMetaState;
    private int mImeOptions;
    private int mOrientation;
    private int mHardkeyboardHidden;
    private boolean mPredicting;
    private Context mThemeContext;
    Mapping selectedCandidate; // Jeremy '12,5,7 renamed from firstMacthed
    // private int selectedIndex; //Jeremy '12,5,7 the index in resultList of
    // selectedCandidate
    Mapping committedCandidate; // Jeremy '12,5,7 renamed from tempMatched
    StringBuffer tempEnglishWord;
    List<Mapping> tempEnglishList;
    public boolean hasPhysicalKeyPressed;
    private boolean mDayiSymbolPrefix = false;
    LinkedList<Mapping> mCandidateList; // Jeremy '12,5,7 renamed from templist
    // private boolean hasSearchPress = false; // Jeremy '11,5,29
    // private boolean hasSearchProcessed = false; // Jeremy '11,5,29
    private Vibrator mVibrator;
    private AudioManager mAudioManager;
    private boolean hasVibration = false;
    private boolean hasSound = false;
    boolean hasNumberMapping = false;
    boolean hasSymbolMapping = false;

    boolean hasQuickSwitch = false;
    // Hard Keyboad Shift + Space Status
    boolean hasShiftPress = false;
    boolean mShiftHandledInOnPress = false;
    boolean onlyShiftPress = false; // Jeremy '15,5,30 shift only to switch between chi/eng
    boolean hasCtrlPress = false; // Jeremy '11,5,13
    boolean hasCtrlWithShift = false; // Sticky flag: true when Ctrl+Shift chord was formed (survives Ctrl keyUp before Shift keyUp)
    boolean lastKeyCtrl = false; // Jeremy '15,5,30 for process physical keyboard ctrl-space with missing space
                                 // down event
    boolean spaceKeyPress = false; // Jeremy '15,5,30 for process physical keyboard ctrl-space with missing
                                   // space down event

    // To keep key press time
    // private long keyPressTime = 0;
    private long mLastShiftTime = 0; // Jeremy '24,1,7: For shift double-tap check
    boolean hasWinPress = false; // Jeremy '12,4,29 windows start key on standard windows keyboard
    // private boolean hasCtrlProcessed = false; // Jeremy '11,6.18
    boolean hasDistinctMultitouch;// Jeremy '11,8,3
    private boolean hasShiftCombineKeyPressed = false; // Jeremy ,11,8, 3
    boolean hasMenuPress = false; // Jeremy '11,5,29
    boolean hasMenuProcessed = false; // Jeremy '11,5,29
    boolean hasEnterProcessed = false; // Jeremy '11,6.18

    boolean hasSpaceProcessed = false;
    boolean hasKeyProcessed = false; // Jeremy '11,8,15 for long pressed key
    int mLongPressKeyTimeout; // Jeremy '11,8, 15 read long press timeout from config
    private boolean mIsHardwareAcceleratedDrawingEnabled = false;
    boolean hasSymbolEntered = false; // Jeremy '11,5,24
    private String mIMActivatedState = ""; // Jeremy '12,5,3, renamed from keyboardSelectedState
    private List<String> activatedIMNameList; // Jeremy '12,4,30 renamed from keyboardList
    private List<String> activatedIMShortNameList; // Jeremy '12,4,30 renamed from keyboardShortname
    List<String> activatedIMList; // jerem '12,4,30 reanmed from keybaordCodeList; package-private for PhysicalKeyHandler
    String currentSoftKeyboard = ""; // Jeremy '12,4,30 reanmed from keybaord_xml;
    SearchServer SearchSrv = null;
    // Auto Commmit Value
    private int auto_commit = 0;
    // Disable physical keyboard candidate words selection
    private boolean disable_physical_selection = false;
    String LDComposingBuffer = ""; // Jeremy '11,7,30 for learning continuous typing phrases
    LIMEPreferenceManager mLIMEPref;
    boolean hasChineseSymbolCandidatesShown = false;
    boolean hasCandidatesShown = false;
    private androidx.appcompat.app.AlertDialog mOptionsDialog;
    private int mKeyboardThemeIndex = -1;

    // Jeremy '24,1,7: Emoji Picker Support (Compose)
    private android.widget.FrameLayout mInputViewContainer;
    private net.toload.main.hd.ComposeLifecycleOwner mComposeLifecycleOwner;
    android.view.View mEmojiKeyboardView; // ComposeView

    // Helper classes for better code organization
    private IMSwitchHelper mIMSwitchHelper;
    private OptionsDialogHelper mOptionsDialogHelper;

    public LIMEService() {

        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        mIsHardwareAcceleratedDrawingEnabled = true;// this.enableHardwareAcceleration();
        // }

    }

    /**
     * Main initialization of the input method component. Be sure to call to
     * super class.
     */
    @Override
    public void onCreate() {

        if (DEBUG)
            Log.i(TAG, "OnCreate()");

        super.onCreate();

        // Set ViewTree owners on the IME window DecorView so Compose's windowRecomposer
        // fallback can find a LifecycleOwner. InputMethodService uses a SoftInputWindow
        // (Dialog subclass); getWindow().getWindow() returns the underlying android.view.Window
        // whose DecorView is the root ancestor of every view in this IME window.
        try {
            android.view.Window imeWin = getWindow().getWindow();
            if (imeWin != null) {
                android.view.View decorView = imeWin.getDecorView();
                mComposeLifecycleOwner = new net.toload.main.hd.ComposeLifecycleOwner();
                mComposeLifecycleOwner.performRestore(null);
                mComposeLifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE);
                mComposeLifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START);
                mComposeLifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME);
                androidx.lifecycle.ViewTreeLifecycleOwner.set(decorView, mComposeLifecycleOwner);
                androidx.savedstate.ViewTreeSavedStateRegistryOwner.set(decorView, mComposeLifecycleOwner);
                androidx.lifecycle.ViewTreeViewModelStoreOwner.set(decorView, mComposeLifecycleOwner);
                Log.d(TAG, "IME window ViewTree owners set on DecorView");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set IME window ViewTree owners", e);
        }

        // Start foreground service to prevent Samsung FreecessHandler from freezing the
        // IME
        startForegroundService();

        // Initialize Global Package Name for paths
        net.toload.main.hd.global.LIME.PACKAGE_NAME = getPackageName();

        SearchSrv = new SearchServer(this);
        mEnglishOnly = false;
        mEnglishFlagShift = false;

        // Construct Preference Access Tool
        mLIMEPref = new LIMEPreferenceManager(this);

        mFixedCandidateViewOn = mLIMEPref.getFixedCandidateViewDisplay();

        mVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mLongPressKeyTimeout = getResources().getInteger(R.integer.config_long_press_key_timeout); // Jeremy '11,8,15
                                                                                                   // read longpress
                                                                                                   // timeout from
                                                                                                   // config resources.

        // initial keyboard list
        activatedIMNameList = new ArrayList<>();
        activatedIMList = new ArrayList<>();
        activatedIMShortNameList = new ArrayList<>();
        activeIM = mLIMEPref.getActiveIM();

        // Initialize helper classes
        mIMSwitchHelper = new IMSwitchHelper(mLIMEPref, getResources());
        mOptionsDialogHelper = new OptionsDialogHelper(mLIMEPref, getResources());

        buildActivatedIMList();

    }

    /**
     * Start foreground service to prevent Samsung FreecessHandler from freezing the
     * IME.
     * This creates a low-priority notification that keeps the service alive.
     */
    @SuppressLint("ForegroundServiceType")
    private void startForegroundService() {
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);

            // Create notification channel for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_MIN // Minimal importance - no sound, no popup
                );
                channel.setDescription("Input Method Service");
                channel.setShowBadge(false);
                channel.enableLights(false);
                channel.enableVibration(false);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            }

            // Create intent to open main activity when notification is tapped
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            // Build the notification (minimal/silent)
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(this);
                builder.setPriority(Notification.PRIORITY_MIN);
            }

            Notification notification = builder
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("輸入法服務執行中")
                    .setSmallIcon(R.drawable.logo)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();

            // Start foreground service with type for Android 14+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification);
            }

            if (DEBUG)
                Log.i(TAG, "Foreground service started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground service: " + e.getMessage());
        }
    }

    /**
     * This is the point where you can do all of your UI initialization. It is
     * called after creation and any configuration change.
     */
    @Override
    public void onInitializeInterface() {

        if (DEBUG)
            Log.i(TAG, "onInitializeInterface()");

        initialViewAndSwitcher(false);
        initCandidateView(); // Force the oncreatedcandidate to be called
        mKeyboardSwitcher.resetKeyboards(true);
        super.onInitializeInterface();

    }

    /**
     * Called by the system when the device configuration changes while your
     * activity is running.
     */
    @Override
    public void onConfigurationChanged(Configuration conf) {

        if (DEBUG)
            Log.i(TAG, "LIMEService:OnConfigurationChanged()");

        // Jeremy '12,4,7 add hardkeyboard hidden configuration changed event and clear
        // composing to avoid fc.
        if (conf.orientation != mOrientation || conf.hardKeyboardHidden != mHardkeyboardHidden) {
            // Jeremy '12,4,21 force clear the composing buffer
            clearComposing(true);

            mOrientation = conf.orientation;
            mHardkeyboardHidden = conf.hardKeyboardHidden;
        }
        initialViewAndSwitcher(true);
        mKeyboardSwitcher.resetKeyboards(true);
        super.onConfigurationChanged(conf);

    }

    /**
     * Called by the framework when your view for creating input needs to be
     * generated. This will be called the first time your input method is
     * displayed, and every time it needs to be re-created such as due to a
     * configuration change.
     */
    @Override
    public View onCreateInputView() {
        if (DEBUG)
            Log.i(TAG, "OnCreateInputView()");

        if (mInputView != null)
            mInputView.closing();
        mInputView = null;

        // Clear dedup guard so onStartInputView runs full initOnStartInput after recreating the view.
        mLastInitializedEditorInfo = null;

        initialViewAndSwitcher(true); // Jeremy '12,4,29. will do buildactivekeyboardlist in init startInput

        android.app.Dialog dialog = getWindow();
        if (dialog != null && dialog.getWindow() != null) {
            android.view.Window window = dialog.getWindow();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.view.View decorView = window.getDecorView();
                int flags = decorView.getSystemUiVisibility();
                
                boolean isMaterial3 = getKeyboardTheme() == net.toload.main.hd.R.style.LIMETheme_Material3;
                boolean isLightTheme = getKeyboardTheme() == net.toload.main.hd.R.style.LIMETheme_Light;
                boolean isSystemDarkMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                boolean isLightMode = isLightTheme || (isMaterial3 && !isSystemDarkMode);

                if (isLightMode) {
                    flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    if (isMaterial3) {
                        window.setNavigationBarColor(mThemeContext.getResources().getColor(net.toload.main.hd.R.color.md_theme_surface, mThemeContext.getTheme()));
                    } else {
                        window.setNavigationBarColor(mThemeContext.getResources().getColor(net.toload.main.hd.R.color.keyboard_background_light, mThemeContext.getTheme()));
                    }
                } else {
                    flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    if (isMaterial3) {
                        window.setNavigationBarColor(mThemeContext.getResources().getColor(net.toload.main.hd.R.color.md_theme_surface, mThemeContext.getTheme()));
                    }
                }
                decorView.setSystemUiVisibility(flags);
            }
        }

        Log.d("EMOJI_DEBUG", "=== onCreateInputView() called ===");
        Log.d("EMOJI_DEBUG", "mFixedCandidateViewOn = " + mFixedCandidateViewOn);

        if (mInputViewContainer == null) {
            Log.d("KBD_DEBUG", "Creating new FrameLayout for input container");
            mInputViewContainer = new android.widget.FrameLayout(this);
            // InputMethodService views usually should use WRAP_CONTENT for height
            mInputViewContainer.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        updateInputViewContainer();
        
        Log.d("KBD_DEBUG", "Input container setup complete. Children count: " + mInputViewContainer.getChildCount());
        return mInputViewContainer;
    }

    private void ensureComposeLifecycleOwner() {
        if (mComposeLifecycleOwner == null) {
            mComposeLifecycleOwner = new net.toload.main.hd.ComposeLifecycleOwner();
            mComposeLifecycleOwner.performRestore(null);
            mComposeLifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE);
            mComposeLifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START);
            mComposeLifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME);
        }
    }

    private void updateInputViewContainer() {
        if (mInputViewContainer == null) {
            Log.w(TAG, "updateInputViewContainer: container is null, skipping");
            return;
        }

        ensureComposeLifecycleOwner();

        // Set ViewTree owners so Compose's windowRecomposer lookup finds them.
        androidx.lifecycle.ViewTreeLifecycleOwner.set(mInputViewContainer, mComposeLifecycleOwner);
        androidx.savedstate.ViewTreeSavedStateRegistryOwner.set(mInputViewContainer, mComposeLifecycleOwner);
        androidx.lifecycle.ViewTreeViewModelStoreOwner.set(mInputViewContainer, mComposeLifecycleOwner);

        mInputViewContainer.removeAllViews();

        Configuration config = getResources().getConfiguration();
        boolean isPhysicalKeyboardConnected = config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;

        // Force fixed mode when physical keyboard is connected to prevent fullscreen issues
        boolean useFixedMode = mFixedCandidateViewOn || isPhysicalKeyboardConnected;

        if (useFixedMode) {
            if (mCandidateInInputView != null) {
                Log.d("KBD_DEBUG", "Adding mCandidateInInputView to container (PhysicalKeyboard=" + isPhysicalKeyboardConnected + ")");
                if (mCandidateInInputView.getParent() != null)
                    ((android.view.ViewGroup) mCandidateInInputView.getParent()).removeView(mCandidateInInputView);

                // Hide the virtual keyboard if physical keyboard is connected
                View keyboardView = mCandidateInInputView.findViewById(R.id.keyboard);
                if (keyboardView != null) {
                    keyboardView.setVisibility(isPhysicalKeyboardConnected ? View.GONE : View.VISIBLE);
                }

                mCandidateInInputView.setVisibility(View.VISIBLE);
                try {
                    android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.gravity = android.view.Gravity.BOTTOM;
                    mInputViewContainer.addView(mCandidateInInputView, lp);

                    // Update mCandidateView to the one inside InputView
                    mCandidateView = mCandidateInInputView.findViewById(R.id.candidatesView);

                    // Root cause of Samsung "Keyboard button on navigation bar" overlap:
                    // Our IME runs edge-to-edge and the candidate bar extends INTO the
                    // navigation bar area. Samsung's IME switcher button is drawn by system
                    // UI at a higher z-order in that same nav-bar zone, visually covering
                    // our candidates and intercepting touch events.
                    //
                    // Fix: When a physical keyboard is connected, register an insets listener
                    // that applies paddingBottom = max(navBar, gesture) to the candidate
                    // container. This pushes candidate CONTENT above the nav bar area so
                    // Samsung's button is in the empty background strip below, not on top
                    // of our candidates.
                    if (isPhysicalKeyboardConnected) {
                        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                                mCandidateInInputView,
                                (view, insets) -> {
                                    int navBottom = insets.getInsets(
                                            androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom;
                                    int gestureBottom = insets.getInsets(
                                            androidx.core.view.WindowInsetsCompat.Type.systemGestures()).bottom;
                                    int bottomPad = Math.max(navBottom, gestureBottom);
                                    view.setPadding(
                                            view.getPaddingLeft(),
                                            view.getPaddingTop(),
                                            view.getPaddingRight(),
                                            bottomPad);
                                    Log.d("KBD_DEBUG", "Candidate container paddingBottom=" + bottomPad
                                            + "px (navBar=" + navBottom + ", gesture=" + gestureBottom + ")");
                                    return insets;
                                });
                        androidx.core.view.ViewCompat.requestApplyInsets(mCandidateInInputView);
                    } else {
                        // Remove any previous insets listener and reset bottom padding when
                        // no physical keyboard is connected.
                        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                                mCandidateInInputView, null);
                        mCandidateInInputView.setPadding(
                                mCandidateInInputView.getPaddingLeft(),
                                mCandidateInInputView.getPaddingTop(),
                                mCandidateInInputView.getPaddingRight(),
                                0);
                    }

                } catch (Exception e) {
                    Log.e("KBD_DEBUG", "FAILED to add mCandidateInInputView to container: " + e.getMessage(), e);
                }

            } else {
                Log.e("KBD_DEBUG", "ERROR: mCandidateInInputView is NULL in fixed mode!");
            }
        } else {
            if (mInputView != null) {
                Log.d("KBD_DEBUG", "Adding keyboard view to container.");
                if (mInputView.getParent() != null)
                    ((android.view.ViewGroup) mInputView.getParent()).removeView(mInputView);
                mInputView.setVisibility(View.VISIBLE);
                try {
                    android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.gravity = android.view.Gravity.BOTTOM;
                    mInputViewContainer.addView(mInputView, lp);
                } catch (Exception e) {
                    Log.e("KBD_DEBUG", "FAILED to add mInputView to container: " + e.getMessage(), e);
                }
            } else {
                Log.e("KBD_DEBUG", "ERROR: mInputView is NULL in normal mode!");
            }
        }

        // Re-add emoji keyboard view only if it was already created (lazy loading)
        if (mEmojiKeyboardView != null) {
            Log.d("KBD_DEBUG", "Re-adding existing emoji picker in container");
            if (mEmojiKeyboardView.getParent() != null) {
                ((android.view.ViewGroup) mEmojiKeyboardView.getParent()).removeView(mEmojiKeyboardView);
            }

            // Set owners directly on emoji view too
            ensureComposeLifecycleOwner();
            androidx.lifecycle.ViewTreeLifecycleOwner.set(mEmojiKeyboardView, mComposeLifecycleOwner);
            androidx.savedstate.ViewTreeSavedStateRegistryOwner.set(mEmojiKeyboardView, mComposeLifecycleOwner);
            androidx.lifecycle.ViewTreeViewModelStoreOwner.set(mEmojiKeyboardView, mComposeLifecycleOwner);

            try {
                android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.gravity = android.view.Gravity.BOTTOM;
                mInputViewContainer.addView(mEmojiKeyboardView, lp);
            } catch (Exception e) {
                Log.e("KBD_DEBUG", "FAILED to add mEmojiKeyboardView to container: " + e.getMessage(), e);
            }
        }
    }


    private void initComposingPopup() {
        if (mComposingPopup == null) {
            mComposingPopup = new ComposingTextPopup(this);
        }
    }

    private void showComposingPopup(String text) {
        // Ensure popup operations run on main thread (may be called from background
        // thread)
        mMainHandler.post(() -> {
            if (mComposingPopup != null) {
                mComposingPopup.updateComposingText(text);
                View anchor = mInputViewContainer != null ? mInputViewContainer
                        : (mCandidateInInputView != null ? mCandidateInInputView : mInputView);
                if (anchor != null && text != null && !text.isEmpty()) {
                    mComposingPopup.show(anchor, 16, 12); // Use 12dp vertical margin for better spacing
                }
            }
        });
    }

    private void hideComposingPopup() {
        // Ensure popup operations run on main thread
        mMainHandler.post(() -> {
            if (mComposingPopup != null) {
                mComposingPopup.hide();
            }
        });
    }

    public void toggleEmojiVisibility() {
        Log.d("EMOJI_DEBUG", "=== toggleEmojiVisibility() called ===");

        // Lazy load emoji picker view only when user actually clicks it
        if (mEmojiKeyboardView == null && mInputViewContainer != null) {
            Log.d("EMOJI_DEBUG", "Creating emoji picker view lazily via ComposeBridge");
            mEmojiKeyboardView = net.toload.main.hd.ComposeBridge.INSTANCE.createEmojiPickerView(this, this);
            if (mEmojiKeyboardView != null) {
                mEmojiKeyboardView.setVisibility(View.GONE);

                // Set owners
                ensureComposeLifecycleOwner();
                androidx.lifecycle.ViewTreeLifecycleOwner.set(mEmojiKeyboardView, mComposeLifecycleOwner);
                androidx.savedstate.ViewTreeSavedStateRegistryOwner.set(mEmojiKeyboardView, mComposeLifecycleOwner);
                androidx.lifecycle.ViewTreeViewModelStoreOwner.set(mEmojiKeyboardView, mComposeLifecycleOwner);

                try {
                    android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.gravity = android.view.Gravity.BOTTOM;
                    mInputViewContainer.addView(mEmojiKeyboardView, lp);
                } catch (Exception e) {
                    Log.e("EMOJI_DEBUG", "FAILED to add mEmojiKeyboardView to container: " + e.getMessage(), e);
                }
            }
        }

        if (mEmojiKeyboardView == null) {
            Log.e("EMOJI_DEBUG", "ERROR: mEmojiKeyboardView is NULL!");
            return;
        }

        Log.d("EMOJI_DEBUG", "mEmojiKeyboardView exists, current visibility: " + mEmojiKeyboardView.getVisibility());
        Log.d("EMOJI_DEBUG",
                "View.VISIBLE=" + View.VISIBLE + ", View.GONE=" + View.GONE + ", View.INVISIBLE=" + View.INVISIBLE);

        if (mEmojiKeyboardView.getVisibility() == View.VISIBLE) {
            Log.d("EMOJI_DEBUG", "Emoji picker is VISIBLE, closing it...");
            closeEmojiPicker();
        } else {
            Log.d("EMOJI_DEBUG", "Emoji picker is HIDDEN, showing it...");

            // FIRST: Hide other views to prevent overlap
            if (mInputView != null) {
                Log.d("EMOJI_DEBUG", "Hiding mInputView");
                mInputView.setVisibility(View.GONE);
            }

            // Hide candidate view BEFORE showing emoji picker
            if (mFixedCandidateViewOn && mCandidateInInputView != null) {
                Log.d("EMOJI_DEBUG", "Hiding mCandidateInInputView (fixed mode)");
                mCandidateInInputView.setVisibility(View.GONE);

                // Also hide all children of CandidateInInputView to prevent any overlap
                if (mCandidateInInputView instanceof android.view.ViewGroup) {
                    android.view.ViewGroup group = (android.view.ViewGroup) mCandidateInInputView;
                    for (int i = 0; i < group.getChildCount(); i++) {
                        group.getChildAt(i).setVisibility(View.GONE);
                        Log.d("EMOJI_DEBUG", "  Hiding child " + i + " of CandidateInInputView: "
                                + group.getChildAt(i).getClass().getSimpleName());
                    }
                }
            } else if (mCandidateViewContainer != null && !mFixedCandidateViewOn) {
                Log.d("EMOJI_DEBUG", "Hiding candidate view");
                hideCandidateView();
            }

            // THEN: Show emoji picker
            mEmojiKeyboardView.setVisibility(View.VISIBLE);
            mEmojiKeyboardView.bringToFront(); // Force to front
            mEmojiKeyboardView.invalidate(); // Force redraw

            // Use the height from ComposeView's layoutParams (set in ComposeBridge)
            // Don't override with hardcoded value
            mEmojiKeyboardView.requestLayout();

            // Force layout update
            mInputViewContainer.requestLayout();
            mInputViewContainer.invalidate();

            Log.d("EMOJI_DEBUG", "Emoji picker should now be visible!");
            Log.d("EMOJI_DEBUG", "mEmojiKeyboardView visibility after: " + mEmojiKeyboardView.getVisibility());
            Log.d("EMOJI_DEBUG", "mEmojiKeyboardView height: " + mEmojiKeyboardView.getHeight());
            Log.d("EMOJI_DEBUG", "mEmojiKeyboardView width: " + mEmojiKeyboardView.getWidth());
            if (mCandidateInInputView != null) {
                Log.d("EMOJI_DEBUG", "mCandidateInInputView visibility: " + mCandidateInInputView.getVisibility());
            }

            // Debug: Check all children in container
            if (mInputViewContainer != null) {
                Log.d("EMOJI_DEBUG", "Container children count: " + mInputViewContainer.getChildCount());
                for (int i = 0; i < mInputViewContainer.getChildCount(); i++) {
                    android.view.View child = mInputViewContainer.getChildAt(i);
                    Log.d("EMOJI_DEBUG", "  Child " + i + ": " + child.getClass().getSimpleName() +
                            " visibility=" + child.getVisibility() +
                            " z=" + child.getZ());
                }
            }
        }
    }

    public void closeEmojiPicker() {
        Log.d("EMOJI_DEBUG", "=== closeEmojiPicker() called ===");
        if (mEmojiKeyboardView != null && mEmojiKeyboardView.getVisibility() == View.VISIBLE) {
            Log.d("EMOJI_DEBUG", "Closing emoji picker");
            mEmojiKeyboardView.setVisibility(View.GONE);

            Configuration closeCfg = getResources().getConfiguration();
            boolean physKeyConnected = closeCfg.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;

            // Restore candidate view in fixed mode (physical keyboard always uses fixed mode)
            if ((mFixedCandidateViewOn || physKeyConnected) && mCandidateInInputView != null) {
                Log.d("EMOJI_DEBUG", "Restoring mCandidateInInputView (fixed mode, physKey=" + physKeyConnected + ")");
                mCandidateInInputView.setVisibility(View.VISIBLE);

                // Restore children — but honour physical-keyboard state:
                // the virtual keyboard view inside the container must stay GONE when a
                // physical keyboard is connected (same rule as updateInputViewContainer).
                if (mCandidateInInputView instanceof android.view.ViewGroup) {
                    android.view.ViewGroup group = (android.view.ViewGroup) mCandidateInInputView;
                    for (int i = 0; i < group.getChildCount(); i++) {
                        android.view.View child = group.getChildAt(i);
                        // Keep virtual keyboard hidden when physical keyboard is attached
                        if (physKeyConnected && child.getId() == R.id.keyboard) {
                            child.setVisibility(View.GONE);
                            Log.d("EMOJI_DEBUG", "  Keeping keyboard child GONE (physical keyboard connected)");
                        } else {
                            child.setVisibility(View.VISIBLE);
                            Log.d("EMOJI_DEBUG", "  Restoring child " + i + " visibility");
                        }
                    }
                }
            } else if (mInputView != null && !physKeyConnected) {
                Log.d("EMOJI_DEBUG", "Showing virtual keyboard again");
                mInputView.setVisibility(View.VISIBLE);
            }
        } else {
            Log.d("EMOJI_DEBUG", "Emoji picker already closed or NULL");
        }
    }

    public void handleComposeBackspace() {
        handleBackspace();
    }

    /**
     * Create and return the view hierarchy used to show candidates.
     * This will be called once, when the candidates are first displayed.
     * You can return null to have no candidates view; the default implementation
     * returns null.
     */

    @Override
    public View onCreateCandidatesView() {

        if (DEBUG)
            Log.i(TAG, "onCreateCandidatesView()");

        @SuppressLint("InflateParams")
        CandidateViewContainer candidateViewContainer = (CandidateViewContainer) getLayoutInflater()
                .inflate(R.layout.candidates, null);
        candidateViewContainer.initViews();
        mCandidateViewContainer = candidateViewContainer;

        mCandidateViewStandAlone = mCandidateViewContainer.findViewById(R.id.candidates);
        mCandidateViewStandAlone = mCandidateViewContainer.findViewById(R.id.candidates);
        mCandidateViewStandAlone.setService(this);


        if (!mFixedCandidateViewOn)
            mCandidateView = mCandidateViewStandAlone;

        return mCandidateViewContainer;

    }

    /**
     * Override this to control when the input method should run in fullscreen mode.
     * Jeremy '11,5,31
     * Override fullscreen editing mode settings for larger screen (>1.4in)
     */

    @Override
    public boolean onEvaluateFullscreenMode() {
        // Always return false to disable the fullscreen extraction mode.
        // This fixes layout issues in landscape on modern devices and ensures
        // the keyboard behaves consistently (overlaying/resizing) rather than
        // taking over the entire screen with a separate text input box.
        return false;
    }

    @Override
    public boolean onEvaluateInputViewShown() {
        Configuration config = getResources().getConfiguration();
        boolean isPhysicalKeyboardConnected = config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
        if (DEBUG) {
            Log.i(TAG, "onEvaluateInputViewShown(): isPhysicalKeyboardConnected=" + isPhysicalKeyboardConnected);
        }
        // Always return true to ensure we have a valid window area at the bottom.
        // We will hide the actual keyboard keys in updateInputViewContainer().
        return true;
    }

    /**
     * This is called when the user is done editing a field. We can use this to
     * reset our state.
     */

    @Override
    public void onFinishInput() {

        if (DEBUG) {
            Log.i(TAG, "onFinishInput()");
        }
        super.onFinishInput();

        if (mInputView != null) {
            mInputView.closing();
        }
        try {
            if (LDComposingBuffer.length() > 0) { // Force interrupt the LD process
                LDComposingBuffer = "";
                SearchSrv.addLDPhrase(null, true);
            }
            // Jeremy '11,8,1 do postfinishinput in searchSrv (learn userdic and LDPhrase).
            SearchSrv.postFinishInput();
        } catch (Exception e) {
            Log.e(TAG, "Error in postFinishInput: " + e.getMessage());
        }
        // Clear current composing text and candidates.
        // Jeremy '12,5,21
        finishComposing();

        // -> 26.May.2011 by Art : Update keyboard list when user click the keyboard.
        try {
            mKeyboardSwitcher.setKeyboardList(SearchSrv.getKeyboardList());
            mKeyboardSwitcher.setImList(SearchSrv.getImList());
        } catch (Exception e) {
            Log.e(TAG, "Error getting keyboard/IM list: " + e.getMessage());
        }

    }

    /**
     * add by Jeremy '12,4,21
     * Send ic.finishComposingText upon composing is about to end
     */
    void finishComposing() {
        if (DEBUG)
            Log.i(TAG, "finishComposing()");
        // Jeremy '11,8,14
        if (mComposing != null && mComposing.length() > 0)
            mComposing.setLength(0);

        InputConnection ic = getCurrentInputConnection();
        if (ic != null)
            ic.finishComposingText();

        selectedCandidate = null;
        // selectedIndex = 0;

        if (mCandidateList != null)
            mCandidateList.clear();
        if (mCandidateView != null)
            mCandidateView.clear();

        hideComposingPopup();
    }

    /**
     * add by Jeremy '12,4,21
     * clearComposing buffer upon composing is about to end
     * add forceClearComposing parameter to control forced clear the system
     * composing buffer
     */
    void clearComposing(boolean forceClearComposing) {
        if (DEBUG)
            Log.i(TAG, "clearComposing()");

        // Log.i(TAG, "===========> clear composing");

        try {
            // Jeremy '11,8,14
            if (mComposing != null && mComposing.length() > 0)
                mComposing.setLength(0);
            if (mCandidateList != null)
                mCandidateList.clear();

            if (forceClearComposing) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    // Use setComposingText("", 0) to clear composing text displayed in editor
                    // Then finishComposingText() to complete the composition
                    ic.setComposingText("", 0);
                    ic.finishComposingText();
                }
            }

            selectedCandidate = null;
            // selectedIndex = 0;

            clearSuggestions();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing composing: " + e.getMessage());
            // ignore candidate clear error
        }
    }

    /**
     * Clear suggestions or candidates in candidate view.
     */
    void clearSuggestions() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            mMainHandler.post(this::clearSuggestions);
            return;
        }
        if (mCandidateView != null) {
            if (DEBUG)
                Log.i(TAG, "clearSuggestions(): "
                        + ", hasCandidatesShown:" + hasCandidatesShown);

            if (!mEnglishOnly && mLIMEPref.getAutoChineseSymbol() // Jeremy '12,4,29 use mEnglishOnly instead of onIM
                    && (hasCandidatesShown || mFixedCandidateViewOn)) { // Change isCandiateShown() to hasCandiatesShown
                mCandidateView.clear();
                if (hasCandidatesShown)
                    updateChineseSymbol(); // Jeremy '12.5,23 do not show chinesesymbol when init for fixed candidate
                                           // view.
            } else {
                mCandidateView.clear();
                hideCandidateView();
            }

        }
        hideComposingPopup();
    }

    /**
     * Jeremy '15,7,8 to avoid candidateView shift up and down when it's not fixed.
     * 
     * NOTE: Composing text now uses PopupWindow, so transparency insets are no
     * longer needed.
     */
    @Override
    public void onComputeInsets(InputMethodService.Insets outInsets) {
        super.onComputeInsets(outInsets);
        // Let the system handle insets automatically.
        // Note: Do NOT set TOUCHABLE_INSETS_CONTENT here — doing so interferes with
        // touch event distribution to the candidate bar on Samsung One UI.
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application. At this point we have been bound to
     * the client, and are now receiving all of the detailed information about
     * the target of our edits.
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        if (DEBUG)
            Log.i(TAG, "onStartInput()");
        super.onStartInput(attribute, restarting);
        initOnStartInput(attribute);
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        if (DEBUG)
            Log.i(TAG, "onStartInputView()");
        super.onStartInputView(attribute, restarting);
        initOnStartInput(attribute);
        initComposingPopup();
    }

    /**
     * Initialization for IM and softkeybaords, and also choose wring lanaguage mode
     * according the input attrubute in editorInfo
     */
    private void initOnStartInput(EditorInfo attribute) {
        if (attribute == mLastInitializedEditorInfo) return;

        if (DEBUG)
            Log.i(TAG, "initOnStartInput(): attribute.inputType & EditorInfo.TYPE_MASK_CLASS: "
                    + (attribute.inputType & EditorInfo.TYPE_MASK_CLASS)
                    + "; attribute.inputType & EditorInfo.TYPE_MASK_VARIATION: "
                    + (attribute.inputType & EditorInfo.TYPE_MASK_VARIATION));

        mLastInitializedEditorInfo = attribute;

        // Jeremy '12,5,29 override the fixCanddiateMode setting in Landscape mode (in
        // landscape mode the candidate bar is always not fixed).
        boolean fixedCandidateMode = mLIMEPref.getFixedCandidateViewDisplay();

        // UI Initialization - Only if views exist
        if (mInputView != null && mInputViewContainer != null) {
            // Jeremy '12,5,6 recreate inputView if fixedCandidateView setting is altered
            // Jeremy '15,7,15 recreate inputView if keyboard theme changed
            if (mFixedCandidateViewOn != fixedCandidateMode
                    || mKeyboardThemeIndex != mLIMEPref.getKeyboardTheme()) {
                Log.d("KBD_DEBUG", "Config change detected (fixedMode=" + fixedCandidateMode + ", theme=" + mLIMEPref.getKeyboardTheme() + ")");
                requestHideSelf(0);
                mInputView.closing();
                mFixedCandidateViewOn = fixedCandidateMode;

                initialViewAndSwitcher(true);
                updateInputViewContainer();

                if (mFixedCandidateViewOn) {
                    if (DEBUG)
                        Log.i(TAG, "Fixed candidateView in on, return nInputViewContainer ");
                    Log.d("KBD_DEBUG", "Setting input view to container (fixed mode)");
                    setInputView(mInputViewContainer);
                } else {
                    Log.d("KBD_DEBUG", "Setting input view to container (normal mode)");
                    setInputView(mInputViewContainer);
                    if (DEBUG)
                        Log.i(TAG, "Fixed candidateView in off, return mInputView ");
                }
            } else {
                Log.d("KBD_DEBUG", "No config change, ensuring input view is set to container");
                updateInputViewContainer();
                setInputView(mInputViewContainer);
            }
        }

        hasPhysicalKeyPressed = false; // Jeremy '11,9,6 reset phsycalkeyflag
        hasCandidatesShown = false;

        // Ensure switcher has basic data even if UI is not ready
        if (mKeyboardSwitcher != null) {
            // Retry loading keyboard list if DB wasn't ready during initialViewAndSwitcher.
            // getKeyboardSize() returns 0 when kbHm is null, so this only fires when needed.
            if (mKeyboardSwitcher.getKeyboardSize() == 0) {
                try {
                    mKeyboardSwitcher.setKeyboardList(SearchSrv.getKeyboardList());
                } catch (Exception e) {
                    Log.e(TAG, "Error loading keyboard list in initOnStartInput: " + e.getMessage());
                }
            }

            // Reset the IM softkeyboard settings. Jeremy '11,6,19
            try {
                mKeyboardSwitcher.setImList(SearchSrv.getImList());
            } catch (Exception e) {
                Log.e(TAG, "Error getting IM list: " + e.getMessage());
            }

            mKeyboardSwitcher.resetKeyboards(
                    mShowArrowKeys != mLIMEPref.getShowArrowKeys() // Jeremy '12,5,22 recreate keyboard if the setting
                                                                   // altered.
                            || mSplitKeyboard != mLIMEPref.getSplitKeyboard()); // Jeremy '12,5,26 recreate keyboard if the
                                                                                // setting altered.
        }

        loadSettings();
        mImeOptions = attribute.imeOptions;

        buildActivatedIMList(); // Jeremy '12,4,29 only this is required here, instead of fully initialKeybaord
        mPredictionOn = true;
        mCompletionOn = false;
        mCompletions = null;
        mCapsLock = false;
        mHasShift = false;

        tempEnglishWord = new StringBuffer();
        tempEnglishList = new LinkedList<>();

        InputModeHelper.InputModeConfig config = InputModeHelper.determineInputMode(
                attribute, mLIMEPref, mImeOptions, mPersistentLanguageMode);

        mEnglishOnly = config.isEnglishOnly;
        mPredictionOn = config.isPredictionOn;
        // Only set completion if fullscreen check passes in existing logic, but helper
        // sets the hint
        if (config.isCompletionOn && isFullscreenMode()) {
            mCompletionOn = true;
        }

        if (mKeyboardSwitcher != null) {
            Log.d("LIME_KBD", "initOnStartInput: activeIM=" + activeIM
                    + " kbSize=" + mKeyboardSwitcher.getKeyboardSize()
                    + " mInputView=" + (mInputView == null ? "null" : "ok"));
            mKeyboardSwitcher.setKeyboardMode(activeIM, config.keyboardMode, mImeOptions,
                    !config.isEnglishOnly, config.isNumber, config.isDateTime);
            Log.d("LIME_KBD", "setKeyboardMode done");
        }

        if (!config.isEnglishOnly && !config.isPhone && !config.isNumber && !config.isDateTime
                && config.keyboardMode != LIMEKeyboardSwitcher.MODE_EMAIL
                && config.keyboardMode != LIMEKeyboardSwitcher.MODE_URL) {
            // Logic for initializing Chinese IM keyboard if not special modes
            initialIMKeyboard();
        }

        if (mEnglishOnly && !mPredictionOn) // Jeremy '12,5,20 Only hide candidateview when prediction mode is not on.
            // Jeremy '12,5,6 clear internal composing buffer in forceHideCandiateView
            forceHideCandidateView(); // Jeremy '12,5,6 zero the canidateView height to force hide it for eng/numeric
                                      // keyboard
        else {
            clearComposing(false);// Jeremy '12,5,24 clear the suggesions and also restore the height of fixed
                                  // candaiteview if it's hide before
            // clearSuggestions(); // do this in clearcomposing already.
        }

        mPredicting = false;
        updateShiftKeyState(getCurrentInputEditorInfo());

    }

    private void loadSettings() {

        hasVibration = mLIMEPref.getVibrateOnKeyPressed();
        hasSound = mLIMEPref.getSoundOnKeyPressed();
        mPersistentLanguageMode = mLIMEPref.getPersistentLanguageMode();
        activeIM = mLIMEPref.getActiveIM();
        hasQuickSwitch = mLIMEPref.getSwitchEnglishModeHotKey();
        mAutoCap = true;

        mPersistentLanguageMode = mLIMEPref.getPersistentLanguageMode();
        mShowArrowKeys = mLIMEPref.getShowArrowKeys();
        mSplitKeyboard = mLIMEPref.getSplitKeyboard();

        disable_physical_selection = mLIMEPref.getDisablePhysicalSelkey();

        auto_commit = mLIMEPref.getAutoCommitValue();
        currentSoftKeyboard = mKeyboardSwitcher.getImKeyboard(activeIM);

    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd, int candidatesStart,
            int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

        if (DEBUG)
            Log.i(TAG, "onUpdateSelection():oldSelStart" + oldSelStart
                    + " oldSelEnd:" + oldSelEnd
                    + " newSelStart:" + newSelStart + " newSelEnd:" + newSelEnd
                    + " candidatesStart:" + candidatesStart + " candidatesEnd:" + candidatesEnd);

        InputConnection ic = getCurrentInputConnection();

        if (ic != null) {
            CharSequence before = ic.getTextBeforeCursor(1, 0);
            if (before != null && before.length() > 0) {
                SearchServer.setLastCommittedChar(before.toString());
            } else {
                SearchServer.setLastCommittedChar(null);
            }
        }

        if (mComposing.length() > 0
                && !(candidatesEnd == candidatesStart) // Jeremy '12,7,2 bug fixed on composition being clear after
                                                       // second word in chrome
                && candidatesStart >= 0 && candidatesEnd > 0 // in composing
        ) {
            if (newSelStart < candidatesStart || newSelStart > candidatesEnd) { // cursor is moved before or after
                                                                                // composing area

                if (mCandidateList != null)
                    mCandidateList.clear();
                // mCandidateView.clear();
                hideCandidateView();

                if (mComposing != null && mComposing.length() > 0) {

                    mComposing.setLength(0);

                    if (ic != null)
                        ic.finishComposingText();
                }
            }
            // Jeremy '13,8,25 setSelection cause inputbox in Chorme failed to input
            // Jeremy '12,5,23 Select the composing text and forbidded moving cursor within
            // the composing text.
            // if (ic != null) ic.setSelection(candidatesStart, candidatesEnd);

        }

    }

    /**
     * This tells us about completions that the editor has determined based on
     * the current text in it. We want to use this in fullscreen mode to show
     * the completions ourself, since the editor can not be seen in that
     * situation.
     */
    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        if (DEBUG)
            Log.i(TAG, "onDisplayCompletions()");
        if (mCompletionOn) {
            mCompletions = completions;
            if (!mEnglishOnly) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
                if (mComposing.length() == 0)
                    updateRelatedPhrase(false);
            }
            if (mEnglishOnly && !mPredictionOn) {
                setSuggestions(buildCompletionList(), false, "");
            }

        }
    }

    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection. It is only needed when using the PROCESS_HARD_KEYS
     * option.
     */
    boolean translateKeyDown(int keyCode, KeyEvent event) {
        return HardKeyHelper.translateKeyDown(this, keyCode, event);
    }

    /**
     * Physical KeyBoard Event Handler Use this to monitor key events being
     * delivered to the application. We get first crack at them, and can either
     * resume them or let them continue to the app.
     */
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        Log.e(TAG, "onKeyDown() called with keyCode=" + keyCode + " event=" + event);
        // Clean code by jeremy '11,8,22
        if (DEBUG)
            Log.i(TAG, "OnKeyDown():keyCode:" + keyCode
                    + ", mComposing = " + mComposing
                    + ", hasMenuPress = " + hasMenuPress
                    + ", hasCtrlPress = " + hasCtrlPress
                    + ", isCtrlPressed = " + event.isCtrlPressed()
                    + ", hasShiftPress = " + hasShiftPress
                    + ", onlyShiftPress = " + onlyShiftPress
                    + ", hasWinPress = " + hasWinPress
                    + ", event.getEventTime() -  event.getDownTime()" + (event.getEventTime() - event.getDownTime())
                    + ", event.getRepeatCount()" + event.getRepeatCount()
                    + ", event.getMetaState()" + Integer.toHexString(event.getMetaState()));

        mKeydownEvent = new KeyEvent(event);
        // Record key pressed time and set key processed flags(key down, for physical
        // keys)
        // Jeremy '11,8,22 using getRepeatCount from event to set processed flags
        if (event.getRepeatCount() == 0) {// !keydown) {
            // keyPressTime = System.currentTimeMillis();
            // keydown = true;
            hasKeyProcessed = false;
            hasMenuProcessed = false; // only do this on first keydown event
            hasEnterProcessed = false;
            hasSpaceProcessed = false;
            hasSymbolEntered = false;
            // Jeremy '15,5,30 for physical keyboard
            onlyShiftPress = false;
            lastKeyCtrl = false;
            spaceKeyPress = false;
        }

        // Dayi Punctuation Fast Input
        if (activeIM != null && activeIM.startsWith("dayi") && !mEnglishOnly) {
            // Method 1: Shift + Key
            if (event.isShiftPressed() || hasShiftPress) {
                String symbol = null;
                if (keyCode == KeyEvent.KEYCODE_COMMA) symbol = "，";
                else if (keyCode == KeyEvent.KEYCODE_PERIOD) symbol = "。";
                else if (keyCode == KeyEvent.KEYCODE_SLASH) symbol = "？";
                else if (keyCode == KeyEvent.KEYCODE_1) symbol = "！";
                else if (keyCode == KeyEvent.KEYCODE_SEMICOLON) symbol = "：";
                
                if (symbol != null) {
                    commitTyped(getCurrentInputConnection());
                    getCurrentInputConnection().commitText(symbol, 1);
                    return true;
                }
            }

            // Method 2: = Prefix
            if (keyCode == KeyEvent.KEYCODE_EQUALS && !event.isShiftPressed() && !hasShiftPress) {
                mDayiSymbolPrefix = true;
                return true;
            }

            if (mDayiSymbolPrefix) {
                if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    mDayiSymbolPrefix = false;
                    return true;
                }
                if (keyCode != KeyEvent.KEYCODE_SHIFT_LEFT && keyCode != KeyEvent.KEYCODE_SHIFT_RIGHT) {
                    String symbol = null;
                    if (keyCode == KeyEvent.KEYCODE_COMMA) symbol = "，";
                    else if (keyCode == KeyEvent.KEYCODE_PERIOD) symbol = "。";
                    else if (keyCode == KeyEvent.KEYCODE_APOSTROPHE) symbol = "、";
                    else if (keyCode == KeyEvent.KEYCODE_SEMICOLON) {
                        if (event.isShiftPressed() || hasShiftPress) symbol = "：";
                        else symbol = "；";
                    }
    
                    if (symbol != null) {
                        commitTyped(getCurrentInputConnection());
                        getCurrentInputConnection().commitText(symbol, 1);
                        mDayiSymbolPrefix = false;
                        return true;
                    } else {
                        mDayiSymbolPrefix = false;
                    }
                }
            }
        }

        switch (keyCode) {
            // Jeremy '11,5,29 Bypass search and menu combination keys.
            case KeyEvent.KEYCODE_MENU:

                hasMenuPress = true;
                break;
            // Add by Jeremy '10, 3, 29. DPAD selection on candidate view
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                    mCandidateView.selectNext();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_DPAD_UP:
                if (hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                    if (activeIM != null && activeIM.startsWith("dayi") && hasPhysicalKeyPressed) {
                        mCandidateView.pagePrev();
                    } else {
                        mCandidateView.selectPrevRow();
                    }
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                    mCandidateView.selectPrev();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                    if (activeIM != null && activeIM.startsWith("dayi") && hasPhysicalKeyPressed) {
                        mCandidateView.pageNext();
                    } else {
                        mCandidateView.selectNextRow();
                    }
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // Log.i("ART","select:"+3);
                if (hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                    pickHighlightedCandidate();
                    return true;
                }
                break;
            // Add by Jeremy '10,3,26, process metakey with
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                hasShiftPress = true;
                onlyShiftPress = true;
                mMetaState = LIMEMetaKeyKeyListener.handleKeyDown(mMetaState, keyCode, event);
                break;
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
                mMetaState = LIMEMetaKeyKeyListener.handleKeyDown(mMetaState, keyCode, event);
                break;
            case MY_KEYCODE_CTRL_LEFT:
            case MY_KEYCODE_CTRL_RIGHT:
                hasCtrlPress = true;
                lastKeyCtrl = true;
                break;
            case MY_KEYCODE_WINDOWS_START:
                hasWinPress = true;
                break;
            case MY_KEYCODE_ESC:
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.

                if (event.getRepeatCount() == 0) {
                    // Jeremy '24,1,7: Handle emoji view back
                    if (mEmojiKeyboardView != null && mEmojiKeyboardView.getVisibility() == View.VISIBLE) {
                        closeEmojiPicker();
                        return true;
                    }

                    if (mInputView != null && mInputView.handleBack()) {
                        Log.i(TAG, "KEYCODE_BACK mInputView handled the backed key");
                        return true;
                    }
                    // Jeremy '12,4,8 rewrite the logic here
                    // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                    // TODO: need to recheck here.
                    else if (!mEnglishOnly
                            && hasCandidatesShown
                            && (mComposing.length() > 0
                                    || (selectedCandidate != null && !selectedCandidate.isComposingCodeRecord()
                                            && !hasChineseSymbolCandidatesShown))) {
                        if (DEBUG)
                            Log.i(TAG, "KEYCODE_BACK clearcomposing only.");
                        clearComposing(false);
                        return true;
                    } else if (!mEnglishOnly && hasCandidatesShown) { // Jeremy '12,6,13
                        hideCandidateView();
                        return true;
                    }

                }
                if (DEBUG)
                    Log.i(TAG, "KEYCODE_BACK return to super.");

                break;

            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                hasPhysicalKeyPressed = true;
                onKey(LIMEBaseKeyboard.KEYCODE_DELETE, null);
                return true;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these, if return
                // false from takeSelectedSuggestion().
                // Process enter for candidate view selection in OnKeyUp() to block
                // the real enter afterward.
                // return false;
                // Log.i("ART", "physical keyboard:"+ keyCode);
                mMetaState = LIMEMetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
                setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState();
                if (!mEnglishOnly) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
                    if (hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                        // To block a real enter after suggestion selection. We have to
                        // return true in OnKeyUp();
                        if (pickHighlightedCandidate()) {
                            hasEnterProcessed = true;
                            return true;
                        } else {
                            hideCandidateView();
                            break;
                        }
                    }
                } else if (// mLIMEPref.getEnglishPrediction() &&
                mPredictionOn && mLIMEPref.getEnglishPredictionOnPhysicalKeyboard()) {
                    resetTempEnglishWord();
                    this.updateEnglishPrediction();
                    break;
                } else // Jeremy '12',7,1 bug fixed on english mode enter not functioning in chrome
                    break;

                /*
                 * case MY_KEYCODE_ESC:
                 * //Jeremy '11,9,7 treat esc as back key
                 * //Jeremy '11,8,14
                 * clearComposing();
                 * InputConnection ic=getCurrentInputConnection();
                 * if(ic!=null) ic.commitText("", 0);
                 * return true;
                 */

            case KeyEvent.KEYCODE_SPACE:
                spaceKeyPress = true;
                hasQuickSwitch = mLIMEPref.getSwitchEnglishModeHotKey();
                // If user enable Quick Switch Mode control then check if has
                // Shift+Space combination
                // '11,5,13 Jeremy added Ctrl-space switch chi/eng
                // '11,6,18 Jeremy moved from on_KEY_UP
                // '12,4,29 Jeremy add hasWinPress + space to switch chi/eng (earth key on zippy
                // keyboard)
                // '12,5,8 Jeremy add send the space key to onKey with translatekeydown for
                // candidate processing if it's not switching chi/eng
                if ((hasQuickSwitch && hasShiftPress) || hasCtrlPress || hasMenuPress || hasWinPress
                        || event.isCtrlPressed()) {
                    if (!hasWinPress)
                        this.switchChiEng(); // Jeremy '12,5,20 move hasWinPress to winstartkey in onkeyUp()
                    if (hasMenuPress)
                        hasMenuProcessed = true;
                    hasSpaceProcessed = true;
                    return true;
                } else {
                    if (activeIM != null && activeIM.startsWith("dayi") && hasCandidatesShown && mCandidateList != null && !mCandidateList.isEmpty() && hasPhysicalKeyPressed) {
                        int candidateIndex = -1;
                        if (mCandidateView != null && mCandidateView.retrieveSelectedIndex() != -1) {
                            candidateIndex = mCandidateView.retrieveSelectedIndex();
                        } else {
                            int offset = (mCandidateView != null) ? mCandidateView.getCurrentPageOffset() : 0;
                            candidateIndex = offset;
                        }
                        if (candidateIndex < mCandidateList.size()) {
                            this.pickCandidateManually(candidateIndex);
                        }
                        return true;
                    }
                    return translateKeyDown(keyCode, event);
                }

            case MY_KEYCODE_SWITCH_CHARSET: // experia pro earth key
            case 1000: // milestone chi/eng key
                switchChiEng();
                break;
            case KeyEvent.KEYCODE_SYM:
            case KeyEvent.KEYCODE_AT:
                // Jeremy '11,8,22 use begintime and eventtime in event to see if long-pressed
                // or not.
                if (!hasKeyProcessed
                        && event.getRepeatCount() > 0
                        && event.getEventTime() - event.getDownTime() > mLongPressKeyTimeout) {
                    // && System.currentTimeMillis() - keyPressTime > mLongPressKeyTimeout){
                    switchChiEng();
                    hasKeyProcessed = true;
                }
                return true;
            case KeyEvent.KEYCODE_TAB: // Jeremy '12.6,22 Force bypassing tab processing to super if not on milestone 2
                                       // with alt on (alt+tab = ~ on milestone2)
                if (!(LIMEMetaKeyKeyListener.getMetaState(mMetaState,
                        LIMEMetaKeyKeyListener.META_ALT_ON) > 0
                        && mLIMEPref.getPhysicalKeyboardType().equals("milestone2")))
                    break;
            case KeyEvent.KEYCODE_GRAVE: // ` (key left of 1) — Ctrl+\ cycles internal IMs (大易↔注音)
                if (hasCtrlPress || event.isCtrlPressed()) {
                    switchToNextActivatedIM(true);
                    return true;
                }
                // No Ctrl: treat like any other character key (translateKeyDown)
                if (!hasMenuPress) {
                    if (translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_PERIOD: // Cmd+. — toggle emoji picker
                if (event.isMetaPressed()) {
                    requestShowSelf(0); // Force show IME window if hidden by physical keyboard
                    toggleEmojiVisibility();
                    return true;
                }
                // No Cmd: normal '.' key — fall through to translateKeyDown
                if (!hasMenuPress) {
                    if (translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
                break;
            default:
                if (!(hasCtrlPress || event.isCtrlPressed() || hasMenuPress)) {
                    // Dayi Fast Candidate Selection: ', [, ], -, \ select candidates 1 through 5
                    if (activeIM != null && activeIM.startsWith("dayi") && hasCandidatesShown && mCandidateList != null && !mCandidateList.isEmpty()) {
                        int candidateIndex = -1;
                        if (keyCode == KeyEvent.KEYCODE_APOSTROPHE) {
                            candidateIndex = 1;
                        } else if (keyCode == KeyEvent.KEYCODE_LEFT_BRACKET) {
                            candidateIndex = 2;
                        } else if (keyCode == KeyEvent.KEYCODE_RIGHT_BRACKET) {
                            candidateIndex = 3;
                        } else if (keyCode == KeyEvent.KEYCODE_MINUS) {
                            candidateIndex = 4;
                        } else if (keyCode == KeyEvent.KEYCODE_BACKSLASH) {
                            candidateIndex = 5;
                        }
                        
                        if (candidateIndex != -1) {
                            int offset = (mCandidateView != null) ? mCandidateView.getCurrentPageOffset() : 0;
                            candidateIndex += offset;
                            if (candidateIndex < mCandidateList.size()) {
                                this.pickCandidateManually(candidateIndex);
                            }
                            return true; // Consume the key event even if candidate doesn't exist to prevent typing the symbol
                        }
                    }

                    if (translateKeyDown(keyCode, event)) {
                        if (DEBUG)
                            Log.i(TAG, "Onkeydown():tranlatekeydown:true");
                        return true;
                    }
                }

        }

        if ((hasCtrlPress || hasMenuPress) && !mEnglishOnly) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
            int primaryKey = event.getUnicodeChar(LIMEMetaKeyKeyListener.getMetaState(mMetaState));
            char t = (char) primaryKey;

            if (hasCtrlPress && // Only working with ctrl Jeremy '11,8,22
                    mCandidateList != null && mCandidateList.size() > 0
                    && mCandidateView != null && hasCandidatesShown) {
                switch (keyCode) {
                    case 8:
                        this.pickCandidateManually(0);
                        return true;
                    case 9:
                        this.pickCandidateManually(1);
                        return true;
                    case 10:
                        this.pickCandidateManually(2);
                        return true;
                    case 11:
                        this.pickCandidateManually(3);
                        return true;
                    case 12:
                        this.pickCandidateManually(4);
                        return true;
                    case 13:
                        this.pickCandidateManually(5);
                        return true;
                    case 14:
                        this.pickCandidateManually(6);
                        return true;
                    case 15:
                        this.pickCandidateManually(7);
                        return true;
                    case 16:
                        this.pickCandidateManually(8);
                        return true;
                    case 7:
                        this.pickCandidateManually(9);
                        return true;
                }
            }
            if ((mComposing == null || mComposing.length() == 0)) {
                // Jeremy '11,8,21. Ctrl-/ to fetch full-shaped chinese symbols1 in
                // candidateview.
                if (t == '/') {
                    if (hasMenuPress)
                        hasMenuProcessed = true;
                    updateChineseSymbol();
                    return true;
                }
                // 27.May.2011 Art : when user click Ctrl + Symbol or number then send Chinese
                // Symobl Characters
                String s = ChineseSymbol.getSymbol(t);
                if (s != null) {
                    clearSuggestions();
                    getCurrentInputConnection().commitText(s, 0);
                    hasSymbolEntered = true;
                    if (hasMenuPress)
                        hasMenuProcessed = true;
                    return true;

                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    void resetTempEnglishWord() {
        tempEnglishWord.delete(0, tempEnglishWord.length());
        tempEnglishList.clear();
    }

    void setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            int clearStatesFlags = 0;
            if (LIMEMetaKeyKeyListener.getMetaState(mMetaState,
                    LIMEMetaKeyKeyListener.META_ALT_ON) == 0)
                clearStatesFlags += KeyEvent.META_ALT_ON;
            if (LIMEMetaKeyKeyListener.getMetaState(mMetaState,
                    LIMEMetaKeyKeyListener.META_SHIFT_ON) == 0)
                clearStatesFlags += KeyEvent.META_SHIFT_ON;
            if (LIMEMetaKeyKeyListener.getMetaState(mMetaState,
                    LIMEMetaKeyKeyListener.META_SYM_ON) == 0)
                clearStatesFlags += KeyEvent.META_SYM_ON;
            ic.clearMetaKeyStates(clearStatesFlags);
        }
    }
    // Contextual menu positions

    /**
     * Use this to monitor key events being delivered to the application. We get
     * first crack at them, and can either resume them or let them continue to
     * the app.
     */
    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (DEBUG)
            Log.i(TAG, "OnKeyUp():keyCode:" + keyCode
                    + ", mComposing = " + mComposing
                    + ", hasCtrlPress:" + hasCtrlPress
                    + ", hasWinPress:" + hasWinPress
                    + ", hasShiftPress = " + hasShiftPress
                    + ", event.getEventTime() -  event.getDownTime()" + (event.getEventTime() - event.getDownTime())

            );

        switch (keyCode) {
            // Jeremy '11,5,29 Bypass search and menu keys.
            // case KeyEvent.KEYCODE_SEARCH:
            // hasSearchPress = false;
            // if(hasSearchProcessed) return true;
            // break;
            case KeyEvent.KEYCODE_CAPS_LOCK:
                // Modified by Art 20130607
                // to switch the cap lock mode
                toggleCapsLock();
            case KeyEvent.KEYCODE_MENU:
                hasMenuPress = false;
                if (hasMenuProcessed)
                    return true;
                break;
            // */------------------------------------------------------------------------
            // Modified by Jeremy '10, 3,12
            // keep track of alt state with mHasAlt.
            // Modified '10, 3, 24 for bug fix and alt-lock implementation
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                hasShiftPress = false;
                mMetaState = LIMEMetaKeyKeyListener.handleKeyUp(mMetaState, keyCode, event);
                // '11,8,28 Jeremy popup keyboard picker instead of nextIM when onIM
                // '11,5,14 Jeremy ctrl-shift switch to next available keyboard;
                // '11,5,24 blocking switching if full-shape symbol
                if (!hasSymbolEntered && hasMenuPress) { 
                    showIMPicker(); // Menu+Shift: keep showing system picker
                    if (hasMenuPress) {
                        hasMenuProcessed = true;
                        hasMenuPress = false;
                    }
                    mMetaState = LIMEMetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
                    setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState();
                    return true;
                } else if (mLIMEPref.getShiftSwitchEnglishMode() && onlyShiftPress) {
                    this.switchChiEng();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
                mMetaState = LIMEMetaKeyKeyListener.handleKeyUp(mMetaState, keyCode, event);
                break;
            case MY_KEYCODE_CTRL_LEFT:
            case MY_KEYCODE_CTRL_RIGHT:
                hasCtrlPress = false;
                break;
            case MY_KEYCODE_WINDOWS_START:
                if (hasSpaceProcessed) // Jeremy '12,5,20 long press to show IM picker, switch chi/eng otherwise for
                                       // the win+space or earth key on zippy
                    if (event.getEventTime() - event.getDownTime() > mLongPressKeyTimeout)
                        showIMPicker();
                    else
                        switchChiEng();
                hasWinPress = false;
                break;
            case KeyEvent.KEYCODE_ENTER:
                // Add by Jeremy '10, 3 ,29. Pick selected selection if candidates
                // shown.
                // Does not block real enter after select the suggestion. !! need
                // fix here!!
                // Let the underlying text editor always handle these, if return
                // false from takeSelectedSuggestion().

                if (hasEnterProcessed) {
                    return true;
                }
                // Jeremy '10, 4, 12 bug fix on repeated enter.
                break;

            case KeyEvent.KEYCODE_SYM:
            case KeyEvent.KEYCODE_AT:
                if (hasKeyProcessed) { // (keyPressTime != 0
                    // && System.currentTimeMillis() - keyPressTime > 700) {
                    // switchChiEng(); // Jeremy '11,8,15 moved to onKeyDown()
                    return true;
                } else if (LIMEMetaKeyKeyListener.getMetaState(mMetaState,
                        LIMEMetaKeyKeyListener.META_SHIFT_ON) > 0 // Jeremy '12,4,29 use mEnglishOnly
                        && !mLIMEPref.getPhysicalKeyboardType().equals("xperiapro")) { // '12,4,1 Jeremy XPERIA Pro does
                                                                                       // not use this key as @
                    // alt-@ is conflict with symbol input thus altered to shift-@ Jeremy '11,8,15
                    // alt-@ switch to next active keyboard.
                    // nextActiveKeyboard(true);
                    showIMPicker(); // Jeremy '11,8,28 Jeremy '11,8,28
                    mMetaState = LIMEMetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
                    setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState();
                    return true;
                    // Long press physical @ key to swtich chn/eng
                } else if ((!mEnglishOnly || mPredictionOn)
                        && translateKeyDown(keyCode, event)) {
                    return true;
                } else {
                    translateKeyDown(keyCode, event);
                    super.onKeyDown(keyCode, mKeydownEvent);
                }
                break;

            case KeyEvent.KEYCODE_SPACE:
                // Jeremy move the chi/eng switching to on_KEY_UP '11,6,18

                if (!spaceKeyPress && lastKeyCtrl) { // missing space down event when ctrl-space is pressed
                    this.switchChiEng();
                    return true;
                }

                if (hasSpaceProcessed)
                    return true;
            default:

        }
        // Update metakeystate of IC maintained by MetaKeyKeyListerner
        // setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState(); moved to OnKey
        // by jeremy '12,6,13

        if (DEBUG)
            Log.i(TAG, "OnKeyUp():keyCode:" + keyCode
                    + ";hasCtrlPress:" + hasCtrlPress
                    + ";hasWinPress:" + hasWinPress
                    + ", event.getEventTime() -  event.getDownTime()" + (event.getEventTime() - event.getDownTime())
                    + " call super.onKeyUp()");

        return super.onKeyUp(keyCode, event);
    }

    boolean superOnKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    boolean superOnKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Public method to commit raw text directly (e.g., raw keycode from
     * CandidateView).
     * Clears composing state and hides candidate view after committing.
     */
    public void commitTyped(String text) {
        if (text == null || text.isEmpty())
            return;

        if (isTranslationModeActive) {
            translateQuery.insert(translateCursorPosition, text);
            translateCursorPosition += text.length();
            translateCursorPositionState.setValue(translateCursorPosition);
            translateQueryState.setValue(translateQuery.toString());
            performTranslationAsync(translateQuery.toString());
        } else {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(text, 1);
            }
        }

        if (text.length() > 0) {
            String lastChar = text.substring(text.length() - 1);
            if (lastChar.charAt(0) > 127) {
                SearchServer.setLastCommittedChar(lastChar);
            }
        }

        // Clear composing state
        clearComposing(true);
        hideComposingPopup();
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    void commitTyped(InputConnection ic) {
        if (DEBUG)
            Log.i(TAG, "commitTyped()");
        if (selectedCandidate == null)
            return;
        try {
            if ((mComposing.length() > 0 // denotes composing just finished
                    || !selectedCandidate.isComposingCodeRecord()) // commit selected candidate if it is not the
                                                                   // composing text. '15,6,4 Jeremy (like related
                                                                   // phrase or English suggestions)
                    && !LIMEUtilities.isUnicodeSurrogate(selectedCandidate.getWord())) { // check if it's surrogate
                                                                                         // characters (emoji) '15,7,19
                                                                                         // Jeremy

                if (!mEnglishOnly
                        || !selectedCandidate.isComposingCodeRecord()
                        || !selectedCandidate.isEnglishSuggestionRecord()) { // Jeremy '12,4,29 use mEnglishOnly instead
                                                                             // of onIM
                    if (selectedCandidate != null && selectedCandidate.getWord() != null
                            && !selectedCandidate.getWord().equals("")) {

                        int firstMatchedLength = 1;

                        if (selectedCandidate.getCode() == null
                                || selectedCandidate.getCode().equals("")) {
                            firstMatchedLength = 1;
                        }

                        String wordToCommit = selectedCandidate.getWord();

                        if (selectedCandidate != null
                                && selectedCandidate.getCode() != null
                                && selectedCandidate.getWord() != null) {
                            if (selectedCandidate
                                    .getCode()
                                    .toLowerCase(Locale.US)
                                    .equals(selectedCandidate.getWord()
                                            .toLowerCase(Locale.US))) {
                                firstMatchedLength = 1;

                            }
                        }

                        if (DEBUG)
                            Log.i(TAG, "commitTyped() committed Length="
                                    + firstMatchedLength);

                        if (isTranslationModeActive) {
                            final String actualWord = (mLIMEPref.getHanCovertOption() == 0) ? wordToCommit : SearchSrv.hanConvert(wordToCommit);
                            translateQuery.insert(translateCursorPosition, actualWord);
                            translateCursorPosition += actualWord.length();
                            translateCursorPositionState.setValue(translateCursorPosition);
                            translateQueryState.setValue(translateQuery.toString());
                            performTranslationAsync(translateQuery.toString());
                        } else {
                            // Do hanConvert before commit
                            // '10, 4, 17 Jeremy
                            if (mLIMEPref.getHanCovertOption() == 0) {
                                if (ic != null)
                                    ic.commitText(wordToCommit, firstMatchedLength);
                            } else {
                                if (mLIMEPref.getHanConvertNotify()) {

                                    Calendar now = Calendar.getInstance();

                                    long nowvalue = now.getTimeInMillis();
                                    long storevalue = mLIMEPref.getParameterLong("han_notify_interval", 0);

                                    // 1 minute idle time
                                    if (nowvalue - storevalue > 60000) {
                                        if (mLIMEPref.getHanCovertOption() == 1) {
                                            Toast.makeText(this, R.string.han_convert_ts, Toast.LENGTH_SHORT).show();
                                        } else if (mLIMEPref.getHanCovertOption() == 2) {
                                            Toast.makeText(this, R.string.han_convert_st, Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    mLIMEPref.setParameter("han_notify_interval", now.getTimeInMillis());
                                }
                                if (ic != null)
                                    ic.commitText(SearchSrv.hanConvert(wordToCommit), firstMatchedLength);
                            }
                        }

                        if (wordToCommit != null && wordToCommit.length() > 0) {
                            String lastChar = wordToCommit.substring(wordToCommit.length() - 1);
                            if (lastChar.charAt(0) > 127) {
                                SearchServer.setLastCommittedChar(lastChar);
                            }
                        }

                        // Art '30,Sep,2011 when show related then clear composing
                        if (selectedCandidate.isEmojiRecord()
                                || selectedCandidate.isChinesePunctuationSymbolRecord()) {
                            clearComposing(true);
                        }

                        // Jeremy '11,7,28 for continuous typing (LD)
                        // Jeremy '12,6,2 get real committed code length from searchserver
                        boolean composingNotFinish = false;
                        // Jeremy '15,6,2 retrieve real code length with selectedCandidate using exact
                        // code match stack in search server
                        int committedCodeLength = SearchSrv.getRealCodeLength(selectedCandidate, mComposing.toString());

                        if (DEBUG)
                            Log.i(TAG, "commitTyped(): committedCodeLength = " + committedCodeLength);

                        if (mComposing.length() > selectedCandidate.getCode().length()) {
                            composingNotFinish = true;
                        }

                        boolean shouldUpdateCandidates = false;
                        if (composingNotFinish) {
                            if (LDComposingBuffer.length() == 0) {
                                // starting LD process
                                LDComposingBuffer = mComposing.toString();
                                if (DEBUG)
                                    Log.i(TAG, "commitTyped():starting LD process, LDBuffer=" + LDComposingBuffer +
                                            ". just committed code= '" + selectedCandidate.getCode() + "'");
                                SearchSrv.addLDPhrase(selectedCandidate, false);
                            } else {
                                // Continuous LD process
                                if (DEBUG)
                                    Log.i(TAG, "commitTyped():Continuous LD process, LDBuffer='" + LDComposingBuffer +
                                            "'. just committed code=" + selectedCandidate.getCode());
                                SearchSrv.addLDPhrase(selectedCandidate, false);
                            }
                            mComposing = mComposing.delete(0, committedCodeLength);
                            if (DEBUG)
                                Log.i(TAG, "commitTyped(): trimmed mComposing = '" + mComposing + "', " +
                                        "+ mComposing.length = " + mComposing.length());

                            if (!mComposing.toString().equals(" ")) {
                                if (mComposing.toString().startsWith(" "))
                                    mComposing = mComposing.deleteCharAt(0);
                                if (DEBUG)
                                    Log.i(TAG, "commitTyped(): new mComposing:'" + mComposing + "'");
                                if (mComposing.length() > 0) { // Jeremy '12,7,11 only fetch remaining composing when
                                                               // length >0
                                    // if (ic != null && mPredictionOn)
                                    // ic.setComposingText(mComposing, 1);
                                    shouldUpdateCandidates = true;
                                }
                            }
                        } else {

                            if (LDComposingBuffer.length() > 0) {// &&
                                                                 // LDComposingBuffer.contains(mComposing.toString())){
                                // Ending continuous LD process (last of LD process)
                                if (DEBUG)
                                    Log.i(TAG, "commitTyped():Ending LD process, LDBuffer=" + LDComposingBuffer +
                                            ". just committed code=" + selectedCandidate.getCode());
                                LDComposingBuffer = "";
                                SearchSrv.addLDPhrase(selectedCandidate, true);
                            } else if (LDComposingBuffer.length() > 0) {
                                // LD process interrupted.
                                if (DEBUG)
                                    Log.i(TAG, "commitTyped():LD process interrupted, LDBuffer=" + LDComposingBuffer +
                                            ". just committed code=" + selectedCandidate.getCode());
                                LDComposingBuffer = "";
                                SearchSrv.addLDPhrase(null, true);
                            }

                        }

                        // Jeremy '13,1,10 do update score and reverse lookup after updateRelatedPhrase
                        // to shorten the time user see related candidates after select a candidate.
                        if (shouldUpdateCandidates) {
                            updateCandidates();
                        } else {
                            committedCandidate = new Mapping(selectedCandidate);
                            selectedCandidate = null;
                            clearComposing(false);
                            updateRelatedPhrase(false);

                            if (committedCandidate != null && committedCandidate.getWord() != null) {
                                SearchSrv.learnRelatedPhraseAndUpdateScore(committedCandidate);

                                // do reverse lookup and display notification if required.
                                SearchSrv.getCodeListStringFromWord(committedCandidate.getWord());
                            }
                        }

                    } else {
                        if (ic != null)
                            ic.commitText(mComposing,
                                    mComposing.length());

                    }
                } else { // English mode or composing code or English run-time suggestion
                    if (ic != null) {
                        ic.commitText(mComposing, mComposing.length());
                        if (!mEnglishOnly)
                            clearComposing(false);
                    }

                }

            } else if (LIMEUtilities.isUnicodeSurrogate(selectedCandidate.getWord())) { // Jeremy '15,7,16
                ic.commitText(selectedCandidate.getWord(), 1);
                clearComposing(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    public void updateShiftKeyState(EditorInfo attr) {
        if (DEBUG)
            Log.i(TAG, "updateShiftKeyState() ");
        InputConnection ic = getCurrentInputConnection();
        if (attr != null && mInputView != null
                && mKeyboardSwitcher.isAlphabetMode() && ic != null) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (mAutoCap && ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = ic.getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        } else {
            if (!mCapsLock && mHasShift) {
                mKeyboardSwitcher.toggleShift();
                mHasShift = false;
            }
        }

    }

    boolean isValidLetter(int code) {
        return Character.isLetter(code);
    }

    boolean isValidDigit(int code) {
        return Character.isDigit(code);
    }

    boolean isValidSymbol(int code) {
        // code has to < 256, a ascii character
        // Fixed: Simplified logic - exclude letters, digits, and space
        return code < 256 && !Character.isLetter(code)
                && !Character.isDigit(code) && code != 32;
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode, boolean sendToSelf) {
        InputConnection ic = getCurrentInputConnection();

        long eventTime = SystemClock.uptimeMillis();
        KeyEvent downEvent = new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_DOWN, keyEventCode, 0, 0, 0, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE);
        KeyEvent upEvent = new KeyEvent(SystemClock.uptimeMillis(), eventTime,
                KeyEvent.ACTION_UP, keyEventCode, 0, 0, 0, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE);
        if (sendToSelf) { // Jeremy '12,5,23 send to this.onKeyDown and onKeyUp if sendToSelf is true.
            if (!this.onKeyDown(keyEventCode, downEvent) && ic != null)
                ic.sendKeyEvent(downEvent);
            if (!this.onKeyUp(keyEventCode, upEvent) && ic != null)
                ic.sendKeyEvent(upEvent);

        } else if (ic != null) {
            ic.sendKeyEvent(downEvent);
            ic.sendKeyEvent(upEvent);
        }

    }

    public void onKey(int primaryCode, int[] keyCodes) {
        onKey(primaryCode, keyCodes, 0, 0);
    }

    public void onKey(int primaryCode, int[] keyCodes, int x, int y) {
        if (DEBUG)
            Log.i(TAG, "OnKey(): primaryCode:" + primaryCode
                    + " hasShiftPress:" + hasShiftPress);

        // Modified by Art
        // This is to fixed the CapsLock issue on Physical keyboard
        if (DEBUG)
            Log.i(TAG, "onKey() before CapsLock check: primaryCode=" + primaryCode + " mCapsLock=" + mCapsLock);
        if (mCapsLock) {
            if (primaryCode >= 97 && primaryCode <= 122) {
                int oldCode = primaryCode;
                primaryCode -= 32;
                if (DEBUG)
                    Log.i(TAG, "onKey() CapsLock conversion: " + oldCode + " -> " + primaryCode);
            }
        }
        // Adjust metakeystate on printed key pressed.
        if (hasPhysicalKeyPressed) { // Jeremy '12,6,11 moved from handleCharacter()
            mMetaState = LIMEMetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
            setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState(); // Jeremy '12,6,13 moved from OnkeyUP by
                                                                           // Jeremy '12,6,13
            if (DEBUG)
                Log.i(TAG, "onKey(): adjustMetaAfterKeypress()");

        }

        // Jeremy '24,1,7: Explicitly disable English prediction for numbers/symbols
        // and phone mode to prevent unwanted candidate bar suggestions like "Pacific"
        if (mLIMEPref.getEnglishPrediction()
                && primaryCode != LIMEBaseKeyboard.KEYCODE_DELETE
                && !mKeyboardSwitcher.isSymbols()
                && !currentSoftKeyboard.contains("phone")) {

            // Check if input character not valid English Character then reset
            // temp english string
            if (!Character.isLetter(primaryCode) && mEnglishOnly) {

                // Jeremy '11,6,10. Select english suggestion with shift+123457890
                if (hasPhysicalKeyPressed && (mCandidateView != null && hasCandidatesShown)) { // Replace
                                                                                               // isCandidateShown()
                                                                                               // with
                                                                                               // hasCandidatesShown by
                                                                                               // Jeremy '12,5,6
                    if (handleSelkey(primaryCode)) {
                        return;
                    }
                    resetTempEnglishWord();
                    if (!hasCtrlPress)
                        clearSuggestions(); // Jeremy '12,4,29 moved from resetcandidateBar
                }

            }
        }

        // Handle English/Lime Keyboard switch
        if (!mEnglishFlagShift
                && (primaryCode == LIMEBaseKeyboard.KEYCODE_SHIFT)) {
            mEnglishFlagShift = true;
        }
        if (primaryCode == LIMEBaseKeyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == LIMEBaseKeyboard.KEYCODE_SHIFT) {
            if (DEBUG)
                Log.i(TAG, "OnKey():KEYCODE_SHIFT, hasPhysicalKeyPressed=" + hasPhysicalKeyPressed
                        + ", hasDistinctMultitouch=" + hasDistinctMultitouch);
            // Simplified: Always handle shift for software keyboard
            if (DEBUG)
                Log.i(TAG, "OnKey():KEYCODE_SHIFT calling handleShift()");

            // Prevent double-toggle if Shift was already handled in onPress
            if (mShiftHandledInOnPress) {
                mShiftHandledInOnPress = false; // Reset flag and skip duplicate execution
            } else {
                handleShift();
            }
        } else if (primaryCode == LIMEBaseKeyboard.KEYCODE_DONE) {// long press on options and shift
            handleClose();
            // Jeremy '12,5,21 process the arrow keys on soft keyboard
        } else if (primaryCode == LIMEBaseKeyboard.KEYCODE_UP) {
            keyDownUp(KeyEvent.KEYCODE_DPAD_UP, hasCandidatesShown);
        } else if (primaryCode == LIMEBaseKeyboard.KEYCODE_DOWN) {
            keyDownUp(KeyEvent.KEYCODE_DPAD_DOWN, hasCandidatesShown);
        } else if (primaryCode == LIMEBaseKeyboard.KEYCODE_RIGHT) {
            keyDownUp(KeyEvent.KEYCODE_DPAD_RIGHT, hasCandidatesShown);
        } else if (primaryCode == LIMEBaseKeyboard.KEYCODE_LEFT) {
            keyDownUp(KeyEvent.KEYCODE_DPAD_LEFT, hasCandidatesShown);
        } else if (primaryCode == LIMEKeyboardView.KEYCODE_OPTIONS) {
            toggleEmojiVisibility();
        } else if (primaryCode == LIMEKeyboardView.KEYCODE_SPACE_LONGPRESS) {
            // Disable options menu on space longpress per user request
        } else if (primaryCode == LIMEKeyboardView.KEYCODE_SYMBOL_KEYBOARD) {
            mEnglishOnly = true;
            mKeyboardSwitcher.setKeyboardMode(activeIM, LIMEKeyboardSwitcher.MODE_PHONE, mImeOptions, false, false,
                    false);
        } else if (primaryCode == KEYCODE_SWITCH_TO_SYMBOL_MODE && mInputView != null) { // ->symbol keyboard
            switchKeyboard(primaryCode);
        } else if (primaryCode == KEYCODE_SWITCH_SYMBOL_KEYBOARD && mInputView != null) { // ->switch symbols1 keyboards
            switchKeyboard(primaryCode);
        } else if (primaryCode == LIMEKeyboardView.KEYCODE_NEXT_IM) {
            switchToNextActivatedIM(true);
        } else if (primaryCode == LIMEKeyboardView.KEYCODE_PREV_IM) {
            switchToNextActivatedIM(false);
        } else if (primaryCode == KEYCODE_SWITCH_TO_ENGLISH_MODE && mInputView != null) { // chi->eng
            switchKeyboard(primaryCode);
            // Jeremy '11,5,31 Rewrite softkeybaord enter/space and english separator
            // processing.
        } else if (primaryCode == KEYCODE_SWITCH_TO_IM_MODE && mInputView != null) { // eng -> chi
            switchKeyboard(primaryCode);
        } else if (primaryCode == MY_KEYCODE_SPACE && "dayi".equals(activeIM) && mComposing.length() > 0) {
            if (hasCandidatesShown) {
                pickCandidateManually(0);
            } else {
                if (mCandidateList != null && !mCandidateList.isEmpty()) {
                    if (mCandidateList.size() == 1) {
                        pickCandidateManually(0);
                    } else {
                        showCandidateView();
                        hasCandidatesShown = true;
                        try {
                            String selkey = SearchSrv.getSelkey();
                            mCandidateView.setSuggestions(mCandidateList, hasPhysicalKeyPressed, selkey);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return;
        } else if ( // Jeremy '12,7,1 bug fixed on enter not functioning in english mode
        // Space key handling for all modes
        ((primaryCode == MY_KEYCODE_SPACE && (
        // English mode: always send space
        mEnglishOnly
                // Chinese mode with dayi and other non-phonetic input methods: always send
                // space
                || (!mEnglishOnly && !activeIM.equals("phonetic"))
                // Chinese mode with phonetic input method: only send space when composing is
                // empty or ends with space
                || (!mEnglishOnly && activeIM.equals("phonetic")
                        && (mComposing.toString().endsWith(" ") || mComposing.length() == 0))))
                || primaryCode == MY_KEYCODE_ENTER)) {

            if (hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                if (!pickHighlightedCandidate()) {// Jeremy '12,5,11 fixed for not sedning related.
                    if (mComposing.length() == 0)
                        hideCandidateView();
                    sendKeyChar((char) primaryCode);

                }

            } else {
                sendKeyChar((char) primaryCode);
            }

        } else {

            handleCharacter(primaryCode);

            // Art 11, 9, 26 Check if need to auto commit composing
            if (auto_commit > 0 && !mEnglishOnly) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
                if (mComposing != null && mComposing.length() == auto_commit &&
                        currentSoftKeyboard != null && currentSoftKeyboard.contains("phone")) {
                    InputConnection ic = getCurrentInputConnection();
                    commitTyped(ic);

                }
            }
        }
    }

    /**
     * Add by Jeremy '10, 3, 24 for options menu in soft keyboard
     */

    private void handleOptions() {
        if (DEBUG)
            Log.i(TAG, "handleOptions()");
        MaterialAlertDialogBuilder builder;

        // Wrap the service context with the app theme to satisfy
        // MaterialAlertDialogBuilder requirements
        // Jeremy '24,1,7: Fix for crash "The style on this component requires your app
        // theme to be Theme.AppCompat"
        ContextThemeWrapper themedContext = new ContextThemeWrapper(this, R.style.AppTheme);
        builder = new MaterialAlertDialogBuilder(themedContext);

        builder.setCancelable(true);
        builder.setIcon(R.drawable.sym_keyboard_done_white);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setTitle(getResources().getString(R.string.ime_name));

        CharSequence itemSettings = getString(R.string.lime_setting_preference);
        CharSequence hanConvert = getString(R.string.han_convert_option_list);

        CharSequence itemSwitchIM = getString(R.string.keyboard_list);
        CharSequence itemSwitchSytemIM = getString(R.string.input_method);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int displayWidth = dm.widthPixels;
        int displayHeight = dm.heightPixels;
        final boolean isLandScape = displayWidth > displayHeight;

        CharSequence itemSplitKeyboard = getString(R.string.split_keyboard);
        if ((mSplitKeyboard == LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY && isLandScape)
                || mSplitKeyboard == LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS)
            itemSplitKeyboard = getString(R.string.merge_keyboard);

        CharSequence[] options;


        final boolean hasSplitOption;

        // Jeremy '12,5,27 do not show split/merge keyboard option if in landscape mode
        // and show arrow keys is on
        if (isLandScape && mShowArrowKeys > 0) {
            hasSplitOption = false;
            options = new CharSequence[] { itemSettings, hanConvert, itemSwitchIM, itemSwitchSytemIM };
        } else {
            hasSplitOption = true;
            options = new CharSequence[] { itemSettings, hanConvert, itemSwitchIM, itemSwitchSytemIM,
                    itemSplitKeyboard };

        }

        builder.setItems(options, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface di, int position) {
                di.dismiss();
                switch (position) {

                    case POS_SETTINGS:
                        launchSettings();
                        break;
                    case POS_HANCONVERT: // Jeremy '11,9,17
                        showHanConvertPicker();
                        break;
                    case POS_KEYBOARD:
                        showIMPicker();
                        break;
                    case POS_METHOD:
                        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();
                        break;
                    case POS_SPLIT_KEYBOARD: // Jeremy '12,5,27 new option to split keyboard; '12,6,9 add orientation
                                             // consideration on split keyboard
                        if (hasSplitOption) {
                            if (mSplitKeyboard == LIMEKeyboard.SPLIT_KEYBOARD_NEVER) {
                                if (isLandScape)
                                    mLIMEPref.setSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY);
                                else
                                    mLIMEPref.setSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS);
                            } else if (mSplitKeyboard == LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS) {
                                if (isLandScape)
                                    mLIMEPref.setSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_NEVER);
                                else
                                    mLIMEPref.setSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY);
                            } else {// LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY
                                if (isLandScape)
                                    mLIMEPref.setSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_NEVER);
                                else
                                    mLIMEPref.setSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS);
                            }

                            handleClose();
                            mKeyboardSwitcher.resetKeyboards(true);
                            break;
                        }

                }
            }
        });

        mOptionsDialog = builder.create();
        Window window = mOptionsDialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = mInputView.getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mOptionsDialog.show();
    }

    private void launchSettings() {
        handleClose();
        Intent intent = new Intent();
        intent.setClass(LIMEService.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Display the active IM name in the composing-text popup (字根區) for 1.5 seconds.
     * The composing popup is more prominent and natural than Toast (which hides behind
     * the keyboard panel in IME service context on Samsung Android 16).
     */
    private void showIMSwitchNotification(String imName) {
        if (imName == null || imName.isEmpty()) return;

        initComposingPopup();
        showComposingPopup(imName);

        // Auto-hide after 1.5 s, but only if user has not started composing
        mMainHandler.postDelayed(() -> {
            if (mComposing.length() == 0) {
                hideComposingPopup();
            }
        }, 1500);
    }

    // Package-private so PhysicalKeyHandler can invoke IM cycling via Ctrl+`
    void switchToNextActivatedIM(boolean forward) { // forward: true, next IM; false prev. IM
        if (DEBUG)
            Log.i(TAG, "switchToNextActivatedIM()");

        // Delegate IM switching to helper
        String activeIMName = mIMSwitchHelper.switchToNextActivatedIM(forward);

        if (activeIMName.isEmpty()) {
            return; // No activated IMs
        }

        // Sync activeIM from helper
        activeIM = mIMSwitchHelper.getActiveIM();
        buildActivatedIMList(); // Sync lists

        // UI actions remain in LIMEService
        clearComposing(false);
        mEnglishOnly = false;
        mLIMEPref.setLanguageMode(false);
        initialIMKeyboard();
        // Show the newly active IM name in the candidate bar for 1.5 s.
        // Toast is unreliable in IME service context on Samsung Android 16
        // (the keyboard panel occludes the bottom-of-screen toast).
        showIMSwitchNotification(activeIMName);

        try {
            mKeyboardSwitcher.setKeyboardList(SearchSrv.getKeyboardList());
            mKeyboardSwitcher.setImList(SearchSrv.getImList());
        } catch (Exception e) {
            Log.e(TAG, "Error updating keyboard list: " + e.getMessage());
        }

        currentSoftKeyboard = mKeyboardSwitcher.getImKeyboard(activeIM);
    }

    private void buildActivatedIMList() {
        // Delegate to helper and sync local lists for backward compatibility
        mIMSwitchHelper.buildActivatedIMList();

        // Sync local lists from helper (for other methods that still use them directly)
        activatedIMNameList.clear();
        activatedIMNameList.addAll(mIMSwitchHelper.getActivatedIMNameList());

        activatedIMShortNameList.clear();
        activatedIMShortNameList.addAll(mIMSwitchHelper.getActivatedIMShortNameList());

        activatedIMList.clear();
        activatedIMList.addAll(mIMSwitchHelper.getActivatedIMList());

        activeIM = mIMSwitchHelper.getActiveIM();

        if (DEBUG)
            Log.i(TAG, "buildActivatedIMList(): delegated to helper, activeIM=" + activeIM);
    }

    /**
     * Add by Jeremy '11,9,17 for han convert (tranditional <-> simplifed) options
     */
    private void showHanConvertPicker() {
        MaterialAlertDialogBuilder builder;

        builder = new MaterialAlertDialogBuilder(this);

        builder.setCancelable(true);
        builder.setIcon(R.drawable.sym_keyboard_done_white);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setTitle(getResources().getString(R.string.han_convert_option_list));
        CharSequence[] items = getResources().getStringArray(R.array.han_convert_options);
        builder.setSingleChoiceItems(items, mLIMEPref.getHanCovertOption(),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface di, int position) {
                        di.dismiss();
                        handleHanConvertSelection(position);
                    }
                });

        mOptionsDialog = builder.create();
        Window window = mOptionsDialog.getWindow();
        if (!(window == null)) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.token = mCandidateViewStandAlone.getWindowToken(); // Jeremy 12,5,4 it's always there
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
            window.setAttributes(lp);
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        }
        mOptionsDialog.show();
    }

    private void handleHanConvertSelection(int position) {
        mLIMEPref.setHanCovertOption(position);

    }

    /**
     * Add by Jeremy '10, 3, 24 for IM picker menu in options menu
     * renamed to showIMPicker from showKeybaordPicer to avoid confusion '12,3,40
     */
    void showIMPicker() {
        if (DEBUG)
            Log.i(TAG, "showIMPicker()");
        buildActivatedIMList();

        // MaterialAlertDialogBuilder requires an AppCompat theme context, but the IME
        // service context does not carry one — wrapping it fixes the crash.
        android.view.ContextThemeWrapper themedCtx = new android.view.ContextThemeWrapper(
                this, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog);
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(themedCtx);

        builder.setCancelable(true);
        builder.setIcon(R.drawable.sym_keyboard_done_white);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setTitle(getResources().getString(R.string.keyboard_list));

        CharSequence[] items = new CharSequence[activatedIMNameList.size()];// =
        // getResources().getStringArray(R.array.keyboard);
        int curKB = 0;
        for (int i = 0; i < activatedIMNameList.size(); i++) {
            items[i] = activatedIMNameList.get(i);
            if (activeIM.equals(activatedIMList.get(i)))
                curKB = i;
        }

        builder.setSingleChoiceItems(items, curKB,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface di, int position) {
                        di.dismiss();
                        handleIMSelection(position);
                    }
                });

        mOptionsDialog = builder.create();
        Window window = mOptionsDialog.getWindow();
        // Jeremy '10, 4, 12
        // The IM is not initialialized. do nothing here if window=null.
        if (!(window == null)) {
            WindowManager.LayoutParams lp = window.getAttributes();
            // Jeremy '11,8,28 Use candidate instead of mInputview because mInputView may
            // not present when using physical keyboard
            lp.token = mCandidateViewStandAlone.getWindowToken(); // always there Jeremy '12,5,4
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
            window.setAttributes(lp);
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        }
        mOptionsDialog.show();

    }

    private void handleIMSelection(int position) {
        if (DEBUG)
            Log.i(TAG, "handleIMSelection() position = " + position);

        // Delegate selection to helper
        mIMSwitchHelper.selectIMByPosition(position);
        activeIM = mIMSwitchHelper.getActiveIM();

        // UI actions remain in LIMEService
        if (!mEnglishOnly)
            clearComposing(true);

        mEnglishOnly = false;
        initialIMKeyboard();

        try {
            mKeyboardSwitcher.setKeyboardList(SearchSrv.getKeyboardList());
            mKeyboardSwitcher.setImList(SearchSrv.getImList());
            currentSoftKeyboard = mKeyboardSwitcher.getImKeyboard(activeIM);
        } catch (Exception e) {
            Log.e(TAG, "Error updating keyboard list: " + e.getMessage());
        }
    }

    public void onText(CharSequence text) {
        if (DEBUG)
            Log.i(TAG, "OnText()");
        InputConnection ic = getCurrentInputConnection();
        if (ic == null)
            return;
        ic.beginBatchEdit();

        if (mPredicting) {
            commitTyped(ic);
            // mJustRevertedSeparator = null;
        } else if (!mEnglishOnly && mComposing.length() > 0) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
            if (!pickHighlightedCandidate()) {
                // If no candidate is highlighted, pick the first one by default
                // to commit the Chinese characters before the emoji
                if (mCandidateList != null && mCandidateList.size() > 0) {
                    pickCandidateManually(0);
                } else {
                    // Fallback: commit raw composing text
                    commitTyped(mComposing.toString());
                }
            }
            // commitTyped(ic);
        }
        ic.commitText(text, 1);
        // ic.commitText(text, 0);

        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    void updateCandidates() {
        this.updateCandidates(false);
    }

    void updateChineseSymbol() {
        // ChineseSymbol chineseSym = new ChineseSymbol();
        hasChineseSymbolCandidatesShown = true;
        List<Mapping> list = ChineseSymbol.getChineseSymoblList();
        if (list.size() > 0) {

            // Setup sel key display if
            String selkey = "1234567890";
            if (disable_physical_selection && hasPhysicalKeyPressed) {
                selkey = "";
            }

            setSuggestions(list, hasPhysicalKeyPressed, selkey);

            if (DEBUG)
                Log.i(TAG, "updateChineseSymbol():"
                        + "mCandidateList.size:" + mCandidateList.size());
        }

    }

    /**
     * Update the list of available candidates from the current composing text.
     * This will need to be filled in by however you are determining candidates.
     */
    public void updateCandidates(final boolean getAllRecords) {

        if (DEBUG)
            Log.i(TAG, "updateCandidate():Update Candidate mComposing:" + mComposing);

        hasChineseSymbolCandidatesShown = false;

        if (mComposing.length() > 0) {

            final LinkedList<Mapping> list = new LinkedList<>();

            String keyString = mComposing.toString();

            // getComposingDisplayString(keyString) updates the CandidateView and Floating Composing Popup.
            // We no longer set the composing text in the input connection (ic.setComposingText) to prevent
            // intermediate radicals/composing text from being injected into the host application's editor.
            getComposingDisplayString(keyString);

            // Art '30,Sep,2011 restrict the length of composing text for Stroke5
            /*
            if (currentSoftKeyboard.contains("wb")) {
                if (keyString.length() > 5) {
                    keyString = keyString.substring(0, 5);
                    mComposing = new StringBuilder();
                    mComposing.append(keyString);
                    // InputConnection ic = getCurrentInputConnection();
                    // if (ic != null && mPredictionOn)
                    // ic.setComposingText(getComposingDisplayString(keyString), 1);
                    // Just update CandidateView composing text
                    getComposingDisplayString(keyString);
                }
            }
            */

            final String finalKeyString = keyString;
            final boolean finalHasPhysicalKeyPressed = hasPhysicalKeyPressed;
            if (queryFuture != null) queryFuture.cancel(true);
            queryFuture = queryExecutor.submit(() -> {

                    try {
                        list.addAll(
                                SearchSrv.getMappingByCode(finalKeyString, !finalHasPhysicalKeyPressed, getAllRecords));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Filter out the raw code (composing text) if it appears as a candidate
                    // This fixes the issue where ASCII code like "nh1" shows up as the first candidate
                    if (list.size() > 0) {
                        java.util.Iterator<Mapping> iterator = list.iterator();
                        while (iterator.hasNext()) {
                            Mapping m = iterator.next();
                            String word = m.getWord();
                            // Only remove if it's an exact ASCII match to the code
                            // We don't want to remove actual Chinese characters that might match the code
                            if (word != null && word.matches("[A-Za-z0-9]+") && word.equalsIgnoreCase(finalKeyString)) {
                                iterator.remove();
                            }
                        }
                    }
                    // Exit early if a newer query has already been started
                    if (Thread.currentThread().isInterrupted()) return;

                    // Setup selection keys
                    String selkey = null;
                    if (disable_physical_selection && finalHasPhysicalKeyPressed) {
                        selkey = "";
                    } else {
                        try {
                            selkey = SearchSrv.getSelkey();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        String mixedModeSelkey = "`";
                        if (hasSymbolMapping && !activeIM.equals("dayi")
                                && !(activeIM.equals("phonetic")
                                        && mLIMEPref.getPhoneticKeyboardType().equals("standard"))) {
                            mixedModeSelkey = " ";
                        }

                        int selkeyOption = mLIMEPref.getSelkeyOption();
                        if (selkeyOption == 1)
                            selkey = mixedModeSelkey + selkey;
                        else if (selkeyOption == 2)
                            selkey = mixedModeSelkey + " " + selkey;
                    }

                    // Emoji Control - Always try to add emojis if enabled
                    if (mLIMEPref.getEmojiMode() && !finalHasPhysicalKeyPressed) {
                        HashMap<String, String> emojiCheck = new HashMap<>();
                        List<Mapping> emojiList = new LinkedList<>();

                        // 1. Try emoji tags for the raw code/composing text first (English/Tag matching)
                        if (!finalKeyString.isEmpty()) {
                            List<Mapping> tagResults = SearchSrv.emojiConvert(finalKeyString, Lime.EMOJI_EN);
                            if (tagResults != null) {
                                for (Mapping m : tagResults) {
                                    if (!emojiCheck.containsKey(m.getWord())) {
                                        emojiList.add(m);
                                        emojiCheck.put(m.getWord(), m.getWord());
                                    }
                                }
                            }
                        }

                        // 2. Try emoji tags for top predicted Chinese words
                        for (int i = 0; i < Math.min(list.size(), 2); i++) {
                            String word = list.get(i).getWord();
                            if (word == null || word.isEmpty()) continue;
                            
                            // Don't repeat search if word is same as composing code (handled above)
                            if (word.equalsIgnoreCase(finalKeyString)) continue;

                            for (int type : new int[]{Lime.EMOJI_TW, Lime.EMOJI_CN, Lime.EMOJI_EN}) {
                                List<Mapping> wordResults = SearchSrv.emojiConvert(word, type);
                                if (wordResults != null) {
                                    for (Mapping m : wordResults) {
                                        if (!emojiCheck.containsKey(m.getWord())) {
                                            emojiList.add(m);
                                            emojiCheck.put(m.getWord(), m.getWord());
                                        }
                                    }
                                }
                            }
                        }

                        if (emojiList.size() > 0) {
                            int insertPosition = mLIMEPref.getEmojiDisplayPosition();
                            if (list.size() <= insertPosition) {
                                insertPosition = list.size();
                            }
                            list.addAll(insertPosition, emojiList);
                        }
                    }

                    if (list.size() > 0) {
                        setSuggestions(list, finalHasPhysicalKeyPressed, selkey);
                        if (DEBUG)
                            Log.i(TAG, "updateCandidates(): display selkey:" + selkey
                                    + ", list.size:" + list.size()
                                    + ", mComposing = " + mComposing);
                    } else {
                        clearSuggestions();
                    }

                    // Show composing window if keyToKeyname got different string. Revised by Jeremy
                    // '11,6,4
                    if (SearchSrv.getTablename() != null) {
                        String keynameString = SearchSrv.keyToKeyname(finalKeyString); // .toLowerCase(Locale.US));
                                                                                       // moved to LimeDB
                        if (mCandidateView != null
                                && !keynameString.toUpperCase(Locale.US).equals(finalKeyString.toUpperCase(Locale.US))
                                && !keynameString.trim().equals("")) {
                            try {
                                Thread.sleep(0);
                            } catch (InterruptedException ignored) {
                                ignored.printStackTrace();
                                return; // terminate thread here, since it is interrupted and more recent
                                        // getMappingByCode will update the suggestions.
                            }
                            showComposingPopup(keynameString);
                        }
                    }
            });

        } else
            // Jermy '11,8,14
            clearSuggestions();
    }

    /*
     * Update English suggestions view
     */
    void updateEnglishPrediction() {

        hasChineseSymbolCandidatesShown = false;
        if (mPredictionOn && mLIMEPref.getEnglishPrediction()) {

            try {

                final LinkedList<Mapping> list = new LinkedList<>();

                if (tempEnglishWord == null || tempEnglishWord.length() == 0) {
                    // Jeremy '11,8,14
                    clearSuggestions();
                } else {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic == null)
                        return;
                    boolean after = false;
                    try {
                        if (ic.getTextAfterCursor(1, 1).length() > 0) {
                            char c = ic.getTextAfterCursor(1, 1).charAt(0);
                            if (!Character.isLetterOrDigit(c)) {
                                after = true;
                            }
                        } else {
                            after = true;
                        }
                    } catch (StringIndexOutOfBoundsException e) {
                        e.printStackTrace();
                        after = true;
                    }

                    boolean matchedtemp = false;

                    if (tempEnglishWord.length() > 0) {
                        try {
                            if (tempEnglishWord.toString()
                                    .equalsIgnoreCase(
                                            ic.getTextBeforeCursor(
                                                    tempEnglishWord.toString()
                                                            .length(),
                                                    1)
                                                    .toString())) {
                                matchedtemp = true;
                            }
                        } catch (StringIndexOutOfBoundsException ignored) {
                            ignored.printStackTrace();
                        }
                    }

                    if (after || matchedtemp) {

                        tempEnglishList.clear();

                        final boolean finalHasPhysicalKeyPressed = hasPhysicalKeyPressed;
                        if (queryFuture != null) queryFuture.cancel(true);
                        queryFuture = queryExecutor.submit(() -> {
                                final Mapping self = new Mapping();
                                self.setWord(tempEnglishWord.toString());
                                self.setComposingCodeRecord();

                                List<Mapping> suggestions = null;
                                try {
                                    suggestions = SearchSrv.getEnglishSuggestions(tempEnglishWord.toString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                try {
                                    Thread.sleep(0);
                                } catch (InterruptedException ignored) {
                                    ignored.printStackTrace();
                                    return; // terminate thread here, since it is interrupted and more recent
                                            // getMappingByCode will update the suggestions.
                                }

                                if ((suggestions != null ? suggestions.size() : 0) > 0) {
                                    list.add(self);
                                    assert suggestions != null;
                                    list.addAll(suggestions);

                                    // Setup sel key display if
                                    String selkey = "1234567890";
                                    if (disable_physical_selection && finalHasPhysicalKeyPressed) {
                                        selkey = "";
                                    }
                                    try {
                                        Thread.sleep(0);
                                    } catch (InterruptedException ignored) {
                                        ignored.printStackTrace();
                                        return; // terminate thread here, since it is interrupted and more recent
                                                // getMappingByCode will update the suggestions.
                                    }

                                    // Emoji Control
                                    // Check the Emoji parameter setting and load icons into the suggestions list
                                    if (mLIMEPref.getEmojiMode()) {
                                        HashMap<String, String> emojiCheck = new HashMap<>();
                                        List<Mapping> emojiList = new LinkedList<>();

                                        if (list.size() > 0) {

                                            List<Mapping> item1;
                                            int insertPosition = mLIMEPref.getEmojiDisplayPosition();
                                            if (list.size() <= insertPosition) {
                                                insertPosition = list.size();
                                            }

                                            item1 = SearchSrv.emojiConvert(list.get(0).getWord(), Lime.EMOJI_EN);
                                            if (item1.size() > 0) {
                                                for (Mapping m : item1) {
                                                    if (emojiCheck.get(m.getWord()) == null) {
                                                        emojiList.add(m);
                                                        emojiCheck.put(m.getWord(), m.getWord());
                                                    }
                                                }
                                            }

                                            if (emojiList.size() > 0) {
                                                list.addAll(insertPosition, emojiList);
                                            }
                                        }
                                    }

                                    // Log.i("EMOJIbefore:", tempEnglishList.size() + "");
                                    tempEnglishList.addAll(list);
                                    setSuggestions(list, finalHasPhysicalKeyPressed, selkey);

                                    // Log.i("EMOJIafter:", tempEnglishList.size() + "");

                                } else {
                                    // Jermy '11,8,14
                                    clearSuggestions();
                                }
                        });
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.i("ART", "Error to update English predication");
            }
        }
    }

    /*
     * Update dictionary view
     */
    void updateRelatedPhrase(final boolean getAllRecords) {
        if (DEBUG)
            Log.i(TAG, "updateRelatedPhrase()");
        hasChineseSymbolCandidatesShown = false;
        // Also use this to control whether need to display the english
        // suggestions words.

        // If there is no Temp Matched word exist then not to display dictionary
        // Modified by Jeremy '10, 4,1. getCode -> getWord
        // if( tempMatched != null && tempMatched.getCode() != null &&
        // !tempMatched.getCode().equals("")){
        if (committedCandidate != null && committedCandidate.getWord() != null
                && !committedCandidate.getWord().equals("")) {

            final boolean finalHasPhysicalKeyPressed = hasPhysicalKeyPressed;
            if (queryFuture != null) queryFuture.cancel(true);
            queryFuture = queryExecutor.submit(() -> {

                    LinkedList<Mapping> list = new LinkedList<>();
                    // Jeremy '11,8,9 Insert completion suggestions from application
                    // in front of related dictionary list in full-screen mode
                    if (mCompletionOn) {
                        list.addAll(buildCompletionList());
                    }

                    if (committedCandidate != null && hasMappingList) {
                        try {
                            if (!committedCandidate.isEmojiRecord()
                                    && !committedCandidate.isChinesePunctuationSymbolRecord()) {
                                list.addAll(SearchSrv.getRelatedPhrase(committedCandidate.getWord(), getAllRecords));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // Exit early if a newer query has already been started
                        if (Thread.currentThread().isInterrupted()) return;

                        if (list.size() > 0) {

                            // Setup sel key display if
                            String selkey = "1234567890";
                            if (disable_physical_selection && finalHasPhysicalKeyPressed) {
                                selkey = "";
                            }

                            setSuggestions(list, finalHasPhysicalKeyPressed && !isFullscreenMode(), selkey);
                        } else {
                            committedCandidate = null;
                            // Jermy '11,8,14
                            clearSuggestions();
                        }
                    }
            });
        }

    }

    private List<Mapping> buildCompletionList() {
        LinkedList<Mapping> list = new LinkedList<>();
        for (int i = 0; i < (mCompletions != null ? mCompletions.length : 0); i++) {
            CompletionInfo ci = mCompletions[i];
            if (ci != null) {
                Mapping temp = new Mapping();
                temp.setWord(ci.getText().toString());
                temp.setCode("");
                temp.setCompletionSuggestionRecord();
                list.add(temp);
            }
        }
        return list;
    }

    private void initCandidateView() {
        if (DEBUG)
            Log.i(TAG, "initCandidateView()");

        mCandidateViewHandler.showCandidateView();
        mCandidateViewHandler.hideCandidateView();
    }

    void showCandidateView() {
        if (DEBUG)
            Log.i(TAG, "showCandidateView()");
        if (hasPhysicalKeyPressed) {
            requestShowSelf(0);
        }

        Configuration config = getResources().getConfiguration();
        boolean isPhysicalKeyboardConnected = config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
        boolean useFixedMode = mFixedCandidateViewOn || isPhysicalKeyboardConnected;

        if (!useFixedMode) {
            mCandidateViewHandler.showCandidateView();
        }
    }

    void hideCandidateView() {
        if (DEBUG)
            Log.i(TAG, "hideCandidateView()");
        if (mCandidateView != null)
            mCandidateView.clear();
        hasCandidatesShown = false;
        hasChineseSymbolCandidatesShown = false;
        if (mCandidateViewStandAlone == null || (!mCandidateViewStandAlone.isShown()))
            return; // escape if mCandidateViewStandAlone is not created or it's not shown '12,5,6,
                    // Jeremy

        mCandidateViewHandler.hideCandidateViewDelayed(DELAY_BEFORE_HIDE_CANDIDATE_VIEW);

    }

    private void forceHideCandidateView() {
        if (DEBUG)
            Log.i(TAG, "forceHideCandidateView()");

        if (mComposing != null && mComposing.length() > 0)
            mComposing.setLength(0);

        selectedCandidate = null;
        // selectedIndex = 0;

        if (mCandidateList != null)
            mCandidateList.clear();

        if (mFixedCandidateViewOn) {
            mCandidateViewInInputView.forceHide();
        } else {
            hideCandidateView();
        }
    }

    public void setSuggestions(List<Mapping> suggestions, boolean showNumber, String diplaySelkey) {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            mMainHandler.post(() -> setSuggestions(suggestions, showNumber, diplaySelkey));
            return;
        }

        // Dayi 3-code Logic: Full-code auto-trigger or delay show
        if ("dayi".equals(activeIM)) {
            // Count actual Chinese characters (ignore emojis and raw code for auto-trigger logic)
            int charCount = 0;
            Mapping uniqueMatch = null;
            if (suggestions != null) {
                for (Mapping m : suggestions) {
                    if (!m.isEmojiRecord() && !m.isComposingCodeRecord()) {
                        charCount++;
                        uniqueMatch = m;
                    }
                }
            }

            if (charCount == 1 && mComposing.length() == 3) {
                pickCandidateManually(suggestions.indexOf(uniqueMatch));
                return;
            }

            // Removed delay show logic to ensure characters appear as user types
        }

        if (suggestions != null && suggestions.size() > 0) {

            if (DEBUG)
                Log.i(TAG, "setSuggestion():suggestions.size=" + suggestions.size()
                        + ", mComposing = " + mComposing
                        + ", mFixedCandidateViewOn:" + mFixedCandidateViewOn
                        + ", hasPhysicalKeyPressed:" + hasPhysicalKeyPressed);

            Configuration config = getResources().getConfiguration();
            boolean isPhysicalKeyboardConnected = config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;

            if (isPhysicalKeyboardConnected) {
                // When physical keyboard is connected, we use the candidate view inside InputView
                // which is forced by updateInputViewContainer()
                if (mCandidateViewInInputView != null) {
                    mCandidateView = mCandidateViewInInputView;
                    if (mCandidateViewStandAlone != null) mCandidateViewStandAlone.clear();
                }
                
                // Ensure IME window is shown but don't force separate candidate window
                requestShowSelf(0);
                setCandidatesViewShown(false); 
            } else if (!mFixedCandidateViewOn && mCandidateView != mCandidateViewStandAlone) {
                mCandidateViewInInputView.clear();
                mCandidateView = mCandidateViewStandAlone;
            } else if (mFixedCandidateViewOn && mCandidateView != mCandidateViewInInputView) {
                mCandidateViewStandAlone.clear();
                hideCandidateView();
                mCandidateView = mCandidateViewInInputView;
                if (mCandidateViewStandAlone != null)
                    mCandidateViewStandAlone.setEmbeddedComposingView(null);
            }

            showCandidateView();

            hasCandidatesShown = true; // Jeremy '15,6,1 move after hideCandidateView if candidateView is fixed.
            hasMappingList = true;

            if (mCandidateView != null) {
                mCandidateList = (LinkedList<Mapping>) suggestions;
                try {

                    if (suggestions.size() > 1 && suggestions.get(1).isExactMatchToCodeRecord()) {
                        selectedCandidate = suggestions.get(1);
                        // selectedIndex = 1;
                        // this is for no exact match condition with code. //do not set default
                        // suggestion for other record type like chinese punctuation symbols1 or related
                        // phrases. Jeremy '15,6,4
                    } else if (suggestions.size() > 0) {
                        selectedCandidate = suggestions.get(0);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mCandidateView.setSuggestions(suggestions, showNumber, diplaySelkey);
                if (DEBUG)
                    Log.i(TAG, "setSuggestion(): mCandidateList.size: " + mCandidateList.size()
                            + ", mComposing = " + mComposing);
            }
        } else {
            if (DEBUG)
                Log.i(TAG, "setSuggestion() with list=null");
            hasMappingList = false;
            // Jeremy '11,8,15
            clearSuggestions();
        }

    }

    /**
     * Helper to map raw keys to keyboard labels (roots) for display
     */
    String getComposingDisplayString(String rawString) {
        StringBuilder sb = new StringBuilder();

        List<LIMEBaseKeyboard.Key> keys = null;
        if (mInputView != null && mInputView.getKeyboard() != null) {
            keys = mInputView.getKeyboard().getKeys();
        }

        // DEBUG LOG
        // Log.e(TAG, "DEBUG: getComposingDisplayString input='" + rawString + "'
        // activeIM='" + activeIM + "'");

        for (int i = 0; i < rawString.length(); i++) {
            char c = rawString.charAt(i);
            boolean puncMatched = false;

            // Try visual mapping first (if available and valid)
            if (keys != null) {
                boolean found = false;
                // First pass: exact match
                for (LIMEBaseKeyboard.Key k : keys) {
                    if (k.codes != null && k.codes.length > 0 && k.codes[0] == c) {
                        // Use label ONLY if it is not ASCII (assumed to be a Root)
                        // OR if we are in English mode (but here we usually want roots)
                        if (k.label != null && k.label.length() > 0 && k.label.charAt(0) > 127) {
                            sb.append(k.label);
                            found = true;
                        }
                        break;
                    }
                }
                // Second pass: case-insensitive
                if (!found) {
                    int lower = Character.toLowerCase(c);
                    int upper = Character.toUpperCase(c);
                    if (lower != c || upper != c) {
                        for (LIMEBaseKeyboard.Key k : keys) {
                            if (k.codes != null && k.codes.length > 0) {
                                int code = k.codes[0];
                                if (code == lower || code == upper) {
                                    if (k.label != null && k.label.length() > 0 && k.label.charAt(0) > 127) {
                                        sb.append(k.label);
                                        found = true;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
                if (found)
                    continue;
            }

            // Fallback to RootMapper
            char mapped = RootMapper.getRoot(activeIM, c);
            if (mapped == c && c != ' ') {
                // Log.e(TAG, "DEBUG: RootMapper returned same char for '" + c + "' (activeIM="
                // + activeIM + ")");
            } else {
                // Log.e(TAG, "DEBUG: RootMapper mapped '" + c + "' -> '" + mapped + "'");
            }
            sb.append(mapped);
        }
        String result = sb.toString();
        // Log.e(TAG, "DEBUG: Display String Result: " + result);

        // Update composing text display (now using floating popup)
        showComposingPopup(result);

        // Pass raw keycode to CandidateView for display as first item
        if (mCandidateView != null) {
            mCandidateView.setRawKeycode(rawString);
        }

        // Clear composing text from input field (we display it in CandidateView
        // instead)
        // Use BatchEdit to ensure atomic update.
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.beginBatchEdit();
            ic.setComposingText("", 0);
            ic.endBatchEdit();
        }

        return result;
    }

    private void handleBackspace() {
        if (DEBUG)
            Log.i(TAG, "handleBackspace() mComposing='" + mComposing + "', len=" + mComposing.length());

        final int length = mComposing.length();
        InputConnection ic = getCurrentInputConnection();

        if (length >= 1) {
            mComposing.delete(length - 1, length);

            if (mComposing.length() == 0) {
                // Composing became empty
                if (ic != null) {
                    // Set empty composing text but DO NOT finish composition yet
                    ic.setComposingText("", 1);
                }
                // Clear candidates
                if (mCandidateView != null) {
                    mCandidateView.clear();
                }
                clearSuggestions(); // Clears internal lists
                hasCandidatesShown = false; // Reset flag
            } else {
                // Composing still has text
                // if (ic != null)
                // ic.setComposingText(getComposingDisplayString(mComposing.toString()), 1);
                // Just update CandidateView composing text
                getComposingDisplayString(mComposing.toString());
                updateCandidates();
            }
        } else if (hasCandidatesShown) {
            hideCandidateView();
        } else {
            // No composing text - send backspace to editor
            if (isTranslationModeActive && translateCursorPosition > 0) {
                translateQuery.deleteCharAt(translateCursorPosition - 1);
                translateCursorPosition--;
                translateCursorPositionState.setValue(translateCursorPosition);
                translateQueryState.setValue(translateQuery.toString());
                performTranslationAsync(translateQuery.toString());
            } else {
                try {
                    if (mEnglishOnly && mLIMEPref.getEnglishPrediction() && mPredictionOn
                            && (!hasPhysicalKeyPressed || mLIMEPref.getEnglishPredictionOnPhysicalKeyboard())) {
                        if (tempEnglishWord != null && tempEnglishWord.length() > 0) {
                            tempEnglishWord.deleteCharAt(tempEnglishWord.length() - 1);
                            updateEnglishPrediction();
                        }
                    }

                    if (ic != null) {
                    // Get a larger chunk of text to properly handle multi-codepoint emojis
                    CharSequence before = ic.getTextBeforeCursor(32, 0);

                    if (before != null && before.length() > 0) {
                        // Use BreakIterator to find the previous grapheme cluster boundary
                        // This correctly handles multi-codepoint emojis (skin tones, ZWJ sequences,
                        // etc.)
                        java.text.BreakIterator breakIterator = java.text.BreakIterator.getCharacterInstance();
                        breakIterator.setText(before.toString());

                        // Move to the end of the text
                        int end = before.length();

                        // Find the previous character boundary
                        int start = breakIterator.preceding(end);
                        if (start == java.text.BreakIterator.DONE) {
                            start = 0;
                        }

                        // Delete from start to end (the entire grapheme cluster)
                        int deleteCount = end - start;
                        if (deleteCount > 0) {
                            ic.deleteSurroundingText(deleteCount, 0);
                        } else {
                            // Fallback to simple delete
                            ic.deleteSurroundingText(1, 0);
                        }
                    } else {
                        keyDownUp(KeyEvent.KEYCODE_DEL, false);
                    }
                } else {
                    keyDownUp(KeyEvent.KEYCODE_DEL, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, "handleBackspace error: " + e);
                try {
                    keyDownUp(KeyEvent.KEYCODE_DEL, false);
                } catch (Exception ex) {
                    Log.e(TAG, "Fallback backspace failed: " + ex);
                }
            }
        }
    }
    }

    public void setCandidatesViewShown(boolean shown) {

        if (DEBUG)
            Log.i(TAG, "setCandidateViewShown():" + shown);
        super.setCandidatesViewShown(shown);

        if (DEBUG)
            Log.i(TAG, "isCandidateViewShown:" + mCandidateViewStandAlone.isShown());

    }

    private void handleShift() {
        if (DEBUG)
            Log.i(TAG, "handleShift()");
        if (mInputView == null) {
            return;
        }

        if (mKeyboardSwitcher.isAlphabetMode()) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
            mHasShift = mCapsLock || !mInputView.isShifted();
            if (mHasShift) {
                mKeyboardSwitcher.toggleShift();
            }
        } else {
            if (mCapsLock) {
                toggleCapsLock();
                mHasShift = false;
            } else if (mHasShift) {
                toggleCapsLock();
                mHasShift = true;
            } else {
                mKeyboardSwitcher.toggleShift();
                mHasShift = mKeyboardSwitcher.isShifted();

            }
        }
    }

    /**
     * Integrated all soft keyboards switching in this function.
     */
    private void switchKeyboard(int primaryCode) {
        if (DEBUG)
            Log.i(TAG, "switchKeyboard() primaryCode = " + primaryCode);
        if (mCapsLock)
            toggleCapsLock();

        // Discard composing for symbol/English mode switches; commit for other switches
        boolean shouldDiscard = (primaryCode == KEYCODE_SWITCH_TO_SYMBOL_MODE
                || primaryCode == KEYCODE_SWITCH_SYMBOL_KEYBOARD
                || primaryCode == KEYCODE_SWITCH_TO_ENGLISH_MODE);
        try {
            if (mComposing != null && mComposing.length() > 0) {
                if (shouldDiscard) {
                    clearComposing(true);
                } else {
                    getCurrentInputConnection().commitText(mComposing, 1);
                    finishComposing();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        clearComposing(false);
        hideCandidateView();

        if (primaryCode == KEYCODE_SWITCH_TO_SYMBOL_MODE) { // Symbol keyboard
            mEnglishOnly = true;
            mKeyboardSwitcher.toggleSymbols();
            if (mFixedCandidateViewOn) {
                forceHideCandidateView();
            }
        } else if (primaryCode == KEYCODE_SWITCH_SYMBOL_KEYBOARD) { // Symbol keyboard
            mEnglishOnly = true;
            mKeyboardSwitcher.switchSymbols();
            if (mFixedCandidateViewOn) {
                forceHideCandidateView();
            }
        } else if (primaryCode == KEYCODE_SWITCH_TO_ENGLISH_MODE) { // Chi --> Eng
            mEnglishOnly = true;
            mLIMEPref.setLanguageMode(true);
            mKeyboardSwitcher.toggleChinese();
            if (mFixedCandidateViewOn) {
                if (!mPredictionOn) {
                    forceHideCandidateView();
                } else {
                    mCandidateViewInInputView.setSuggestions(null, false); // reset the candidate view if it's force
                                                                           // hided before
                }
            }
        } else if (primaryCode == KEYCODE_SWITCH_TO_IM_MODE) { // Eng --> Chi moved from SwitchKeyboardIM by Jeremy
                                                               // '12,4,29
            mEnglishOnly = false;
            mLIMEPref.setLanguageMode(false);
            initialIMKeyboard();
            if (mFixedCandidateViewOn) {
                mCandidateViewInInputView.setSuggestions(null, false); // reset the candiate view if it's force hided
                                                                       // before
            }
        }

        mHasShift = false;
        updateShiftKeyState(getCurrentInputEditorInfo());

        // Update keyboard xml information
        currentSoftKeyboard = mKeyboardSwitcher.getImKeyboard(activeIM);

    }

    /**
     * For physical keybaord to switch between chinese and english mode.
     */
    void switchChiEng() {
        if (DEBUG)
            Log.i(TAG, "switchChiEng(): mEnglishOnly:" + mEnglishOnly);

        // Jeremy '12,4,21 force clear before switching chi/eng
        clearComposing(false);

        mKeyboardSwitcher.toggleChinese();
        mEnglishOnly = !mKeyboardSwitcher.isChinese();
        mLIMEPref.setLanguageMode(mEnglishOnly);

        if (DEBUG)
            Log.i(TAG, "switchChiEng(): mEnglishOnly updated as " + mEnglishOnly);

        if (mEnglishOnly) {
            Toast.makeText(this, R.string.typing_mode_english,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.typing_mode_mixed,
                    Toast.LENGTH_SHORT).show();
        }
        clearSuggestions(); // Jeremy '11,9,5
    }

    @Override
    public android.view.inputmethod.InputConnection getCurrentInputConnection() {
        android.view.inputmethod.InputConnection ic = super.getCurrentInputConnection();
        if (ic == null) return null;
        if (isTranslationModeActive) {
            return new android.view.inputmethod.InputConnectionWrapper(ic, true) {
                @Override
                public boolean commitText(CharSequence text, int newCursorPosition) {
                    if (isTranslationModeActive) {
                        translateQuery.insert(translateCursorPosition, text);
                        translateCursorPosition += text.length();
                        translateCursorPositionState.setValue(translateCursorPosition);
                        translateQueryState.setValue(translateQuery.toString());
                        performTranslationAsync(translateQuery.toString());
                        return true;
                    }
                    return super.commitText(text, newCursorPosition);
                }
            };
        }
        return ic;
    }

    public void toggleTranslationMode(boolean active) {
        isTranslationModeActive = active;
        isTranslationModeState.setValue(active);
        
        InputConnection ic = getCurrentInputConnection();
        if (!active) {
            if (translateQuery.length() > 0 && ic != null) {
                ic.finishComposingText();
            }
            translateQuery.setLength(0);
            translateQueryState.setValue("");
            translatedResult = "";
            translatedResultState.setValue("");
            translateCursorPosition = 0;
            translateCursorPositionState.setValue(0);
        } else {
            translateQuery.setLength(0);
            translateQueryState.setValue("");
            translatedResult = "";
            translatedResultState.setValue("");
            translateCursorPosition = 0;
            translateCursorPositionState.setValue(0);
        }
    }

    public void updateTranslateCursorPosition(int position) {
        if (position >= 0 && position <= translateQuery.length()) {
            translateCursorPosition = position;
            translateCursorPositionState.setValue(position);
        }
    }

    public void performTranslationAsync(final String query) {
        if (query == null || query.trim().isEmpty()) {
            translatedResult = "";
            new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    translatedResultState.setValue("");
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null && isTranslationModeActive) {
                        ic.setComposingText("", 0);
                    }
                }
            });
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                    String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=t&q=" + encodedQuery;
                    
                    java.net.URL url = new java.net.URL(urlStr);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        
                        String res = response.toString();
                        if (res.startsWith("[[[")) {
                            int firstQuote = res.indexOf("\"", 3);
                            if (firstQuote != -1) {
                                int secondQuote = res.indexOf("\"", firstQuote + 1);
                                if (secondQuote != -1) {
                                    final String translated = res.substring(firstQuote + 1, secondQuote);
                                    
                                    new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            translatedResult = translated;
                                            translatedResultState.setValue(translated);
                                            
                                            InputConnection ic = getCurrentInputConnection();
                                            if (ic != null && isTranslationModeActive) {
                                                ic.setComposingText(translated, 1);
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("TRANSLATE_DEBUG", "Failed to translate: " + e.getMessage(), e);
                }
            }
        }).start();
    }

    @SuppressLint("InflateParams")
    private void initialViewAndSwitcher(boolean forceRecreate) {
        if (DEBUG)
            Log.i(TAG, "initialViewAndSwitcher() mKeyboardThemeIndex = " + mKeyboardThemeIndex
                    + ", mLIMEPref.getKeyboardTheme() = " + mLIMEPref.getKeyboardTheme());

        boolean mForceRecreate = forceRecreate;
        if (mKeyboardThemeIndex != mLIMEPref.getKeyboardTheme()) {
            mKeyboardThemeIndex = mLIMEPref.getKeyboardTheme();
            mForceRecreate = true;
            mThemeContext = null;
            if (mKeyboardSwitcher != null)
                mKeyboardSwitcher.resetKeyboards(true);
        }

        if (mThemeContext == null) {
            mThemeContext = new ContextThemeWrapper(this, getKeyboardTheme());
            if (mKeyboardSwitcher != null)
                mKeyboardSwitcher.setThemedContext(mThemeContext);

        }

        Configuration config = getResources().getConfiguration();
        boolean isPhysicalKeyboardConnected = config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
        boolean useFixedMode = mFixedCandidateViewOn || isPhysicalKeyboardConnected;

        if (useFixedMode) { // Have candidateview in InputView
            // Create inputView if it's null
            if (mCandidateInInputView == null || mForceRecreate) {

                mCandidateInInputView = (CandidateInInputViewContainer) LayoutInflater.from(mThemeContext).inflate(
                        R.layout.inputcandidate, null);
                mInputView = mCandidateInInputView.findViewById(R.id.keyboard);
                mInputView.setOnKeyboardActionListener(this);
                hasDistinctMultitouch = mInputView.hasDistinctMultitouch();
                mInputView.setHardwareAcceleratedDrawingEnabled(mIsHardwareAcceleratedDrawingEnabled);
                mCandidateInInputView.initViews();
                mCandidateViewInInputView = mCandidateInInputView.findViewById(R.id.candidatesView);
                mCandidateViewInInputView.setService(this);

            }
            if (mCandidateView != mCandidateViewInInputView)
                mCandidateView = mCandidateViewInInputView;

        } else {
            if (mInputView == null || forceRecreate) {
                mInputView = (LIMEKeyboardView) LayoutInflater.from(mThemeContext).inflate(R.layout.input, null);
                mInputView.setOnKeyboardActionListener(this);
                mInputView.setHardwareAcceleratedDrawingEnabled(mIsHardwareAcceleratedDrawingEnabled);

            }
            mCandidateView = mCandidateViewStandAlone;

        }

        // Check if mKeyboardSwitcher == null
        if (mKeyboardSwitcher == null) {
            mKeyboardSwitcher = new LIMEKeyboardSwitcher(this, mThemeContext);
        }
        mKeyboardSwitcher.setInputView(mInputView);
        buildActivatedIMList();
        mKeyboardSwitcher.setActivatedIMList(activatedIMList, activatedIMNameList, activatedIMShortNameList);

        if (mKeyboardSwitcher.getKeyboardSize() == 0 && SearchSrv != null) {
            try {
                mKeyboardSwitcher.setKeyboardList(SearchSrv.getKeyboardList());
                mKeyboardSwitcher.setImList(SearchSrv.getImList());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * For initializing Chinese IM and corresponding soft keyboards.
     */
    private void initialIMKeyboard() {
        if (DEBUG)
            Log.i(TAG, "initalizeIMKeyboard(): keyboardSelection:" + activeIM);
        // mEnglishOnly = false;
        // super.setCandidatesViewShown(false);

        switch (activeIM) {
            case "custom":
                mKeyboardSwitcher.setKeyboardMode(activeIM,
                        LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);

                hasNumberMapping = mLIMEPref.getAllowNumberMapping();
                hasSymbolMapping = mLIMEPref.getAllowSymoblMapping();
                break;
            case "phonetic":
                mKeyboardSwitcher.setKeyboardMode(activeIM,
                        LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);
                // Jeremy '11,6,18 ETEN 26 has no number mapping
                boolean standardPhonetic = !(mLIMEPref.getPhoneticKeyboardType().equals("eten26"));
                hasNumberMapping = standardPhonetic;
                hasSymbolMapping = standardPhonetic;
                break;
            case "dayi":
                mKeyboardSwitcher.setKeyboardMode(activeIM,
                        LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);
                hasNumberMapping = true;
                hasSymbolMapping = true;
                break;
            default:
                mKeyboardSwitcher.setKeyboardMode(activeIM,
                        LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);
                break;
        }
        // Jeremy '11,9,3 for phone numeric key direct input on chacha
        if (mLIMEPref.getPhysicalKeyboardType().equals("chacha"))
            hasNumberMapping = false;
        String tablename = activeIM;
        if (tablename.equals("custom") || tablename.equals("phone")) {
            tablename = "custom";
        }
        // Jeremy '11,6,10 pass hasnumbermapping and hassymbolmapping to searchservice
        // for selkey validation.
        if (DEBUG)
            Log.i(TAG, "switchKeyboard() current keyboard:" +
                    tablename + " hasnumbermapping:" + hasNumberMapping + " hasSymbolMapping:" + hasSymbolMapping);
        SearchSrv.setTablename(tablename, hasNumberMapping, hasSymbolMapping);
    }

    boolean handleSelkey(int primaryCode) {
        if (DEBUG)
            Log.i(TAG, "handleSelKey()");
        // Jeremy '12,4,1 only do selkey on starndard keyboard

        // Check if disable physical key option is open
        if ((disable_physical_selection && hasPhysicalKeyPressed)
                || !mLIMEPref.getPhysicalKeyboardType().equals("normal_keyboard")) {
            return false;
        }

        if (DEBUG)
            Log.i(TAG, "handleSelkey():primarycode:" + primaryCode);

        int i = -1;

        // Dayi 3-code selection mapping: Space(0), [ (1), ] (2), - (3), \ (4), ' (5)
        if ("dayi".equals(activeIM) && hasCandidatesShown) {
            switch (primaryCode) {
                case '[': i = 1; break;
                case ']': i = 2; break;
                case '-': i = 3; break;
                case '\\': i = 4; break;
                case '\'': i = 5; break;
                case ' ': i = 0; break;
            }
            if (i != -1) {
                pickCandidateManually(i);
                return true;
            }
        }

        if (mComposing.length() > 0 && !mEnglishOnly) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
            String selkey = "";

            // Jeremy '12,7,5 rewrite the selkey processing
            if (!(disable_physical_selection && hasPhysicalKeyPressed)) {
                try {
                    selkey = SearchSrv.getSelkey();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }

                String mixedModeSelkey = "`";
                if (hasSymbolMapping && !activeIM.equals("dayi")
                        && !(activeIM.equals("phonetic")
                                && mLIMEPref.getPhoneticKeyboardType().equals("standard"))) {
                    mixedModeSelkey = " ";
                }

                int selkeyOption = mLIMEPref.getSelkeyOption();
                if (selkeyOption == 1)
                    selkey = mixedModeSelkey + selkey;
                else if (selkeyOption == 2)
                    selkey = mixedModeSelkey + " " + selkey;

                i = selkey.indexOf((char) primaryCode);

                // Jeremy '12,7,11 bypass space as first tone for phonetic
                if (i >= 0 && selkey.charAt(i) == ' '
                        && primaryCode == MY_KEYCODE_SPACE && activeIM.equals("phonetic")
                        // && mLIMEPref.getParameterBoolean("doLDPhonetic", true)
                        && !(mComposing.toString().endsWith(" ") || mComposing.length() == 0)) {
                    return false;
                }

            }

            // Jeremy '12,4,29 use mEnglishOnly instead of onIM
        } else if (mEnglishOnly || (mComposing.length() == 0)) {
            // related candidates view
            String relatedSelkey = "!@#$%^&*()";
            i = relatedSelkey.indexOf(primaryCode);
        }

        if (i < 0 || i >= mCandidateList.size()) {
            return false;
        } else {
            pickCandidateManually(i);
            return true;
        }

    }

    /**
     * This method construct candidate view and add key code to composing object
     */
    private void handleCharacter(int primaryCode) {
        // Jeremy '11,6,9 Cleaned code!!
        if (DEBUG)
            Log.i(TAG, "handleCharacter():primaryCode:" + primaryCode
                    + ", metaState = " + mMetaState
                    + ", hasPhysicalKeyPressed = " + hasPhysicalKeyPressed
                    + ", currentSoftKeyboard=" + currentSoftKeyboard
                    + ", mCandidateView=" + (mCandidateView != null ? "ok" : "null"));

        // Jeremy '11,6,6 processing physical keyboard selkeys.
        // Move here '11,6,9 to have lower priority than hasnumbermapping
        if (hasPhysicalKeyPressed && (mCandidateView != null && hasCandidatesShown)) { // Replace isCandidateShown()
                                                                                       // with hasCandidatesShown by
                                                                                       // Jeremy '12,5,6
            if (handleSelkey(primaryCode)) {
                updateShiftKeyState(getCurrentInputEditorInfo());
                if (DEBUG)
                    Log.i(TAG, "handleCharacter() sel key found return now");
                return;
            }
        }

        if (!mEnglishOnly && !mKeyboardSwitcher.isSymbols()) {

            InputConnection ic = getCurrentInputConnection();

            if (DEBUG)
                Log.i(TAG, "HandleCharacter():"
                        + " ic != null:" + (ic != null)
                        + " isValidLetter:" + isValidLetter(primaryCode)
                        + " isValidDigit:" + isValidDigit(primaryCode)
                        + " isValidSymbol:" + isValidSymbol(primaryCode)
                        + " hasSymbolMapping:" + hasSymbolMapping
                        + " hasNumberMapping:" + hasNumberMapping
                        + " (primaryCode== MY_KEYCODE_SPACE && keyboardSelection.equals(phonetic):"
                        + (primaryCode == MY_KEYCODE_SPACE && activeIM.equals("phonetic"))
                        + " mEnglishOnly:" + mEnglishOnly);

            if ((!hasSymbolMapping) && (primaryCode == ',' || primaryCode == '.')) { // Chinese , and . processing
                                                                                     // //Jeremy '12,4,29 use
                                                                                     // mEnglishOnly instead of onIM
                mComposing.append((char) primaryCode);
                // InputConnection ic=getCurrentInputConnection();
                // InputConnection ic=getCurrentInputConnection();
                // if (ic != null)
                // ic.setComposingText(getComposingDisplayString(mComposing.toString()), 1);
                // Just update CandidateView composing text
                getComposingDisplayString(mComposing.toString());
                updateCandidates();
                // misMatched = mComposing.toString();
            } else if (!hasSymbolMapping && !hasNumberMapping // Jeremy '11,10.19 fixed to bypass number key in et26 and
                                                              // hsu
                    && (isValidLetter(primaryCode)
                            || (primaryCode == MY_KEYCODE_SPACE && activeIM.equals("phonetic"))) // Jeremy '11,9,6 for
                                                                                                 // et26 and hsu
                    && !mEnglishOnly) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
                // Log.i(TAG,"handlecharacter(), onIM and no number and no symbol mapping");
                mComposing.append((char) primaryCode);
                // InputConnection ic=getCurrentInputConnection();
                // InputConnection ic=getCurrentInputConnection();
                // if (ic != null)
                // ic.setComposingText(getComposingDisplayString(mComposing.toString()), 1);
                // Just update CandidateView composing text
                getComposingDisplayString(mComposing.toString());
                updateCandidates();
                // misMatched = mComposing.toString();
            } else if (!hasSymbolMapping
                    && hasNumberMapping
                    && (isValidLetter(primaryCode) || isValidDigit(primaryCode))
                    && !mEnglishOnly) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
                mComposing.append((char) primaryCode);
                // InputConnection ic=getCurrentInputConnection();
                // InputConnection ic=getCurrentInputConnection();
                // if (ic != null)
                // ic.setComposingText(getComposingDisplayString(mComposing.toString()), 1);
                // Just update CandidateView composing text
                getComposingDisplayString(mComposing.toString());
                updateCandidates();
                // misMatched = mComposing.toString();
            } else if (hasSymbolMapping
                    && !hasNumberMapping
                    && (isValidLetter(primaryCode) || isValidSymbol(primaryCode)
                            || (primaryCode == MY_KEYCODE_SPACE && activeIM.equals("phonetic"))) // Jeremy '11,9,6 for
                                                                                                 // chacha
                    && !mEnglishOnly) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
                mComposing.append((char) primaryCode);
                // InputConnection ic=getCurrentInputConnection();
                // InputConnection ic=getCurrentInputConnection();
                // if (ic != null)
                // ic.setComposingText(getComposingDisplayString(mComposing.toString()), 1);
                // Just update CandidateView composing text
                getComposingDisplayString(mComposing.toString());
                updateCandidates();
                // misMatched = mComposing.toString();
            } else if (hasSymbolMapping && !hasNumberMapping
                    && mComposing != null && mComposing.length() >= 1
                    && getCurrentInputConnection().getTextBeforeCursor(1, 1).charAt(0) == 'w'
                    && Character.isDigit((char) primaryCode)
                    && !mEnglishOnly) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
                // 27.May.2011 Art : This is the method to check user input type
                // if first previous character is w and second char is number then enable im
                // mode.
                mComposing.append((char) primaryCode);
                // InputConnection ic=getCurrentInputConnection();
                // InputConnection ic=getCurrentInputConnection();
                // if (ic != null)
                // ic.setComposingText(getComposingDisplayString(mComposing.toString()), 1);
                // Just update CandidateView composing text
                getComposingDisplayString(mComposing.toString());
                updateCandidates();
                // misMatched = mComposing.toString();
            } else if (hasSymbolMapping
                    && hasNumberMapping
                    && (isValidSymbol(primaryCode)
                            || (primaryCode == MY_KEYCODE_SPACE && activeIM.equals("phonetic"))
                            || isValidLetter(primaryCode) || isValidDigit(primaryCode))
                    && !mEnglishOnly) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
                // Fixed: Ensure proper character handling for dayi input method
                mComposing.append((char) primaryCode);
                // if (ic != null)
                // ic.setComposingText(getComposingDisplayString(mComposing.toString()), 1);
                // Just update CandidateView composing text
                getComposingDisplayString(mComposing.toString());
                updateCandidates();

            } else {
                // Fixed: Simplified character handling - directly commit if no other conditions
                // match
                if (DEBUG)
                    Log.i(TAG, "handleCharacter() fallback case: primaryCode=" + primaryCode + " char="
                            + (char) primaryCode);
                if (hasCandidatesShown) {
                    if (!pickHighlightedCandidate()) {
                        if (ic != null)
                            ic.commitText(String.valueOf((char) primaryCode), 1);
                    }
                } else {
                    if (ic != null)
                        ic.commitText(String.valueOf((char) primaryCode), 1);
                }
                // Jeremy '12,4,21
                finishComposing();

            }

        } else {
            /*
             * Handle when user input English Characters
             */
            if (DEBUG)
                Log.i(TAG, "handleCharacter() english only mode without prediction, committext = "
                        + (char) primaryCode);
            if (isInputViewShown()) {
                if (mInputView.isShifted()) {
                    primaryCode = Character.toUpperCase(primaryCode);
                }
            }

            if (mLIMEPref.getEnglishPrediction() && mPredictionOn && !mKeyboardSwitcher.isSymbols()
                    && !currentSoftKeyboard.contains("phone")
                    && (!hasPhysicalKeyPressed || mLIMEPref.getEnglishPredictionOnPhysicalKeyboard())) {
                if (Character.isLetter((char) primaryCode)) {
                    this.tempEnglishWord.append((char) primaryCode);
                    this.updateEnglishPrediction();
                } else {
                    resetTempEnglishWord();
                    this.updateEnglishPrediction();
                }

            }

            getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
        }

        if (!(!hasPhysicalKeyPressed && hasDistinctMultitouch))
            updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleClose() {
        if (DEBUG)
            Log.i(TAG, "handleClose()");
        // cancel candidate view if it's shown

        // Jeremy '12,4,23 need to check here.
        finishComposing();

        requestHideSelf(0);
        mInputView.closing();
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mInputView.getKeyboard().isShifted()) {
            if (now - mLastShiftTime < SHIFT_LOCK_TIMEOUT) {
                toggleCapsLock();
            }
        }
        mLastShiftTime = now;
    }

    void toggleCapsLock() {
        mCapsLock = !mCapsLock;
        if (mKeyboardSwitcher.isAlphabetMode()) {
            ((LIMEKeyboard) mInputView.getKeyboard()).setShiftLocked(mCapsLock);
        } else {
            if (mCapsLock) {
                if (DEBUG) {
                    Log.i(TAG, "toggleCapsLock():mCapsLock:true");
                }
                if (!mKeyboardSwitcher.isShifted())
                    mKeyboardSwitcher.toggleShift();
                ((LIMEKeyboard) mInputView.getKeyboard()).setShiftLocked(true);
            } else {
                if (DEBUG) {
                    Log.i(TAG, "toggleCapsLock():mCapsLock:false");
                }
                ((LIMEKeyboard) mInputView.getKeyboard()).setShiftLocked(false);
                if (mKeyboardSwitcher.isShifted())
                    mKeyboardSwitcher.toggleShift();

            }
        }
    }

    /*
     * public boolean isWordSeparator(int code) {
     * //Jeremy '11,5,31
     * String separators = getResources().getString(R.string.word_separators);
     * return separators.contains(String.valueOf((char) code));
     * 
     * }
     */
    // Jeremy '12,5,11 add return value from mCandidate.takeselectedsuggestion()
    public boolean pickHighlightedCandidate() {
        return mCandidateView != null && mCandidateView.takeSelectedSuggestion();
    }

    public void requestFullRecords(boolean isRelatedPhrase) {
        if (DEBUG)
            Log.i(TAG, "requestFullRecords()");

        if (isRelatedPhrase)
            this.updateRelatedPhrase(true);
        else
            this.updateCandidates(true);

    }

    public void removeCandidateManually(int index) {
        if (mCandidateList != null && index >= 0 && index < mCandidateList.size()) {
            Mapping mapping = mCandidateList.get(index);
            if (mapping.isRelatedPhraseRecord()) {
                
                androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(mThemeContext)
                    .setTitle("Delete Suggestion")
                    .setMessage("Remove '" + mapping.getWord() + "' from related words list?")
                    .setPositiveButton("Delete", (dialogInterface, which) -> {
                        SearchSrv.deleteRelatedPhrase(mapping.getPword(), mapping.getWord());
                        mCandidateList.remove(index);
                        setSuggestions(mCandidateList, false, "");
                        Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();

                Window window = dialog.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.token = mInputView != null ? mInputView.getWindowToken() : null;
                    lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
                    window.setAttributes(lp);
                    window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                }
                dialog.show();
            }
        }
    }

    public void pickCandidateManually(int index) {
        if (DEBUG)
            Log.i(TAG, "pickCandidateManually():"
                    + "Pick up candidate at index : " + index);

        // This is to prevent if user select the index more than the list
        if (mCandidateList != null && index >= mCandidateList.size()) {
            return;
        }

        if (mCandidateList != null && mCandidateList.size() > 0) {
            selectedCandidate = mCandidateList.get(index);
            // selectedIndex = index;
        }

        InputConnection ic = getCurrentInputConnection();

        if (mCompletionOn && mCompletions != null && index >= 0
                && selectedCandidate.isPartialMatchToCodeRecord()
                && index < mCompletions.length) { // user picked the completion suggestion item.
            CompletionInfo ci = mCompletions[index];
            if (ic != null)
                ic.commitCompletion(ci);
            if (DEBUG)
                Log.i(TAG, "pickSuggestionManually():mCompletionOn:" + mCompletionOn);

        } else if ((mComposing.length() > 0
                || (selectedCandidate != null && !selectedCandidate.isComposingCodeRecord()))
                && !mEnglishOnly) { // user picked candidates from composing candidate or related phrase candidates
            // Jeremy '12,4,29 use mEnglishOnly instead of onIM
            commitTyped(ic);
        } else if (mLIMEPref.getEnglishPrediction() && tempEnglishList != null
                && tempEnglishList.size() > 0) { // user picked English prediction suggestions

            // Log.i("EMOJI-commit-index:", index + "");
            // Log.i("EMOJI-commit:", tempEnglishList.size() + "");

            if (this.tempEnglishList.get(index).isEmojiRecord()) {
                if (ic != null)
                    ic.commitText(
                            this.tempEnglishList.get(index).getWord() + " ", 0);
            } else {
                if (ic != null)
                    ic.commitText(
                            this.tempEnglishList.get(index).getWord()
                                    .substring(tempEnglishWord.length())
                                    + " ",
                            0);
            }

            resetTempEnglishWord();

            clearSuggestions();

        }

        /*
        if (currentSoftKeyboard.contains("wb")) {
            if (ic != null && mPredictionOn)
                ic.setComposingText("", 0);
        }
        */

    }

    public void swipeRight() {
        // if (mCompletionOn) {
        pickHighlightedCandidate();
        // }
    }

    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
        handleOptions();
    }

    /**
     * First method to call after key press
     */
    public void onPress(int primaryCode) {
        if (DEBUG)
            Log.i(TAG, "onPress(): code = " + primaryCode);
        // Record key press time (press down)
        // keyPressTime = System.currentTimeMillis();
        // To identify the source of character (Software keyboard or physical
        // keyboard)
        hasPhysicalKeyPressed = false;

        // Jeremy '24,1,7: Enable shift handling on press for all touch modes to support
        // stable hybrid behavior
        if (primaryCode == LIMEBaseKeyboard.KEYCODE_SHIFT) {
            if (DEBUG)
                Log.i(TAG, "onPress():KEYCODE_SHIFT, calling handleShift()");
            hasShiftPress = true;
            mShiftHandledInOnPress = true; // Set flag to avoid duplicate processing in onKey
            hasShiftCombineKeyPressed = false;
            handleShift();
        } else if (hasShiftPress) {
            hasShiftCombineKeyPressed = true;
        }
        doVibrateSound(primaryCode);

    }

    public void doVibrateSound(int primaryCode) {
        if (DEBUG)
            Log.i(TAG, "doVibrateSound()");
        if (hasVibration) {
            // Jeremy '11,9,1 add preference on vibrate level
            mVibrator.vibrate(mLIMEPref.getVibrateLevel());
        }
        if (hasSound) {
            int sound = AudioManager.FX_KEYPRESS_STANDARD;
            switch (primaryCode) {
                case LIMEBaseKeyboard.KEYCODE_DELETE:
                    sound = AudioManager.FX_KEYPRESS_DELETE;
                    break;
                case MY_KEYCODE_ENTER:
                    sound = AudioManager.FX_KEYPRESS_RETURN;
                    break;
                case MY_KEYCODE_SPACE:
                    sound = AudioManager.FX_KEYPRESS_SPACEBAR;
                    break;
            }
            float FX_VOLUME = 1.0f;
            mAudioManager.playSoundEffect(sound, FX_VOLUME);
        }
    }
    /*
     * public boolean isValidTime(Date target) {
     * Calendar srcCal = Calendar.getInstance();
     * srcCal.setTime(new Date());
     * Calendar destCal = Calendar.getInstance();
     * destCal.setTime(target);
     * 
     * return srcCal.getTimeInMillis() - destCal.getTimeInMillis() < 1800000;
     * 
     * }
     */

    /**
     * Last method to execute when key release
     */
    public void onRelease(int primaryCode) {
        if (DEBUG)
            Log.i(TAG, "onRelease(): code = " + primaryCode);
        // Jeremy '24,1,7: Enable shift release handling for all touch modes
        if (primaryCode == LIMEBaseKeyboard.KEYCODE_SHIFT) {
            hasShiftPress = false;
            if (hasShiftCombineKeyPressed) {
                hasShiftCombineKeyPressed = false;
                updateShiftKeyState(getCurrentInputEditorInfo());
            }
        } else if (!hasShiftPress) {
            updateShiftKeyState(getCurrentInputEditorInfo());

        }
    }

    @Override
    public void onDestroy() {
        if (DEBUG)
            Log.i(TAG, "onDestroy()");

        // Cancel pending query and shut down the executor
        if (queryFuture != null) {
            queryFuture.cancel(true);
            queryFuture = null;
        }
        queryExecutor.shutdownNow();

        // Clean up resources to prevent callback warnings
        try {
            if (mInputView != null) {
                mInputView.closing();
                mInputView = null;
            }
            if (mCandidateView != null) {
                mCandidateView = null;
            }
            // Clear any pending callbacks or handlers
            clearComposing(true);
        } catch (Exception e) {
            // Ignore cleanup exceptions
        }

        super.onDestroy();
    }

    /*
     * @Override
     * public void onUpdateCursor(Rect newCursor) {
     * if(DEBUG)
     * Log.i(TAG, "onUpdateCursor(): Top:"
     * + newCursor.top + ". Right:" + newCursor.right
     * + ". bottom:" + newCursor.bottom + ". left:" + newCursor.left );
     * 
     * 
     * if(mCandidateView!=null)
     * mCandidateView.onUpdateCursor(newCursor);
     * super.onUpdateCursor(newCursor);
     * }
     */
    @Override
    public void onCancel() {
        if (DEBUG)
            Log.i(TAG, "onCancel()");
        // clearComposing(); Jeremy '12,4,10 avoid clearcomposing when user slide
        // outside the candidate area

    }

    // jeremy '11,9, 5 hideCanddiate when inputView is closed
    @Override
    public void updateInputViewShown() {
        if (mInputView == null)
            return;
        if (DEBUG)
            Log.i(TAG, "updateInputViewShown(): mInputView.isShown(): " + mInputView.isShown());
        super.updateInputViewShown();
        if (!mInputView.isShown() && !hasPhysicalKeyPressed)
            hideCandidateView();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        if (DEBUG)
            Log.i(TAG, "onFinishInputView()");
        super.onFinishInputView(finishingInput);
        hideCandidateView(); // Jeremy '12,5,7 hideCandiate when inputview is closed but not yet leave the
                             // original field (onfinishinput() will not called).
    }

    /**
     * Switch to symbol keyboard (Emoji key)
     */
    public void switchSymbols() {
        if (DEBUG)
            Log.i(TAG, "switchSymbols()");
        if (mKeyboardSwitcher != null) {
            mKeyboardSwitcher.switchSymbols();
        }
    }

    private int getKeyboardTheme() {
        return KEYBOARD_THEMES[mKeyboardThemeIndex].mStyleId;
    }

    private static class CandidateViewHandler extends Handler {

        private final WeakReference<LIMEService> mLIMEService;
        private final int MSG_SHOW_CANDIDATE_VIEW = 1;
        private final int MSG_HIDE_CANDIDATE_VIEW = 2;

        CandidateViewHandler(LIMEService im) {
            super(android.os.Looper.getMainLooper());
            mLIMEService = new WeakReference<>(im);
        }

        @Override
        public void handleMessage(Message msg) {
            if (DEBUG)
                Log.i(TAG, "CandidateViewHandler.handleMessage(): message:" + msg.what);
            LIMEService mLIMEInstance = mLIMEService.get();
            if (mLIMEInstance == null)
                return;
            switch (msg.what) {
                case MSG_SHOW_CANDIDATE_VIEW:
                    mLIMEInstance.setCandidatesViewShown(true);
                    break;
                case MSG_HIDE_CANDIDATE_VIEW:
                    mLIMEInstance.setCandidatesViewShown(false);
                    break;
            }
        }

        void showCandidateView() {
            removeMessages(MSG_HIDE_CANDIDATE_VIEW); // cancel previous hide messages if any
            sendMessage(obtainMessage(MSG_SHOW_CANDIDATE_VIEW));
        }

        void hideCandidateView() {
            sendMessage(obtainMessage(MSG_HIDE_CANDIDATE_VIEW));
        }

        void hideCandidateViewDelayed(int delay) {
            sendMessageDelayed(obtainMessage(MSG_HIDE_CANDIDATE_VIEW), delay);
        }
    }

    private static class KeyboardTheme {
        final String mName;
        final int mThemeId;
        final int mStyleId;

        KeyboardTheme(String name, int themeId, int styleId) {
            mName = name;
            mThemeId = themeId;
            mStyleId = styleId;
        }
    }

}
