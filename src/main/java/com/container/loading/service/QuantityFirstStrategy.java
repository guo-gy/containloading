package com.container.loading.service;

import com.container.loading.model.Cylinder;
import java.util.List;

/**
 * 数量优先策略
 * 按圆柱体体积从小到大排序，优先放置更多数量的小物体
 */
public class QuantityFirstStrategy implements LoadingStrategy {
    
    @Override
    public void sortCylinders(List<Cylinder> cylinders) {
        // 按体积从小到大排序，优先放置小物体
        cylinders.sort((c1, c2) -> Double.compare(
            Math.PI * c1.getRadius() * c1.getRadius() * c1.getHeight(),
            Math.PI * c2.getRadius() * c2.getRadius() * c2.getHeight()
        ));
    }
    
    @Override
    public String getStrategyName() {
        return "数量优先";
    }
} 