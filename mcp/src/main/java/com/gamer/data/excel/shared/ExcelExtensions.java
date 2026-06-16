package com.gamer.data.excel.shared;

/**
 * Excel 文件扩展名常量（与 {@code ModelGen#FILE_EXT_*} 取值一致），供不依赖代码生成模块的界面与读写工具使用，避免打包时牵出 {@code gen} 包。
 *
 * @author liuyunhui
 */
public class ExcelExtensions {

    /** 禁止实例化 */
    private ExcelExtensions() {}

    /** .xls 扩展名（带点） */
    public static final String FILE_EXT_XLS = ".xls";

    /** .xlsx 扩展名（带点） */
    public static final String FILE_EXT_XLSX = ".xlsx";
}
