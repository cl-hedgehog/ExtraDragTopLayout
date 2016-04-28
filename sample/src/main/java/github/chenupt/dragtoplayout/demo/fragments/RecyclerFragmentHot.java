package github.chenupt.dragtoplayout.demo.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.greenrobot.event.EventBus;
import github.chenupt.dragtoplayout.Logger;
import github.chenupt.dragtoplayout.demo.DataService;
import github.chenupt.dragtoplayout.demo.R;
import github.chenupt.dragtoplayout.demo.utils.MyEvent;
import github.chenupt.multiplemodel.recycler.ModelRecyclerAdapter;

/**
 * @ClassName: RecyclerFragmentHot
 * @Description:
 * @author bohe
 * @date 2016/4/28 14:01
 */
public class RecyclerFragmentHot extends RecyclerBaseFragment {

    private static String TAG = RecyclerFragmentHot.class.getSimpleName();

    public static RecyclerFragmentHot newInstance(String name) {
        RecyclerFragmentHot fragment = new RecyclerFragmentHot();
        TAG = name;
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recycler, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
    }

    private void initViews() {
        RecyclerView recyclerView = (RecyclerView) getView().findViewById(R.id.recycler_view);
        // init recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        ModelRecyclerAdapter adapter = new ModelRecyclerAdapter(getActivity(), DataService.getInstance().getModelManager());
        adapter.setList(DataService.getInstance().getList());
        recyclerView.setAdapter(adapter);
        // set data source
        adapter.notifyDataSetChanged();

        // TODO: 设置recyclerView，方便Ay中对recyclerView进行控制
        setRecyclerView(recyclerView);
        // attach top listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                Logger.e("TAG= ", TAG + ", getShouldDelegateTouch= " + getShouldDelegateTouch());
                EventBus.getDefault().post(new MyEvent(getShouldDelegateTouch(), TAG));
            }
        });

    }
}


