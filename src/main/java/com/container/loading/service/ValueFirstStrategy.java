package com.container.loading.service;

import com.container.loading.model.Cylinder;
import java.util.List;

/**
 * 价值优先策略
 * 按圆柱体价值从高到低排序，优先放置高价值的物体
 */
public class ValueFirstStrategy implements LoadingStrategy {
    
    @Override
    public void sortCylinders(List<Cylinder> cylinders) {
        // 按价值从高到低排序
        cylinders.sort((c1, c2) -> Double.compare(c2.getValue(), c1.getValue()));
    }
    
    @Override
    public String getStrategyName() {
        return "价值优先";
    }
} 