package com.cm.util.template;

/**
 * @author shichao.xia
 * @date 2019/1/16 下午5:04
 */
public abstract class BizProcessCallBack<T> {

    /**
     * 检查入参
     */
    public abstract void checkParams();

    /**
     * 逻辑处理
     *
     * @return T
     */
    public abstract T process();

    /**
     * 处理失败后执行的操作
     */
    public void fail() {
    }

    /**
     * 处理成功后执行的操作
     */
    public void afterProcess() {
    }

}
