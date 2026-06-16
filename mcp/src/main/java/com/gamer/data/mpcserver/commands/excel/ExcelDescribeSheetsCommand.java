package com.gamer.data.mpcserver.commands.excel;

import java.io.File;

import org.apache.poi.ss.usermodel.Row;
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
 * 列出 Excel 的工作表信息。
 *
 * <p>xlsx：走 {@link ExcelXlsxEventHelper#describeXlsxSheets(File)}，仅解析 workbook 与各 sheet 的 dimension，避免整本 {@code XSSFWorkbook}。</p>
 * <p>xls：仍用 {@link ExcelWorkbookCache} 打开工作簿。</p>
 */
@Process("excel_describe_sheets")
public class ExcelDescribeSheetsCommand implements CommandHandler {
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

        File file = new File(filePath.trim());
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Excel文件不存在: " + file.getAbsolutePath());
        }

        if (ExcelXlsxEventHelper.isXlsxFile(file)) {
            String out = ExcelXlsxEventHelper.describeXlsxSheets(file);
            McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_EXCEL, "describeSheets xlsx(streamMeta) file="
                + file.getAbsolutePath(), out);
            return CommandResult.of(out);
        }

        Workbook wb = ExcelWorkbookCache.getInstance().getReadOnly(file);

        int sheetCount = wb.getNumberOfSheets();
        StringBuilder sb = new StringBuilder();
        sb.append("SHEET_COUNT=").append(sheetCount).append("\n");
        sb.append("SHEET\tROWS\tCOLS\n");

        for (int i = 0; i < sheetCount; i++) {
            Sheet sheet = wb.getSheetAt(i);
            if (sheet == null) {
                continue;
            }

            String sheetName = sheet.getSheetName();
            int lastRow = sheet.getLastRowNum();
            int rows = lastRow + 1;
            if (rows < 0) {
                rows = 0;
            }

            int cols = getHeaderCols(sheet);
            sb.append(sheetName).append("\t").append(rows).append("\t").append(cols).append("\n");
        }

        String out = sb.toString();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_EXCEL, "describeSheets file="
            + file.getAbsolutePath(), out);
        return CommandResult.of(out);
    }

    /**
     * 用第 0 行列数估算 sheet 宽度。若第 0 行为空则尝试第 1 行，最多检查前 3 行。
     */
    private int getHeaderCols(Sheet sheet) {
        if (sheet == null) {
            return 0;
        }
        int checkRows = Math.min(sheet.getLastRowNum() + 1, 3);
        for (int r = 0; r < checkRows; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            short lastCell = row.getLastCellNum();
            if (lastCell > 0) {
                return lastCell;
            }
        }
        return 0;
    }
}
