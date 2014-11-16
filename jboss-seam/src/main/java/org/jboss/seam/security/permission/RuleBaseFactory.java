package org.jboss.seam.security.permission;

import org.drools.RuleBase;
import org.drools.StatefulSession;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.drools.SeamGlobalResolver;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.management.JpaIdentityStore;

@Name("ruleBaseFactory")
@Scope(ScopeType.APPLICATION)
@BypassInterceptors
@Install(classDependencies="org.drools.RuleBase")
@Startup
public class RuleBaseFactory {

	private static RuleBase securityRuleBase;

	public static RuleBaseFactory instance() {
		return (RuleBaseFactory) Component.getInstance("ruleBaseFactory", true);
	}

	@Factory(value = "securityRuleBase", scope = ScopeType.STATELESS, autoCreate = true)
	public RuleBase getSecurityRuleBase() {
		if (securityRuleBase == null) {
			setSecurityRuleBase((RuleBase) Component.getInstance(
					RuleBasedPermissionResolver.RULES_COMPONENT_NAME, true));
		}

		return securityRuleBase;
	}

	public void setSecurityRuleBase(RuleBase securityRuleBase) {
		this.securityRuleBase = securityRuleBase;
	}

	@Factory(value = "securityContext", scope = ScopeType.EVENT, autoCreate = true)
	public StatefulSession getSecurityContext() {
		RuleBase sRules;
		StatefulSession sContext;
		Identity identity;
		
		sRules = (RuleBase) Component.getInstance("securityRules");
		if (sRules == null) {
			return null;
		}

		sContext = sRules.newStatefulSession(true);
		sContext.setGlobalResolver(new SeamGlobalResolver(sContext
				.getGlobalResolver()));
		
		identity = Identity.instance();
		if (identity != null) {
			sContext.insert(identity.getPrincipal());

			// If we were authenticated with the JpaIdentityStore, then insert the authenticated
			// UserAccount into the security context.
			if (Contexts.isEventContextActive() && Contexts.isSessionContextActive() &&
				       Contexts.getEventContext().isSet(JpaIdentityStore.AUTHENTICATED_USER)) {
				sContext.insert(Contexts.getEventContext().get(JpaIdentityStore.AUTHENTICATED_USER));
			}
		}

		return sContext;
	}
}
