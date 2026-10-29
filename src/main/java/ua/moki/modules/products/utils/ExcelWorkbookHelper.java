package ua.moki.modules.products.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ExcelWorkbookHelper {

    public record ExcelRow(int rowNumber, List<String> cells) {
        public String getCell(int index) {
            if (index < 0 || index >= cells.size()) {
                return "";
            }
            String val = cells.get(index);
            return val != null ? val.trim() : "";
        }

        public boolean isEmpty() {
            for (String cell : cells) {
                if (cell != null && !cell.trim().isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    public static final List<String> TEMPLATE_HEADERS = List.of(
            "Назва (укр)",
            "Ціна",
            "Закупівельна ціна",
            "Знижка (%)",
            "Характеристика",
            "Виробник",
            "Тип товару",
            "Категорія (укр)",
            "Підкатегорія (укр)",
            "Значення розміру",
            "Одиниця розміру",
            "Фасування",
            "Дозволити власну вагу",
            "Мін. кастомна вага (г)",
            "Group ID",
            "Назва параметра варіації",
            "Значення варіації"
    );

    private ExcelWorkbookHelper() {
    }

    public static List<ExcelRow> readFirstSheet(InputStream inputStream) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel-файл не містить жодного аркуша");
            }

            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<ExcelRow> rows = new ArrayList<>();

            for (Row row : sheet) {
                int lastCellNum = row.getLastCellNum();
                List<String> cells = new ArrayList<>(Math.max(lastCellNum, 0));

                for (int colIdx = 0; colIdx < lastCellNum; colIdx++) {
                    Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    cells.add(extractCellValue(cell, evaluator));
                }

                rows.add(new ExcelRow(row.getRowNum() + 1, cells));
            }

            return rows;
        }
    }

    private static String extractCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            CellValue evaluated = evaluator.evaluate(cell);
            if (evaluated == null) {
                return "";
            }
            return switch (evaluated.getCellType()) {
                case STRING -> evaluated.getStringValue().trim();
                case NUMERIC -> formatNumeric(evaluated.getNumberValue());
                case BOOLEAN -> evaluated.getBooleanValue() ? "так" : "ні";
                default -> "";
            };
        }

        return switch (cellType) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> formatNumeric(cell.getNumericCellValue());
            case BOOLEAN -> cell.getBooleanCellValue() ? "так" : "ні";
            default -> "";
        };
    }

    private static String formatNumeric(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    public static byte[] generateTemplateXlsx() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Sheet mainSheet = workbook.createSheet("Товари");
            fillSheet(mainSheet, List.of(TEMPLATE_HEADERS), headerStyle);

            Sheet exampleSheet = workbook.createSheet("Приклад та довідник");
            fillSheet(exampleSheet, buildExampleRows(), headerStyle);

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private static void fillSheet(Sheet sheet, List<List<String>> rowsData, CellStyle headerStyle) {
        for (int r = 0; r < rowsData.size(); r++) {
            Row row = sheet.createRow(r);
            List<String> rowValues = rowsData.get(r);
            for (int c = 0; c < rowValues.size(); c++) {
                Cell cell = row.createCell(c, CellType.STRING);
                cell.setCellValue(rowValues.get(c));
                if (r == 0) {
                    cell.setCellStyle(headerStyle);
                }
            }
        }

        for (int c = 0; c < TEMPLATE_HEADERS.size(); c++) {
            sheet.setColumnWidth(c, 24 * 256);
        }
    }

    private static List<List<String>> buildExampleRows() {
        return List.of(
                TEMPLATE_HEADERS,
                List.of(
                        "Манго сушене натуральне Royal",
                        "220",
                        "160",
                        "0",
                        "Країна: В'єтнам; Склад: 100% манго; Без цукру: Так",
                        "Royal",
                        "Звичайний",
                        "Сухофрукти",
                        "Манго",
                        "500",
                        "г",
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""
                ),
                List.of(
                        "Мигдаль золотий сирий",
                        "480",
                        "370",
                        "5",
                        "Країна: США; Калібр: 27/30; Обробка: Сирий",
                        "Moki",
                        "На вагу",
                        "Горіхи",
                        "Мигдаль",
                        "1",
                        "кг",
                        "100:55; 250:130; 500:250; 1000:480*",
                        "так",
                        "100",
                        "",
                        "",
                        ""
                ),
                List.of(
                        "Цукерки Чорнослив з волоським горіхом 250г",
                        "145",
                        "105",
                        "0",
                        "Країна: Україна; Глазур: Кондитерська; Начинка: Чорнослив з горіхом",
                        "Amanti",
                        "Варіація",
                        "Цукерки",
                        "Фрукти в шоколаді",
                        "250",
                        "г",
                        "",
                        "",
                        "",
                        "amanti-prunes-walnut",
                        "Вага",
                        "250г"
                )
        );
    }
}
