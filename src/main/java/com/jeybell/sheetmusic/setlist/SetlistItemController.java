package com.jeybell.sheetmusic.setlist;

import com.jeybell.sheetmusic.setlist.dto.SetlistItemRequest;
import com.jeybell.sheetmusic.setlist.dto.SetlistItemResponse;
import com.jeybell.sheetmusic.setlist.dto.SetlistItemUpdateRequest;
import com.jeybell.sheetmusic.setlist.dto.SetlistReorderRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SetlistItemController {

    private final SetlistItemService setlistItemService;

    public SetlistItemController(SetlistItemService setlistItemService) {
        this.setlistItemService = setlistItemService;
    }

    @PostMapping("/api/setlists/{setlistId}/items")
    public ResponseEntity<SetlistItemResponse> addItem(
            @PathVariable("setlistId") Long setlistId,
            @Valid @RequestBody SetlistItemRequest request
    ) {
        SetlistItemResponse response = setlistItemService.addItem(setlistId, request);
        return ResponseEntity
                .created(Objects.requireNonNull(URI.create("/api/setlist-items/" + response.setlistItemId())))
                .body(response);
    }

    @PutMapping("/api/setlist-items/{itemId}")
    public ResponseEntity<SetlistItemResponse> updateItem(
            @PathVariable("itemId") Long itemId,
            @Valid @RequestBody SetlistItemUpdateRequest request
    ) {
        return ResponseEntity.ok(setlistItemService.updateItem(itemId, request));
    }

    @DeleteMapping("/api/setlist-items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable("itemId") Long itemId) {
        setlistItemService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/setlists/{setlistId}/items/reorder")
    public ResponseEntity<Void> reorderItems(
            @PathVariable("setlistId") Long setlistId,
            @RequestBody SetlistReorderRequest request
    ) {
        setlistItemService.reorderItems(setlistId, request);
        return ResponseEntity.noContent().build();
    }
}
