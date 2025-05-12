package com.container.loading.model;

import lombok.Data;

@Data
public class Cylinder {
    private int id;          // 货物编号
    private double radius;   // 半径
    private double height;   // 高度
    private double value;    // 价值
    private double x;        // x坐标位置
    private double y;        // y坐标位置
    private double z;        // z坐标位置
    private String color;    // 颜色（RGB格式，例如：#FF0000）
} 