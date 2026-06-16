package com.timemanager.service;

import com.timemanager.common.BusinessException;
import com.timemanager.mapper.ProjectMapper;
import com.timemanager.mapper.UserMapper;
import com.timemanager.mapper.WorkHourMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ReportService {

    private final WorkHourMapper workHourMapper;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;

    public ReportService(WorkHourMapper workHourMapper, UserMapper userMapper, ProjectMapper projectMapper) {
        this.workHourMapper = workHourMapper;
        this.userMapper = userMapper;
        this.projectMapper = projectMapper;
    }

    // ====== 个人统计 ======

    public Map<String, Object> personalReport(int year, int month, Long userId, String role) {
        BigDecimal totalHours;
        List<Map<String, Object>> details;

        if ("USER".equals(role)) {
            totalHours = workHourMapper.reportPersonalTotal(userId, year, month);
            details = workHourMapper.reportPersonal(userId, year, month);
        } else if ("PM".equals(role)) {
            // PM 看的是自己在所有管理的项目中的工时
            totalHours = workHourMapper.reportPersonalTotalByPm(userId, year, month, userId);
            details = workHourMapper.reportPersonalByPm(userId, year, month, userId);
        } else if ("DEPT_MANAGER".equals(role) || "ADMIN".equals(role)) {
            // 部门经理/管理员查看全量数据当作 department 视角
            // 但 personal 接口应该是个人视角 — 这里取 userId 指定的人
            totalHours = workHourMapper.reportPersonalTotalByDept(year, month);
            details = workHourMapper.reportPersonalByDept(year, month);
        } else {
            totalHours = workHourMapper.reportPersonalTotal(userId, year, month);
            details = workHourMapper.reportPersonal(userId, year, month);
        }

        // 计算比例
        List<Map<String, Object>> detailList = new ArrayList<>();
        BigDecimal finalTotal = totalHours;
        for (Map<String, Object> row : details) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("projectName", row.get("projectName"));
            BigDecimal hours = (BigDecimal) row.get("hours");
            item.put("hours", hours);
            double ratio = finalTotal.compareTo(BigDecimal.ZERO) > 0
                    ? hours.divide(finalTotal, 4, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            item.put("ratio", Math.round(ratio * 100.0) / 100.0);
            detailList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalHours", totalHours);
        result.put("details", detailList);
        return result;
    }

    // ====== 项目统计 ======

    public Map<String, Object> projectReport(int year, int month, Long projectId, Long userId, String role) {
        // 权限校验：PM 只能看自己的项目
        if ("PM".equals(role)) {
            var project = projectMapper.findById(projectId);
            if (project == null || !userId.equals(project.getManagerId())) {
                throw new BusinessException("无权查看此项目统计");
            }
        }

        BigDecimal totalHours;
        List<Map<String, Object>> details;

        if ("ADMIN".equals(role) || "DEPT_MANAGER".equals(role) || "USER".equals(role)) {
            totalHours = workHourMapper.reportProjectTotal(projectId, year, month);
            details = workHourMapper.reportProject(projectId, year, month);
        } else {
            totalHours = workHourMapper.reportProjectTotalByManager(projectId, year, month, userId);
            details = workHourMapper.reportProjectByManager(projectId, year, month, userId);
        }

        // 计算比例
        List<Map<String, Object>> detailList = new ArrayList<>();
        BigDecimal finalTotal = totalHours;
        for (Map<String, Object> row : details) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("moduleName", row.get("moduleName"));
            item.put("estimatedHours", row.get("estimatedHours"));
            BigDecimal actualHours = (BigDecimal) row.get("actualHours");
            item.put("actualHours", actualHours);
            double ratio = BigDecimal.valueOf(((Number) row.get("estimatedHours")).intValue())
                    .compareTo(BigDecimal.ZERO) > 0
                    ? actualHours.divide(BigDecimal.valueOf(((Number) row.get("estimatedHours")).intValue()), 4, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            item.put("ratio", Math.round(ratio * 100.0) / 100.0);
            detailList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalHours", totalHours);
        result.put("details", detailList);
        return result;
    }

    // ====== 部门统计 ======

    public Map<String, Object> departmentReport(int year, int month, Long userId, String role) {
        BigDecimal totalHours;
        long userCount;
        List<Map<String, Object>> details;

        if ("ADMIN".equals(role) || "DEPT_MANAGER".equals(role)) {
            Map<String, Object> agg = workHourMapper.reportDepartmentAgg(year, month);
            totalHours = agg != null && agg.get("totalHours") != null
                    ? (BigDecimal) agg.get("totalHours") : BigDecimal.ZERO;
            userCount = agg != null && agg.get("userCount") != null
                    ? ((Number) agg.get("userCount")).longValue() : 0L;
            details = workHourMapper.reportDepartment(year, month);
        } else if ("PM".equals(role)) {
            Map<String, Object> agg = workHourMapper.reportDepartmentAggByManager(year, month, userId);
            totalHours = agg != null && agg.get("totalHours") != null
                    ? (BigDecimal) agg.get("totalHours") : BigDecimal.ZERO;
            userCount = agg != null && agg.get("userCount") != null
                    ? ((Number) agg.get("userCount")).longValue() : 0L;
            details = workHourMapper.reportDepartmentByManager(year, month, userId);
        } else {
            // USER 看个人
            totalHours = workHourMapper.reportPersonalTotal(userId, year, month);
            userCount = 1;
            details = workHourMapper.reportPersonal(userId, year, month);
        }

        // 计算比例
        List<Map<String, Object>> detailList = new ArrayList<>();
        BigDecimal finalTotal = totalHours;
        for (Map<String, Object> row : details) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("projectName", row.get("projectName"));
            BigDecimal hours = (BigDecimal) row.get("hours");
            item.put("hours", hours);
            double ratio = finalTotal.compareTo(BigDecimal.ZERO) > 0
                    ? hours.divide(finalTotal, 4, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            item.put("ratio", Math.round(ratio * 100.0) / 100.0);
            detailList.add(item);
        }

        double avgHours = userCount > 0
                ? totalHours.divide(BigDecimal.valueOf(userCount), 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalHours", totalHours);
        result.put("avgHours", avgHours);
        result.put("userCount", userCount);
        result.put("details", detailList);
        return result;
    }

    // ====== Excel 导出 ======

    public byte[] exportExcel(String type, int year, int month, Long userId, String role) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(getSheetName(type, year, month));

            // 样式
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setFont(createFont(wb, true, (short) 12, IndexedColors.WHITE.getIndex()));
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = wb.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // 标题行
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(getTitle(type, year, month));
            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

            // 表头行
            String[] headers = getHeaders(type);
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据
            List<Map<String, Object>> data = getReportData(type, year, month, userId, role);
            int rowIdx = 2;
            for (Map<String, Object> row : data) {
                Row dataRow = sheet.createRow(rowIdx++);
                String[] keys = getKeys(type);
                for (int i = 0; i < keys.length; i++) {
                    Cell cell = dataRow.createCell(i);
                    Object val = row.get(keys[i]);
                    if (val instanceof BigDecimal) {
                        cell.setCellValue(((BigDecimal) val).doubleValue());
                    } else if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                    } else if (val instanceof Double) {
                        cell.setCellValue((Double) val);
                    } else if (val != null) {
                        cell.setCellValue(val.toString());
                    } else {
                        cell.setCellValue("");
                    }
                    cell.setCellStyle(dataStyle);
                }
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        }
    }

    // ====== 辅助方法 ======

    private Font createFont(Workbook wb, boolean bold, short size, short color) {
        Font font = wb.createFont();
        font.setBold(bold);
        font.setFontHeightInPoints(size);
        font.setColor(color);
        return font;
    }

    private String getSheetName(String type, int year, int month) {
        return switch (type) {
            case "personal" -> "个人统计";
            case "project" -> "项目统计";
            case "department" -> "部门统计";
            default -> "报表";
        } + "_" + year + "_" + month;
    }

    private String getTitle(String type, int year, int month) {
        return switch (type) {
            case "personal" -> year + "年" + month + "月个人工时统计";
            case "project" -> year + "年" + month + "月项目工时统计";
            case "department" -> year + "年" + month + "月部门工时统计";
            default -> year + "年" + month + "月工时报表";
        };
    }

    private String[] getHeaders(String type) {
        return switch (type) {
            case "personal" -> new String[]{"项目名称", "工时(小时)", "占比"};
            case "project" -> new String[]{"模块名称", "预估工时(小时)", "实际工时(小时)", "完成率"};
            case "department" -> new String[]{"项目名称", "工时(小时)", "占比"};
            default -> new String[]{};
        };
    }

    private String[] getKeys(String type) {
        return switch (type) {
            case "personal" -> new String[]{"projectName", "hours", "ratio"};
            case "project" -> new String[]{"moduleName", "estimatedHours", "actualHours", "ratio"};
            case "department" -> new String[]{"projectName", "hours", "ratio"};
            default -> new String[]{};
        };
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getReportData(String type, int year, int month, Long userId, String role) {
        Map<String, Object> report;
        BigDecimal totalHours;
        List<Map<String, Object>> details;

        switch (type) {
            case "personal":
                report = personalReport(year, month, userId, role);
                totalHours = (BigDecimal) report.get("totalHours");
                details = (List<Map<String, Object>>) report.get("details");
                // 添加合计行
                List<Map<String, Object>> personalData = new ArrayList<>(details);
                Map<String, Object> totalRow = new LinkedHashMap<>();
                totalRow.put("projectName", "合计");
                totalRow.put("hours", totalHours);
                totalRow.put("ratio", 1.0);
                personalData.add(totalRow);
                return personalData;
            case "project":
                // 默认取第一个项目？ 这里由前端传 projectId，但 export 只传 type/year/month
                // 对于项目统计导出，我们需要额外传 projectId，但 URI 设计是 /api/reports/export?type=project&year=...&projectId=...
                // 由 controller 处理
                return List.of();
            case "department":
                report = departmentReport(year, month, userId, role);
                totalHours = (BigDecimal) report.get("totalHours");
                details = (List<Map<String, Object>>) report.get("details");
                List<Map<String, Object>> deptData = new ArrayList<>(details);
                Map<String, Object> deptTotal = new LinkedHashMap<>();
                deptTotal.put("projectName", "合计");
                deptTotal.put("hours", totalHours);
                deptTotal.put("ratio", 1.0);
                deptData.add(deptTotal);
                return deptData;
            default:
                return List.of();
        }
    }

    // 用于导出项目统计时传入 projectId
    public byte[] exportProjectExcel(int year, int month, Long projectId, Long userId, String role) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("项目统计_" + year + "_" + month);

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setFontHeightInPoints((short) 12);
            hFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = wb.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(year + "年" + month + "月项目工时统计");
            CellStyle titleStyle = wb.createCellStyle();
            Font tFont = wb.createFont();
            tFont.setBold(true);
            tFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(tFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

            // 表头
            String[] headers = {"模块名称", "预估工时(小时)", "实际工时(小时)", "完成率"};
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据
            Map<String, Object> report = projectReport(year, month, projectId, userId, role);
            BigDecimal totalHours = (BigDecimal) report.get("totalHours");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> details = (List<Map<String, Object>>) report.get("details");

            int rowIdx = 2;
            for (Map<String, Object> row : details) {
                Row dataRow = sheet.createRow(rowIdx++);
                dataRow.createCell(0).setCellValue((String) row.get("moduleName"));
                dataRow.createCell(1).setCellValue(((Number) row.get("estimatedHours")).doubleValue());
                dataRow.createCell(2).setCellValue(((BigDecimal) row.get("actualHours")).doubleValue());
                double ratio = row.get("ratio") instanceof Double ? (Double) row.get("ratio") : 0.0;
                dataRow.createCell(3).setCellValue(ratio * 100 + "%");
                for (int i = 0; i < 4; i++) dataRow.getCell(i).setCellStyle(dataStyle);
            }

            // 合计行
            Row totalRow2 = sheet.createRow(rowIdx);
            totalRow2.createCell(0).setCellValue("合计");
            totalRow2.createCell(1).setCellValue("");
            totalRow2.createCell(2).setCellValue(totalHours.doubleValue());
            double totalRatio = BigDecimal.valueOf(details.stream()
                    .mapToDouble(d -> ((Number) d.get("estimatedHours")).doubleValue()).sum())
                    .compareTo(BigDecimal.ZERO) > 0
                    ? totalHours.divide(BigDecimal.valueOf(details.stream()
                    .mapToDouble(d -> ((Number) d.get("estimatedHours")).doubleValue()).sum()), 4, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            totalRow2.createCell(3).setCellValue(Math.round(totalRatio * 100.0) + "%");
            for (int i = 0; i < 4; i++) totalRow2.getCell(i).setCellStyle(dataStyle);

            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        }
    }
}
