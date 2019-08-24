package org.jerome.bigdata.datastream.datastream.trigger;

import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.triggers.TriggerResult;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

public class CustomProcessingTimeTrigger extends Trigger<Object, TimeWindow> {
    private static final long serialVersionUID = 1L;

    private CustomProcessingTimeTrigger() {}

    private static int flag = 0;
    @Override
    public TriggerResult onElement(Object element, long timestamp, TimeWindow window, TriggerContext ctx) {
        ctx.registerProcessingTimeTimer(window.maxTimestamp());
        // CONTINUE是代表不做输出，也即是，此时我们想要实现比如100条输出一次，
        // 而不是窗口结束再输出就可以在这里实现。
        if(flag > 9){
            flag = 0;
            return TriggerResult.FIRE;
        }else{
            flag++;
        }
        System.out.println("onElement : "+element);
        return TriggerResult.CONTINUE;
    }

    @Override
    public TriggerResult onEventTime(long time, TimeWindow window, TriggerContext ctx) throws Exception {
        return TriggerResult.CONTINUE;
    }

    @Override
    public TriggerResult onProcessingTime(long time, TimeWindow window, TriggerContext ctx) {
        return TriggerResult.FIRE;
    }

    @Override
    public void clear(TimeWindow window, TriggerContext ctx) throws Exception {
        ctx.deleteProcessingTimeTimer(window.maxTimestamp());
    }

    @Override
    public boolean canMerge() {
        return true;
    }

    @Override
    public void onMerge(TimeWindow window,
                        OnMergeContext ctx) {
        // only register a timer if the time is not yet past the end of the merged window
        // this is in line with the logic in onElement(). If the time is past the end of
        // the window onElement() will fire and setting a timer here would fire the window twice.
        long windowMaxTimestamp = window.maxTimestamp();
        if (windowMaxTimestamp > ctx.getCurrentProcessingTime()) {
            ctx.registerProcessingTimeTimer(windowMaxTimestamp);
        }
    }

    @Override
    public String toString() {
        return "ProcessingTimeTrigger()";
    }

    /**
     * Creates a new trigger that fires once system time passes the end of the window.
     */
    public static CustomProcessingTimeTrigger create() {
        return new CustomProcessingTimeTrigger();
    }

}
