/*
 * Copyright 2015 chenupt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * imitations under the License.
 */

package github.chenupt.dragtoplayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;


/**
 * Created by chenupt@gmail.com on 2015/1/18.
 * Description : Drag down to show a menu panel on the top.
 */
public class DragTopLayout extends FrameLayout {

    private ViewDragHelper dragHelper;
    private int dragRange;
    private View dragContentView;
    private View topView;
    private ImageView img;

    private int contentTop;
    private int topViewHeight;
    private float ratio;
    private boolean isRefreshing;
    private boolean shouldIntercept = true;

    private PanelListener panelListener;
    private float refreshRatio = 1.5f;
    private boolean overDrag = true;
    private int collapseOffset;
    private int topViewId = -1;
    private int dragContentViewId = -1;
    private boolean captureTop = true;

    // Used for scrolling
    private boolean dispatchingChildrenDownFaked = false;
    private boolean dispatchingChildrenContentView = false;
    private float dispatchingChildrenStartedAtY = Float.MAX_VALUE;

    private PanelState panelState = PanelState.EXPANDED;


    public static enum PanelState {

        COLLAPSED(0),
        EXPANDED(1),
        SLIDING(2);

        private int asInt;

        PanelState(int i) {
            this.asInt = i;
        }

        static PanelState fromInt(int i) {
            switch (i) {
                case 0:
                    return COLLAPSED;
                case 2:
                    return SLIDING;
                default:
                case 1:
                    return EXPANDED;
            }
        }

        public int toInt() {
            return asInt;
        }
    }


    public DragTopLayout(Context context) {
        this(context, null);
    }

    public DragTopLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragTopLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        dragHelper = ViewDragHelper.create(this, 1.0f, callback);

        // init from attrs
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DragTopLayout);
        setCollapseOffset(a.getDimensionPixelSize(R.styleable.DragTopLayout_dtlCollapseOffset, collapseOffset));
        overDrag = a.getBoolean(R.styleable.DragTopLayout_dtlOverDrag, overDrag);
        dragContentViewId = a.getResourceId(R.styleable.DragTopLayout_dtlDragContentView, -1);
        topViewId = a.getResourceId(R.styleable.DragTopLayout_dtlTopView, -1);
        initOpen(a.getBoolean(R.styleable.DragTopLayout_dtlOpen, true));
        captureTop = a.getBoolean(R.styleable.DragTopLayout_dtlCaptureTop, true);
        a.recycle();
    }

    private void initOpen(boolean initOpen) {
        if (initOpen) {
            panelState = PanelState.EXPANDED;
        } else {
            panelState = PanelState.COLLAPSED;
        }
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() < 2) {
            throw new RuntimeException("Content view must contains two child views at least.");
        }

        if (topViewId != -1 && dragContentViewId == -1) {
            throw new IllegalArgumentException("You have set \"dtlTopView\" but not \"dtlDragContentView\". Both are required!");
        }

        if (dragContentViewId != -1 && topViewId == -1) {
            throw new IllegalArgumentException("You have set \"dtlDragContentView\" but not \"dtlTopView\". Both are required!");
        }

        if (dragContentViewId != -1 && topViewId != -1) {
            bindId(this);
        } else {
            topView = getChildAt(0);
            dragContentView = getChildAt(1);
        }

    }

    private void bindId(View view) {
        topView = view.findViewById(topViewId);
        dragContentView = view.findViewById(dragContentViewId);

        if (topView == null) {
            throw new IllegalArgumentException("\"dtlTopView\" with id = \"@id/" + getResources().getResourceEntryName
                    (topViewId) + "\" has NOT been found. Is a child with that id in this " + getClass().getSimpleName() + "?");
        }


        if (dragContentView == null) {
            throw new IllegalArgumentException("\"dtlDragContentView\" with id = \"@id/" + getResources().getResourceEntryName
                    (dragContentViewId) + "\" has NOT been found. Is a child with that id in this " + getClass().getSimpleName
                    () + "?");
        }
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        dragRange = getHeight();
        // In case of resetting the content top to target position before sliding.
        int contentTopTemp = contentTop;
        resetTopViewHeight();
        resetContentHeight();
        topView.layout(left, Math.min(topView.getPaddingTop(), contentTop - topViewHeight), right, contentTop);
        dragContentView.layout(left, contentTopTemp, right, contentTopTemp + dragContentView.getHeight());

    }


    private void resetTopViewHeight() {
        int newTopHeight = topView.getHeight();
        // Top layout is changed
        if (topViewHeight != newTopHeight) {
            if (panelState == PanelState.EXPANDED) {
                contentTop = newTopHeight;
                handleSlide(newTopHeight);
            } else if (panelState == PanelState.COLLAPSED) {
                // update the drag content top when it is collapsed.
                contentTop = collapseOffset;
            }
            topViewHeight = newTopHeight;
        }
    }

    private void resetContentHeight() {
        if (dragContentView != null && dragContentView.getHeight() != 0) {
            ViewGroup.LayoutParams layoutParams = dragContentView.getLayoutParams();
            layoutParams.height = getHeight() - collapseOffset;
            dragContentView.setLayoutParams(layoutParams);
        }
    }

    private void handleSlide(final int top) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                dragHelper.smoothSlideViewTo(dragContentView, getPaddingLeft(), top);
                postInvalidate();
            }
        });
    }

    private void resetDragContent(boolean anim, int top) {
        contentTop = top;
        if (anim) {
            dragHelper.smoothSlideViewTo(dragContentView, getPaddingLeft(), contentTop);
            postInvalidate();
        } else {
            requestLayout();
        }
    }

    private void calculateRatio(float top) {
        // ratio>0时，contentView的新位置坐标大，需要隐藏TopView，top为drag时候获取到的view的目标坐标
        ratio = (top - collapseOffset) / (topViewHeight - collapseOffset);
        if (dispatchingChildrenContentView) {
            resetDispatchingContentView();
        }
        // 对外的UI或数据操作接口，refreshRatio为阈值，比之大的时候，这样的下拉动作可以触发执行某些操作如刷新
        if (panelListener != null) {
            // Calculate the ratio while dragging.
            panelListener.onSliding(ratio);
            if (ratio > refreshRatio && !isRefreshing) {
                isRefreshing = true;
                panelListener.onRefresh();
            }
        }
    }

    private void updatePanelState() {
        // 根据拖动过程中view top值的变化，更新状态
        if (contentTop <= getPaddingTop() + collapseOffset) {
            panelState = PanelState.COLLAPSED;
        } else if (contentTop >= topView.getHeight()) {
            // why not getPaddingTop() + collapseOffset + topView.getHeight() ---> clampViewPositionVertical
            panelState = PanelState.EXPANDED;
        } else {
            panelState = PanelState.SLIDING;
        }

        if (panelListener != null) {
            panelListener.onPanelStateChanged(panelState);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {

        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.panelState = panelState.toInt();

        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {

        if (!(state instanceof SavedState)) {
            // FIX #10
            super.onRestoreInstanceState(BaseSavedState.EMPTY_STATE);
            return;
        }

        SavedState s = (SavedState) state;
        super.onRestoreInstanceState(s.getSuperState());

        this.panelState = PanelState.fromInt(s.panelState);
        if (panelState == PanelState.COLLAPSED) {
            closeTopView(false);
        } else {
            openTopView(false);
        }
    }

    private ViewDragHelper.Callback callback = new ViewDragHelper.Callback() {
        // child为当前触摸区域下的View,如果返回true,就可以拖拽.
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            // 先利用tryCaptureView判断当前touch的view是否是目标拖动view，返回true则拖动处理，false不处理。
            if (child == topView && captureTop) {
                dragHelper.captureChildView(dragContentView, pointerId);
                return false;
            }
            // return child == dragContentView
            // ：修改原因：加强条件，dragContentView可以移动的前提也是shouldIntercept==true
            return child == dragContentView && shouldIntercept;
        }

        // 拖动过程中位置的改变
        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            // 在onViewPositionChanged方法中控制拖动后新位置的处理。因为拖动过程中还需对TopView进行相应地处理，所以在方法内记录拖动的top位置，
            // 并在onLayout回调方法中处理最新位置的现实。
            contentTop = top;
            requestLayout();// ---触发onLayout重新测量子控件的位置和大小
            calculateRatio(contentTop);
            updatePanelState();
        }

        //获取垂直方向允许拖拽的区域
        @Override
        public int getViewVerticalDragRange(View child) {
            return dragRange;
        }

        //top为child即将移动到的垂直位置的值,但是返回值会最终决定移动到的值
        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            // 默认返回0,子类需要重写以定义垂直拖动的目标位置
            // 拖动位置的处理，可以处理拖动过程中的最高位置或者最低位置
            int finalTop;
            if (overDrag) {
                // Drag over the top view height.
                finalTop = Math.max(top, getPaddingTop() + collapseOffset);
            } else {
                finalTop = Math.min(topViewHeight, Math.max(top, getPaddingTop() + collapseOffset));
            }
            // 处理拖动的最高高度不超过上边界。 top是尝试要垂直移动的终点坐标（应该是手势滑动到的位置），返回实际的终点坐标
            return finalTop;
        }

        // 松手时的回调
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            // 拖动手势释放后的处理, xvel和yvel是速度
            // yvel > 0 Fling down || yvel < 0 Fling up
            // 当释放手势后判断手势方向利用settleCapturedViewAt方法进行处理最终滚动位置。其中yvel参数>0代表快速往下滑动，否则为快速往上滑动。
            int top;
            if (yvel > 0 || contentTop > topViewHeight) {
                top = topViewHeight + getPaddingTop();
            } else {
                top = getPaddingTop() + collapseOffset;
            }
            // Settle the captured view at the given (left, top) position.
            dragHelper.settleCapturedViewAt(releasedChild.getLeft(), top);
            postInvalidate();
        }

        // 拖动后状态的改变
        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);

        }
    };

    @Override
    public void computeScroll() {
        // 覆盖computeScroll方法，用以实现拖动后的滚动效果--一般固定写法
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            // 将触摸事件能够传递给ViewDragHelper
            boolean intercept = shouldIntercept && dragHelper.shouldInterceptTouchEvent(ev);
            return intercept;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        if (!dispatchingChildrenContentView) {
            try {
                // There seems to be a bug on certain devices: "pointerindex out of range" in viewdraghelper
                // https://github.com/umano/AndroidSlidingUpPanel/issues/351
                dragHelper.processTouchEvent(event); //EXPAND的时候才执行，否则是contentView自己的滑动事件？
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // ratio == 0, when top = 0, COLLAPSED
        // test：以下是处理特殊情况的code，COLLAPSED时先向上在快速向下滑动能保证回到首条后topview能Expand
        if (action == MotionEvent.ACTION_MOVE && ratio == 0.0f) {
            dispatchingChildrenContentView = true;// true 表明contentView自己去处理滑动事件
            if (!dispatchingChildrenDownFaked) {
                dispatchingChildrenStartedAtY = event.getY();// 记录contentView开始处理滑动的初始坐标
                event.setAction(MotionEvent.ACTION_DOWN);
                dispatchingChildrenDownFaked = true;
            }
            dragContentView.dispatchTouchEvent(event);
        }
        // dispatchingChildrenStartedAtY 变量的作用：你接管滑动的初始坐标小于滑动到的坐标时，不再接管，事件交由ViewDragHelper
        if (dispatchingChildrenContentView && dispatchingChildrenStartedAtY < event.getY()) {
            resetDispatchingContentView();
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            resetDispatchingContentView();// 结束性的事件，恢复
            dragContentView.dispatchTouchEvent(event);
        }
        // true标记touch事件已经被消费掉，不再向下传递
        return true;
    }

    private void resetDispatchingContentView() {
        dispatchingChildrenDownFaked = false;
        dispatchingChildrenContentView = false;
        dispatchingChildrenStartedAtY = Float.MAX_VALUE;
    }


    //================
    // public
    //================

    public PanelState getState() {
        return panelState;
    }

    public void openTopView(boolean anim) {
        // Before created
        if (dragContentView.getHeight() == 0) {
            panelState = PanelState.EXPANDED;
            if (panelListener != null) {
                panelListener.onSliding(1.0f);
            }
        } else {
            resetDragContent(anim, topViewHeight);
        }
    }

    public void closeTopView(boolean anim) {
        if (dragContentView.getHeight() == 0) {
            panelState = PanelState.COLLAPSED;
            if (panelListener != null) {
                panelListener.onSliding(0.0f);
            }
        } else {
            resetDragContent(anim, getPaddingTop() + collapseOffset);
        }
    }

    public void updateTopViewHeight(int height) {
        ViewGroup.LayoutParams layoutParams = topView.getLayoutParams();
        layoutParams.height = height;
        topView.setLayoutParams(layoutParams);
    }

    public void toggleTopView() {
        toggleTopView(false);
    }

    public void toggleTopView(boolean touchMode) {
        switch (panelState) {
            case COLLAPSED:
                openTopView(true);
                if (touchMode) {
                    setTouchMode(true);
                }
                break;
            case EXPANDED:
                closeTopView(true);
                if (touchMode) {
                    setTouchMode(false);// topView收起的时候是不用dragHelper接管touchEvent的
                }
                break;
        }
    }

    public DragTopLayout setTouchMode(boolean shouldIntercept) {
        this.shouldIntercept = shouldIntercept;
        return this;
    }

    public boolean getTouchMode() {
        return this.shouldIntercept;
    }


    /**
     * Setup the drag listener.
     *
     * @return SetupWizard
     */
    public DragTopLayout setPanelListener(PanelListener panelListener) {
        this.panelListener = panelListener;
        return this;
    }

    /**
     * Set the refresh position while dragging you want.
     * The default value is 1.5f.
     *
     * @return SetupWizard
     */
    public DragTopLayout setRefreshRatio(float ratio) {
        this.refreshRatio = ratio;
        return this;
    }

    /**
     * Set enable drag over.
     * The default value is true.
     *
     * @return SetupWizard
     */
    public DragTopLayout setOverDrag(boolean overDrag) {
        this.overDrag = overDrag;
        return this;
    }

    /**
     * Set the content view. Pass the id of the view (R.id.xxxxx).
     * This one will be set as the content view and will be dragged together with the topView
     *
     * @param id The id (R.id.xxxxx) of the content view.
     * @return
     */
    public DragTopLayout setDragContentViewId(int id) {
        this.dragContentViewId = id;
        return this;
    }

    /**
     * Set the top view. The top view is the header view that will be dragged out.
     * Pass the id of the view (R.id.xxxxx)
     *
     * @param id The id (R.id.xxxxx) of the top view
     * @return
     */
    public DragTopLayout setTopViewId(int id) {
        this.topViewId = id;
        return this;
    }

    public boolean isOverDrag() {
        return overDrag;
    }

    /**
     * Get refresh state
     */
    public boolean isRefreshing() {
        return isRefreshing;
    }

    public void setRefreshing(boolean isRefreshing) {
        this.isRefreshing = isRefreshing;
    }

    /**
     * Complete refresh and reset the refresh state.
     */
    public void onRefreshComplete() {
        isRefreshing = false;
    }

    /**
     * Set the collapse offset
     *
     * @return SetupWizard
     */
    public DragTopLayout setCollapseOffset(int px) {
        collapseOffset = px;
        resetContentHeight();
        return this;
    }

    public int getCollapseOffset() {
        return collapseOffset;
    }


    // ---------------------

    public interface PanelListener {
        /**
         * Called while the panel state is changed.
         */
        public void onPanelStateChanged(PanelState panelState);

        /**
         * Called while dragging.
         * ratio >= 0.
         */
        public void onSliding(float ratio);

        /**
         * Called while the ratio over refreshRatio.
         */
        public void onRefresh();
    }

/*    public static class SimplePanelListener implements PanelListener {

        @Override
        public void onPanelStateChanged(PanelState panelState) {

        }

        @Override
        public void onSliding(float ratio) {

        }

        @Deprecated
        @Override
        public void onRefresh() {

        }
    }*/

    /**
     * Save the instance state
     */
    @SuppressLint("ParcelCreator")
    private static class SavedState extends BaseSavedState {

        int panelState;

        SavedState(Parcelable superState) {
            super(superState);
        }

    }
}
