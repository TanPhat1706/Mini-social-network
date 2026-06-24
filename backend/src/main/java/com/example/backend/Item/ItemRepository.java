package com.example.backend.Item;

import com.example.backend.Enum.CosmeticType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {

    List<Item> findByActiveTrueOrderByDisplayOrderAsc();

    List<Item> findByActiveTrueAndTypeOrderByDisplayOrderAsc(CosmeticType type);
}
