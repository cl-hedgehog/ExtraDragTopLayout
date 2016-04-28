package github.chenupt.dragtoplayout.demo;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import github.chenupt.dragtoplayout.DragTopLayout;
import github.chenupt.dragtoplayout.Logger;
import github.chenupt.dragtoplayout.demo.fragments.RecyclerBaseFragment;
import github.chenupt.dragtoplayout.demo.fragments.RecyclerFragmentHot;
import github.chenupt.dragtoplayout.demo.fragments.RecyclerFragmentNew;
import github.chenupt.dragtoplayout.demo.utils.MyEvent;

public class ViewpagerRecyclerActivity extends AppCompatActivity {

    @Bind(R.id.tool_bar)
    Toolbar toolBar;
    @Bind(R.id.drag_layout)
    DragTopLayout dragLayout;

    ViewPager viewPager;
    RelativeLayout topView;
    LinearLayout contentView;
    RadioGroup rgTab;
    RadioButton rbtnHot;
    RadioButton rbtnNew;
    ImageView ivLoading;
    TextView tvNickName;

    RecyclerBaseFragment fragmentHot;
    RecyclerBaseFragment fragmentNew;
    RecyclerBaseFragment fragmentCurrent;
    FragmentPagerAdapter ftAdapter;
    List<RecyclerBaseFragment> fragmentList = new ArrayList<>();
    int currentIndex = 0;

    final String[] mTitles = {"fragmentHot", "fragmentNew"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vp_recycler);
        ButterKnife.bind(this);
        toolBar.setTitle("ViewpagerRecyclerActivity");
        setSupportActionBar(toolBar);

        initView();
        initListener();
        viewPager.setCurrentItem(0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    private void initView() {
        topView = (RelativeLayout) dragLayout.findViewById(R.id.ll_top_view);
        contentView = (LinearLayout) dragLayout.findViewById(R.id.ll_content_view);

        ivLoading = (ImageView) topView.findViewById(R.id.iv_loading);
        tvNickName = (TextView) topView.findViewById(R.id.tv_nickname);
        rgTab = (RadioGroup) contentView.findViewById(R.id.rg_tab);
        rbtnHot = (RadioButton) contentView.findViewById(R.id.rbtn_hot);
        rbtnNew = (RadioButton) contentView.findViewById(R.id.rbtn_new);
        viewPager = (ViewPager) contentView.findViewById(R.id.view_pager);

        rbtnHot.setChecked(true);
        rbtnHot.setTextColor(getResources().getColor(R.color.colorPrimary));
        rbtnNew.setTextColor(getResources().getColor(R.color.gray_dark));

        fragmentHot = RecyclerFragmentHot.newInstance("fragmentHot");
        fragmentNew = RecyclerFragmentNew.newInstance("fragmentNew");
        fragmentList.add(fragmentHot);
        fragmentList.add(fragmentNew);

        ftAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public RecyclerBaseFragment getItem(int position) {
                return fragmentList.get(position);
            }

            @Override
            public int getCount() {
                return fragmentList == null ? 0 : fragmentList.size();
            }
        };
        viewPager.setAdapter(ftAdapter);
    }

    private void initListener() {
        rgTab.setOnCheckedChangeListener(new CheckListener());

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setSelectedFragment(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // todo：1.及时更新各个Viewpager下Fragment的状态
        dragLayout.setPanelListener(new DragTopLayout.PanelListener() {
            @Override
            public void onPanelStateChanged(DragTopLayout.PanelState panelState) {
                //when  top show, all fragment scroll to first item
                // 有一个到顶部，其他都到顶部
                if (panelState == DragTopLayout.PanelState.EXPANDED) {
                    for (int i = 0; i < fragmentList.size(); i++) {
                        if (i != currentIndex) {
                            fragmentList.get(i).scrollToFirstItem();
                        }
                    }
                }
            }

            @Override
            public void onSliding(float ratio) {

            }

            @Override
            public void onRefresh() {
                tvNickName.setText("is Loading");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dragLayout.setRefreshing(false);
                        tvNickName.setText("Loading ok");
                    }
                }, 2000);
            }
        });

        topView.setClickable(true);
        topView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ViewpagerRecyclerActivity.this, "onClickTop", Toast.LENGTH_SHORT).show();
            }
        });

        tvNickName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ViewpagerRecyclerActivity.this, "onClick TextView", Toast.LENGTH_SHORT).show();
            }
        });
        ivLoading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ViewpagerRecyclerActivity.this, "onClick ivLoading", Toast.LENGTH_SHORT).show();
            }
        });


    }


    private class CheckListener implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.rbtn_hot:
                    currentIndex = 0;
                    break;
                case R.id.rbtn_new:
                    currentIndex = 1;
                    break;
                default:
                    break;
            }
            viewPager.setCurrentItem(currentIndex);
        }
    }

    private void setSelectedFragment(int type) {
        currentIndex = type;
        // update status
        updateTopStatus(currentIndex);
        if (type == 0) {
            // fisrt tab
            rbtnHot.setTextColor(getResources().getColor(R.color.colorPrimary));
            rbtnNew.setTextColor(getResources().getColor(R.color.gray_dark));
        } else if (type == 1) {
            rbtnNew.setTextColor(getResources().getColor(R.color.colorPrimary));
            rbtnHot.setTextColor(getResources().getColor(R.color.gray_dark));

        }

    }

    /**
     * todo：2.切换时更新各个Viewpager下Fragment的状态
     *
     * @param currentIndex
     */
    private void updateTopStatus(int currentIndex) {
        fragmentCurrent = fragmentList.get(currentIndex);
        if (dragLayout.getState() != DragTopLayout.PanelState.COLLAPSED) {
            if (!fragmentCurrent.getShouldDelegateTouch()) {
                // topView显示时，却不是首条显示时，更换为首条显示===防止意外情况，实际可能不会出现
                fragmentCurrent.scrollToFirstItem();
                dragLayout.setTouchMode(true);
            }
        } else {
            dragLayout.setTouchMode(fragmentCurrent.getShouldDelegateTouch());
        }
    }

    /**
     * todo：3.接收事件，根据滑动情况实时更新DragLayout的拦截flag，只接收本Fragment中的滑动
     *
     * @param event
     */
    public void onEvent(MyEvent event) {
        boolean b = (boolean) event.getObj();
        String from = event.getMsg();
        Logger.e("from= ", from + ",shouldIntercept= " + b);
        if (from.equals(mTitles[currentIndex])) {
            dragLayout.setTouchMode(b);
            if (!b && dragLayout.getState() == DragTopLayout.PanelState.EXPANDED) {
                // 防止异常情况的补充
                dragLayout.closeTopView(false);
            }
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }
}
