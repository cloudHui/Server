package com.gamer.data.mpcserver.commands.excel;

import java.io.File;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
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
 * 复制一个 sheet（复制单元格值/公式/简单样式），覆盖目标 sheet 内容。
 *
 * <p>
 * 参数：
 * </p>
 * <ul>
 * <li>fileAbsolutePath（必填）：Excel 文件绝对路径</li>
 * <li>srcSheetName（必填）：源 Sheet 名称</li>
 * <li>dstSheetName（必填）：目标 Sheet 名称</li>
 * </ul>
 */
@Process("excel_copy_sheet")
public class ExcelCopySheetCommand implements CommandHandler {
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

        String srcSheetName = McpUtils.text(params, "srcSheetName");
        if (srcSheetName == null || srcSheetName.trim().isEmpty()) {
            srcSheetName = McpUtils.text(params, "srcSheet");
        }
        if (srcSheetName == null || srcSheetName.trim().isEmpty()) {
            throw new IllegalArgumentException("params.srcSheetName不能为空");
        }

        String dstSheetName = McpUtils.text(params, "dstSheetName");
        if (dstSheetName == null || dstSheetName.trim().isEmpty()) {
            dstSheetName = McpUtils.text(params, "dstSheet");
        }
        if (dstSheetName == null || dstSheetName.trim().isEmpty()) {
            throw new IllegalArgumentException("params.dstSheetName不能为空");
        }

        File file = new File(filePath.trim());
        Workbook wb = ExcelWorkbookCache.getInstance().get(file);
        Sheet src = wb.getSheet(srcSheetName);
        if (src == null) {
            throw new IllegalArgumentException("找不到源sheet: " + srcSheetName);
        }

        Sheet dst = wb.getSheet(dstSheetName);
        if (dst == null) {
            dst = wb.createSheet(dstSheetName);
        } else {
            clearSheet(dst);
        }

        copySheetStructure(wb, src, dst);

        ExcelMcpUtil.saveWorkbook(wb, file);
        ExcelWorkbookCache.getInstance().markFileSaved(file, wb);
        String ok = "Successfully copied sheet src=" + srcSheetName + " -> dst=" + dstSheetName + ", file="
            + file.getAbsolutePath();
        McpServiceLog.cmdAndResult(ctx.log(), McpServiceLog.SERVICE_EXCEL,
            "copySheet file=" + file.getAbsolutePath() + " src=" + srcSheetName + " dst=" + dstSheetName, ok);
        return CommandResult.of(ok);
    }

    private void clearSheet(Sheet sheet) {
        if (sheet == null) {
            return;
        }

        // 清空合并单元格（倒序删除，避免索引变化）
        int merged = sheet.getNumMergedRegions();
        for (int i = merged - 1; i >= 0; i--) {
            try {
                sheet.removeMergedRegion(i);
            } catch (Exception ignored) {
                // ignore
            }
        }

        int lastRow = sheet.getLastRowNum();
        for (int r = 0; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            short lastCell = row.getLastCellNum();
            for (int c = 0; c < lastCell; c++) {
                Cell cell = row.getCell(c);
                if (cell != null) {
                    cell.setBlank();
                }
            }
        }
    }

    private void copySheetStructure(Workbook wb, Sheet src, Sheet dst) {
        if (wb == null || src == null || dst == null) {
            return;
        }

        // 先计算 maxCol，用于拷贝列宽
        int maxCol = 0;
        int lastRow = src.getLastRowNum();
        for (int r = 0; r <= lastRow; r++) {
            Row row = src.getRow(r);
            if (row == null) {
                continue;
            }
            short lc = row.getLastCellNum();
            if (lc > maxCol) {
                maxCol = lc;
            }
        }
        for (int c = 0; c < maxCol; c++) {
            try {
                dst.setColumnWidth(c, src.getColumnWidth(c));
            } catch (Exception ignored) {
            }
        }

        // 合并单元格
        int merged = src.getNumMergedRegions();
        for (int i = 0; i < merged; i++) {
            try {
                dst.addMergedRegion(src.getMergedRegion(i));
            } catch (Exception ignored) {
            }
        }

        // 行/列单元格
        for (int r = 0; r <= lastRow; r++) {
            Row srcRow = src.getRow(r);
            if (srcRow == null) {
                continue;
            }
            Row dstRow = dst.getRow(r);
            if (dstRow == null) {
                dstRow = dst.createRow(r);
            }
            try {
                dstRow.setHeight(srcRow.getHeight());
            } catch (Exception ignored) {
            }

            short lastCell = srcRow.getLastCellNum();
            for (int c = 0; c < lastCell; c++) {
                Cell srcCell = srcRow.getCell(c);
                if (srcCell == null) {
                    continue;
                }

                Cell dstCell = dstRow.getCell(c);
                if (dstCell == null) {
                    dstCell = dstRow.createCell(c);
                }

                CellType type = srcCell.getCellType();
                if (type == null) {
                    continue;
                }

                switch (type) {
                    case STRING:
                        dstCell.setCellValue(srcCell.getStringCellValue());
                        break;
                    case NUMERIC:
                        dstCell.setCellValue(srcCell.getNumericCellValue());
                        break;
                    case BOOLEAN:
                        dstCell.setCellValue(srcCell.getBooleanCellValue());
                        break;
                    case FORMULA:
                        dstCell.setCellFormula(srcCell.getCellFormula());
                        break;
                    case BLANK:
                        dstCell.setBlank();
                        break;
                    default:
                        // ERROR/其他类型：退化为字符串
                        dstCell.setCellValue(srcCell.toString());
                        break;
                }

                // 样式克隆（跨 workbook 需要 cloneStyleFrom）
                try {
                    CellStyle srcStyle = srcCell.getCellStyle();
                    if (srcStyle != null) {
                        CellStyle dstStyle = wb.createCellStyle();
                        dstStyle.cloneStyleFrom(srcStyle);
                        dstCell.setCellStyle(dstStyle);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}
