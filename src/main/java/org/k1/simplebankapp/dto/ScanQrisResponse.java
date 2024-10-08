package org.k1.simplebankapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScanQrisResponse {
    private String senderName;
    private Double amount;
    private String qrCode;
}
