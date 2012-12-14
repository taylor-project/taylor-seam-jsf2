package org.jboss.seam.core;

import static org.jboss.seam.annotations.Install.BUILT_IN;

import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

/**
 * Manager component for the default locale.
 * 
 * @author miked
 */
@Scope(ScopeType.EVENT)
@Name("org.jboss.seam.core.defaultLocale")
@Install(precedence = BUILT_IN)
@AutoCreate
@BypassInterceptors
public class DefaultLocale {

	protected java.util.Locale locale;

	@Create
	public void create() {
		this.locale = java.util.Locale.getDefault();
	}

	public java.util.Locale getDefault() {
		return locale;
	}

	public void setDefault(java.util.Locale locale) {
		//Remove locale here so that the default you set has a chance of being used
		Contexts.getEventContext().remove("org.jboss.seam.core.locale");
		this.locale = locale;
	}
	
	public static DefaultLocale instance(){
		return (DefaultLocale) Component.getInstance(DefaultLocale.class);
	}
	
}