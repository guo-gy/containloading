package com.container.loading.service;

import com.container.loading.model.Container;
import com.container.loading.model.Cylinder;
import java.util.*;

/**
 * 价值最大化策略
 * 使用启发式算法寻找总价值最大的装载方案
 */
public class ValueMaximizationStrategy implements LoadingStrategy {
    
    @Override
    public void sortCylinders(List<Cylinder> cylinders) {
        // 首先按价值密度（价值/体积）排序
        cylinders.sort((c1, c2) -> {
            double density1 = calculateValueDensity(c1);
            double density2 = calculateValueDensity(c2);
            return Double.compare(density2, density1); // 价值密度从高到低
        });
    }
    
    /**
     * 计算圆柱体的价值密度
     * @param cylinder 圆柱体
     * @return 价值密度（价值/体积）
     */
    private double calculateValueDensity(Cylinder cylinder) {
        double volume = Math.PI * Math.pow(cylinder.getRadius(), 2) * cylinder.getHeight();
        return cylinder.getValue() / volume;
    }
    
    @Override
    public String getStrategyName() {
        return "价值最大化";
    }
    
    /**
     * 使用价值最大化算法优化装载
     * 这个方法会在LoadingService中被调用
     * 
     * @param cylinders 圆柱体列表
     * @param container 容器
     * @return 优化后的圆柱体放置顺序
     */
    public List<Cylinder> optimizeLoading(List<Cylinder> cylinders, Container container) {
        // 复制原始列表，防止修改原始数据
        List<Cylinder> result = new ArrayList<>(cylinders);
        
        // 1. 首先使用价值密度排序
        sortCylinders(result);
        
        // 2. 使用贪心算法尝试初步放置
        List<Cylinder> initialPlacement = new ArrayList<>(result);
        
        // 3. 使用局部搜索优化初步方案
        return localSearch(initialPlacement, container);
    }
    
    /**
     * 局部搜索优化算法
     * 尝试交换、替换等操作，寻找更优的解
     * 
     * @param initialPlacement 初步放置方案
     * @param container 容器
     * @return 优化后的放置方案
     */
    private List<Cylinder> localSearch(List<Cylinder> initialPlacement, Container container) {
        List<Cylinder> currentBest = new ArrayList<>(initialPlacement);
        double currentBestValue = calculateTotalValue(initialPlacement);
        boolean improved;
        
        // 最大迭代次数，防止无限循环
        int maxIterations = 100;
        int iteration = 0;
        
        do {
            improved = false;
            iteration++;
            
            // 尝试交换操作
            for (int i = 0; i < currentBest.size(); i++) {
                for (int j = i + 1; j < currentBest.size(); j++) {
                    List<Cylinder> candidateSolution = new ArrayList<>(currentBest);
                    // 交换两个圆柱体的位置
                    Collections.swap(candidateSolution, i, j);
                    
                    double candidateValue = calculateTotalValue(candidateSolution);
                    if (candidateValue > currentBestValue) {
                        currentBest = candidateSolution;
                        currentBestValue = candidateValue;
                        improved = true;
                    }
                }
            }
            
            // 尝试移除某些低价值圆柱体，以放入更多高价值密度的小圆柱体
            if (!improved && iteration < maxIterations / 2) {
                List<Cylinder> candidateRemoval = new ArrayList<>(currentBest);
                // 按价值密度从低到高排序
                candidateRemoval.sort(Comparator.comparingDouble(this::calculateValueDensity));
                
                // 移除价值密度最低的圆柱体
                if (!candidateRemoval.isEmpty()) {
                    Cylinder removed = candidateRemoval.remove(0);
                    double volumeFreed = Math.PI * Math.pow(removed.getRadius(), 2) * removed.getHeight();
                    
                    // 尝试找到能放入的、价值密度更高的小圆柱体
                    List<Cylinder> candidates = new ArrayList<>(initialPlacement);
                    candidates.removeAll(candidateRemoval);
                    
                    // 按价值密度从高到低排序
                    candidates.sort((c1, c2) -> Double.compare(
                        calculateValueDensity(c2), calculateValueDensity(c1)
                    ));
                    
                    double additionalVolume = 0;
                    double additionalValue = 0;
                    List<Cylinder> toAdd = new ArrayList<>();
                    
                    // 贪心地添加小圆柱体，直到填满释放的空间
                    for (Cylinder candidate : candidates) {
                        double candidateVolume = Math.PI * Math.pow(candidate.getRadius(), 2) * candidate.getHeight();
                        if (additionalVolume + candidateVolume <= volumeFreed * 1.1) { // 允许10%的误差
                            additionalVolume += candidateVolume;
                            additionalValue += candidate.getValue();
                            toAdd.add(candidate);
                        }
                    }
                    
                    // 如果替换后总价值更高，则接受此方案
                    if (additionalValue > removed.getValue()) {
                        candidateRemoval.addAll(toAdd);
                        double candidateValue = calculateTotalValue(candidateRemoval);
                        if (candidateValue > currentBestValue) {
                            currentBest = candidateRemoval;
                            currentBestValue = candidateValue;
                            improved = true;
                        }
                    }
                }
            }
            
        } while (improved && iteration < maxIterations);
        
        return currentBest;
    }
    
    /**
     * 计算圆柱体列表的总价值
     * 
     * @param cylinders 圆柱体列表
     * @return 总价值
     */
    private double calculateTotalValue(List<Cylinder> cylinders) {
        return cylinders.stream()
                .mapToDouble(Cylinder::getValue)
                .sum();
    }
} 