package com.example.backend.Item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/shop/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    // --- 1. LẤY TẤT CẢ VẬT PHẨM ĐANG BÁN ---
    // API: GET /api/shop/items
    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllActiveItems());
    }

    // --- 2. LẤY VẬT PHẨM THEO PHÂN LOẠI ---
    // API: GET /api/shop/items/type?type=AVATAR_FRAME
    @GetMapping("/type")
    public ResponseEntity<List<ItemResponse>> getItemsByType(@RequestParam("type") String type) {
        return ResponseEntity.ok(itemService.getItemsByType(type));
    }

    // --- 3. THÊM VẬT PHẨM MỚI VÀO SHOP ---
    // API: POST /api/shop/items
    // Lưu ý: Trong thực tế API này nên được bảo vệ, chỉ ADMIN mới được gọi
    @PostMapping
    public ResponseEntity<ItemResponse> createItem(@RequestBody ItemRequest request) {
        try {
            ItemResponse newItem = itemService.createItem(request);
            return ResponseEntity.ok(newItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{itemId}/buy")
    public ResponseEntity<?> buyItem(@PathVariable Integer itemId) {
        try {
            // Lấy StudentCode từ JWT Token của người đang đăng nhập
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // Gọi Service xử lý mua hàng
            String resultMessage = itemService.buyItem(studentCode, itemId);
            
            // Trả về JSON thành công
            return ResponseEntity.ok(Map.of("message", resultMessage));
        } catch (RuntimeException e) {
            // Trả về lỗi nếu không đủ tiền hoặc đã mua rồi
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // 🟢 1. API XEM TỦ ĐỒ
    // URL: GET /api/shop/items/inventory
    @GetMapping("/inventory")
    public ResponseEntity<?> getMyInventory() {
        try {
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok(itemService.getUserInventory(studentCode));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 🟢 2. API TRANG BỊ VIỀN
    // URL: PUT /api/shop/items/{itemId}/equip
    @PutMapping("/{itemId}/equip")
    public ResponseEntity<?> equipItem(@PathVariable Integer itemId) {
        try {
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            String message = itemService.equipFrame(studentCode, itemId);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 🟢 3. API THÁO VIỀN
    // URL: PUT /api/shop/items/unequip
    @PutMapping("/unequip")
    public ResponseEntity<?> unequipItem() {
        try {
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            String message = itemService.unequipFrame(studentCode);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}