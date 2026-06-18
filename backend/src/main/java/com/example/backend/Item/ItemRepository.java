package com.example.backend.Item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {
    
    // Spring Data JPA tự động hiểu: SELECT * FROM items WHERE active = true
    // Dùng để lấy toàn bộ danh sách đồ đang bán trong Ngân hàng điểm
    List<Item> findByActiveTrue();

    // Tự động hiểu: SELECT * FROM items WHERE active = true AND type = ?
    // Dùng để lọc riêng biệt (Ví dụ: chỉ lấy danh sách viền Avatar)
    List<Item> findByActiveTrueAndType(String type);
}