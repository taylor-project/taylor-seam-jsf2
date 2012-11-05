package org.jboss.seam.web;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

import javax.servlet.ServletRequest;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

/**
 * Manager component for the current locale that is aware of the HTTP request
 * locale
 * 
 * @author Gavin King
 */
@Scope(ScopeType.EVENT)
@Name("org.jboss.seam.core.locale")
@Install(precedence = FRAMEWORK - 1)
@BypassInterceptors
@AutoCreate
public class Locale extends org.jboss.seam.core.Locale {

	@Create
	@Override
	public void create() {
		ServletContexts servletContexts = ServletContexts.getInstance();
		ServletRequest request = servletContexts == null ? null
				: servletContexts.getRequest();
		if(request == null){
			super.create();
		}else{
			this.locale = request.getLocale();
		}
	}

}