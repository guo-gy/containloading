package com.container.loading.service;

import com.container.loading.model.Cylinder;
import java.util.List;

/**
 * 装箱策略接口
 * 定义不同装箱策略的共同方法
 */
public interface LoadingStrategy {
    
    /**
     * 对圆柱体进行排序
     * @param cylinders 需要排序的圆柱体列表
     */
    void sortCylinders(List<Cylinder> cylinders);
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getStrategyName();
} 