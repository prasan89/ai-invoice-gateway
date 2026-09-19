package com.aiinvoice.auth.context;

import com.aiinvoice.auth.entity.User;

public final class PrincipalContext {
    private static final ThreadLocal<User> PRINCIPAL = new ThreadLocal<>();
    public static void set(User user) { PRINCIPAL.set(user); }
    public static User get() { return PRINCIPAL.get(); }
    public static void clear() { PRINCIPAL.remove(); }
}
