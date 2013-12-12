/*
 * Copyright (C) 2013 Onur-Hayri Bakici
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

package com.thehayro.view;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.Interpolator;

import com.thehayro.internal.Constants;

import java.lang.reflect.Field;

import static com.thehayro.internal.Constants.ADAPTER_STATE;
import static com.thehayro.internal.Constants.FLAG_LEFT_IS_NOT_NULL;
import static com.thehayro.internal.Constants.FLAG_RIGHT_IS_NOT_NULL;
import static com.thehayro.internal.Constants.LOG_TAG;
import static com.thehayro.internal.Constants.PAGE_POSITION_CENTER;
import static com.thehayro.internal.Constants.PAGE_POSITION_LEFT;
import static com.thehayro.internal.Constants.PAGE_POSITION_RIGHT;
import static com.thehayro.internal.Constants.SUPER_STATE;

/**
 * ViewPager that allows infinite scrolling.
 */
public class InfiniteViewPager extends ViewPager {

    private static final String TAG = "InfiniteViewPager";

    private boolean mIsPagerEnabled = true;

    private int mCurrPosition = PAGE_POSITION_CENTER;
    private OnInfinitePageChangeListener mListener;

    public InfiniteViewPager(Context context) {
        this(context, null);
        postInitViewPager();
    }

    public InfiniteViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        postInitViewPager();
    }

    private CustomScroller mScroller = null;

    private InfinitePagerAdapter.OnInitializeListener mOnInitializeListener = new InfinitePagerAdapter.OnInitializeListener() {
        @Override
        public void onInitialized(int initState) {
            final InfinitePagerAdapter adapter = (InfinitePagerAdapter) getAdapter();
            if (adapter == null) {
                return;
            }

            boolean isLeftExists  = (initState & FLAG_LEFT_IS_NOT_NULL) != 0;
            boolean isRightExists = (initState & FLAG_RIGHT_IS_NOT_NULL) != 0;

            if (!isLeftExists && !isRightExists) {
                adapter.movePageContents(PAGE_POSITION_CENTER, PAGE_POSITION_LEFT);
                adapter.movePagesLeft();
                adapter.setCount(1);
                adapter.notifyDataSetChanged();
                setCurrentItem(PAGE_POSITION_LEFT, false);
            } else if (!isRightExists) {
                adapter.movePageContents(PAGE_POSITION_CENTER, PAGE_POSITION_RIGHT);
                adapter.movePageContents(PAGE_POSITION_LEFT, PAGE_POSITION_CENTER);
                adapter.movePagesRight();
                boolean hasLeft = adapter.fillPage(PAGE_POSITION_LEFT);
                if (!hasLeft) {
                    adapter.movePageContents(PAGE_POSITION_CENTER, PAGE_POSITION_LEFT);
                    adapter.movePageContents(PAGE_POSITION_RIGHT, PAGE_POSITION_CENTER);
                    adapter.movePagesLeft();
                    setCurrentItem(PAGE_POSITION_CENTER, false);
                    adapter.setCount(2);
                    adapter.notifyDataSetChanged();
                } else {
                    setCurrentItem(PAGE_POSITION_RIGHT, false);
                }
            } else if (!isLeftExists) {
                adapter.movePageContents(PAGE_POSITION_CENTER, PAGE_POSITION_LEFT);
                adapter.movePageContents(PAGE_POSITION_RIGHT, PAGE_POSITION_CENTER);
                adapter.movePagesLeft();
                boolean hasRight = adapter.fillPage(PAGE_POSITION_RIGHT);
                if (!hasRight) {
                    adapter.setCount(2);
                    adapter.notifyDataSetChanged();
                }
                setCurrentItem(PAGE_POSITION_LEFT, false);
            }
        }
    };



    /**
     * Override the Scroller instance with our own class so we can change the
     * duration
     */
    private void postInitViewPager() {
        try {
            Field scroller = ViewPager.class.getDeclaredField("mScroller");
            scroller.setAccessible(true);
            Field interpolator = ViewPager.class.getDeclaredField("sInterpolator");
            interpolator.setAccessible(true);

            mScroller = new CustomScroller(getContext(),
                    (Interpolator) interpolator.get(null));
            scroller.set(this, mScroller);
        } catch (Exception e) {
        }
    }

    /**
     * Set the factor by which the duration will change
     */
    public void setAnimationDuration(int animationDuration) {
        if (mScroller != null) {
            mScroller.setAnimationDuration(animationDuration);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final InfinitePagerAdapter adapter = (InfinitePagerAdapter) getAdapter();
        if (adapter == null) {
            Log.d(LOG_TAG, " onSaveInstanceState adapter == null");
            return super.onSaveInstanceState();
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(SUPER_STATE, super.onSaveInstanceState());
        bundle.putString(ADAPTER_STATE, adapter.getStringRepresentation(adapter.getCurrentIndicator()));

        return bundle;
    }

    @Override
    public void onRestoreInstanceState(final Parcelable state) {
        final InfinitePagerAdapter adapter = (InfinitePagerAdapter) getAdapter();
        if (adapter == null) {
            if (Constants.DEBUG) {
                Log.w(LOG_TAG, "onRestoreInstanceState adapter == null");
            }
            super.onRestoreInstanceState(state);
            return;
        }
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            final String representation = bundle.getString(ADAPTER_STATE);
            final Object c = adapter.convertToIndicator(representation);
            adapter.setCurrentIndicator(c);
            super.onRestoreInstanceState(bundle.getParcelable(SUPER_STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }

    private void initInfiniteViewPager() {
        setCurrentItem(PAGE_POSITION_CENTER, false);
        setOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float positionOffset, int positionOffsetPixels) {
                if (mListener != null && getAdapter() != null) {
                    final InfinitePagerAdapter adapter = (InfinitePagerAdapter) getAdapter();
                    mListener.onPageScrolled(adapter.getCurrentIndicator(), positionOffset, positionOffsetPixels);
                }
            }

            @Override
            public void onPageSelected(int position) {
                mCurrPosition = position;
                if (Constants.DEBUG) {
                    Log.d(TAG, "on page " + position);
                }
                if (mListener != null && getAdapter() != null) {
                    final InfinitePagerAdapter adapter = (InfinitePagerAdapter) getAdapter();
                    mListener.onPageSelected(adapter.getCurrentIndicator(), position);
                }
            }

            @Override
            public void onPageScrollStateChanged(final int state) {
                if (mListener != null) {
                    mListener.onPageScrollStateChanged(state);
                }
                final InfinitePagerAdapter adapter = (InfinitePagerAdapter) getAdapter();
                if (adapter == null) {
                    return;
                }

                switch (state) {
                    case ViewPager.SCROLL_STATE_IDLE:
                        if (mCurrPosition == PAGE_POSITION_LEFT) {
                            if (adapter.canScrollPrevious()) {
                                adapter.movePageContents(PAGE_POSITION_CENTER, PAGE_POSITION_RIGHT);
                                adapter.movePageContents(PAGE_POSITION_LEFT, PAGE_POSITION_CENTER);
                                adapter.fillPage(PAGE_POSITION_LEFT);
                                setCurrentItem(PAGE_POSITION_CENTER, false);
                            }
                        } else if (mCurrPosition == PAGE_POSITION_RIGHT) {
                            if (adapter.canScrollNext()) {
                                adapter.movePageContents(PAGE_POSITION_CENTER, PAGE_POSITION_LEFT);
                                adapter.movePageContents(PAGE_POSITION_RIGHT, PAGE_POSITION_CENTER);
                                adapter.fillPage(PAGE_POSITION_RIGHT);
                                setCurrentItem(PAGE_POSITION_CENTER, false);
                            }
                        }

                        mIsPagerEnabled = true;
                        break;
                    case ViewPager.SCROLL_STATE_SETTLING:
                        mIsPagerEnabled = false;
                        break;
                }
            }
        });
    }

    @Override
    public final void setCurrentItem(final int item) {
        /*if (item != PAGE_POSITION_CENTER) {
            throw new RuntimeException("Cannot change page index unless its 1.");
        }*/
        super.setCurrentItem(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mIsPagerEnabled) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mIsPagerEnabled) {
            return super.onInterceptTouchEvent(event);
        }

        return true;
    }

    /**
     * Set the current {@code indicator}.
     * @param indicator the new indicator to set.
     */
    public final void setCurrentIndicator(final Object indicator) {
        final PagerAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        final InfinitePagerAdapter infinitePagerAdapter = (InfinitePagerAdapter) adapter;
        final Object currentIndicator = infinitePagerAdapter.getCurrentIndicator();
        if (currentIndicator.getClass() != indicator.getClass()) {
            return;
        }
        infinitePagerAdapter.reset();
        infinitePagerAdapter.setCurrentIndicator(indicator);
        for (int i = 0; i < Constants.PAGE_COUNT; i++) {
            infinitePagerAdapter.fillPage(i);
        }
    }

    @Override
    public final void setOffscreenPageLimit(final int limit) {
        if (limit != getOffscreenPageLimit()) {
            throw new RuntimeException("OffscreenPageLimit cannot be changed.");
        }
        super.setOffscreenPageLimit(limit);
    }

    @Override
    public void setAdapter(final PagerAdapter adapter) {
        if (adapter instanceof InfinitePagerAdapter) {
            ((InfinitePagerAdapter) adapter).setOnInitializeListener(mOnInitializeListener);
            super.setAdapter(adapter);
            initInfiniteViewPager();
        } else {
            throw new IllegalArgumentException("Adapter should be an instance of InfinitePagerAdapter.");
        }
    }

    public void setOnInfinitePageChangeListener(OnInfinitePageChangeListener listener) {
        mListener = listener;
    }

    /**
     * Callback interface for responding to changing state of the selected indicator.
     */
    public static interface OnInfinitePageChangeListener {

        /**
         * This method will be invoked when the current page is scrolled, either as part
         * of a programmatically initiated smooth scroll or a user initiated touch scroll.
         *
         * @param indicator Indicator of the first page currently being displayed.
         * @param positionOffset Value from [0, 1) indicating the offset from the page at position.
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        void onPageScrolled(Object indicator, float positionOffset, int positionOffsetPixels);

        /**
         * This method will be invoked when a new page has been selected.
         * @param indicator the indicator of this page.
         */
        void onPageSelected(Object indicator, int position);

        /**
         * Called when the scroll state changes. Useful for discovering when the user
         * begins dragging, when the pager is automatically settling to the current page,
         * or when it is fully stopped/idle.
         *
         * @param state The new scroll state.
         * @see ViewPager#SCROLL_STATE_IDLE
         * @see ViewPager#SCROLL_STATE_DRAGGING
         * @see ViewPager#SCROLL_STATE_SETTLING
         */
        void onPageScrollStateChanged(final int state);
    }
}
