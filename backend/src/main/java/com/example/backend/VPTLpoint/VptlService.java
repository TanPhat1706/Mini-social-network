package com.example.backend.VPTLpoint;

import com.example.backend.User.User;
import com.example.backend.User.UserDailyStat;
import com.example.backend.User.UserDailyStatRepository;
import com.example.backend.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class VptlService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDailyStatRepository dailyStatRepository;

    // --- CẤU HÌNH LUẬT TÍNH ĐIỂM (RULE CONFIG) ---
    // Nhóm Social (Thưởng theo ngày)
    private static final int EXP_DAILY_POST = 5;      // Đăng 1 bài đầu tiên trong ngày
    private static final int EXP_DAILY_LIKE = 3;      // Mỗi lượt like hợp lệ
    private static final int EXP_DAILY_COMMENT = 3;   // Mỗi lượt comment hợp lệ
    private static final int EXP_DAILY_SHARE = 2;     // Mỗi lượt share hợp lệ
    private static final int MAX_SOCIAL_INTERACTIONS_PER_DAY = 10; // Tối đa 10 lượt like/comment/share mỗi ngày

    // Nhóm Game
    private static final int GAME_SCORE_DIVISOR = 10; // Điểm game / 10 = EXP & Points

    // CẤU HÌNH LEVEL (Mốc điểm để lên cấp)
    private static final int[] LEVEL_THRESHOLDS = {0, 100, 300, 600, 1000, 2000, 5000};

    /**
     * 1. XỬ LÝ EXP VÀ TIỀN TỪ GAME (Gọi khi kết thúc game)
     */
    @Transactional
    public void addGameExp(Integer userId, int gameScore) {
        int rewardAmount = gameScore / GAME_SCORE_DIVISOR;
        if (rewardAmount > 0) {
            // Cấp cả EXP và Tiền với số lượng bằng nhau
            grantRewards(userId, rewardAmount, rewardAmount);
        }
    }

    /**
     * 2. XỬ LÝ EXP VÀ TIỀN TỪ SOCIAL (Gọi sau khi user đăng bài, like, comment...)
     */
    @Transactional
    public void trackSocialActivity(Integer userId, String actionType) {
        LocalDate today = LocalDate.now();

        UserDailyStat stat = dailyStatRepository.findByUserIdAndDate(userId, today)
                .orElse(new UserDailyStat(userId, today));

        boolean shouldAddReward = false;
        int rewardToAdd = 0;
        String normalizedActionType = actionType.toUpperCase();

        // Giới hạn tối đa số lượt tương tác social trong ngày (like/comment/share)
        if (isInteractionAction(normalizedActionType)) {
            int totalInteractions = stat.getLikeCount() + stat.getCommentCount() + stat.getShareCount();
            if (totalInteractions >= MAX_SOCIAL_INTERACTIONS_PER_DAY) {
                return;
            }
        }

        switch (normalizedActionType) {
            case "POST":
                if (stat.getPostCount() == 0) {
                    rewardToAdd = EXP_DAILY_POST;
                    shouldAddReward = true;
                }
                stat.setPostCount(stat.getPostCount() + 1);
                break;

            case "LIKE":
                stat.setLikeCount(stat.getLikeCount() + 1);
                rewardToAdd = EXP_DAILY_LIKE;
                shouldAddReward = true;
                break;

            case "COMMENT":
                stat.setCommentCount(stat.getCommentCount() + 1);
                rewardToAdd = EXP_DAILY_COMMENT;
                shouldAddReward = true;
                break;

            case "SHARE":
                stat.setShareCount(stat.getShareCount() + 1);
                rewardToAdd = EXP_DAILY_SHARE;
                shouldAddReward = true;
                break;
        }

        dailyStatRepository.save(stat);

        // Nếu đạt chỉ tiêu thì cộng EXP và Tiền vào User
        if (shouldAddReward) {
            grantRewards(userId, rewardToAdd, rewardToAdd);
            System.out.println("🎉 User " + userId + " nhận được " + rewardToAdd + " EXP & VPTL Points từ hoạt động " + actionType);
        }
    }

    /**
     * 3. HÀM LÕI: CỘNG EXP, CỘNG TIỀN VÀ TỰ ĐỘNG TĂNG LEVEL
     * (Đã đổi tên từ addExpToUser thành grantRewards cho sát nghĩa)
     */
    private void grantRewards(Integer userId, int expAmount, int pointsAmount) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        // 1. Cộng dồn EXP
        int oldExp = user.getExp();
        int newExp = oldExp + expAmount;
        user.setExp(newExp);

        // 2. Cộng dòng tiền (VPTL Points)
        // Dùng toán tử 3 ngôi check null để phòng trường hợp data cũ dưới DB bị lỗi
        int currentPoints = (user.getVptlPoints() != null) ? user.getVptlPoints() : 0;
        user.setVptlPoints(currentPoints + pointsAmount);

        // 3. Tính toán Level mới
        int currentLevel = user.getLevel();
        int newLevel = calculateLevel(newExp);

        if (newLevel > currentLevel) {
            user.setLevel(newLevel);
            System.out.println("🚀 CHÚC MỪNG! User " + userId + " đã thăng cấp lên Level " + newLevel);
            // TODO: Bắn Notification
        }

        userRepository.save(user);
    }

    private int calculateLevel(int totalExp) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (totalExp >= LEVEL_THRESHOLDS[i]) {
                return i + 1; 
            }
        }
        return 1;
    }

    private boolean isInteractionAction(String actionType) {
        return "LIKE".equals(actionType) || "COMMENT".equals(actionType) || "SHARE".equals(actionType);
    }
}