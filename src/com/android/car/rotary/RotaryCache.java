/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.os.SystemClock;
import android.util.LruCache;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Cache of rotation and nudge history of rotary controller. With this cache, the users can reverse
 * course and go back where they were if they accidentally nudge too far.
 */
class RotaryCache {

    /** The cache is disabled. */
    @VisibleForTesting
    static final int CACHE_TYPE_DISABLED = 1;
    /** Entries in the cache will expire after a period of time. */
    @VisibleForTesting
    static final int CACHE_TYPE_EXPIRED_AFTER_SOME_TIME = 2;
    /** Entries in the cache will never expire as long as RotaryService is alive. */
    @VisibleForTesting
    static final int CACHE_TYPE_NEVER_EXPIRE = 3;

    @IntDef(flag = true, value = {
            CACHE_TYPE_DISABLED, CACHE_TYPE_EXPIRED_AFTER_SOME_TIME, CACHE_TYPE_NEVER_EXPIRE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CacheType {
    }

    // TODO(b/153390767): get rid of static variables.
    @NonNull
    private static Utils sUtils = Utils.getInstance();

    /** Cache of last focused node by focus area. */
    @NonNull
    private final FocusHistoryCache mFocusHistoryCache;

    /** Cache of target focus area by source focus area and direction (up, down, left or right). */
    @NonNull
    private final FocusAreaHistoryCache mFocusAreaHistoryCache;

    /** A record of when a node was focused. */
    private static class FocusHistory {

        /** A node representing a focusable {@link View} or a {@link FocusArea}. */
        @NonNull
        final AccessibilityNodeInfo node;

        /** The {@link SystemClock#uptimeMillis} when this history was recorded. */
        final long timestamp;

        FocusHistory(@NonNull AccessibilityNodeInfo node, long timestamp) {
            this.node = node;
            this.timestamp = timestamp;
        }
    }

    /**
     * A combination of a source focus area and a direction (up, down, left or right). Used as a key
     * in {@link #mFocusAreaHistoryCache}.
     */
    private static class FocusAreaHistory {

        @NonNull
        final AccessibilityNodeInfo sourceFocusArea;
        final int direction;

        FocusAreaHistory(@NonNull AccessibilityNodeInfo sourceFocusArea, int direction) {
            this.sourceFocusArea = sourceFocusArea;
            this.direction = direction;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FocusAreaHistory that = (FocusAreaHistory) o;
            return direction == that.direction
                    && Objects.equals(sourceFocusArea, that.sourceFocusArea);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceFocusArea, direction);
        }
    }

    /** A cache of the last focused node by focus area. */
    private class FocusHistoryCache extends LruCache<AccessibilityNodeInfo, FocusHistory> {

        /** Type of the cache. */
        private final @CacheType int mCacheType;

        /** How many milliseconds before an entry in the cache expires. */
        private final int mExpirationTimeMs;

        FocusHistoryCache(@CacheType int cacheType, int size, int expirationTimeMs) {
            super(size);
            mCacheType = cacheType;
            mExpirationTimeMs = expirationTimeMs;
            if (mCacheType == CACHE_TYPE_EXPIRED_AFTER_SOME_TIME && mExpirationTimeMs <= 0) {
                throw new IllegalArgumentException(
                        "Expiration time must be positive if CacheType is "
                                + "CACHE_TYPE_EXPIRED_AFTER_SOME_TIME");
            }
        }

        boolean enabled() {
            return mCacheType != CACHE_TYPE_DISABLED;
        }

        boolean isValidFocusHistory(@Nullable FocusHistory focusHistory, long elapsedRealtime) {
            if (focusHistory == null || focusHistory.node == null) {
                return false;
            }
            switch (mCacheType) {
                case CACHE_TYPE_NEVER_EXPIRE:
                    return true;
                case CACHE_TYPE_EXPIRED_AFTER_SOME_TIME:
                    return elapsedRealtime - focusHistory.timestamp < mExpirationTimeMs;
                default:
                    return false;
            }
        }

        @Override
        protected void entryRemoved(boolean evicted, AccessibilityNodeInfo key,
                FocusHistory oldValue, FocusHistory newValue) {
            Utils.recycleNode(key);
            Utils.recycleNode(oldValue.node);
        }
    }

    /**
     * A cache of the target focus area to nudge to, by source focus area and direction (up, down,
     * left or right).
     */
    private class FocusAreaHistoryCache extends LruCache<FocusAreaHistory, FocusHistory> {

        /** Type of the cache. */
        private final @CacheType int mCacheType;

        /** How many milliseconds before an entry in the cache expires. */
        private final int mExpirationTimeMs;

        FocusAreaHistoryCache(@CacheType int cacheType, int size, int expirationTimeMs) {
            super(size);
            mCacheType = cacheType;
            mExpirationTimeMs = expirationTimeMs;
            if (mCacheType == CACHE_TYPE_EXPIRED_AFTER_SOME_TIME && mExpirationTimeMs <= 0) {
                throw new IllegalArgumentException(
                        "Expiration time must be positive if CacheType is "
                                + "CACHE_TYPE_EXPIRED_AFTER_SOME_TIME");
            }
        }

        boolean enabled() {
            return mCacheType != CACHE_TYPE_DISABLED;
        }

        boolean isValidFocusHistory(@Nullable FocusHistory focusHistory, long elapsedRealtime) {
            if (focusHistory == null || focusHistory.node == null) {
                return false;
            }
            switch (mCacheType) {
                case CACHE_TYPE_NEVER_EXPIRE:
                    return true;
                case CACHE_TYPE_EXPIRED_AFTER_SOME_TIME:
                    return elapsedRealtime - focusHistory.timestamp < mExpirationTimeMs;
                default:
                    return false;
            }
        }

        @Override
        protected void entryRemoved(boolean evicted, FocusAreaHistory key, FocusHistory oldValue,
                FocusHistory newValue) {
            Utils.recycleNode(key.sourceFocusArea);
            Utils.recycleNode(oldValue.node);
        }
    }

    RotaryCache(@CacheType int focusHistoryCacheType,
            int focusHistoryCacheSize,
            int focusHistoryExpirationTimeMs,
            @CacheType int focusAreaHistoryCacheType,
            int focusAreaHistoryCacheSize,
            int focusAreaHistoryExpirationTimeMs) {
        mFocusHistoryCache = new FocusHistoryCache(
                focusHistoryCacheType, focusHistoryCacheSize, focusHistoryExpirationTimeMs);
        mFocusAreaHistoryCache = new FocusAreaHistoryCache(focusAreaHistoryCacheType,
                focusAreaHistoryCacheSize, focusAreaHistoryExpirationTimeMs);
    }

    /**
     * Searches the cache to find the last focused node in the given {@code focusArea}. Returns the
     * node, or null if there is nothing in the cache or the cache is stale. The caller is
     * responsible for recycling the result.
     */
    AccessibilityNodeInfo getFocusedNode(@NonNull AccessibilityNodeInfo focusArea,
            long elapsedRealtime) {
        if (mFocusHistoryCache.enabled()) {
            FocusHistory focusHistory = mFocusHistoryCache.get(focusArea);
            if (mFocusHistoryCache.isValidFocusHistory(focusHistory, elapsedRealtime)) {
                return copyNode(focusHistory.node);
            }
        }
        return null;
    }

    /**
     * Caches the last focused node by focus area. A copy of {@code focusArea} and {@code
     * focusedNode} will be saved in the cache.
     */
    void saveFocusedNode(@NonNull AccessibilityNodeInfo focusArea,
            @NonNull AccessibilityNodeInfo focusedNode, long elapsedRealtime) {
        if (mFocusHistoryCache.enabled()) {
            mFocusHistoryCache.put(
                    copyNode(focusArea), new FocusHistory(copyNode(focusedNode), elapsedRealtime));
        }
    }

    /**
     * Searches the cache to find the target focus area for a nudge in a given {@code direction}
     * from a given focus area. Returns the focus area, or null if there is nothing in the cache or
     * the cache is stale. The caller is responsible for recycling the result.
     */
    AccessibilityNodeInfo getTargetFocusArea(@NonNull AccessibilityNodeInfo sourceFocusArea,
            int direction, long elapsedRealtime) {
        if (mFocusAreaHistoryCache.enabled()) {
            FocusHistory focusHistory =
                    mFocusAreaHistoryCache.get(new FocusAreaHistory(sourceFocusArea, direction));
            if (mFocusAreaHistoryCache.isValidFocusHistory(focusHistory, elapsedRealtime)) {
                return copyNode(focusHistory.node);
            }
        }
        return null;
    }

    /**
     * Caches the focus area nudge history. A copy of {@code sourceFocusArea} and {@code
     * targetFocusArea} will be saved in the cache.
     */
    void saveTargetFocusArea(@NonNull AccessibilityNodeInfo sourceFocusArea,
            @NonNull AccessibilityNodeInfo targetFocusArea, int direction, long elapsedRealtime) {
        if (mFocusAreaHistoryCache.enabled()) {
            int oppositeDirection = getOppositeDirection(direction);
            mFocusAreaHistoryCache
                    .put(new FocusAreaHistory(copyNode(targetFocusArea), oppositeDirection),
                            new FocusHistory(copyNode(sourceFocusArea), elapsedRealtime));
        }
    }

    /** Clears the focus area nudge history cache. */
    void clearFocusAreaHistory() {
        if (mFocusAreaHistoryCache.enabled()) {
            mFocusAreaHistoryCache.evictAll();
        }
    }

    @VisibleForTesting
    boolean isFocusAreaHistoryCacheEmpty() {
        return mFocusAreaHistoryCache.size() == 0;
    }

    /** Returns the direction opposite the given {@code direction} */
    @VisibleForTesting
    static int getOppositeDirection(int direction) {
        switch (direction) {
            case View.FOCUS_LEFT:
                return View.FOCUS_RIGHT;
            case View.FOCUS_RIGHT:
                return View.FOCUS_LEFT;
            case View.FOCUS_UP:
                return View.FOCUS_DOWN;
            case View.FOCUS_DOWN:
                return View.FOCUS_UP;
        }
        throw new IllegalArgumentException("direction must be "
                + "FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, or FOCUS_RIGHT.");
    }

    /** Sets a mock Utils instance for testing. */
    @VisibleForTesting
    static void setUtils(@NonNull Utils utils) {
        sUtils = utils;
    }

    private static AccessibilityNodeInfo copyNode(@Nullable AccessibilityNodeInfo node) {
        return sUtils.copyNode(node);
    }
}
