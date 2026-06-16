package com.gamer.data.mpcserver.commands.excel;

import java.io.File;

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
 * 对指定 sheet range 生成“简单截图”（PNG base64）。
 *
 * <p>
 * 实现说明：
 * </p>
 * <ul>
 * <li>使用 Java2D 直接渲染单元格文本与网格，不依赖 Excel 渲染器。</li>
 * <li>为避免输出过大，range 行列会被限制（默认 maxRows=20, maxCols=10）。</li>
 * </ul>
 *
 * <p>
 * 参数：
 * </p>
 * <ul>
 * <li>fileAbsolutePath（必填）：Excel 文件绝对路径</li>
 * <li>sheetName（必填）：Sheet 名称</li>
 * <li>range（可选）：A1 或 A1:C3；未提供则按 used range 推断</li>
 * <li>maxRows（可选）：默认 20</li>
 * <li>maxCols（可选）：默认 10</li>
 * <li>maxCellTextLen（可选）：默认 18</li>
 * </ul>
 */
@Process("excel_screen_capture")
public class ExcelScreenCaptureCommand implements CommandHandler {
    private static final int DEFAULT_MAX_ROWS = 20;
    private static final int DEFAULT_MAX_COLS = 10;
    private static final int DEFAULT_MAX_CELL_TEXT_LEN = 18;

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
        Integer maxRows = McpUtils.intVal(params, "maxRows");
        Integer maxCols = McpUtils.intVal(params, "maxCols");
        Integer maxCellTextLen = McpUtils.intVal(params, "maxCellTextLen");

        int mr = maxRows == null || maxRows <= 0 ? DEFAULT_MAX_ROWS : maxRows;
        int mc = maxCols == null || maxCols <= 0 ? DEFAULT_MAX_COLS : maxCols;
        int mct = maxCellTextLen == null || maxCellTextLen <= 0 ? DEFAULT_MAX_CELL_TEXT_LEN : maxCellTextLen;

        File file = new File(filePath.trim());
        // 与 read/describe 共用 Workbook 缓存，避免重复解析同一路径 xlsx/xls。
        Workbook wb = ExcelWorkbookCache.getInstance().getReadOnly(file);
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

        String base64 = ExcelMcpUtil.renderRangeToPngBase64(wb, sheet, cr, mr, mc, mct);
        String out = "PNG_BASE64=" + base64;
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_EXCEL,
            "screenCapture file=" + file.getAbsolutePath() + " sheet=" + sheetName + " range="
                + (range == null ? "" : range.trim()) + " maxRows=" + mr + " maxCols=" + mc,
            out);
        return CommandResult.of(out);
    }
}
