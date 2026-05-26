package com.example.backend.Automation_Test;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PostLifecycleE2ETest {

    /** Độ trễ trước mỗi click/gõ phím (slow motion). */
    private static final int ACTION_DELAY_MS = 250;
    /** Độ trễ giữa các bước kịch bản để dễ quan sát. */
    private static final int STEP_DELAY_MS = 300;

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
                Thread.sleep(ACTION_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void pause(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void waitForPageReady(WebDriver driver, WebDriverWait wait) {
        wait.until(webDriver -> "complete".equals(
                ((JavascriptExecutor) webDriver).executeScript("return document.readyState")));
    }

    private static void waitForCreatePostReady(WebDriver driver, WebDriverWait wait) {
        By trigger = By.cssSelector("[data-testid='create-post-trigger']");
        wait.until(ExpectedConditions.presenceOfElementLocated(trigger));
        wait.until(webDriver -> {
            String text = webDriver.findElement(trigger).getText();
            return text != null && !text.contains("Đang tải...");
        });
    }

    /** Gõ vào input/textarea controlled (React) — sendKeys thường không cập nhật state. */
    private static void fillReactField(JavascriptExecutor js, WebElement element, String text) {
        element.click();
        js.executeScript(
                "const el = arguments[0];"
                        + "const value = arguments[1];"
                        + "el.focus();"
                        + "const proto = el.tagName === 'TEXTAREA'"
                        + "  ? window.HTMLTextAreaElement.prototype"
                        + "  : window.HTMLInputElement.prototype;"
                        + "const setter = Object.getOwnPropertyDescriptor(proto, 'value').set;"
                        + "setter.call(el, value);"
                        + "el.dispatchEvent(new InputEvent('input', { bubbles: true, cancelable: true }));",
                element, text);
    }

    private static void login(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
            String identifier, String password) {
        wait.until(ExpectedConditions.urlContains("/login"));
        waitForPageReady(driver, wait);
        pause(STEP_DELAY_MS);

        By identifierField = By.cssSelector("[data-testid='login-identifier']");
        By passwordField = By.cssSelector("[data-testid='login-password']");
        By loginButton = By.cssSelector("button.btn-login");

        WebElement idInput = wait.until(ExpectedConditions.visibilityOfElementLocated(identifierField));
        WebElement pwInput = wait.until(ExpectedConditions.visibilityOfElementLocated(passwordField));

        idInput.click();
        pause(STEP_DELAY_MS);
        fillReactField(js, idInput, identifier);
        pause(STEP_DELAY_MS);

        pwInput.click();
        pause(STEP_DELAY_MS);
        fillReactField(js, pwInput, password);
        pause(STEP_DELAY_MS);

        clickStable(driver, wait, js, loginButton);
        pause(STEP_DELAY_MS);
    }

    /** Tìm lại element và click bằng JS — tránh StaleElement sau re-render/reload. */
    private static void clickStable(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, By locator) {
        wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                WebElement el = wait.until(ExpectedConditions.elementToBeClickable(locator));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", el);
                js.executeScript("arguments[0].click();", el);
                return;
            } catch (StaleElementReferenceException e) {
                if (attempt == 4) {
                    throw e;
                }
            }
        }
    }

    private static void clickWhenEnabled(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, By locator) {
        wait.until(webDriver -> {
            WebElement el = webDriver.findElement(locator);
            return el.isDisplayed() && el.isEnabled();
        });
        clickStable(driver, wait, js, locator);
    }

    public static void main(String[] args) {

        // 1. Khởi tạo Driver gốc
        WebDriver originalDriver = new ChromeDriver();

        // 2. Sử dụng Public Class vừa tạo thay vì class ẩn danh
        WebDriverListener slowMotionListener = new SlowMotionListener();

        // 3. Bọc Driver lại để kích hoạt Slow Motion
        WebDriver driver = new EventFiringDecorator<>(slowMotionListener).decorate(originalDriver);

        driver.manage().window().maximize();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
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
            login(driver, wait, js, "2511", "2511");

            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/"));
            pause(STEP_DELAY_MS);
            waitForPageReady(driver, wait);
            waitForCreatePostReady(driver, wait);

            System.out.println("Step 2: Mở hộp thoại đăng bài...");
            clickStable(driver, wait, js, By.cssSelector("[data-testid='create-post-trigger']"));

            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("[data-testid='create-post-content'] textarea")));

            System.out.println("Step 3: Nhập nội dung bài viết...");
            By postTextarea = By.cssSelector("[data-testid='create-post-content'] textarea");
            WebElement textareaPost = wait.until(ExpectedConditions.visibilityOfElementLocated(postTextarea));
            fillReactField(js, textareaPost, postContent);
            pause(STEP_DELAY_MS);

            System.out.println("Step 4: Nhấn nút Đăng...");
            By submitPost = By.cssSelector("[data-testid='create-post-submit']");
            clickWhenEnabled(driver, wait, js, submitPost);

            // Đợi dialog đóng (không còn textarea đăng bài)
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("[data-testid='create-post-content'] textarea")));

            System.out.println("Step 5: Đăng xuất tài khoản User...");
            waitForPageReady(driver, wait);
            clickStable(driver, wait, js, By.cssSelector("[data-testid='header-settings-button']"));

            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("[data-testid='header-logout']")));
            clickStable(driver, wait, js, By.cssSelector("[data-testid='header-logout']"));
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/login"));
            pause(STEP_DELAY_MS);

            // ------------------------------------------------------------------------
            // GIAI ĐOẠN 2: ADMIN ĐĂNG NHẬP VÀ DUYỆT BÀI
            // ------------------------------------------------------------------------
            System.out.println("\nStep 6: Đăng nhập tài khoản Admin (141204)...");
            login(driver, wait, js, "141204", "1412");
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/admin/dashboard"));
            pause(STEP_DELAY_MS);

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
            login(driver, wait, js, "2511", "2511");
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/"));
            pause(STEP_DELAY_MS);

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
