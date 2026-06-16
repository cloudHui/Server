package com.gamer.data.mpcserver.commands.excel;

import java.io.File;

import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;

/**
 * 创建“表格区域”（MCP 实现为：NamedRange + AutoFilter）。
 *
 * <p>
 * POI 无法直接完整模拟 Excel 结构化表（XSSFTable）特性，
 * 因此本工具提供最小可用语义：把某个 A1 区域注册为命名区域，并启用自动筛选。
 * </p>
 *
 * <p>
 * 参数：
 * </p>
 * <ul>
 * <li>fileAbsolutePath（必填）：Excel 文件绝对路径</li>
 * <li>sheetName（必填）：Sheet 名称</li>
 * <li>tableName（必填）：表名（用于 NamedRange 名称）</li>
 * <li>range（可选）：A1 或 A1:C3；未指定则按 used range 自动推断</li>
 * </ul>
 */
@Process("excel_create_table")
public class ExcelCreateTableCommand implements CommandHandler {
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

        String tableName = McpUtils.text(params, "tableName");
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("params.tableName不能为空");
        }

        String range = McpUtils.text(params, "range");
        File file = new File(filePath.trim());
        Workbook wb = ExcelWorkbookCache.getInstance().get(file);
        Sheet sheet = wb.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("找不到sheet: " + sheetName);
        }

        ExcelMcpUtil.CellRange cr;
        if (range != null && !range.trim().isEmpty()) {
            cr = ExcelMcpUtil.parseA1Range(range);
        } else {
            cr = ExcelMcpUtil.calcUsedRange(sheet);
        }

        // 建 NamedRange：同名存在则直接复用 Name 并更新 refersToFormula
        Name existed = wb.getName(tableName);

        CellReference a = new CellReference(cr.startRow, cr.startCol, true, true);
        CellReference b = new CellReference(cr.endRow, cr.endCol, true, true);
        String refers = "'" + sheet.getSheetName() + "'!" + a.formatAsString() + ":" + b.formatAsString();

        if (existed != null) {
            try {
                existed.setRefersToFormula(refers);
            } catch (Exception ignored) {
                // ignore
            }
        } else {
            Name nameObj = wb.createName();
            nameObj.setNameName(tableName);
            nameObj.setRefersToFormula(refers);
        }

        // 启用 AutoFilter（通常以首行作为 header）
        try {
            sheet.setAutoFilter(new CellRangeAddress(cr.startRow, cr.endRow, cr.startCol, cr.endCol));
        } catch (Exception ignored) {
            // ignore
        }

        ExcelMcpUtil.saveWorkbook(wb, file);
        ExcelWorkbookCache.getInstance().markFileSaved(file, wb);
        String ok = "Successfully created table: name=" + tableName + " refers=" + refers;
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_EXCEL,
            "createTableRegion file=" + file.getAbsolutePath() + " sheet=" + sheetName + " tableName=" + tableName
                + " range=" + (range == null ? "" : range.trim()),
            ok);
        return CommandResult.of(ok);
    }
}
