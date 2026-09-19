package com.aiinvoice.auth.context;

import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> TENANT = new ThreadLocal<>();
    public static void set(UUID id) { TENANT.set(id); }
    public static UUID get() { return TENANT.get(); }
    public static UUID getOrDefault() {
        UUID t = TENANT.get();
        return t != null ? t : UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
    public static void clear() { TENANT.remove(); }
}
