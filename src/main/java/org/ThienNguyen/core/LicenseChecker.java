package org.ThienNguyen.core; 

import org.bukkit.Bukkit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LicenseChecker {

    
    private static final LocalDate EXPIRY_DATE = LocalDate.of(2026, 3, 6);

    public static boolean isExpired() {
        LocalDate today = LocalDate.now();
        return today.isAfter(EXPIRY_DATE);
    }

    public static String getExpiryDateString() {
        return EXPIRY_DATE.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}