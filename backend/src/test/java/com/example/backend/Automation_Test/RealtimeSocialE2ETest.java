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

public class RealtimeSocialE2ETest {

    // 🟢 TÁCH LISTENER LÀM CHẬM RA PUBLIC CLASS
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
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        WebDriverListener slowMotionListener = new SlowMotionListener();

        // 1. KHỞI TẠO HAI TRÌNH DUYỆT ĐỘC LẬP
        System.out.println("Khởi tạo Trình duyệt 1 (Tài khoản Nhận - 2511)...");
        WebDriver driver2511 = new EventFiringDecorator<>(slowMotionListener).decorate(new ChromeDriver());
        WebDriverWait wait2511 = new WebDriverWait(driver2511, Duration.ofSeconds(15));

        // Thu nhỏ trình duyệt 1 để nằm nửa trái màn hình
        driver2511.manage().window().setSize(new org.openqa.selenium.Dimension(900, 1000));
        driver2511.manage().window().setPosition(new org.openqa.selenium.Point(0, 0));

        System.out.println("Khởi tạo Trình duyệt 2 (Tài khoản Tương tác - 1412)...");
        WebDriver driver1412 = new EventFiringDecorator<>(slowMotionListener).decorate(new ChromeDriver());
        WebDriverWait wait1412 = new WebDriverWait(driver1412, Duration.ofSeconds(15));

        // Thu nhỏ trình duyệt 2 để nằm nửa phải màn hình
        driver1412.manage().window().setSize(new org.openqa.selenium.Dimension(900, 1000));
        driver1412.manage().window().setPosition(new org.openqa.selenium.Point(900, 0));
        JavascriptExecutor js1412 = (JavascriptExecutor) driver1412;

        try {
            System.out.println("=== BẮT ĐẦU KỊCH BẢN KIỂM THỬ REAL-TIME WEBSOCKET ===");

            // ------------------------------------------------------------------------
            // GIAI ĐOẠN 1: LOGIN HAI TRÌNH DUYỆT
            // ------------------------------------------------------------------------
            // Đăng nhập 2511 (Cửa sổ Trái)
            driver2511.get("http://localhost:5173/login");
            wait2511.until(ExpectedConditions
                    .visibilityOfElementLocated(By.xpath("//input[@placeholder='Email hoặc Mã Sinh Viên']")))
                    .sendKeys("2511");
            driver2511.findElement(By.xpath("//input[@type='password' or @placeholder='Mật khẩu']")).sendKeys("2511");
            driver2511.findElement(By.cssSelector("button.btn-login")).click();
            wait2511.until(ExpectedConditions.urlToBe("http://localhost:5173/"));

            // Đăng nhập 1412 (Cửa sổ Phải)
            driver1412.get("http://localhost:5173/login");
            wait1412.until(ExpectedConditions
                    .visibilityOfElementLocated(By.xpath("//input[@placeholder='Email hoặc Mã Sinh Viên']")))
                    .sendKeys("1412");
            driver1412.findElement(By.xpath("//input[@type='password' or @placeholder='Mật khẩu']")).sendKeys("1412");
            driver1412.findElement(By.cssSelector("button.btn-login")).click();
            wait1412.until(ExpectedConditions.urlToBe("http://localhost:5173/"));

            // ------------------------------------------------------------------------
            // GIAI ĐOẠN 2: THAO TÁC LIKE & XEM THÔNG BÁO REAL-TIME
            // ------------------------------------------------------------------------
            System.out.println("Step 1 (1412): Nhấn nút Thích bài viết đầu tiên...");
            // 🟢 CHIẾN LƯỢC MỚI: Bắt nút 'Thích' của bài đầu tiên (Index 1) và dùng
            // presence
            WebElement btnLike = wait1412.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("(//button[contains(., 'Thích')])[1]")));

            js1412.executeScript("arguments[0].scrollIntoView({block: 'center'});", btnLike);
            Thread.sleep(1000); // Chờ UI ngừng cuộn

            // Ép click bằng Javascript xuyên giáp MUI
            js1412.executeScript("arguments[0].click();", btnLike);

            System.out.println("Step 2 (2511): Mở hộp thông báo và xem Real-time...");

            // 🟢 ĐÃ SỬA: Thêm "/.." ở cuối XPath để lấy thẻ HTML cha (thẻ span bọc bên
            // ngoài icon)
            WebElement btnNotify = wait2511.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@data-testid='NotificationsIcon']/..")));

            // Ép click bằng Javascript. Lúc này arguments[0] là thẻ <span> nên hàm click()
            // sẽ chạy hoàn hảo!
            ((JavascriptExecutor) driver2511).executeScript("arguments[0].click();", btnNotify);

            // Tạm dừng 3 giây để ngắm thông báo Like đổ về
            Thread.sleep(3000);

            // ------------------------------------------------------------------------
            // GIAI ĐOẠN 3: THAO TÁC SHARE & XỬ LÝ NATIVE PROMPT JAVASCRIPT
            // ------------------------------------------------------------------------
            System.out.println("Step 3 (1412): Nhấn nút Chia sẻ...");
            // Khóa mục tiêu vào nút Chia sẻ của bài đầu tiên
            WebElement btnShare = wait1412.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("(//button[contains(., 'Chia sẻ')])[1]")));
            js1412.executeScript("arguments[0].click();", btnShare);

            System.out.println("Step 4 (1412): Xử lý hộp thoại Nhập nội dung chia sẻ (JS Prompt)...");
            Alert sharePrompt = wait1412.until(ExpectedConditions.alertIsPresent());
            sharePrompt.sendKeys("Hãy cùng nhau đi lên văn phòng AWS nhé");
            sharePrompt.accept();

            System.out.println("Step 5 (1412): Xử lý hộp thoại báo Thành công (JS Alert)...");
            Alert successAlert = wait1412.until(ExpectedConditions.alertIsPresent());
            successAlert.accept();

            System.out.println("Step 6 (1412): Refresh lại trang...");
            driver1412.navigate().refresh();
            wait1412.until(ExpectedConditions.urlToBe("http://localhost:5173/"));

            // ------------------------------------------------------------------------
            // GIAI ĐOẠN 4: BÌNH LUẬN CÔNG KHAI VÀ ẨN DANH
            // ------------------------------------------------------------------------
            System.out.println("Step 7 (1412): Mở khu vực Bình luận của bài viết gốc...");

            // 🟢 CÚ BẮT BÀI ĐỈNH CAO: Bài viết gốc lúc này đã bị đẩy xuống vị trí số 2
            WebElement btnComment = wait1412.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("(//button[contains(., 'Bình luận')])[2]")));

            js1412.executeScript("arguments[0].scrollIntoView({block: 'center'});", btnComment);
            Thread.sleep(1000);
            js1412.executeScript("arguments[0].click();", btnComment);

            System.out.println("Step 8 (1412): Viết bình luận công khai...");
            WebElement inputComment = wait1412.until(ExpectedConditions
                    .visibilityOfElementLocated(By.xpath("//textarea[@placeholder='Viết bình luận...']")));
            inputComment.sendKeys("Good Morning");

            // 🟢 ĐÃ SỬA: Bắt thẳng Icon bằng dấu * (phớt lờ namespace SVG), lấy cái cuối
            // cùng (của bài viết 2), rồi lùi lên thẻ cha
            WebElement btnSend = wait1412.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("(//*[@data-testid='SendIcon'])[last()]/..")));

            // Dừng 0.5s để React kịp bỏ trạng thái "disabled" của nút Gửi sau khi nhận chữ
            Thread.sleep(500);
            js1412.executeScript("arguments[0].click();", btnSend);

            System.out.println("Step 9 (1412): Bật chế độ Bình luận Ẩn danh...");
            WebElement labelAnonymous = wait1412.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//label[contains(., 'Bình luận ẩn danh')]")));
            js1412.executeScript("arguments[0].click();", labelAnonymous);

            System.out.println("Step 10 (1412): Viết bình luận Ẩn danh...");
            // Ô input sẽ bị React clear sau khi gửi, ta cần gõ lại
            inputComment.sendKeys("Happy BirthDay");

            // Dừng thêm 0.5s cho an toàn trước khi bấm gửi lần 2
            Thread.sleep(500);
            js1412.executeScript("arguments[0].click();", btnSend);

            // ------------------------------------------------------------------------
            // GIAI ĐOẠN 5: XEM THÀNH QUẢ TRÊN TÀI KHOẢN NHẬN
            // ------------------------------------------------------------------------
            System.out.println("\n[XÁC NHẬN]: Cửa sổ (2511) đã nhận đủ thông báo Like, Share, Comment Realtime!");

            // Ngồi xem thành quả 10 giây
            Thread.sleep(10000);

            System.out.println("\n=== KỊCH BẢN KIỂM THỬ REAL-TIME HOÀN TẤT ===");

        } catch (Exception e) {
            System.err.println("!!! [THẤT BẠI] Kịch bản gãy. Chi tiết lỗi:");
            e.printStackTrace();
        } finally {
            System.out.println("\nĐóng toàn bộ trình duyệt.");
            driver1412.quit();
            driver2511.quit();
        }
    }
}