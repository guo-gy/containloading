package com.container.loading.service;

import com.container.loading.model.Cylinder;
import java.util.List;

/**
 * 编号优先策略
 * 按圆柱体编号从小到大排序，优先放置编号较小的物体
 */
public class IdFirstStrategy implements LoadingStrategy {
    
    @Override
    public void sortCylinders(List<Cylinder> cylinders) {
        // 按编号从小到大排序
        cylinders.sort((c1, c2) -> Integer.compare(c1.getId(), c2.getId()));
    }
    
    @Override
    public String getStrategyName() {
        return "编号优先";
    }
} 