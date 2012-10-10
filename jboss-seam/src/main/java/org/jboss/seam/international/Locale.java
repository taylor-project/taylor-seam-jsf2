package org.jboss.seam.international;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;

/**
 * Manager component for the current locale that is aware of the selected locale
 * 
 * @author Gavin King
 */
@Scope(ScopeType.EVENT)
@Name("org.jboss.seam.core.locale")
@Install(precedence = FRAMEWORK, dependencies = "org.jboss.seam.international.localeSelector")
//@BypassInterceptors
@AutoCreate
public class Locale extends org.jboss.seam.core.Locale {

	@Create
	@Override
	public void create() {
		if(Contexts.isSessionContextActive()){
			this.locale = LocaleSelector.instance().getLocale();
		}else{
			super.create();
		}
	}

}