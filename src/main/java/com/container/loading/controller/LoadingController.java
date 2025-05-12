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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class LoadingController {

    @Autowired
    private LoadingService loadingService;

    @GetMapping("/")
    public String index(Model model) {
        // 向视图传递可用策略列表
        model.addAttribute("strategies", loadingService.getAvailableStrategies());
        return "index";
    }
    
    @GetMapping("/strategies")
    @ResponseBody
    public Map<String, String> getStrategies() {
        // 提供获取策略列表的API
        return loadingService.getAvailableStrategies();
    }

    @PostMapping("/upload")
    @ResponseBody
    public Map<String, Object> handleFileUpload(
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam("length") double length,
                                         @RequestParam("width") double width,
                                         @RequestParam("height") double height,
                                         @RequestParam(value = "strategy", defaultValue = "volume") String strategy) throws IOException {
        // 读取Excel文件
        List<Cylinder> cylinders = ExcelUtil.readCylinders(file);
        
        // 创建集装箱对象
        Container container = new Container();
        container.setLength(length);
        container.setWidth(width);
        container.setHeight(height);
        
        // 计算装箱方案，使用指定策略
        loadingService.calculateLoading(cylinders, container, strategy);
        
        // 计算未放置的圆柱体数量
        List<Cylinder> unplacedCylinders = cylinders.stream()
                .filter(c -> c.getZ() < 0)
                .collect(Collectors.toList());
        
        // 获取当前使用的策略名称
        String strategyName = loadingService.getAvailableStrategies().get(strategy);
        
        // 将结果写入新的Excel文件
        ExcelUtil.writeResults(cylinders, "result.xlsx", strategyName);
        
        // 返回结果数据
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("cylinders", cylinders);
        resultMap.put("unplacedCount", unplacedCylinders.size());
        resultMap.put("totalCount", cylinders.size());
        resultMap.put("strategy", strategyName);
        
        return resultMap;
    }
} 