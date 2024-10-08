package org.k1.simplebankapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.k1.simplebankapp.dto.BaseResponse;
import org.k1.simplebankapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = "Account")
@RestController
@RequestMapping("v1/accounts")
public class AccountController {
    @Autowired
    private AccountService accountService;

    @GetMapping()
    @Operation(summary = "Get account details",
            description = "Endpoint to get account details",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> getAccount(Principal principal) {
        return ResponseEntity.ok(BaseResponse.success(accountService.findAllByUser(principal), "Success get account"));
    }
}
