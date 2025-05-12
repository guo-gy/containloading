package com.container.loading.service;

import com.container.loading.model.Cylinder;
import java.util.List;

/**
 * 大体积优先策略
 * 按圆柱体体积从大到小排序
 */
public class VolumeFirstStrategy implements LoadingStrategy {
    
    @Override
    public void sortCylinders(List<Cylinder> cylinders) {
        // 按体积从大到小排序
        cylinders.sort((c1, c2) -> Double.compare(
            Math.PI * c2.getRadius() * c2.getRadius() * c2.getHeight(),
            Math.PI * c1.getRadius() * c1.getRadius() * c1.getHeight()
        ));
    }
    
    @Override
    public String getStrategyName() {
        return "大体积优先";
    }
} 