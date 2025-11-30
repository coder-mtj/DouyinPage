package com.mtj.DouyinBackend.star;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stars")
public class StarUserController {

    private final StarUserService service;

    public StarUserController(StarUserService service) {
        this.service = service;
    }

    @GetMapping
    public StarUserPageResponse list(@RequestParam(name = "page", defaultValue = "0") int page,
                                     @RequestParam(name = "size", defaultValue = "10") int size) {
        return service.getPage(page, size);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") long id,
                                       @RequestBody UpdateStarUserRequestBody body) {
        service.updateUser(id, body);
        return ResponseEntity.noContent().build();
    }
}
