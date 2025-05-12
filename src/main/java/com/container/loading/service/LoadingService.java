package com.container.loading.service;

import com.container.loading.model.Container;
import com.container.loading.model.Cylinder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Comparator;

@Service
public class LoadingService {
    private final Random random = new Random();
    private final Map<String, LoadingStrategy> strategies;
    
    // 构造函数，初始化所有策略
    public LoadingService() {
        strategies = new HashMap<>();
        strategies.put("volume", new VolumeFirstStrategy());
        strategies.put("quantity", new QuantityFirstStrategy());
        strategies.put("id", new IdFirstStrategy());
        strategies.put("value", new ValueFirstStrategy());
        strategies.put("valuemax", new ValueMaximizationStrategy());
    }
    
    // 获取所有可用策略
    public Map<String, String> getAvailableStrategies() {
        Map<String, String> strategyMap = new HashMap<>();
        strategies.forEach((key, strategy) -> 
            strategyMap.put(key, strategy.getStrategyName()));
        return strategyMap;
    }
    
    // 生成随机颜色
    private String generateRandomColor() {
        // 生成随机的 RGB 值
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        // 转换为十六进制格式
        return String.format("#%02X%02X%02X", r, g, b);
    }
    
    // 检查圆柱体是否与已放置的圆柱体重叠
    private boolean checkOverlap(Cylinder newCylinder, List<Cylinder> placedCylinders) {
        for (Cylinder placed : placedCylinders) {
            // 计算圆柱体底面圆心之间的平面距离
            double dx = newCylinder.getX() - placed.getX();
            double dy = newCylinder.getY() - placed.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            // 如果底面圆心距离小于两个圆柱体半径之和，则发生重叠
            if (distance < (newCylinder.getRadius() + placed.getRadius())) {
                // 检查高度方向是否重叠
                double newZBottom = newCylinder.getZ();
                double newZTop = newZBottom + newCylinder.getHeight();
                double placedZBottom = placed.getZ();
                double placedZTop = placedZBottom + placed.getHeight();
                
                // 如果高度方向也重叠，则判定为发生碰撞
                if (!(newZTop <= placedZBottom || newZBottom >= placedZTop)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // 检查圆柱体是否超出容器边界
    private boolean checkContainerBounds(Cylinder cylinder, Container container) {
        // 检查X轴方向
        if (cylinder.getX() - cylinder.getRadius() < 0 || 
            cylinder.getX() + cylinder.getRadius() > container.getLength()) {
            return false;
        }
        
        // 检查Y轴方向
        if (cylinder.getY() - cylinder.getRadius() < 0 || 
            cylinder.getY() + cylinder.getRadius() > container.getWidth()) {
            return false;
        }
        
        // 检查Z轴方向
        if (cylinder.getZ() < 0 || 
            cylinder.getZ() + cylinder.getHeight() > container.getHeight()) {
            return false;
        }
        
        return true;
    }
    
    // 尝试查找可放置的位置 - 为数量优先策略优化
    private boolean findValidPosition(Cylinder cylinder, Container container, List<Cylinder> placedCylinders, String strategyKey) {
        // 针对不同策略调整网格精度
        double gridStepFactor;
        if ("quantity".equals(strategyKey)) {
            // 数量优先策略使用更密集的网格
            gridStepFactor = 40.0;
        } else {
            // 其他策略使用常规网格
            gridStepFactor = 20.0;
        }
        
        double gridStep = Math.min(container.getLength(), container.getWidth()) / gridStepFactor;
        gridStep = Math.max(0.05, Math.min(gridStep, cylinder.getRadius())); // 确保网格精度不会太小，也不会大于圆柱体半径
        
        // 定义高度层数
        ArrayList<Double> heightLevels = new ArrayList<>();
        heightLevels.add(0.0); // 地面层
        
        // 把已放置圆柱体的顶面高度添加到层数列表
        for (Cylinder placed : placedCylinders) {
            double topZ = placed.getZ() + placed.getHeight();
            if (!heightLevels.contains(topZ)) {
                heightLevels.add(topZ);
            }
        }
        
        // 根据策略类型调整搜索顺序
        if ("quantity".equals(strategyKey)) {
            // 数量优先策略：优先填满底层
            heightLevels.sort(Comparator.naturalOrder());
        } else {
            // 其他策略：按自然顺序
            heightLevels.sort(Comparator.naturalOrder());
        }
        
        // 遍历每个高度层
        for (double z : heightLevels) {
            // 创建临时网格点列表
            List<GridPoint> gridPoints = new ArrayList<>();
            
            // 在每个高度层，生成网格点
            for (double x = cylinder.getRadius(); x <= container.getLength() - cylinder.getRadius(); x += gridStep) {
                for (double y = cylinder.getRadius(); y <= container.getWidth() - cylinder.getRadius(); y += gridStep) {
                    gridPoints.add(new GridPoint(x, y));
                }
            }
            
            // 根据不同策略对网格点进行排序
            if ("quantity".equals(strategyKey)) {
                // 数量优先：优先选择靠近原点的位置
                gridPoints.sort(Comparator.comparingDouble(p -> Math.sqrt(p.x * p.x + p.y * p.y)));
            } else if ("volume".equals(strategyKey)) {
                // 体积优先：优先选择靠近边缘的位置，以便放置大件物品
                gridPoints.sort(Comparator.comparingDouble(p -> 
                    -Math.sqrt((container.getLength() - p.x) * (container.getLength() - p.x) + 
                              (container.getWidth() - p.y) * (container.getWidth() - p.y))
                ));
            }
            
            // 遍历排序后的网格点
            for (GridPoint point : gridPoints) {
                // 设置当前待测位置
                cylinder.setX(point.x);
                cylinder.setY(point.y);
                cylinder.setZ(z);
                
                // 检查是否在容器内且不与其他圆柱体重叠
                if (checkContainerBounds(cylinder, container) && !checkOverlap(cylinder, placedCylinders)) {
                    return true; // 找到有效位置
                }
            }
        }
        
        return false; // 没有找到有效位置
    }
    
    public void calculateLoading(List<Cylinder> cylinders, Container container, String strategyKey) {
        // 获取并应用排序策略
        LoadingStrategy strategy = strategies.getOrDefault(strategyKey, strategies.get("volume"));
        
        // 特殊处理价值最大化策略
        if ("valuemax".equals(strategyKey) && strategy instanceof ValueMaximizationStrategy) {
            // 增强价值最大化策略处理 - 预先计算多种可能的装载组合
            ValueMaximizationStrategy valueMaxStrategy = (ValueMaximizationStrategy) strategy;
            
            // 先按价值密度排序
            strategy.sortCylinders(cylinders);
            
            // 保存初始排序
            List<Cylinder> initialOrder = new ArrayList<>(cylinders);
            
            // 计算当前排序方式的装载结果
            List<Cylinder> placedCylinders = new ArrayList<>();
            List<Cylinder> unplacedCylinders = new ArrayList<>();
            double initialTotalValue = 0;
            
            // 先尝试装载
            for (Cylinder cylinder : cylinders) {
                cylinder.setColor(generateRandomColor());
                boolean placed = findValidPosition(cylinder, container, placedCylinders, strategyKey);
                
                if (placed) {
                    placedCylinders.add(cylinder);
                    initialTotalValue += cylinder.getValue();
                } else {
                    unplacedCylinders.add(cylinder);
                    cylinder.setX(-cylinder.getRadius());
                    cylinder.setY(-cylinder.getRadius());
                    cylinder.setZ(-cylinder.getHeight());
                }
            }
            
            // 记录最佳方案
            double bestTotalValue = initialTotalValue;
            List<Cylinder> bestPlacement = new ArrayList<>(placedCylinders);
            
            // 尝试不同的策略排序
            tryAlternativeStrategy(cylinders, container, "volume", bestPlacement, bestTotalValue);
            tryAlternativeStrategy(cylinders, container, "quantity", bestPlacement, bestTotalValue);
            
            // 尝试价值降序排序
            List<Cylinder> valueOrder = new ArrayList<>(cylinders);
            valueOrder.sort((c1, c2) -> Double.compare(c2.getValue(), c1.getValue()));
            tryExplicitOrder(cylinders, container, valueOrder, bestPlacement, bestTotalValue);
            
            // 尝试价值/体积比排序
            List<Cylinder> densityOrder = new ArrayList<>(cylinders);
            densityOrder.sort((c1, c2) -> {
                double volume1 = Math.PI * c1.getRadius() * c1.getRadius() * c1.getHeight();
                double volume2 = Math.PI * c2.getRadius() * c2.getRadius() * c2.getHeight();
                return Double.compare(c2.getValue() / volume2, c1.getValue() / volume1);
            });
            tryExplicitOrder(cylinders, container, densityOrder, bestPlacement, bestTotalValue);
            
            // 应用最佳方案
            restoreBestPlacement(cylinders, bestPlacement);
            
            // 进行第二轮优化
            unplacedCylinders = cylinders.stream()
                    .filter(c -> c.getZ() < 0)
                    .collect(java.util.stream.Collectors.toList());
            
            placedCylinders = cylinders.stream()
                    .filter(c -> c.getZ() >= 0)
                    .collect(java.util.stream.Collectors.toList());
                    
            // 再次尝试优化放置
            if (!unplacedCylinders.isEmpty()) {
                for (Cylinder cylinder : unplacedCylinders) {
                    tryRefinedPlacement(cylinder, container, placedCylinders, strategyKey);
                }
            }
            
            // 记录未放置数量统计
            long finalUnplacedCount = cylinders.stream().filter(c -> c.getZ() < 0).count();
            if (finalUnplacedCount > 0) {
                System.out.println("警告：使用" + strategy.getStrategyName() + "策略，有" + finalUnplacedCount + "个圆柱体无法放入容器中。");
            }
            
            return;
        }
        
        // 常规策略处理
        strategy.sortCylinders(cylinders);
        
        List<Cylinder> placedCylinders = new ArrayList<>();
        List<Cylinder> unplacedCylinders = new ArrayList<>();
        
        // 遍历每个圆柱体，寻找最优放置位置
        for (Cylinder cylinder : cylinders) {
            // 为圆柱体分配随机颜色
            cylinder.setColor(generateRandomColor());
            
            // 寻找有效放置位置，传入策略标识
            boolean placed = findValidPosition(cylinder, container, placedCylinders, strategyKey);
            
            if (placed) {
                placedCylinders.add(cylinder);
            } else {
                unplacedCylinders.add(cylinder);
                // 设置无效位置（可选：放在容器外以便识别）
                cylinder.setX(-cylinder.getRadius());
                cylinder.setY(-cylinder.getRadius());
                cylinder.setZ(-cylinder.getHeight());
            }
        }
        
        // 如果有未能放置的圆柱体，进行第二轮尝试
        // 这次使用更严格的网格搜索寻找可能的位置
        if (!unplacedCylinders.isEmpty()) {
            for (Cylinder cylinder : unplacedCylinders) {
                // 再次尝试更精细的位置搜索
                tryRefinedPlacement(cylinder, container, placedCylinders, strategyKey);
            }
        }
        
        // 如果仍有未能放置的圆柱体，显示警告（但不抛出异常，让用户看到部分装箱结果）
        long unplacedCount = cylinders.stream().filter(c -> c.getZ() < 0).count();
        if (unplacedCount > 0) {
            System.out.println("警告：使用" + strategy.getStrategyName() + "策略，有" + unplacedCount + "个圆柱体无法放入容器中。");
        }
    }
    
    // 尝试替代策略排序，看是否能获得更高价值的装载方案
    private void tryAlternativeStrategy(List<Cylinder> originalCylinders, Container container, 
                                      String strategyKey, List<Cylinder> bestPlacement, double bestTotalValue) {
        // 复制圆柱体列表
        List<Cylinder> testCylinders = new ArrayList<>();
        for (Cylinder original : originalCylinders) {
            Cylinder copy = new Cylinder();
            copy.setId(original.getId());
            copy.setRadius(original.getRadius());
            copy.setHeight(original.getHeight());
            copy.setValue(original.getValue());
            testCylinders.add(copy);
        }
        
        // 应用策略排序
        LoadingStrategy strategy = strategies.get(strategyKey);
        if (strategy != null) {
            strategy.sortCylinders(testCylinders);
            
            // 尝试装载
            List<Cylinder> placedCylinders = new ArrayList<>();
            double totalValue = 0;
            
            for (Cylinder cylinder : testCylinders) {
                cylinder.setColor(generateRandomColor());
                boolean placed = findValidPosition(cylinder, container, placedCylinders, strategyKey);
                
                if (placed) {
                    placedCylinders.add(cylinder);
                    totalValue += cylinder.getValue();
                } else {
                    cylinder.setX(-cylinder.getRadius());
                    cylinder.setY(-cylinder.getRadius());
                    cylinder.setZ(-cylinder.getHeight());
                }
            }
            
            // 如果总价值更高，更新最佳方案
            if (totalValue > bestTotalValue) {
                bestTotalValue = totalValue;
                bestPlacement.clear();
                bestPlacement.addAll(placedCylinders);
            }
        }
    }
    
    // 尝试指定顺序的装载方案
    private void tryExplicitOrder(List<Cylinder> originalCylinders, Container container, 
                               List<Cylinder> orderedCylinders, List<Cylinder> bestPlacement, double bestTotalValue) {
        // 复制圆柱体列表
        List<Cylinder> testCylinders = new ArrayList<>();
        Map<Integer, Cylinder> idMap = new HashMap<>();
        
        for (Cylinder original : originalCylinders) {
            Cylinder copy = new Cylinder();
            copy.setId(original.getId());
            copy.setRadius(original.getRadius());
            copy.setHeight(original.getHeight());
            copy.setValue(original.getValue());
            testCylinders.add(copy);
            idMap.put(copy.getId(), copy);
        }
        
        // 按指定顺序排序
        List<Cylinder> sortedCylinders = new ArrayList<>();
        for (Cylinder ordered : orderedCylinders) {
            Cylinder copy = idMap.get(ordered.getId());
            if (copy != null) {
                sortedCylinders.add(copy);
            }
        }
        
        // 尝试装载
        List<Cylinder> placedCylinders = new ArrayList<>();
        double totalValue = 0;
        
        for (Cylinder cylinder : sortedCylinders) {
            cylinder.setColor(generateRandomColor());
            boolean placed = findValidPosition(cylinder, container, placedCylinders, "valuemax");
            
            if (placed) {
                placedCylinders.add(cylinder);
                totalValue += cylinder.getValue();
            } else {
                cylinder.setX(-cylinder.getRadius());
                cylinder.setY(-cylinder.getRadius());
                cylinder.setZ(-cylinder.getHeight());
            }
        }
        
        // 如果总价值更高，更新最佳方案
        if (totalValue > bestTotalValue) {
            bestTotalValue = totalValue;
            bestPlacement.clear();
            bestPlacement.addAll(placedCylinders);
        }
    }
    
    // 恢复最佳放置方案到原始圆柱体列表
    private void restoreBestPlacement(List<Cylinder> originalCylinders, List<Cylinder> bestPlacement) {
        // 创建最佳放置的ID到位置映射
        Map<Integer, Cylinder> bestPositions = new HashMap<>();
        for (Cylinder placed : bestPlacement) {
            bestPositions.put(placed.getId(), placed);
        }
        
        // 应用最佳位置到原始圆柱体
        for (Cylinder original : originalCylinders) {
            if (bestPositions.containsKey(original.getId())) {
                Cylinder bestPos = bestPositions.get(original.getId());
                original.setX(bestPos.getX());
                original.setY(bestPos.getY());
                original.setZ(bestPos.getZ());
            } else {
                // 未放置的圆柱体
                original.setX(-original.getRadius());
                original.setY(-original.getRadius());
                original.setZ(-original.getHeight());
            }
        }
    }
    
    // 使用更精细的搜索策略尝试放置 - 优化版本
    private void tryRefinedPlacement(Cylinder cylinder, Container container, List<Cylinder> placedCylinders, String strategyKey) {
        // 针对数量优先策略使用更密集的网格
        double gridStepFactor = "quantity".equals(strategyKey) ? 80.0 : 50.0;
        double gridStep = Math.min(container.getLength(), container.getWidth()) / gridStepFactor;
        gridStep = Math.max(0.05, Math.min(gridStep, cylinder.getRadius() / 2)); // 确保网格精度不会太小
        
        // 创建网格点列表
        List<GridPoint3D> gridPoints = new ArrayList<>();
        
        // 生成三维空间中的所有可能位置
        double zStep = Math.min(cylinder.getHeight() / 2, 0.5); // 高度方向的步长
        for (double z = 0; z <= container.getHeight() - cylinder.getHeight(); z += zStep) {
            for (double x = cylinder.getRadius(); x <= container.getLength() - cylinder.getRadius(); x += gridStep) {
                for (double y = cylinder.getRadius(); y <= container.getWidth() - cylinder.getRadius(); y += gridStep) {
                    gridPoints.add(new GridPoint3D(x, y, z));
                }
            }
        }
        
        // 根据策略调整网格点排序
        if ("quantity".equals(strategyKey)) {
            // 数量优先：优先底层且靠近原点的位置
            gridPoints.sort(Comparator
                .comparingDouble((GridPoint3D p) -> p.z) // 先按Z排序
                .thenComparingDouble(p -> Math.sqrt(p.x * p.x + p.y * p.y))); // 然后按到原点的距离
        } else if ("volume".equals(strategyKey)) {
            // 体积优先：优先底层且靠边缘的位置
            gridPoints.sort(Comparator
                .comparingDouble((GridPoint3D p) -> p.z) // 先按Z排序
                .thenComparingDouble(p -> -Math.sqrt(
                    (container.getLength() - p.x) * (container.getLength() - p.x) + 
                    (container.getWidth() - p.y) * (container.getWidth() - p.y)))); // 然后按到边缘的距离（倒序）
        } else {
            // 其他策略：优先底层
            gridPoints.sort(Comparator.comparingDouble(p -> p.z));
        }
        
        // 遍历排序后的网格点
        for (GridPoint3D point : gridPoints) {
            cylinder.setX(point.x);
            cylinder.setY(point.y);
            cylinder.setZ(point.z);
            
            // 检查是否在容器内
            if (checkContainerBounds(cylinder, container) && !checkOverlap(cylinder, placedCylinders)) {
                // 找到有效位置，更新列表
                placedCylinders.add(cylinder);
                return;
            }
        }
        
        // 如果仍然找不到位置，则标记为无法放置
        cylinder.setX(-cylinder.getRadius());
        cylinder.setY(-cylinder.getRadius());
        cylinder.setZ(-cylinder.getHeight());
    }
    
    // 辅助类：二维网格点
    private static class GridPoint {
        double x, y;
        
        GridPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
    
    // 辅助类：三维网格点
    private static class GridPoint3D {
        double x, y, z;
        
        GridPoint3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    // 保留原方法，默认使用体积优先策略，保持向后兼容性
    public void calculateLoading(List<Cylinder> cylinders, Container container) {
        calculateLoading(cylinders, container, "volume");
    }
} 