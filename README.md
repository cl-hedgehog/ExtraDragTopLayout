引用的项目Github地址：[https://github.com/chenupt/DragTopLayout](https://github.com/chenupt/DragTopLayout)。
使用过程中遇到了部分bug，原来的Demo中也有一些地方不合适，修改并添加了Viewpager+RecyclerView的使用范例。并在原来的DragTopLayout的类中添加了注释。增加的例子从主页右上角的菜单键点开，最后两个就是了，名称为ViewpagerRecyclerActivity和StripTabHideActivity。其中
ViewpagerRecyclerActivity更像新浪微博的个人主页，两个tab中的列表只要有一个达到顶部并使得topView展开，另一个tab中的列表自动滚动到第一条。
StripTabHideActivity将tab切换放在了topView里。

#### 1.知识预备
* 事件传递机制
* ViewDragHelper使用分析：[Android ViewDragHelper源码解析](http://www.cnblogs.com/lqstayreal/p/4500219.html)

#### 2.控件思路分析
* 目标效果：豌豆荚主页效果
* 思路分析：整个View分为topView和contentView两部分，并且利用ViewDragHelper接管Touch事件，是否接管由一个标志性的boolean变量shouldIntercept决定，使得topView可以根据手势的滑动收缩Collapsed或者展开Expand，其中：
1.topView收缩时：ViewDragHelper不接管Touch事件，由contentView自己处理。
2.topView展开时：ViewDragHelper接管Touch事件，事件拦截，contentView不处理。
3.状态的衔接：滑动过程中contentView中有监听滑动动作，保证对滑动到顶部的事件实时监听，
比如对于RecyclerView调用方法：AttachUtil.isRecyclerViewAttach(recyclerView)可以获取当前是否需要拖管touch事件。
并通过回调或者EventBus通信传递到使用DragTopLayout的页面，可以对该控件的标志变量shouldIntercept进行实时更新，保证Touch事件有恰当的处理（根据各自的场景由ViewDragHelper或者contentView处理滑动）。
* 存在的问题：主要bug点在于：
1.tab点击选择页面时，如果页面是recyclerview点击时不会触发onscroll()所以不会更新点击事件托管状态。但是页面使用的是ListView或者GridView时点击tab后会触它们的发onscroll(),就触发了当前首条是否显示的状态传递，主页面收到了可能会误用，导致DragTopLayout的状态不对了。所以如果Fragment中使用的是recyclerview，需要在切换到该Fragment中获取recyclerView的状态来判断shouldIntercept。
使用viewpager和framelayout也有不同的影响，因为前者选中当前界面的时候还会触发相邻的ListView或者GridView的onScroll()。
2.tab空间区域点击判断cotentView的问题，区域误判，场景误判。返回true的前提还有当前是托管状态。--加强判断条件
3.一个tab页面滑到顶部联动第二tab页面个自动到顶部时，第二tab页面从很靠后位置滑动到顶部，切换tab的时候还未滑动完，导致topview的状态变化，因为这个方法引发onScroll；去掉耗时较长的动画滑动，将recyclerViewParent.smoothScrollToPosition(0);
改为recyclerViewParent.getLayoutManager().scrollToPosition(0);

#### 3.app使用中偶现问题
对偶现问题的调查-从代码角度；从现象角度--如何必现或尽最大可能重现

Q1：各分页面fagment各自滑动后点击tab切换，topview状态或者fragment的滑动状态不对。

Q2：实现新浪微博个人主页的效果，有一个页面滑动到顶部且引起topview显示后，另一个页面也滑动到顶部。

Q3：快速上下暴力滑动时topview没有收缩。

Q4：topView和contentView边缘临界滑动，topView不隐藏。
解决方法：
关键解决实时获取正确的指示变量并设置到页面的DragTopLayout实例中。
* 内部修改：加强变量的判断。tryCaptureView中

// return child == dragContentView

// 修改原因：加强条件，dragContentView可以移动的前提也是shouldIntercept==true

return child == dragContentView && shouldIntercept;

* 使用时注意：

A1：tab的点击切换页面的时，同步更新控件的状态，只监听当前显示的页面的滑动：除了传递boolean的shouldIntercept，还要再传递boolean值的来源标志。

A2：在状态监听中检测各个tab下列表滑动的状态，适当的条件下自动滑动到首条数据recyclerView.smoothScrollToPosition(0);改为recyclerView.getLayoutManager().scrollToPosition(0);

A3：弄清异常状态的值，对异常状态调整。topView显示与否与contentView中的滑动托管的boolean值shouldIntercept。

A4：临界状态，由contentView过渡到topView滑动时内部判断不准确，改为在使用的Activity中接收到的onEvent()或者回调接口中判断。

#### 4.引用文档
* [仿豌豆荚ViewPager下拉：DragTopLayout](http://www.open-open.com/lib/view/open1422430262923.html)

* [ViewDragHelper源码分析](http://www.jianshu.com/p/07d717ef0b28?utm_campaign=hugo&utm_medium=reader_share&utm_content=note)

* [Android ViewDragHelper源码解析](http://www.cnblogs.com/lqstayreal/p/4500219.html)

