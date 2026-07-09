package com.example.backend.Item;

import com.example.backend.Enum.CosmeticType;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final UserInventoryRepository inventoryRepository;

    public List<ItemResponse> getAllActiveItems() {
        return itemRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ItemResponse> getItemsByType(CosmeticType type) {
        return itemRepository.findByActiveTrueAndTypeOrderByDisplayOrderAsc(type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ItemResponse createItem(ItemRequest request) {
        Item item = Item.builder()
                .code(request.getCode())
                .name(request.getName())
                .type(request.getType())
                .theme(request.getTheme())
                .rarity(request.getRarity())
                .effectKey(request.getEffectKey())
                .price(request.getPrice())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder())
                .build();

        Item savedItem = itemRepository.save(item);
        return mapToResponse(savedItem);
    }

    @Transactional
    public String buyItem(String studentCode, Integer itemId) {
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại trên hệ thống."));

        if (!item.getActive()) {
            throw new RuntimeException("Vật phẩm này đã ngừng bán.");
        }

        if (inventoryRepository.existsByUserIdAndItemId(user.getId(), item.getId())) {
            throw new RuntimeException("Bạn đã sở hữu vật phẩm này rồi, không cần mua lại đâu!");
        }

        if (user.getVptlPoints() < item.getPrice()) {
            throw new RuntimeException("Số dư VPTL Points không đủ. Cày thêm game đi bạn ơi!");
        }

        user.setVptlPoints(user.getVptlPoints() - item.getPrice());
        userRepository.save(user);

        UserInventory newInventory = new UserInventory();
        newInventory.setUserId(user.getId());
        newInventory.setItemId(item.getId());
        inventoryRepository.save(newInventory);

        return "Chúc mừng! Bạn đã mua thành công: " + item.getName();
    }

    public List<ItemResponse> getUserInventory(String studentCode) {
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Integer> itemIds = inventoryRepository.findByUserId(user.getId()).stream()
                .map(UserInventory::getItemId)
                .collect(Collectors.toList());

        return itemRepository.findAllById(itemIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public String equipFrame(String studentCode, Integer itemId) {
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!inventoryRepository.existsByUserIdAndItemId(user.getId(), itemId)) {
            throw new RuntimeException("Bạn chưa sở hữu vật phẩm này! Vui lòng mua trước khi trang bị.");
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại"));

        if (item.getType() == CosmeticType.AVATAR_FRAME) {
            user.setCurrentAvatarFrame(item.getEffectKey());
        } else if (item.getType() == CosmeticType.NAME_COLOR) {
            user.setCurrentNameColor(item.getEffectKey());
        } else {
            throw new RuntimeException("Loại vật phẩm này không hỗ trợ trang bị!");
        }

        userRepository.save(user);
        return "Đã trang bị thành công: " + item.getName();
    }

    @Transactional
    public String unequipFrame(String studentCode) {
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setCurrentAvatarFrame(null);
        user.setCurrentNameColor(null);
        userRepository.save(user);

        return "Đã tháo trang bị (Viền & Màu tên). Trở về phong cách mộc mạc!";
    }

    private ItemResponse mapToResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .code(item.getCode())
                .name(item.getName())
                .type(item.getType())
                .theme(item.getTheme())
                .rarity(item.getRarity())
                .effectKey(item.getEffectKey())
                .price(item.getPrice())
                .description(item.getDescription())
                .displayOrder(item.getDisplayOrder())
                .active(item.getActive())
                .build();
    }
}
