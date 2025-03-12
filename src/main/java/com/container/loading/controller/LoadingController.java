package com.container.loading.controller;

import com.container.loading.model.Container;
import com.container.loading.model.Cylinder;
import com.container.loading.service.LoadingService;
import com.container.loading.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class LoadingController {

    @Autowired
    private LoadingService loadingService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    @ResponseBody
    public List<Cylinder> handleFileUpload(@RequestParam("file") MultipartFile file,
                                         @RequestParam("length") double length,
                                         @RequestParam("width") double width,
                                         @RequestParam("height") double height) throws IOException {
        // 读取Excel文件
        List<Cylinder> cylinders = ExcelUtil.readCylinders(file);
        
        // 创建集装箱对象
        Container container = new Container();
        container.setLength(length);
        container.setWidth(width);
        container.setHeight(height);
        
        // 计算装箱方案
        loadingService.calculateLoading(cylinders, container);
        
        // 将结果写入新的Excel文件
        ExcelUtil.writeResults(cylinders, "result.xlsx");
        
        return cylinders;
    }
} 