package com.container.loading.service;

import com.container.loading.model.Container;
import com.container.loading.model.Cylinder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Comparator;
import java.util.Random;

@Service
public class LoadingService {
    private final Random random = new Random();
    
    // 生成随机颜色
    private String generateRandomColor() {
        // 生成随机的 RGB 值
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        // 转换为十六进制格式
        return String.format("#%02X%02X%02X", r, g, b);
    }
    
    public void calculateLoading(List<Cylinder> cylinders, Container container) {
        // 按体积从大到小排序
        cylinders.sort((c1, c2) -> Double.compare(
            Math.PI * c2.getRadius() * c2.getRadius() * c2.getHeight(),
            Math.PI * c1.getRadius() * c1.getRadius() * c1.getHeight()
        ));
        
        // 简单的装箱策略：从底部开始，逐层放置
        double currentX = 0;
        double currentY = 0;
        double currentZ = 0;
        double layerHeight = 0;
        
        for (Cylinder cylinder : cylinders) {
            // 为圆柱体分配随机颜色
            cylinder.setColor(generateRandomColor());
            
            // 如果当前行放不下，移到下一行
            if (currentX + 2 * cylinder.getRadius() > container.getLength()) {
                currentX = 0;
                currentY += 2 * cylinder.getRadius();
                
                // 如果当前层放不下，移到下一层
                if (currentY + 2 * cylinder.getRadius() > container.getWidth()) {
                    currentX = 0;
                    currentY = 0;
                    currentZ += layerHeight;
                    layerHeight = 0;
                }
            }
            
            // 更新圆柱体位置
            cylinder.setX(currentX + cylinder.getRadius());
            cylinder.setY(currentY + cylinder.getRadius());
            cylinder.setZ(currentZ);
            
            // 更新当前层的最大高度
            layerHeight = Math.max(layerHeight, cylinder.getHeight());
            
            // 移动到下一个位置
            currentX += 2 * cylinder.getRadius();
            
            // 检查是否超出容器限制
            if (currentZ + cylinder.getHeight() > container.getHeight()) {
                throw new RuntimeException("容器空间不足，无法完成装箱");
            }
        }
    }
} 