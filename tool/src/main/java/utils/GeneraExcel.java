package utils;

import utils.other.excel.ExcelUtil;

/**
 * @author admin
 * @className GenerXml
 * @description
 * @createDate 2025/10/11 16:03
 */
public class GeneraExcel {

	public static void main(String[] args){
		//读取文件生成结构
		ExcelUtil.readExcelCreateJavaHead("TableModel.xlsx", "tool");
	}
}
