package org.jboss.seam.security.permission;

import org.drools.RuleBase;
import org.drools.StatefulSession;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.drools.SeamGlobalResolver;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.management.JpaIdentityStore;

@Name("securityContext")
@Scope(ScopeType.EVENT)
@AutoCreate
@BypassInterceptors
public class SecurityContext {

	private StatefulSession sContext;

	@Unwrap
	public StatefulSession getSecurityContext() {
		if (sContext == null) {
			RuleBase sRules;
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

				// If we were authenticated with the JpaIdentityStore, then
				// insert the authenticated
				// UserAccount into the security context.
				if (Contexts.isEventContextActive()
						&& Contexts.isSessionContextActive()
						&& Contexts.getEventContext().isSet(
								JpaIdentityStore.AUTHENTICATED_USER)) {
					sContext.insert(Contexts.getEventContext().get(
							JpaIdentityStore.AUTHENTICATED_USER));
				}
			}
		}
		return sContext;
	}

	@Destroy
	public void destroy() {
		if (sContext != null) {
			sContext.dispose();
		}
	}
}
