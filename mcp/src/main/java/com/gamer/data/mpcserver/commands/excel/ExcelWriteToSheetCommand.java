package com.gamer.data.mpcserver.commands.excel;

import java.io.File;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * 写入 Excel sheet 的指定 A1 范围。
 *
 * <p>参数（params）：</p>
 * <ul>
 *   <li>fileAbsolutePath（必填）：Excel 文件绝对路径</li>
 *   <li>sheetName（必填）：目标 Sheet 名称</li>
 *   <li>newSheet（可选）：Sheet 不存在时是否新建（true/false 或 "true"/"false"）</li>
 *   <li>range（必填）：如 A1 或 A1:C3</li>
 *   <li>values（必填）：二维数组 JSON 字符串 或 TSV 字符串 或单值</li>
 * </ul>
 *
 * <p>values 支持：</p>
 * <ul>
 *   <li>JSON：二维数组（[ [a,b], [c,d] ]）</li>
 *   <li>TSV：用换行分行、用 tab 分列</li>
 *   <li>纯文本：写入 range 左上角</li>
 * </ul>
 */
@Process("excel_write_to_sheet")
public class ExcelWriteToSheetCommand implements CommandHandler {
    @Override
    public CommandResult handle(CommandContext ctx, JsonNode params) throws Exception {
        if (ctx == null) {
            throw new IllegalArgumentException("CommandContext不能为空");
        }
        String filePath = McpUtils.text(params, "fileAbsolutePath");
        if (filePath == null || filePath.trim().isEmpty()) {
            filePath = McpUtils.text(params, "file");
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("params.fileAbsolutePath不能为空");
        }

        String sheetName = McpUtils.text(params, "sheetName");
        if (sheetName == null || sheetName.trim().isEmpty()) {
            sheetName = McpUtils.text(params, "sheet");
        }
        if (sheetName == null || sheetName.trim().isEmpty()) {
            throw new IllegalArgumentException("params.sheetName不能为空");
        }

        String range = McpUtils.text(params, "range");
        if (range == null || range.trim().isEmpty()) {
            throw new IllegalArgumentException("params.range不能为空（如 A1 或 A1:C3）");
        }

        String values = McpUtils.text(params, "values");
        if (values == null) {
            throw new IllegalArgumentException("params.values不能为空");
        }

        String newSheetStr = McpUtils.text(params, "newSheet");
        boolean newSheet = false;
        if (newSheetStr != null) {
            String s = newSheetStr.trim();
            newSheet = "true".equalsIgnoreCase(s) || "1".equals(s);
        }

        File file = new File(filePath.trim());
        Workbook wb = ExcelWorkbookCache.getInstance().get(file);
        Sheet sheet = wb.getSheet(sheetName);
        if (sheet == null) {
            if (!newSheet) {
                throw new IllegalArgumentException("Sheet不存在: " + sheetName + ", newSheet=false");
            }
            sheet = wb.createSheet(sheetName);
        }

        ExcelMcpUtil.CellRange cr = ExcelMcpUtil.parseA1Range(range);
        ObjectMapper mapper = ctx.mapper();
        List<List<String>> values2D = ExcelMcpUtil.parseValues2D(values, mapper);

        ExcelMcpUtil.fillRangeByValues(sheet, cr, values2D);

        ExcelMcpUtil.saveWorkbook(wb, file);
        ExcelWorkbookCache.getInstance().markFileSaved(file, wb);
        String ok = "Successfully wrote range=" + range + " sheet=" + sheetName + " file=" + file.getAbsolutePath();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_EXCEL,
            "writeToSheet file=" + file.getAbsolutePath() + " sheet=" + sheetName + " range=" + range + " newSheet=" + newSheet,
            ok);
        return CommandResult.of(ok);
    }
}

