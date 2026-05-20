package com.example.backend.Automation_Test;

import org.openqa.selenium.Alert;
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

public class UserLifecycleE2ETest {

    // 🟢 TÁCH LISTENER THÀNH PUBLIC CLASS ĐỂ TRÁNH LỖI BẢO MẬT CỦA JAVA
    public static class SlowMotionListener implements WebDriverListener {
        @Override
        public void beforeClick(WebElement element) {
            delay();
        }

        @Override
        public void beforeSendKeys(WebElement element, CharSequence... keysToSend) {
            delay();
        }

        // Hàm tạo độ trễ 1000 mili-giây (1 giây)
        private void delay() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // 1. Khởi tạo WebDriver GỐC
        WebDriver originalDriver = new ChromeDriver();
        
        // 2. 🟢 GỌI PUBLIC CLASS VỪA TẠO
        WebDriverListener slowMotionListener = new SlowMotionListener();
        
        // 3. 🟢 BỌC DRIVER GỐC LẠI (Tất cả lệnh gọi 'driver' từ đây về sau sẽ bị làm chậm tự động)
        WebDriver driver = new EventFiringDecorator<>(slowMotionListener).decorate(originalDriver);

        // Phóng to cửa sổ bằng driver đã được bọc
        driver.manage().window().maximize();

        // Khởi tạo bộ chờ Explicit Wait 20 giây để chống lỗi bất đồng bộ của React
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Sinh dữ liệu ngẫu nhiên theo thời gian để tránh trùng lặp dữ liệu
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        String testStudentCode = "SV" + timestamp;
        String testEmail = "sinhvien" + timestamp + "@gmail.com";
        String testFullName = "Nguyễn Văn Sinh Viên " + timestamp;
        String testClass = "SE101";
        String testPassword = "Password123@";

        try {
            System.out.println("=== BẮT ĐẦU KỊCH BẢN KIỂM THỬ ĐĂNG KÝ - DUYỆT - ĐĂNG NHẬP ===");

            // ------------------------------------------------------------------------
            // GIAI ĐOẠN 1: ĐĂNG KÝ TÀI KHOẢN MỚI
            // ------------------------------------------------------------------------
            System.out.println("Step 1: Truy cập trang đăng nhập chính...");
            driver.get("http://localhost:5173/login");

            System.out.println("Step 2: Tìm và nhấn nút 'Tạo tài khoản mới'...");
            WebElement btnRegisterNew = wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector("button.btn-register-new")));
            btnRegisterNew.click();

            // Đợi URL chuyển hướng sang trang đăng ký thành công
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/register"));
            System.out.println("-> Đã chuyển hướng thành công sang trang đăng ký.");

            System.out.println("Step 3: Điền toàn bộ thông tin đăng ký tài khoản...");
            driver.findElement(By.name("fullName")).sendKeys(testFullName);
            driver.findElement(By.name("studentCode")).sendKeys(testStudentCode);
            driver.findElement(By.name("className")).sendKeys(testClass);
            driver.findElement(By.name("email")).sendKeys(testEmail);            
            driver.findElement(By.name("password")).sendKeys(testPassword);

            System.out.println("Step 4: Gửi biểu mẫu đăng ký...");
            driver.findElement(By.xpath("//button[contains(text(), 'Đăng ký')]")).click();

            System.out.println("Step 5: Xử lý thông báo Alert từ hệ thống...");
            // Đợi Alert xuất hiện trên trình duyệt
            wait.until(ExpectedConditions.alertIsPresent());
            Alert successAlert = driver.switchTo().alert();
            System.out.println("-> Nội dung Alert: " + successAlert.getText());
            successAlert.accept(); // Nhấn nút OK trên Alert

            // Đợi hệ thống tự động nhảy về trang Login sau khi bấm OK
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/login"));
            System.out.println("-> Đăng ký thành công! Đã quay trở về trang đăng nhập.");

            // ------------------------------------------------------------------------
            // GIAI ĐOẠN 2: ĐĂNG NHẬP ADMIN & DUYỆT TÀI KHOẢN MỚI NẰM DƯỚI ĐÁY TRANG
            // ------------------------------------------------------------------------
            System.out.println("\nStep 6: Đăng nhập tài khoản Quản trị viên (Admin)...");

            // BẮT ĐÚNG THEO PLACEHOLDER TRÊN GIAO DIỆN BẰNG XPATH
            WebElement adminUsername = wait.until(
                    ExpectedConditions
                            .visibilityOfElementLocated(By.xpath("//input[@placeholder='Email hoặc Mã Sinh Viên']")));
            adminUsername.sendKeys("141204");

            // BẮT Ô MẬT KHẨU THEO TYPE VÀ PLACEHOLDER
            driver.findElement(By.xpath("//input[@type='password' or @placeholder='Mật khẩu']")).sendKeys("1412");
            driver.findElement(By.cssSelector("button.btn-login")).click();

            // Đợi hệ thống kiểm tra Role và nhảy vào Dashboard của Admin
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/admin/dashboard"));
            System.out.println("-> Đăng nhập Admin thành công, đã vào trang Dashboard.");

            System.out.println("Step 7: Điều hướng sang trang Quản lý người dùng...");
            WebElement navUsers = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath(
                            "//span[contains(@class, 'MuiListItemText-primary') and contains(text(), 'Quản lý Người dùng')]")));
            navUsers.click();

            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/admin/users"));
            System.out.println("Step 8: Thực hiện kỹ thuật Cuộn chuột (Scroll) xuống đáy trang để hiển thị user mới...");
            
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(1000); 

            System.out.println("Step 9: Tìm dòng chứa Mã sinh viên '" + testStudentCode + "' và nhấn Icon 'Duyệt'...");

            String xpathApproveBtn = "//tr[td[contains(text(), '" + testStudentCode + "')]]" +
                    "//button[descendant::*[local-name()='path' and @d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2m-2 15-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8z']]";

            WebElement btnApprove = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath(xpathApproveBtn)));

            js.executeScript("arguments[0].scrollIntoView(true);", btnApprove);
            Thread.sleep(500); 

            btnApprove.click();
            System.out.println("-> Đã kích hoạt phê duyệt tài khoản thành công bằng Icon!");

            System.out.println("Step 10: Đăng xuất tài khoản Admin...");
            WebElement navLogout = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='Đăng xuất']")));
            navLogout.click();
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/login"));
            System.out.println("-> Đã đăng xuất Admin và quay lại màn hình Login.");

            // ------------------------------------------------------------------------
            // GIAI ĐOẠN 3: ĐĂNG NHẬP BẰNG TÀI KHOẢN MỚI SAU KHI ĐƯỢC DUYỆT
            // ------------------------------------------------------------------------
            System.out.println("\nStep 11: Tiến hành đăng nhập bằng tài khoản sinh viên vừa tạo...");

            WebElement studentUsername = wait.until(
                    ExpectedConditions
                            .visibilityOfElementLocated(By.xpath("//input[@placeholder='Email hoặc Mã Sinh Viên']")));
            studentUsername.sendKeys(testStudentCode);

            driver.findElement(By.xpath("//input[@type='password' or @placeholder='Mật khẩu']")).sendKeys(testPassword);
            driver.findElement(By.cssSelector("button.btn-login")).click();

            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/"));
            System.out.println("-> [XÁC NHẬN]: Đăng nhập tài khoản mới thành công! Đã vào trang chủ hệ thống.");
            System.out.println("\n=== KỊCH BẢN KIỂM THỬ TỰ ĐỘNG CHẠY THÀNH CÔNG 100% ===");

        } catch (Exception e) {
            System.err.println("!!! [THẤT BẠI] Kịch bản kiểm thử bị gãy tại bước thực thi. Chi tiết lỗi:");
            e.printStackTrace();
        } finally {
            System.out.println("\nĐóng trình duyệt kiểm thử.");
            driver.quit();
        }
    }
}