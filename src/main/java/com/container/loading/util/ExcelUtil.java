package com.container.loading.util;

import com.container.loading.model.Cylinder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtil {

    /**
     * 从Excel文件读取圆柱体数据
     * @param file Excel文件
     * @return 圆柱体列表
     * @throws IOException 文件读取异常
     */
    public static List<Cylinder> readCylinders(MultipartFile file) throws IOException {
        List<Cylinder> cylinders = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 跳过标题行
            boolean isFirstRow = true;
            int id = 1;
            
            for (Row row : sheet) {
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }
                
                // 读取半径和高度数据
                double radius = 0;
                double height = 0;
                double value = 0;
                
                Cell radiusCell = row.getCell(0);
                if (radiusCell != null) {
                    radius = radiusCell.getNumericCellValue();
                }
                
                Cell heightCell = row.getCell(1);
                if (heightCell != null) {
                    height = heightCell.getNumericCellValue();
                }
                
                // 读取第三列中的价值数据（如果存在）
                Cell valueCell = row.getCell(2);
                if (valueCell != null) {
                    try {
                        value = valueCell.getNumericCellValue();
                    } catch (Exception e) {
                        // 如果无法读取为数值，则设为0
                        value = 0;
                    }
                }
                
                // 创建圆柱体对象
                Cylinder cylinder = new Cylinder();
                cylinder.setId(id++);
                cylinder.setRadius(radius);
                cylinder.setHeight(height);
                cylinder.setValue(value);
                
                // 初始未放置位置设为负坐标
                cylinder.setX(-radius);
                cylinder.setY(-radius);
                cylinder.setZ(-height);
                
                cylinders.add(cylinder);
            }
        }
        
        return cylinders;
    }

    /**
     * 将结果写入Excel文件
     * @param cylinders 圆柱体列表
     * @param filePath 文件路径
     * @param strategyName 使用的策略名称
     * @throws IOException 文件写入异常
     */
    public static void writeResults(List<Cylinder> cylinders, String filePath, String strategyName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("装箱结果");
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("货物ID");
            headerRow.createCell(1).setCellValue("半径");
            headerRow.createCell(2).setCellValue("高度");
            headerRow.createCell(3).setCellValue("价值");
            headerRow.createCell(4).setCellValue("X坐标");
            headerRow.createCell(5).setCellValue("Y坐标");
            headerRow.createCell(6).setCellValue("Z坐标");
            headerRow.createCell(7).setCellValue("装载状态");
            headerRow.createCell(8).setCellValue("使用策略");
            
            // 填充数据行
            int rowNum = 1;
            for (Cylinder cylinder : cylinders) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(cylinder.getId());
                row.createCell(1).setCellValue(cylinder.getRadius());
                row.createCell(2).setCellValue(cylinder.getHeight());
                row.createCell(3).setCellValue(cylinder.getValue());
                row.createCell(4).setCellValue(cylinder.getX());
                row.createCell(5).setCellValue(cylinder.getY());
                row.createCell(6).setCellValue(cylinder.getZ());
                row.createCell(7).setCellValue(cylinder.getZ() >= 0 ? "已装载" : "未装载");
                row.createCell(8).setCellValue(strategyName);
            }
            
            // 自动调整列宽
            for (int i = 0; i < 9; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }
} 