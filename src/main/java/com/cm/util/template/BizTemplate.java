package com.cm.util.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shichao.xia
 * @date 2019/1/16 下午5:03
 */
public class BizTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(BizTemplate.class);

    private BizTemplate() {
    }

    /**
     * 有返回值的biz模板
     *
     * @param action 回调方法
     * @param <T>    类型
     * @return 执行结果
     */
    public static <T> T execute(BizProcessCallBack<T> action) {
        T result = null;
        try {
            // 参数检验
            {
                action.checkParams();
            }
            // 执行业务逻辑
            {
                result = action.process();
            }
        } catch (Exception e) {
            LOGGER.error("Biz层执行业务逻辑出现异常：{}", e.getMessage(), e);
            // 执行业务逻辑出现异常
            {
                action.fail();
            }
        } finally {
            try {
                // 执行finally中方法
                {
                    action.afterProcess();
                }
            } catch (Exception e) {
                LOGGER.error("Biz层在执行finally中方法出现异常：{}", e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * 没有返回值的biz模板
     *
     * @param action 回调方法
     */
    public static void executeNoResult(BizProcessCallBackNoResult action) {
        execute(action);
    }
}
