package com.gamer.data.mpcserver.commands.excel;

import java.io.File;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.McpServiceLog;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;

/**
 * 格式化 Excel 指定 range（MCP 实现：应用简单 CellStyle）。
 *
 * <p>
 * styles（字符串）支持：
 * </p>
 * <ul>
 * <li>JSON 对象，例如：{"bold":true,"align":"center","vAlign":"middle"}</li>
 * </ul>
 *
 * <p>
 * 为了保证跨 POI 版本可编译，颜色相关字段先不做强约束。
 * </p>
 */
@Process("excel_format_range")
public class ExcelFormatRangeCommand implements CommandHandler {
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
            throw new IllegalArgumentException("params.range不能为空（如 A1:C3）");
        }

        String styles = McpUtils.text(params, "styles");
        if (styles == null) {
            styles = "";
        }

        File file = new File(filePath.trim());
        Workbook wb = ExcelWorkbookCache.getInstance().get(file);
        Sheet sheet = wb.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("找不到sheet: " + sheetName);
        }

        ExcelMcpUtil.CellRange cr = ExcelMcpUtil.parseA1Range(range);
        JsonNode styleNode = null;
        String s = styles.trim();
        if (!s.isEmpty() && (s.startsWith("{") || s.startsWith("["))) {
            try {
                // styles 预期是 object；如果传了 array，按空处理
                styleNode = ctx.mapper().readTree(s);
            } catch (Exception ignored) {
            }
        }

        CellStyle style = ExcelMcpUtil.buildSimpleCellStyle(wb, styleNode);
        ExcelMcpUtil.applyStyleToRange(sheet, cr, style);

        ExcelMcpUtil.saveWorkbook(wb, file);
        ExcelWorkbookCache.getInstance().markFileSaved(file, wb);
        String ok = "Successfully formatted range=" + range + " sheet=" + sheetName + " file=" + file.getAbsolutePath();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_EXCEL,
            "formatRange file=" + file.getAbsolutePath() + " sheet=" + sheetName + " range=" + range, ok);
        return CommandResult.of(ok);
    }
}
