package github.chenupt.dragtoplayout.demo.utils;

/**
 * @author bohe
 * @ClassName: MyEvent
 * @Description: 自定义的Event，用于EventBus
 * @date 2016/4/13 17:56
 */
public class MyEvent {

    private Object mObj;
    private String mMsg;
    public MyEvent(Object obj, String msg) {
        // TODO Auto-generated constructor stub
        mObj = obj;
        mMsg = msg;
    }
    public String getMsg(){
        return mMsg;
    }

    public Object getObj(){
        return mObj;
    }
}

