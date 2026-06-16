package com.gamer.data.mpcserver.commands.excel;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.SAXParserFactory;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.Process;

/**
 * xlsx 事件模型（SAX）辅助：轻量列举 sheet、按区域流式读单元格，避免整本 {@link org.apache.poi.xssf.usermodel.XSSFWorkbook} 载入。
 *
 * <p>
 * 说明：POI 的 SAX 路径仍会为 {@link ReadOnlySharedStringsTable} 解析 sharedStrings.xml； 对大表主要收益是<strong>不按行构建整张 Sheet 的
 * DOM</strong>，并可早停于 endRow 之后。
 * </p>
 *
 * <p>
 * 约束：与项目其它 MCP 命令一致，不使用 lambda/stream/try-with-resources。
 * </p>
 */
@Process("excel_xlsx_event_helper")
public final class ExcelXlsxEventHelper {

    /** 文件扩展名小写比较。 */
    public static final String XLSX_EXT_LOWER = ".xlsx";

    /** 满足任一则对 read 走 SAX：文件较大、或请求窗口面积较大。 */
    public static final long STREAM_FILE_BYTES_THRESHOLD = 600000L;
    public static final int STREAM_MIN_ROW_SPAN = 250;
    public static final int STREAM_MIN_COL_SPAN = 120;
    public static final long STREAM_MIN_CELL_ESTIMATE = 28000L;

    private ExcelXlsxEventHelper() {}

    /**
     * 只读打开 xlsx OPC 包（避免 close 时 save 刷新磁盘修改时间）。
     */
    private static OPCPackage openReadOnly(File xlsxFile) throws Exception {
        return OPCPackage.open(xlsxFile, PackageAccess.READ);
    }

    /**
     * 关闭只读 OPC 包（revert，避免 READ 包 close/save 改写磁盘修改时间）。
     */
    private static void closeReadOnly(OPCPackage pkg) {
        if (pkg == null) {
            return;
        }
        try {
            pkg.revert();
        } catch (Exception ignored) {
            // 关闭失败时忽略
        }
    }

    /**
     * 判断是否为 xlsx（仅按扩展名）。
     */
    public static boolean isXlsxFile(File file) {
        if (file == null) {
            return false;
        }
        String name = file.getName();
        return name.toLowerCase().trim().endsWith(XLSX_EXT_LOWER);
    }

    /**
     * 是否对读表请求采用 xlsx 流式（SAX）路径（调用方已保证不需要按 DOM 才能做的样式细节时）。
     */
    public static boolean shouldStreamRead(File file, int startRow, int startCol, int endRow, int endCol) {
        if (!isXlsxFile(file) || !file.isFile()) {
            return false;
        }
        if (endRow < startRow || endCol < startCol) {
            return false;
        }
        long spanRows = (long)endRow - startRow + 1L;
        long spanCols = (long)endCol - startCol + 1L;
        long estCells = spanRows * spanCols;
        return file.length() >= STREAM_FILE_BYTES_THRESHOLD || spanRows >= STREAM_MIN_ROW_SPAN
            || spanCols >= STREAM_MIN_COL_SPAN || estCells >= STREAM_MIN_CELL_ESTIMATE;
    }

    /**
     * 从 workbook 流解析 sheet 名称列表（顺序与 Excel 一致）。
     */
    private static XMLReader newNamespaceAwareXmlReader() throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newSAXParser().getXMLReader();
    }

    public static List<String> readSheetNamesInOrder(File xlsxFile) throws Exception {
        if (xlsxFile == null || !xlsxFile.exists()) {
            throw new IllegalArgumentException("Excel文件不存在");
        }
        OPCPackage pkg = null;
        InputStream wbStream = null;
        try {
            pkg = openReadOnly(xlsxFile);
            XSSFReader reader = new XSSFReader(pkg);
            wbStream = reader.getWorkbookData();
            final List<String> names = new ArrayList<>();
            XMLReader xmlReader = newNamespaceAwareXmlReader();
            xmlReader.setContentHandler(new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    String tag = localName == null ? "" : localName;
                    if (tag.isEmpty() && qName != null) {
                        int p = qName.lastIndexOf(':');
                        tag = p >= 0 ? qName.substring(p + 1) : qName;
                    }
                    if (!"sheet".equals(tag)) {
                        return;
                    }
                    if (attributes == null) {
                        return;
                    }
                    String n = attributes.getValue("name");
                    if (n == null || n.trim().isEmpty()) {
                        return;
                    }
                    names.add(n);
                }
            });
            xmlReader.parse(new InputSource(wbStream));
            return names;
        } finally {
            McpUtils.tryClose(wbStream);
            closeReadOnly(pkg);
        }
    }

    /**
     * 列出 xlsx 全部工作表的行/列规模（从各 sheet 的 dimension 估计，不做全表 SharedStrings/Sheet 全量 DOM）。
     *
     * @return 与历史 {@link ExcelDescribeSheetsCommand} 相同结构：SHEET_COUNT、表头、各 sheet 一行
     */
    public static String describeXlsxSheets(File xlsxFile) throws Exception {
        OPCPackage pkg = null;
        try {
            pkg = openReadOnly(xlsxFile);
            XSSFReader reader = new XSSFReader(pkg);
            StringBuilder sb = new StringBuilder();
            Iterator<InputStream> sheets = reader.getSheetsData();
            XSSFReader.SheetIterator shIt = (XSSFReader.SheetIterator)sheets;
            int sheetCount = 0;
            StringBuilder body = new StringBuilder();
            body.append("SHEET\tROWS\tCOLS\n");
            while (shIt.hasNext()) {
                InputStream sheetStream = null;
                try {
                    sheetStream = shIt.next();
                    String sheetName = shIt.getSheetName();
                    String ref = readDimensionRef(sheetStream);
                    int rows = 0;
                    int cols = 0;
                    if (ref != null && !ref.trim().isEmpty()) {
                        try {
                            CellRangeAddress cra = CellRangeAddress.valueOf(ref.trim());
                            rows = cra.getLastRow() - cra.getFirstRow() + 1;
                            cols = cra.getLastColumn() - cra.getFirstColumn() + 1;
                        } catch (Exception ignored) {
                            CellReference cr = new CellReference(ref.trim());
                            rows = cr.getRow() + 1;
                            cols = cr.getCol() + 1;
                        }
                    }
                    if (rows < 0) {
                        rows = 0;
                    }
                    if (cols < 0) {
                        cols = 0;
                    }
                    sheetCount++;
                    body.append(sheetName).append('\t').append(rows).append('\t').append(cols).append('\n');
                } finally {
                    McpUtils.tryClose(sheetStream);
                }
            }
            sb.append("SHEET_COUNT=").append(sheetCount).append('\n');
            sb.append(body);
            return sb.toString();
        } finally {
            closeReadOnly(pkg);
        }
    }

    private static String readDimensionRef(InputStream sheetXml) throws Exception {
        if (sheetXml == null) {
            return null;
        }
        final String[] holder = new String[1];
        XMLReader xmlReader = newNamespaceAwareXmlReader();
        xmlReader.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
                String tag = localName == null ? "" : localName;
                if (tag.isEmpty() && qName != null) {
                    int p = qName.lastIndexOf(':');
                    tag = p >= 0 ? qName.substring(p + 1) : qName;
                }
                if (!"dimension".equals(tag)) {
                    return;
                }
                if (attributes != null) {
                    holder[0] = attributes.getValue("ref");
                }
                throw new SAXEarlyStopException();
            }
        });
        try {
            xmlReader.parse(new InputSource(sheetXml));
        } catch (SAXEarlyStopException ok) {
            // dimension 已读出
        }
        return holder[0];
    }

    /**
     * 流式读取 xlsx 指定 sheet 的矩形区域为 TSV 正文（不含 FILE= 头等，由调用方拼接）。
     *
     * @param showFormula
     *            true 时尽量输出公式文本（由 POI 事件层 formulasNotResults 控制）
     */
    public static String readRangeAsTsvBody(File xlsxFile, String sheetName, int startRow, int startCol, int endRow,
        int endCol, boolean showFormula) throws Exception {
        if (sheetName == null || sheetName.trim().isEmpty()) {
            throw new IllegalArgumentException("sheetName不能为空");
        }
        if (endRow < startRow || endCol < startCol) {
            throw new IllegalArgumentException("非法行列范围");
        }
        OPCPackage pkg = null;
        InputStream sheetStream = null;
        try {
            pkg = openReadOnly(xlsxFile);
            ReadOnlySharedStringsTable sst = new ReadOnlySharedStringsTable(pkg);
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();
            if (styles == null) {
                styles = new StylesTable();
            }

            sheetStream = openSheetStreamByName(reader, sheetName.trim());
            if (sheetStream == null) {
                throw new IllegalArgumentException("找不到sheet: " + sheetName);
            }

            DataFormatter dataFormatter = new DataFormatter();
            StringBuilder sb = new StringBuilder();
            TsvSheetContentsHandler handler = new TsvSheetContentsHandler(sb, startRow, startCol, endRow, endCol);
            XSSFSheetXMLHandler sheetHandler =
                new XSSFSheetXMLHandler(styles, null, sst, handler, dataFormatter, showFormula);

            XMLReader xmlReader = newNamespaceAwareXmlReader();
            xmlReader.setContentHandler(sheetHandler);
            try {
                xmlReader.parse(new InputSource(sheetStream));
            } catch (SAXPastEndRowRuntime done) {
                // 已超过 endRow，早停
            } catch (SAXException e) {
                if (!isCausedByPastEndRow(e)) {
                    throw e;
                }
            }
            handler.flushTrailing();
            return sb.toString();
        } finally {
            McpUtils.tryClose(sheetStream);
            closeReadOnly(pkg);
        }
    }

    private static boolean isCausedByPastEndRow(Throwable e) {
        Throwable t = e;
        for (int depth = 0; depth < 10 && t != null; depth++) {
            if (t instanceof SAXPastEndRowRuntime) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static InputStream openSheetStreamByName(XSSFReader reader, String targetName) throws Exception {
        Iterator<InputStream> sheets = reader.getSheetsData();
        XSSFReader.SheetIterator shIt = (XSSFReader.SheetIterator)sheets;
        while (shIt.hasNext()) {
            InputStream stream = shIt.next();
            String name = shIt.getSheetName();
            if (name != null && name.equals(targetName)) {
                return stream;
            }
            McpUtils.tryClose(stream);
        }
        return null;
    }

    /**
     * 解析完 dimension 后终止 SAX。
     */
    private static final class SAXEarlyStopException extends SAXException {
        private static final long serialVersionUID = 1L;
    }

    /**
     * 已超过请求最大行，终止 sheet 解析（非受检异常，适配 POI 4.1 {@link SheetContentsHandler#startRow(int)} 签名）。
     */
    private static final class SAXPastEndRowRuntime extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static final class TsvSheetContentsHandler implements SheetContentsHandler {
        private final StringBuilder sb;
        private final int startRow;
        private final int startCol;
        private final int endRow;
        private final int endCol;

        private int currentRowNum = -1;
        private final TreeMap<Integer, String> colValues = new TreeMap<>();

        TsvSheetContentsHandler(StringBuilder sb, int startRow, int startCol, int endRow, int endCol) {
            this.sb = sb;
            this.startRow = startRow;
            this.startCol = startCol;
            this.endRow = endRow;
            this.endCol = endCol;
        }

        @Override
        public void startRow(int rowNum) {
            if (rowNum > endRow) {
                throw new SAXPastEndRowRuntime();
            }
            currentRowNum = rowNum;
            colValues.clear();
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum < startRow || rowNum > endRow) {
                colValues.clear();
                return;
            }
            flushCurrentRow(rowNum);
            colValues.clear();
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (cellReference == null || cellReference.trim().isEmpty()) {
                return;
            }
            CellReference ref = new CellReference(cellReference);
            int r = ref.getRow();
            if (r < startRow || r > endRow) {
                return;
            }
            int c = ref.getCol();
            if (c < startCol || c > endCol) {
                return;
            }
            String v = formattedValue == null ? "" : McpUtils.oneLine(formattedValue);
            colValues.put(c, v);
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // MCP 读表不导出页眉页脚
        }

        void flushTrailing() {
            if (currentRowNum >= startRow && currentRowNum <= endRow && !colValues.isEmpty()) {
                flushCurrentRow(currentRowNum);
                colValues.clear();
            }
        }

        private void flushCurrentRow(int rowNum) {
            sb.append(rowNum);
            for (int c = startCol; c <= endCol; c++) {
                sb.append('\t');
                String v = colValues.get(c);
                if (v != null) {
                    sb.append(v);
                }
            }
            sb.append('\n');
        }
    }
}
