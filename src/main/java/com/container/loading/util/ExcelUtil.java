package com.container.loading.util;

import com.container.loading.model.Cylinder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtil {
    
    public static List<Cylinder> readCylinders(MultipartFile file) throws IOException {
        List<Cylinder> cylinders = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        
        int rowNum = 1; // 从第二行开始读取（跳过表头）
        Row row;
        while ((row = sheet.getRow(rowNum)) != null) {
            Cylinder cylinder = new Cylinder();
            cylinder.setId(rowNum);
            cylinder.setRadius(row.getCell(0).getNumericCellValue());
            cylinder.setHeight(row.getCell(1).getNumericCellValue());
            cylinders.add(cylinder);
            rowNum++;
        }
        
        workbook.close();
        return cylinders;
    }
    
    public static void writeResults(List<Cylinder> cylinders, String outputPath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("装箱结果");
        
        // 创建表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("货物编号");
        headerRow.createCell(1).setCellValue("X坐标");
        headerRow.createCell(2).setCellValue("Y坐标");
        headerRow.createCell(3).setCellValue("Z坐标");
        
        // 写入数据
        int rowNum = 1;
        for (Cylinder cylinder : cylinders) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(cylinder.getId());
            row.createCell(1).setCellValue(cylinder.getX());
            row.createCell(2).setCellValue(cylinder.getY());
            row.createCell(3).setCellValue(cylinder.getZ());
        }
        
        // 自动调整列宽
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // 保存文件
        workbook.write(new java.io.FileOutputStream(outputPath));
        workbook.close();
    }
} 