package com.example.backend.VPTLpoint;

import com.example.backend.User.User;
import com.example.backend.User.UserDailyStat;
import com.example.backend.User.UserDailyStatRepository;
import com.example.backend.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VptlServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDailyStatRepository dailyStatRepository;

    @InjectMocks
    private VptlService vptlService;

    private User currentUser;
    private UserDailyStat dailyStat;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1);
        currentUser.setExp(50);
        currentUser.setVptlPoints(100);
        currentUser.setLevel(1);

        dailyStat = new UserDailyStat(1, LocalDate.now());
    }

    // ==========================================
    // 1. TEST HÀM GAME EXP (addGameExp)
    // ==========================================

    @Test
    void addGameExp_whenScoreTooLow_shouldNotGrantReward() {
        vptlService.addGameExp(1, 9); // 9 / 10 = 0
        verify(userRepository, never()).findById(anyInt());
    }

    @Test
    void addGameExp_whenUserNotFound_shouldDoNothing() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());
        vptlService.addGameExp(1, 100);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addGameExp_whenValidScore_shouldAddExpAndPoints() {
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));
        
        vptlService.addGameExp(1, 250); // 250 / 10 = 25 điểm

        assertEquals(75, currentUser.getExp()); // 50 + 25
        assertEquals(125, currentUser.getVptlPoints()); // 100 + 25
        assertEquals(1, currentUser.getLevel()); // Chưa đủ 100 exp để lên level 2
        verify(userRepository).save(currentUser);
    }

    @Test
    void addGameExp_whenExpCrossThreshold_shouldLevelUp() {
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));
        
        vptlService.addGameExp(1, 600); // 600 / 10 = 60 điểm

        assertEquals(110, currentUser.getExp()); // 50 + 60 = 110 (> 100)
        assertEquals(2, currentUser.getLevel()); // Phải nhảy lên Level 2
        verify(userRepository).save(currentUser);
    }

    @Test
    void addGameExp_whenVptlPointsNull_shouldHandleSafely() {
        currentUser.setVptlPoints(null); // Giả lập dữ liệu lỗi dưới DB
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));
        
        vptlService.addGameExp(1, 100); // Nhận 10 điểm

        assertEquals(10, currentUser.getVptlPoints()); // 0 + 10
        verify(userRepository).save(currentUser);
    }

    // ==========================================
    // 2. TEST HÀM SOCIAL ACTIVITY (trackSocialActivity)
    // ==========================================

    @Test
    void trackSocialActivity_whenLimitReached_shouldReturnEarly() {
        dailyStat.setLikeCount(5);
        dailyStat.setCommentCount(3);
        dailyStat.setShareCount(2); // Tổng = 10 (Max)
        
        when(dailyStatRepository.findByUserIdAndDate(eq(1), any(LocalDate.class)))
                .thenReturn(Optional.of(dailyStat));

        vptlService.trackSocialActivity(1, "LIKE");

        // Đảm bảo không lưu stat và không cộng tiền
        verify(dailyStatRepository, never()).save(any(UserDailyStat.class));
        verify(userRepository, never()).findById(anyInt());
    }

    @Test
    void trackSocialActivity_whenFirstPost_shouldAddReward() {
        dailyStat.setPostCount(0);
        when(dailyStatRepository.findByUserIdAndDate(eq(1), any(LocalDate.class)))
                .thenReturn(Optional.of(dailyStat));
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));

        vptlService.trackSocialActivity(1, "POST");

        assertEquals(1, dailyStat.getPostCount());
        assertEquals(55, currentUser.getExp()); // 50 + 5 (EXP_DAILY_POST)
        verify(dailyStatRepository).save(dailyStat);
        verify(userRepository).save(currentUser);
    }

    @Test
    void trackSocialActivity_whenSecondPost_shouldNotAddReward() {
        dailyStat.setPostCount(1); // Đã đăng 1 bài rồi
        when(dailyStatRepository.findByUserIdAndDate(eq(1), any(LocalDate.class)))
                .thenReturn(Optional.of(dailyStat));

        vptlService.trackSocialActivity(1, "POST");

        assertEquals(2, dailyStat.getPostCount());
        // Chỉ lưu stat, KHÔNG gọi hàm userRepository.findById để cộng thưởng
        verify(dailyStatRepository).save(dailyStat);
        verify(userRepository, never()).findById(anyInt()); 
    }

    @Test
    void trackSocialActivity_whenLike_shouldAddReward() {
        when(dailyStatRepository.findByUserIdAndDate(eq(1), any(LocalDate.class)))
                .thenReturn(Optional.of(dailyStat));
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));

        vptlService.trackSocialActivity(1, "LIKE");

        assertEquals(1, dailyStat.getLikeCount());
        assertEquals(53, currentUser.getExp()); // 50 + 3
        verify(dailyStatRepository).save(dailyStat);
    }

    @Test
    void trackSocialActivity_whenComment_shouldAddReward() {
        when(dailyStatRepository.findByUserIdAndDate(eq(1), any(LocalDate.class)))
                .thenReturn(Optional.of(dailyStat));
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));

        vptlService.trackSocialActivity(1, "COMMENT");

        assertEquals(1, dailyStat.getCommentCount());
        assertEquals(53, currentUser.getExp()); // 50 + 3
        verify(dailyStatRepository).save(dailyStat);
    }

    @Test
    void trackSocialActivity_whenShare_shouldAddReward() {
        when(dailyStatRepository.findByUserIdAndDate(eq(1), any(LocalDate.class)))
                .thenReturn(Optional.of(dailyStat));
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));

        vptlService.trackSocialActivity(1, "SHARE");

        assertEquals(1, dailyStat.getShareCount());
        assertEquals(52, currentUser.getExp()); // 50 + 2
        verify(dailyStatRepository).save(dailyStat);
    }

    @Test
    void trackSocialActivity_whenUnknownAction_shouldOnlySaveStat() {
        when(dailyStatRepository.findByUserIdAndDate(eq(1), any(LocalDate.class)))
                .thenReturn(Optional.of(dailyStat));

        vptlService.trackSocialActivity(1, "HACK_ACTION");

        // Các biến đếm không thay đổi
        assertEquals(0, dailyStat.getPostCount());
        assertEquals(0, dailyStat.getLikeCount());

        verify(dailyStatRepository).save(dailyStat);
        verify(userRepository, never()).findById(anyInt());
    }
}