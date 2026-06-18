package com.example.backend.Item;

import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 🟢 FIX LỖI 1: Import thư viện Transactional chuẩn của Spring
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    // 🟢 FIX LỖI 2: Đảm bảo đã khai báo inventoryRepository
    @Autowired
    private UserInventoryRepository inventoryRepository;

    public List<ItemResponse> getAllActiveItems() {
        List<Item> items = itemRepository.findByActiveTrue();
        return items.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<ItemResponse> getItemsByType(String type) {
        List<Item> items = itemRepository.findByActiveTrueAndType(type);
        return items.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public ItemResponse createItem(ItemRequest request) {
        Item item = new Item();
        item.setName(request.getName());
        item.setType(request.getType());
        item.setImageUrl(request.getImageUrl());
        item.setPrice(request.getPrice());
        item.setDescription(request.getDescription());

        Item savedItem = itemRepository.save(item);
        return mapToResponse(savedItem);
    }

    // 🟢 Dùng @Transactional đã được import đúng ở trên
    @Transactional
    public String buyItem(String studentCode, Integer itemId) {
        // 1. Tìm User và Item
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại trên hệ thống."));

        // 2. Kiểm tra xem đồ có đang được bán không
        if (!item.getActive()) {
            throw new RuntimeException("Vật phẩm này đã ngừng bán.");
        }

        // 3. Kiểm tra xem User đã mua món này chưa
        if (inventoryRepository.existsByUserIdAndItemId(user.getId(), item.getId())) {
            throw new RuntimeException("Bạn đã sở hữu vật phẩm này rồi, không cần mua lại đâu!");
        }

        // 4. Kiểm tra ví tiền
        if (user.getVptlPoints() < item.getPrice()) {
            throw new RuntimeException("Số dư VPTL Points không đủ. Cày thêm game đi bạn ơi!");
        }

        // 5. THỰC THI GIAO DỊCH
        // Trừ tiền
        user.setVptlPoints(user.getVptlPoints() - item.getPrice());
        userRepository.save(user);

        // Cất đồ vào tủ (Lưu lịch sử mua hàng)
        UserInventory newInventory = new UserInventory();
        newInventory.setUserId(user.getId());
        newInventory.setItemId(item.getId());
        inventoryRepository.save(newInventory);

        return "Chúc mừng! Bạn đã mua thành công: " + item.getName();
    }

    private ItemResponse mapToResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .type(item.getType())
                .imageUrl(item.getImageUrl())
                .price(item.getPrice())
                .description(item.getDescription())
                .active(item.getActive())
                .build();
    }

    // 🟢 1. API LẤY TỦ ĐỒ (INVENTORY)
    public List<ItemResponse> getUserInventory(String studentCode) {
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Lấy danh sách ID các món đồ user đã mua
        List<UserInventory> inventoryRecords = inventoryRepository.findByUserId(user.getId());
        List<Integer> itemIds = inventoryRecords.stream()
                .map(UserInventory::getItemId)
                .collect(Collectors.toList());

        // Dùng danh sách ID đó để kéo thông tin chi tiết các món đồ ra
        List<Item> ownedItems = itemRepository.findAllById(itemIds);

        return ownedItems.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // 🟢 2. API TRANG BỊ VẬT PHẨM (EQUIP)
    @Transactional
    public String equipFrame(String studentCode, Integer itemId) {
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra xem User có thật sự mua món này chưa (Chống hack)
        if (!inventoryRepository.existsByUserIdAndItemId(user.getId(), itemId)) {
            throw new RuntimeException("Bạn chưa sở hữu vật phẩm này! Vui lòng mua trước khi trang bị.");
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại"));

        // 🎯 LOGIC PHÂN LOẠI TRANG BỊ (Mở rộng thoải mái ở đây)
        if ("AVATAR_FRAME".equals(item.getType())) {
            user.setCurrentAvatarFrame(item.getImageUrl());
        } else if ("NAME_COLOR".equals(item.getType())) {
            user.setCurrentNameColor(item.getImageUrl());
        } else {
            throw new RuntimeException("Loại vật phẩm này không hỗ trợ trang bị!");
        }

        userRepository.save(user);
        return "Đã trang bị thành công: " + item.getName();
    }

    // 🟢 3. API THÁO TRANG BỊ (UNEQUIP)
    @Transactional
    public String unequipFrame(String studentCode) {
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Tạm thời: Tháo toàn bộ (Cả viền lẫn màu tên).
        // Nếu sau này bạn muốn tháo riêng từng cái, ta sẽ truyền thêm tham số type.
        user.setCurrentAvatarFrame(null);
        user.setCurrentNameColor(null);
        userRepository.save(user);

        return "Đã tháo trang bị (Viền & Màu tên). Trở về phong cách mộc mạc!";
    }
}