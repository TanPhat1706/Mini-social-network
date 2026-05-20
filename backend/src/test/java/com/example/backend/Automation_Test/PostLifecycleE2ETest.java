package com.example.backend.Automation_Test;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PostLifecycleE2ETest {

    // 🟢 SỬA LỖI TẠI ĐÂY: Tách Listener ra thành một Public Static Class rõ ràng
    public static class SlowMotionListener implements WebDriverListener {
        @Override
        public void beforeClick(WebElement element) {
            delay();
        }

        @Override
        public void beforeSendKeys(WebElement element, CharSequence... keysToSend) {
            delay();
        }

        private void delay() {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        // 1. Khởi tạo Driver gốc
        WebDriver originalDriver = new ChromeDriver();

        // 2. Sử dụng Public Class vừa tạo thay vì class ẩn danh
        WebDriverListener slowMotionListener = new SlowMotionListener();

        // 3. Bọc Driver lại để kích hoạt Slow Motion
        WebDriver driver = new EventFiringDecorator<>(slowMotionListener).decorate(originalDriver);

        driver.manage().window().maximize();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        String uniquePostCode = "[Mã: " + System.currentTimeMillis() % 100000 + "]";
        String postContent = "Xin chào buổi sáng, ngày 20 tháng 5 " + uniquePostCode;
        try {
            System.out.println("=== BẮT ĐẦU KỊCH BẢN KIỂM THỬ: ĐĂNG BÀI - DUYỆT - KIỂM TRA ===");

            // ------------------------------------------------------------------------
            // GIAI ĐOẠN 1: USER ĐĂNG NHẬP VÀ ĐĂNG BÀI VIẾT
            // ------------------------------------------------------------------------
            System.out.println("Step 1: Truy cập và đăng nhập tài khoản User (2511)...");
            driver.get("http://localhost:5173/login");

            WebElement userIdentifier = wait.until(ExpectedConditions
                    .visibilityOfElementLocated(By.xpath("//input[@placeholder='Email hoặc Mã Sinh Viên']")));
            userIdentifier.sendKeys("2511");
            driver.findElement(By.xpath("//input[@type='password' or @placeholder='Mật khẩu']")).sendKeys("2511");
            driver.findElement(By.cssSelector("button.btn-login")).click();

            // Đợi trang chủ load xong
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/"));

            System.out.println("Step 2: Mở hộp thoại đăng bài...");
            // 🟢 CHIẾN LƯỢC MỚI: Bắt nút bấm mở hộp thoại bằng CSS.
            // Đặc điểm của nó là dạng nút chữ (textPrimary) và trải dài (fullWidth)
            WebElement btnOpenPost = wait.until(
                    ExpectedConditions
                            .elementToBeClickable(By.cssSelector("button.MuiButton-textPrimary.MuiButton-fullWidth")));
            btnOpenPost.click();

            System.out.println("Step 3: Nhập nội dung bài viết...");
            // 🟢 Bắt ô nhập liệu dựa vào class thẻ textarea đa dòng (inputMultiline) của
            // MUI
            WebElement textareaPost = wait.until(
                    ExpectedConditions
                            .visibilityOfElementLocated(By.cssSelector("textarea.MuiInputBase-inputMultiline")));
            textareaPost.sendKeys(postContent);

            System.out.println("Step 4: Nhấn nút Đăng...");
            // 🟢 Bắt nút Đăng dựa vào class nút nền đặc (containedPrimary)
            WebElement btnSubmitPost = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector("button.MuiButton-containedPrimary.MuiButton-fullWidth")));
            btnSubmitPost.click();

            System.out.println("Step 5: Đăng xuất tài khoản User...");
            // Nhấn vào icon Cài đặt (góc phải) dựa trên thuộc tính aria-label cực xịn
            WebElement btnSettings = wait.until(ExpectedConditions
                    .elementToBeClickable(By.xpath("//button[@aria-label='Cài đặt & Quyền riêng tư']")));
            btnSettings.click();

            // Nhấn nút Đăng xuất trong menu xổ xuống
            WebElement menuLogout = wait.until(ExpectedConditions
                    .elementToBeClickable(By.xpath("//li[@role='menuitem' and contains(., 'Đăng xuất')]")));
            menuLogout.click();
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/login"));

            // ------------------------------------------------------------------------
            // GIAI ĐOẠN 2: ADMIN ĐĂNG NHẬP VÀ DUYỆT BÀI
            // ------------------------------------------------------------------------
            System.out.println("\nStep 6: Đăng nhập tài khoản Admin (141204)...");
            WebElement adminIdentifier = wait.until(ExpectedConditions
                    .visibilityOfElementLocated(By.xpath("//input[@placeholder='Email hoặc Mã Sinh Viên']")));
            adminIdentifier.sendKeys("141204");
            driver.findElement(By.xpath("//input[@type='password' or @placeholder='Mật khẩu']")).sendKeys("1412");
            driver.findElement(By.cssSelector("button.btn-login")).click();
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/admin/dashboard"));

            System.out.println("Step 7: Vào mục Quản lý Bài viết...");
            WebElement navPosts = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                    "//span[contains(@class, 'MuiListItemText-primary') and contains(text(), 'Quản lý Bài viết')]")));
            navPosts.click();
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/admin/posts")); // Tạm định URL là
                                                                                         // /admin/posts, bạn có thể
                                                                                         // chỉnh lại nếu khác

            System.out.println("Step 8: Tìm và duyệt bài viết vừa đăng...");
            // Tìm đúng dòng có chứa nội dung (mã tự sinh) của user, sau đó bắt nút có
            // aria-label="Duyệt bài"
            String xpathApprovePost = "//tr[td[contains(., '" + uniquePostCode
                    + "')]]//button[@aria-label='Duyệt bài']";
            WebElement btnApprovePost = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpathApprovePost)));

            // Cuộn cho nút duyệt lọt vào khung hình rồi click
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", btnApprovePost);
            btnApprovePost.click();
            System.out.println("-> Đã duyệt bài viết thành công!");

            System.out.println("Step 9: Đăng xuất Admin...");
            WebElement adminLogout = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//span[contains(@class, 'MuiListItemText-primary') and contains(text(), 'Đăng xuất')]")));
            adminLogout.click();
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/login"));

            // ------------------------------------------------------------------------
            // GIAI ĐOẠN 3: USER XEM LẠI BÀI ĐÃ ĐƯỢC DUYỆT
            // ------------------------------------------------------------------------
            System.out.println("\nStep 10: Đăng nhập lại User để kiểm tra bài viết...");
            WebElement checkIdentifier = wait.until(ExpectedConditions
                    .visibilityOfElementLocated(By.xpath("//input[@placeholder='Email hoặc Mã Sinh Viên']")));
            checkIdentifier.sendKeys("2511");
            driver.findElement(By.xpath("//input[@type='password' or @placeholder='Mật khẩu']")).sendKeys("2511");
            driver.findElement(By.cssSelector("button.btn-login")).click();
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/"));

            System.out.println(
                    "\n[THÀNH CÔNG] Đã hoàn tất luồng! Dừng kịch bản 10 giây để chiêm ngưỡng bài viết mới trên đầu trang...");

            // Tạm dừng cứng 10 giây ở cuối cùng để bạn xem thành quả (Màn hình Home)
            Thread.sleep(10000);

        } catch (Exception e) {
            System.err.println("!!! [THẤT BẠI] Kịch bản gãy. Chi tiết lỗi:");
            e.printStackTrace();
        } finally {
            System.out.println("\nĐóng trình duyệt.");
            driver.quit();
        }
    }
}