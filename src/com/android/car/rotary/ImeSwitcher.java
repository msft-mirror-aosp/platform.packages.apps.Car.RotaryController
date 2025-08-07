/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.rotary;

import static android.provider.Settings.Secure.DEFAULT_INPUT_METHOD;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.util.dump.DualDumpOutputStream;

import java.util.List;
import java.util.Locale;

/**
 * This helper class manages Input Method Editor (IME) switching between rotary and touch modes.
 * It activates the dedicated rotary IME in rotary mode and the touch IME in touch mode. This class
 * is only used if a touch IME and a dedicated rotary IME exist.
 */
class ImeSwitcher {
    private static final String SHARED_PREFS = "com.android.car.rotary.ImeSwitcher";
    private static final String TOUCH_INPUT_METHOD_PREFIX = "TOUCH_INPUT_METHOD_";
    private static final String INPUT_METHOD_SUBTYPE_MODE_KEYBOARD = "keyboard";
    private static final String INPUT_METHOD_SUBTYPE_MODE_ROTARY = "rotary";

    @NonNull
    private final SharedPreferences mPrefs;
    @NonNull
    private final InputMethodManager mInputMethodManager;
    @NonNull
    private final UserManager mUserManager;
    @NonNull
    private final ContentResolver mContentResolver;

    /** Component name of rotary IME. */
    @NonNull
    private final String mRotaryInputMethod;

    /** Component name of default IME used in touch mode. */
    @NonNull
    private final String mDefaultTouchInputMethod;

    /** Component name of current IME used in touch mode. */
    @NonNull
    private String mTouchInputMethod;

    /** Observer to update {@link #mTouchInputMethod} when the user switches IMEs. */
    @Nullable
    private ContentObserver mInputMethodObserver;

    private ImeSwitcher(@NonNull Context context,
            @NonNull InputMethodManager imm,
            @NonNull ContentResolver contentResolver,
            @NonNull String rotaryInputMethod,
            @NonNull String defaultTouchInputMethod) {
        mContentResolver = contentResolver;
        mInputMethodManager = imm;
        mRotaryInputMethod = rotaryInputMethod;
        mDefaultTouchInputMethod = defaultTouchInputMethod;

        mPrefs = context.createDeviceProtectedStorageContext().getSharedPreferences(
                SHARED_PREFS, Context.MODE_PRIVATE);

        mUserManager = context.getSystemService(UserManager.class);

        mTouchInputMethod = mPrefs.getString(TOUCH_INPUT_METHOD_PREFIX
                + mUserManager.getUserName(), mDefaultTouchInputMethod);
        if (mTouchInputMethod.isEmpty()
                || !Utils.isInstalledIme(mTouchInputMethod, mInputMethodManager)) {
            // Workaround for b/323013736.
            L.e("mTouchInputMethod is empty or not installed!");
            mTouchInputMethod = mDefaultTouchInputMethod;
        }

        // Switch from the rotary IME to the touch IME in case RotaryService failed to reset IME
        // before it was killed (e.g., run `adb reboot` when it is in rotary mode).
        switchIme(/* inRotaryMode= */ false);
    }

    /**
     * Returns an instance of this class as needed.
     * <p>We only need to switch IME when there is a dedicated rotary IME and a touch IME.
     * Besides, we need a non-null ContentResolver to set the current IME.
     */
    @Nullable
    static ImeSwitcher getOptionalInstance(@NonNull Context context,
            @Nullable ContentResolver contentResolver) {
        if (contentResolver == null) {
            return null;
        }
        InputMethodManager imm = context.getSystemService(InputMethodManager.class);
        String rotaryInputMethod = getRotaryInputMethod(imm);
        L.d("rotaryInputMethod: " + rotaryInputMethod);
        if (TextUtils.isEmpty(rotaryInputMethod)) {
            return null;
        }
        String defaultTouchInputMethod = getDefaultTouchInputMethod(imm);
        L.d("defaultTouchInputMethod: " + defaultTouchInputMethod);
        if (TextUtils.isEmpty(defaultTouchInputMethod)) {
            return null;
        }
        return new ImeSwitcher(context, imm, contentResolver, rotaryInputMethod,
                defaultTouchInputMethod);
    }

    /**
     * Registers an observer to updates {@link #mTouchInputMethod} whenever the user switches IMEs.
     */
    void registerInputMethodObserver() {
        if (mInputMethodObserver != null) {
            throw new IllegalStateException("Input method observer already registered");
        }
        mInputMethodObserver = new ContentObserver(new Handler(Looper.myLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                // Either the user switched input methods or we did. In the former case, update
                // mTouchInputMethod and save it so we can switch back after switching to the rotary
                // input method.
                String inputMethod = getCurrentIme();
                L.d("Current IME changed to " + inputMethod);
                if (!TextUtils.isEmpty(inputMethod) && !inputMethod.equals(mRotaryInputMethod)) {
                    mTouchInputMethod = inputMethod;
                    String userName = mUserManager.getUserName();
                    L.d("Save mTouchInputMethod(" + mTouchInputMethod + ") for user "
                            + userName);
                    mPrefs.edit()
                            .putString(TOUCH_INPUT_METHOD_PREFIX + userName, mTouchInputMethod)
                            .apply();
                }
            }
        };
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(DEFAULT_INPUT_METHOD),
                /* notifyForDescendants= */ false,
                mInputMethodObserver);
    }

    /** Unregisters the observer registered by {@link #registerInputMethodObserver}. */
    void unregisterInputMethodObserver() {
        if (mInputMethodObserver == null) {
            return;
        }
        mContentResolver.unregisterContentObserver(mInputMethodObserver);
        mInputMethodObserver = null;
    }

    /**
     * Switches to the rotary IME when entering rotary mode, or to the touch IME when entering touch
     * mode.
     */
    void switchIme(boolean inRotaryMode) {
        String oldIme = getCurrentIme();
        if (inRotaryMode != mRotaryInputMethod.equals(oldIme)) {
            String newIme = inRotaryMode ? mRotaryInputMethod : mTouchInputMethod;
            setCurrentIme(oldIme, newIme);
        }
    }

    void dump(@NonNull DualDumpOutputStream dumpOutputStream, @NonNull String fieldName,
            long fieldId) {
        long fieldToken = dumpOutputStream.start(fieldName, fieldId);
        dumpOutputStream.write("rotaryInputMethod", RotaryProtos.ImeSwitcher.ROTARY_INPUT_METHOD,
                mRotaryInputMethod);
        dumpOutputStream.write("defaultTouchInputMethod",
                RotaryProtos.ImeSwitcher.DEFAULT_TOUCH_INPUT_METHOD, mDefaultTouchInputMethod);
        dumpOutputStream.write("touchInputMethod", RotaryProtos.ImeSwitcher.TOUCH_INPUT_METHOD,
                mTouchInputMethod);
        dumpOutputStream.end(fieldToken);
    }

    @NonNull
    private String getCurrentIme() {
        return Settings.Secure.getString(mContentResolver, DEFAULT_INPUT_METHOD);
    }

    private void setCurrentIme(String oldIme, String newIme) {
        validateImeConfiguration(newIme);
        boolean result =
                Settings.Secure.putString(mContentResolver, DEFAULT_INPUT_METHOD, newIme);
        L.successOrFailure("Switching IME from " + oldIme + " to " + newIme, result);
    }

    /**
     * Ensure that the IME configuration passed as argument is also available in
     * {@link InputMethodManager}.
     *
     * @throws IllegalStateException if the ime configuration passed as argument is not available
     *                               in {@link InputMethodManager}
     */
    private void validateImeConfiguration(String imeConfiguration) {
        if (!Utils.isInstalledIme(imeConfiguration, mInputMethodManager)) {
            throw new IllegalStateException(String.format("%s is not installed (run "
                            + "`adb shell ime list -a -s` to list all installed input methods)",
                    imeConfiguration));
        }
    }

    /**
     * Similar to IMMS's default IME selection, this method selects an enabled IMEs as follows:
     * First, it seeks a system non-auxiliary IME with system language subtype and "keyboard"
     * layout. If unavailable, it defaults to the first system non-auxiliary IME.
     * If that also isn't found, it selects the very first IME in the enabled list, if there is any.
     */
    @Nullable
    private static String getDefaultTouchInputMethod(InputMethodManager imm) {
        List<InputMethodInfo> enabledImes = imm.getEnabledInputMethodList();
        if (enabledImes.isEmpty()) {
            L.e("No IME enabled! Run `adb shell ime list -s` to list installed input methods ");
            return null;
        }
        // We'd prefer to fall back on a system IME, since that is safer.
        int i = enabledImes.size();
        int firstFoundSystemIme = -1;
        Locale systemLocale = Resources.getSystem().getConfiguration().getLocales().get(0);
        while (i > 0) {
            i--;
            InputMethodInfo imi = enabledImes.get(i);
            if (imi.isAuxiliaryIme()) {
                continue;
            }
            if (imi.isSystem()
                    && containsSubtypeOf(imi, systemLocale, INPUT_METHOD_SUBTYPE_MODE_KEYBOARD)) {
                L.v("Found default touch IME:" + imi);
                return imi.getComponent().flattenToShortString();
            }
            if (firstFoundSystemIme < 0 && imi.isSystem()) {
                firstFoundSystemIme = i;
            }
        }
        L.v(String.format("Default to %s IME",
                firstFoundSystemIme >= 0 ? "system non-auxiliary" : " first enabled"));
        InputMethodInfo imi = enabledImes.get(Math.max(firstFoundSystemIme, 0));
        return imi.getComponent().flattenToShortString();
    }

    @Nullable
    private static String getRotaryInputMethod(InputMethodManager imm) {
        // getInputMethodList() is used rather than getEnabledInputMethodList() because the Rotary
        // IME could be installed on the system but not actively enabled.
        List<InputMethodInfo> installedImes = imm.getInputMethodList();
        for (InputMethodInfo imi : installedImes) {
            List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(imi,
                    /* allowsImplicitlyEnabledSubtypes= */ true);
            for (InputMethodSubtype subtype : subtypes) {
                if (INPUT_METHOD_SUBTYPE_MODE_ROTARY.equals(subtype.getMode())) {
                    return imi.getComponent().flattenToShortString();
                }
            }
        }
        return null;
    }

    private static boolean containsSubtypeOf(@NonNull InputMethodInfo imi, @NonNull Locale locale,
            @NonNull String mode) {
        for (int i = 0; i < imi.getSubtypeCount(); ++i) {
            final InputMethodSubtype subtype = imi.getSubtypeAt(i);
            if (!subtype.getMode().equals(mode)) {
                continue;
            }
            // Ignore country and check language only.
            String language = locale.getLanguage();
            if (subtype.getLocaleObject().getLanguage().equals(language)) {
                return true;
            }
        }
        return false;
    }
}
