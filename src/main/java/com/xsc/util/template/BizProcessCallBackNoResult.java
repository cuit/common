package com.xsc.util.template;

/**
 * @author shichao.xia
 * @date 2019/1/16 下午5:04
 */
public abstract class BizProcessCallBackNoResult extends BizProcessCallBack<Void> {

    @Override
    public Void process() {
        processNoResult();
        return null;
    }

    public abstract void processNoResult();
}
