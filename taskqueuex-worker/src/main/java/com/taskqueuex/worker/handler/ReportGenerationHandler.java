package com.taskqueuex.worker.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskqueuex.common.entity.Job;
import com.taskqueuex.common.enums.JobType;
import com.taskqueuex.worker.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ReportGenerationHandler implements JobHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReportGenerationHandler.class);

    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    private static final String[] FIRST_NAMES = {
        "Alice", "Bob", "Charlie", "Diana", "Edward", "Fiona", "George", "Hannah",
        "Ivan", "Julia", "Kevin", "Laura", "Michael", "Nina", "Oscar", "Patricia",
        "Quentin", "Rachel", "Samuel", "Tina", "Ulysses", "Victoria", "William", "Xena",
        "Yusuf", "Zara", "Aiden", "Bella", "Carter", "Delilah", "Ethan", "Freya"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
        "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
        "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young"
    };

    private static final String[] DEPARTMENTS = {
        "Engineering", "Sales", "Marketing", "Finance", "Operations",
        "Human Resources", "Customer Support", "Product", "Legal", "Data Science"
    };

    private static final String[] REGIONS = {
        "North America", "Europe", "Asia Pacific", "Latin America", "Middle East & Africa"
    };

    private static final String[] PRODUCTS = {
        "Enterprise Platform", "Cloud Storage Pro", "Analytics Suite", "Security Shield",
        "DevOps Toolkit", "API Gateway", "Data Pipeline", "ML Workbench",
        "Monitoring Dashboard", "Collaboration Hub"
    };

    public ReportGenerationHandler(StorageService storageService, ObjectMapper objectMapper) {
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(Job job) throws Exception {
        logger.info("Processing report generation job {}", job.getId());

        JsonNode payload = objectMapper.readTree(job.getPayload());

        String reportType = payload.has("reportType") ? payload.get("reportType").asText() : "sales";
        int rowCount = payload.has("rowCount") ? payload.get("rowCount").asInt() : 1000;
        String title = payload.has("title") ? payload.get("title").asText() : "Generated Report";

        // Cap at 50,000 rows to prevent abuse
        rowCount = Math.min(rowCount, 50_000);

        logger.info("Generating {} report with {} rows for job {}", reportType, rowCount, job.getId());

        String csv = switch (reportType.toLowerCase()) {
            case "sales" -> generateSalesReport(title, rowCount);
            case "employee" -> generateEmployeeReport(title, rowCount);
            case "inventory" -> generateInventoryReport(title, rowCount);
            default -> generateSalesReport(title, rowCount);
        };

        // Upload to MinIO
        String fileName = reportType + "_report_" + job.getId() + "_" + Instant.now().toEpochMilli() + ".csv";
        String objectName = storageService.upload(csv, fileName);

        job.setOutputLocation(objectName);

        logger.info("Generated and uploaded {} report ({} rows, {} bytes) for job {} to {}",
            reportType, rowCount, csv.length(), job.getId(), objectName);
    }

    private String generateSalesReport(String title, int rowCount) throws InterruptedException {
        Random random = ThreadLocalRandom.current();
        StringBuilder csv = new StringBuilder(rowCount * 120); // pre-size for performance

        // Header section
        csv.append("# ").append(title).append("\n");
        csv.append("# Generated: ").append(Instant.now().atZone(java.time.ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))).append("\n");
        csv.append("# Total Records: ").append(rowCount).append("\n");
        csv.append("#\n");

        // Column headers
        csv.append("transaction_id,date,salesperson,department,region,product,quantity,unit_price,total_amount,discount_pct,net_amount,payment_method,status\n");

        double totalRevenue = 0;
        int totalQuantity = 0;
        String[] paymentMethods = {"Credit Card", "Wire Transfer", "ACH", "Check", "PayPal"};
        String[] statuses = {"Completed", "Completed", "Completed", "Completed", "Pending", "Refunded"}; // weighted

        for (int i = 1; i <= rowCount; i++) {
            String txnId = String.format("TXN-%06d", i);
            LocalDate date = LocalDate.of(2025, 1, 1).plusDays(random.nextInt(365));
            String salesperson = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String department = DEPARTMENTS[random.nextInt(DEPARTMENTS.length)];
            String region = REGIONS[random.nextInt(REGIONS.length)];
            String product = PRODUCTS[random.nextInt(PRODUCTS.length)];
            int quantity = 1 + random.nextInt(50);
            double unitPrice = 49.99 + random.nextDouble() * 950.01; // $49.99 - $999.99
            double totalAmount = quantity * unitPrice;
            double discountPct = random.nextInt(100) < 30 ? (5 + random.nextInt(26)) : 0; // 30% chance of discount
            double netAmount = totalAmount * (1 - discountPct / 100.0);
            String paymentMethod = paymentMethods[random.nextInt(paymentMethods.length)];
            String status = statuses[random.nextInt(statuses.length)];

            totalRevenue += netAmount;
            totalQuantity += quantity;

            csv.append(txnId).append(",")
               .append(date).append(",")
               .append(escapeCsv(salesperson)).append(",")
               .append(department).append(",")
               .append(region).append(",")
               .append(escapeCsv(product)).append(",")
               .append(quantity).append(",")
               .append(String.format("%.2f", unitPrice)).append(",")
               .append(String.format("%.2f", totalAmount)).append(",")
               .append(String.format("%.1f", discountPct)).append(",")
               .append(String.format("%.2f", netAmount)).append(",")
               .append(paymentMethod).append(",")
               .append(status).append("\n");

            // Simulate processing time — batch pause every 500 rows
            if (i % 500 == 0) {
                Thread.sleep(50);
                logger.debug("Processed {}/{} sales rows", i, rowCount);
            }
        }

        // Summary footer
        csv.append("#\n");
        csv.append("# SUMMARY\n");
        csv.append("# Total Revenue: $").append(String.format("%,.2f", totalRevenue)).append("\n");
        csv.append("# Total Quantity Sold: ").append(String.format("%,d", totalQuantity)).append("\n");
        csv.append("# Average Order Value: $").append(String.format("%,.2f", totalRevenue / rowCount)).append("\n");

        return csv.toString();
    }

    private String generateEmployeeReport(String title, int rowCount) throws InterruptedException {
        Random random = ThreadLocalRandom.current();
        StringBuilder csv = new StringBuilder(rowCount * 100);

        csv.append("# ").append(title).append("\n");
        csv.append("# Generated: ").append(Instant.now().atZone(java.time.ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))).append("\n");
        csv.append("# Total Records: ").append(rowCount).append("\n");
        csv.append("#\n");

        csv.append("employee_id,first_name,last_name,email,department,region,hire_date,salary,performance_score,projects_completed,hours_logged\n");

        String[] performanceLevels = {"Exceeds Expectations", "Meets Expectations", "Meets Expectations", "Needs Improvement"};

        for (int i = 1; i <= rowCount; i++) {
            String empId = String.format("EMP-%05d", i);
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@company.com";
            String department = DEPARTMENTS[random.nextInt(DEPARTMENTS.length)];
            String region = REGIONS[random.nextInt(REGIONS.length)];
            LocalDate hireDate = LocalDate.of(2015, 1, 1).plusDays(random.nextInt(3650));
            int salary = 55_000 + random.nextInt(145_000); // $55k - $200k
            String performance = performanceLevels[random.nextInt(performanceLevels.length)];
            int projectsCompleted = random.nextInt(25);
            int hoursLogged = 1600 + random.nextInt(600);

            csv.append(empId).append(",")
               .append(firstName).append(",")
               .append(lastName).append(",")
               .append(email).append(",")
               .append(department).append(",")
               .append(region).append(",")
               .append(hireDate).append(",")
               .append(salary).append(",")
               .append(escapeCsv(performance)).append(",")
               .append(projectsCompleted).append(",")
               .append(hoursLogged).append("\n");

            if (i % 500 == 0) {
                Thread.sleep(50);
                logger.debug("Processed {}/{} employee rows", i, rowCount);
            }
        }

        return csv.toString();
    }

    private String generateInventoryReport(String title, int rowCount) throws InterruptedException {
        Random random = ThreadLocalRandom.current();
        StringBuilder csv = new StringBuilder(rowCount * 100);

        csv.append("# ").append(title).append("\n");
        csv.append("# Generated: ").append(Instant.now().atZone(java.time.ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))).append("\n");
        csv.append("# Total Records: ").append(rowCount).append("\n");
        csv.append("#\n");

        csv.append("sku,product_name,category,warehouse,quantity_on_hand,reorder_point,unit_cost,total_value,last_restocked,supplier,status\n");

        String[] categories = {"Electronics", "Software Licenses", "Hardware", "Networking", "Peripherals", "Storage", "Security", "Cloud Credits"};
        String[] warehouses = {"US-East-1", "US-West-2", "EU-West-1", "AP-Southeast-1", "US-Central-1"};
        String[] suppliers = {"TechCorp", "GlobalSupply Inc", "CloudVendor Ltd", "DataParts Co", "NetGear Systems", "SecureSource"};
        String[] stockStatuses = {"In Stock", "In Stock", "In Stock", "Low Stock", "Out of Stock", "Backordered"};

        double totalInventoryValue = 0;

        for (int i = 1; i <= rowCount; i++) {
            String sku = String.format("SKU-%04d-%03d", random.nextInt(10000), random.nextInt(1000));
            String productName = PRODUCTS[random.nextInt(PRODUCTS.length)] + " v" + (1 + random.nextInt(5)) + "." + random.nextInt(10);
            String category = categories[random.nextInt(categories.length)];
            String warehouse = warehouses[random.nextInt(warehouses.length)];
            int qtyOnHand = random.nextInt(5000);
            int reorderPoint = 50 + random.nextInt(200);
            double unitCost = 9.99 + random.nextDouble() * 490.01;
            double totalValue = qtyOnHand * unitCost;
            LocalDate lastRestocked = LocalDate.of(2025, 1, 1).plusDays(random.nextInt(365));
            String supplier = suppliers[random.nextInt(suppliers.length)];
            String status = qtyOnHand == 0 ? "Out of Stock" : (qtyOnHand < reorderPoint ? "Low Stock" : stockStatuses[random.nextInt(stockStatuses.length)]);

            totalInventoryValue += totalValue;

            csv.append(sku).append(",")
               .append(escapeCsv(productName)).append(",")
               .append(category).append(",")
               .append(warehouse).append(",")
               .append(qtyOnHand).append(",")
               .append(reorderPoint).append(",")
               .append(String.format("%.2f", unitCost)).append(",")
               .append(String.format("%.2f", totalValue)).append(",")
               .append(lastRestocked).append(",")
               .append(escapeCsv(supplier)).append(",")
               .append(status).append("\n");

            if (i % 500 == 0) {
                Thread.sleep(50);
                logger.debug("Processed {}/{} inventory rows", i, rowCount);
            }
        }

        csv.append("#\n");
        csv.append("# SUMMARY\n");
        csv.append("# Total Inventory Value: $").append(String.format("%,.2f", totalInventoryValue)).append("\n");

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    public String getJobType() {
        return JobType.REPORT_GENERATION.name();
    }
}
