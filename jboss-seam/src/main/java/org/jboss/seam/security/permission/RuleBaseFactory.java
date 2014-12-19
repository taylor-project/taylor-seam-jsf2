package org.jboss.seam.security.permission;

import org.drools.RuleBase;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

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
}
