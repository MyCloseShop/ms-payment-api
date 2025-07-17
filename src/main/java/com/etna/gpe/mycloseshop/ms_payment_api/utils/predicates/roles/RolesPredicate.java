package com.etna.gpe.mycloseshop.ms_payment_api.utils.predicates.roles;

import com.etna.gpe.mycloseshop.common_api.ms_login.enums.Roles;

import java.util.function.Predicate;

public class RolesPredicate {

    private RolesPredicate() {
        throw new IllegalStateException("Utility class");
    }

    public static Predicate<Roles> getRolesClientUserManagerPredicate() {
        return role -> role == Roles.ROLE_USER || role == Roles.ROLE_MANAGER
                || role == Roles.ROLE_CLIENT;
    }
}
