package com.atguigu.yygh.cmn.test;

import com.alibaba.excel.EasyExcel;

public class ReadTest {
    public static void main(String[] args) {
        String fileName = "D:\\work\\excal\\stu.xlsx";
        EasyExcel.read(fileName,Stu.class,new ExcelListener()).sheet().doRead();
    }
}
