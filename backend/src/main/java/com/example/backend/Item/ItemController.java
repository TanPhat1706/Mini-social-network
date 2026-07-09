package com.example.backend.Item;

import com.example.backend.Enum.CosmeticType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shop/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllActiveItems());
    }

    @GetMapping("/type")
    public ResponseEntity<List<ItemResponse>> getItemsByType(@RequestParam("type") CosmeticType type) {
        return ResponseEntity.ok(itemService.getItemsByType(type));
    }

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
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            String resultMessage = itemService.buyItem(studentCode, itemId);
            return ResponseEntity.ok(Map.of("message", resultMessage));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/inventory")
    public ResponseEntity<?> getMyInventory() {
        try {
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok(itemService.getUserInventory(studentCode));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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
