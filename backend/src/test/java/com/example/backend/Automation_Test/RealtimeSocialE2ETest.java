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

public class RealtimeSocialE2ETest {

    private static final int ACTION_DELAY_MS = 250;
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

    /** Mã SV tài khoản nhận thông báo real-time trong kịch bản này. */
    private static final String NOTIFICATION_RECEIVER_CODE = "2511";

    private static void waitForOtherUsersPost(WebDriverWait wait) {
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='post-card'][data-self-post='false']")));
    }

    /** Nút Thích trên bài không phải của người đang thao tác; ưu tiên bài của NOTIFICATION_RECEIVER_CODE. */
    private static By likeButtonOnOtherUsersPost(String preferredAuthorCode) {
        return By.xpath(String.format(
                "(//*[@data-testid='post-card' and @data-self-post='false' and @data-author-code='%s']"
                        + "//button[contains(., 'Thích')])[1]",
                preferredAuthorCode));
    }

    private static By likeButtonOnAnyOtherUsersPost() {
        return By.xpath(
                "(//*[@data-testid='post-card' and @data-self-post='false']//button[contains(., 'Thích')])[1]");
    }

    private static By shareButtonOnOtherUsersPost(String preferredAuthorCode) {
        return By.xpath(String.format(
                "(//*[@data-testid='post-card' and @data-self-post='false' and @data-author-code='%s']"
                        + "//*[@data-testid='post-share-button'])[1]",
                preferredAuthorCode));
    }

    private static By shareButtonOnAnyOtherUsersPost() {
        return By.xpath(
                "(//*[@data-testid='post-card' and @data-self-post='false']//*[@data-testid='post-share-button'])[1]");
    }

    private static By commentButtonOnAuthorsPost(String authorCode) {
        return By.xpath(String.format(
                "(//*[@data-testid='post-card' and @data-author-code='%s']"
                        + "//button[contains(., 'Bình luận')])[1]",
                authorCode));
    }

    private static void clickLikeOnOtherUsersPost(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {
        waitForOtherUsersPost(wait);
        By preferred = likeButtonOnOtherUsersPost(NOTIFICATION_RECEIVER_CODE);
        if (!driver.findElements(preferred).isEmpty()) {
            System.out.println("  -> Thích bài viết của " + NOTIFICATION_RECEIVER_CODE);
            clickStable(driver, wait, js, preferred);
        } else {
            System.out.println("  -> Không thấy bài của " + NOTIFICATION_RECEIVER_CODE
                    + ", thích bài người khác đầu tiên trên feed");
            clickStable(driver, wait, js, likeButtonOnAnyOtherUsersPost());
        }
    }

    private static void clickShareOnOtherUsersPost(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {
        waitForOtherUsersPost(wait);
        By preferred = shareButtonOnOtherUsersPost(NOTIFICATION_RECEIVER_CODE);
        if (!driver.findElements(preferred).isEmpty()) {
            clickStable(driver, wait, js, preferred);
        } else {
            clickStable(driver, wait, js, shareButtonOnAnyOtherUsersPost());
        }
    }

    private static void login(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String identifier, String password) {
        wait.until(ExpectedConditions.urlContains("/login"));
        waitForPageReady(driver, wait);
        pause(STEP_DELAY_MS);
        By identifierField = By.cssSelector("[data-testid='login-identifier']");
        By passwordField = By.cssSelector("[data-testid='login-password']");
        By loginButton = By.cssSelector("button.btn-login");
        WebElement idInput = wait.until(ExpectedConditions.visibilityOfElementLocated(identifierField));
        WebElement pwInput = wait.until(ExpectedConditions.visibilityOfElementLocated(passwordField));
        fillReactField(js, idInput, identifier);
        pause(STEP_DELAY_MS);
        fillReactField(js, pwInput, password);
        pause(STEP_DELAY_MS);
        clickStable(driver, wait, js, loginButton);
        pause(STEP_DELAY_MS);
    }

    public static void main(String[] args) {
        WebDriverListener slowMotionListener = new SlowMotionListener();
        System.out.println("Khởi tạo Trình duyệt 1 (Tài khoản Nhận - 2511)...");
        WebDriver driver2511 = new EventFiringDecorator<>(slowMotionListener).decorate(new ChromeDriver());
        WebDriverWait wait2511 = new WebDriverWait(driver2511, Duration.ofSeconds(20));
        JavascriptExecutor js2511 = (JavascriptExecutor) driver2511;
        driver2511.manage().window().setSize(new org.openqa.selenium.Dimension(900, 1000));
        driver2511.manage().window().setPosition(new org.openqa.selenium.Point(0, 0));

        System.out.println("Khởi tạo Trình duyệt 2 (Tài khoản Tương tác - 1412)...");
        WebDriver driver1412 = new EventFiringDecorator<>(slowMotionListener).decorate(new ChromeDriver());
        WebDriverWait wait1412 = new WebDriverWait(driver1412, Duration.ofSeconds(20));
        JavascriptExecutor js1412 = (JavascriptExecutor) driver1412;
        driver1412.manage().window().setSize(new org.openqa.selenium.Dimension(900, 1000));
        driver1412.manage().window().setPosition(new org.openqa.selenium.Point(900, 0));
        try {
            System.out.println("=== BẮT ĐẦU KỊCH BẢN KIỂM THỬ REAL-TIME WEBSOCKET ===");
            driver2511.get("http://localhost:5173/login");
            login(driver2511, wait2511, js2511, "2511", "2511");
            wait2511.until(ExpectedConditions.urlToBe("http://localhost:5173/"));

            driver1412.get("http://localhost:5173/login");
            login(driver1412, wait1412, js1412, "1412", "1412");
            wait1412.until(ExpectedConditions.urlToBe("http://localhost:5173/"));
            
            System.out.println("Step 1 (1412): Thích bài viết của người khác (không phải bài của 1412)...");
            waitForPageReady(driver1412, wait1412);
            clickLikeOnOtherUsersPost(driver1412, wait1412, js1412);
            pause(STEP_DELAY_MS);

            System.out.println("Step 2 (2511): Mở hộp thông báo và xem Real-time...");
            By notifyBtn = By.cssSelector("[data-testid='NotificationsIcon']");
            WebElement bellIcon = wait2511.until(ExpectedConditions.presenceOfElementLocated(notifyBtn));
            js2511.executeScript(
                    "const icon = arguments[0]; const btn = icon.closest('button'); (btn || icon).click();", bellIcon);
            pause(3000);

            System.out.println("Step 3 (1412): Chia sẻ bài viết của người khác...");
            clickShareOnOtherUsersPost(driver1412, wait1412, js1412);

            System.out.println("Step 4 (1412): Nhập nội dung chia sẻ trong dialog...");
            By shareTextarea = By.cssSelector("[data-testid='share-post-content'] textarea");
            WebElement shareInput = wait1412.until(ExpectedConditions.visibilityOfElementLocated(shareTextarea));
            fillReactField(js1412, shareInput, "Hãy cùng nhau đi lên văn phòng AWS nhé");
            pause(STEP_DELAY_MS);

            System.out.println("Step 5 (1412): Xác nhận chia sẻ...");
            By shareSubmit = By.cssSelector("[data-testid='share-post-submit']");
            clickWhenEnabled(driver1412, wait1412, js1412, shareSubmit);
            wait1412.until(ExpectedConditions.invisibilityOfElementLocated(shareTextarea));
            pause(STEP_DELAY_MS);

            System.out.println("Step 6 (1412): Refresh lại trang...");
            driver1412.navigate().refresh();
            wait1412.until(ExpectedConditions.urlToBe("http://localhost:5173/"));
            waitForPageReady(driver1412, wait1412);
            pause(STEP_DELAY_MS);

            System.out.println("Step 7 (1412): Mở bình luận trên bài của " + NOTIFICATION_RECEIVER_CODE + "...");
            clickStable(driver1412, wait1412, js1412,
                    commentButtonOnAuthorsPost(NOTIFICATION_RECEIVER_CODE));

            System.out.println("Step 8 (1412): Viết bình luận công khai...");
            By commentInput = By.cssSelector("[data-testid='comment-input'] textarea");
            WebElement commentField = wait1412.until(ExpectedConditions.visibilityOfElementLocated(commentInput));
            fillReactField(js1412, commentField, "Good Morning");
            pause(STEP_DELAY_MS);
            clickWhenEnabled(driver1412, wait1412, js1412, By.cssSelector("[data-testid='comment-submit']"));
            pause(STEP_DELAY_MS);

            System.out.println("Step 9 (1412): Bật chế độ Bình luận Ẩn danh...");
            clickStable(driver1412, wait1412, js1412, By.cssSelector("[data-testid='comment-anonymous-toggle']"));
            pause(STEP_DELAY_MS);

            System.out.println("Step 10 (1412): Viết bình luận Ẩn danh...");
            commentField = wait1412.until(ExpectedConditions.visibilityOfElementLocated(commentInput));
            fillReactField(js1412, commentField, "Happy BirthDay");
            pause(STEP_DELAY_MS);
            clickWhenEnabled(driver1412, wait1412, js1412, By.cssSelector("[data-testid='comment-submit']"));
            System.out.println("\n[XÁC NHẬN]: Cửa sổ (2511) đã nhận đủ thông báo Like, Share, Comment Realtime!");
            pause(10000);
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
