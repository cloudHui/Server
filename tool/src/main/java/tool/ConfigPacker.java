package tool;

import tools.other.excel.ExcelUtil;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 配置打包工具
 * 扫描 tool/src/main/resources/xml 目录下所有 xlsx 文件，按类名分组打包为二进制文件
 */
public class ConfigPacker {

    private static final String CONFIG_DIR = "config";
    private static final String EXCEL_DIR = "tool/src/main/resources/xml";

    public static void main(String[] args) {
        try {
            packAllExcel();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    public static void packAllExcel() throws Exception {
        System.out.println("开始扫描 Excel 文件...");

        // Excel 是唯一配置源：先按列名/类型/说明生成 model.tablemodel 下的 Java 类，
        // 随后的读取阶段只反射这些类并输出 .dat，不再通过 JSON 拼装配置对象。
        ExcelUtil.scanResourcesCreateJavaHead("tool");

        // 扫描 tool/src/main/resources/xml 目录下所有 xlsx 文件
        Path excelDir = Paths.get(EXCEL_DIR);
        if (!Files.isDirectory(excelDir)) {
            System.err.println("错误: Excel 目录不存在: " + excelDir.toAbsolutePath());
            return;
        }

        List<Path> xlsxFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.list(excelDir)) {
            stream.filter(p -> p.toString().endsWith(".xlsx"))
                  .filter(p -> !p.getFileName().toString().startsWith("~$")) // 过滤临时文件
                  .forEach(xlsxFiles::add);
        }

        if (xlsxFiles.isEmpty()) {
            System.err.println("警告: 未找到 xlsx 文件");
            return;
        }

        System.out.println("找到 " + xlsxFiles.size() + " 个 xlsx 文件");

        // 创建输出目录
        Path configDir = Paths.get(CONFIG_DIR);
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
            System.out.println("创建目录: " + configDir.toAbsolutePath());
        }

        // 处理每个 xlsx 文件
        for (Path xlsxFile : xlsxFiles) {
            String fileName = xlsxFile.getFileName().toString();
            System.out.println("\n处理文件: " + fileName);

            // 读取 Excel 所有 sheet 的数据
            List<Object> properties = new ArrayList<>();
            ExcelUtil.readExcelJavaValue(fileName, properties, null);

            if (properties.isEmpty()) {
                System.out.println("  跳过: 无有效数据");
                continue;
            }

            // 按类名分组
            Map<String, List<Object>> classGroupMap = new HashMap<>();
            for (Object obj : properties) {
                String className = obj.getClass().getSimpleName();
                classGroupMap.computeIfAbsent(className, k -> new ArrayList<>()).add(obj);
            }

            // 按类名分别写入二进制文件
            for (Map.Entry<String, List<Object>> entry : classGroupMap.entrySet()) {
                String className = entry.getKey();
                List<Object> dataList = entry.getValue();

                String datFileName = className.toLowerCase() + "_models.dat";
                Path outputFile = configDir.resolve(datFileName);

                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFile.toFile()))) {
                    oos.writeObject(dataList);
                }

                System.out.println("  打包: " + outputFile.getFileName() + " (" + className + ": " + dataList.size() + " 条)");
            }
        }

        System.out.println("\n所有配置打包完成");
    }
}
