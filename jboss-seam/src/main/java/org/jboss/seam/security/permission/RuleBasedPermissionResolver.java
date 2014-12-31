package org.jboss.seam.security.permission;

import static org.jboss.seam.ScopeType.SESSION;
import static org.jboss.seam.annotations.Install.BUILT_IN;

import java.io.Serializable;
import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.drools.FactHandle;
import org.drools.RuleBase;
import org.drools.StatefulSession;
import org.drools.ClassObjectFilter;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.Seam;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.Role;

/**
 * A permission resolver that uses a Drools rule base to perform permission checks
 *  
 * @author Shane Bryzak
 */
@Name("org.jboss.seam.security.ruleBasedPermissionResolver")
@Scope(SESSION)
@BypassInterceptors
@Install(precedence=BUILT_IN, classDependencies="org.drools.WorkingMemory")
@Startup
public class RuleBasedPermissionResolver implements PermissionResolver, Serializable
{      
   public static final String RULES_COMPONENT_NAME = "securityRules";   
   
   private static final LogProvider log = Logging.getLogProvider(RuleBasedPermissionResolver.class);
   
   @Create
   public boolean create()
   {
      return getSecurityContext() != null;
   }
   
   protected void initSecurityContext()
   {
	// No longer need to do anything
   }
   
   /**
    * Performs a permission check for the specified name and action
    * 
    * @param target Object The target of the permission check
    * @param action String The action to be performed on the target
    * @return boolean True if the user has the specified permission
    */
   public boolean hasPermission(Object target, String action)
   {           
      StatefulSession securityContext = getSecurityContext();
      
      if (securityContext == null) return false;      
      
      List<FactHandle> handles = new ArrayList<FactHandle>();  

      PermissionCheck check;
      
      synchronized( securityContext )
      {
         if (!(target instanceof String) && !(target instanceof Class))
         {
            handles.add( securityContext.insert(target) );
         }
         else if (target instanceof Class)
         {
            String componentName = Seam.getComponentName((Class) target);
            target = componentName != null ? componentName : ((Class) target).getName();
         }
         
         check = new PermissionCheck(target, action);         
         
         try
         {
            synchronizeContext();
            
            handles.add( securityContext.insert(check) );
   
            securityContext.fireAllRules();
         }
         finally
         {
            for (FactHandle handle : handles)
            {
               securityContext.retract(handle);
            }
            securityContext.dispose();
         }
      }
      
      return check.isGranted();
   }
   
   public void filterSetByAction(Set<Object> targets, String action)
   {
      Iterator iter = targets.iterator();
      while (iter.hasNext())
      {
         Object target = iter.next();
         if (hasPermission(target, action)) iter.remove();
      }
   }
   
   public boolean checkConditionalRole(String roleName, Object target, String action)
   {      
      StatefulSession securityContext = getSecurityContext();
      if (securityContext == null) return false;
      
      RoleCheck roleCheck = new RoleCheck(roleName);
      
      List<FactHandle> handles = new ArrayList<FactHandle>();
      PermissionCheck check = new PermissionCheck(target, action);
      
      synchronized( securityContext )
      {
         if (!(target instanceof String) && !(target instanceof Class))
         {
            handles.add( securityContext.insert(target) );
         }
         else if (target instanceof Class)
         {
            String componentName = Seam.getComponentName((Class) target);
            target = componentName != null ? componentName : ((Class) target).getName();
         }
         
         try
         {
            handles.add( securityContext.insert(check));
            
            // Check if there are any additional requirements
            securityContext.fireAllRules();
            if (check.hasRequirements())
            {
               for (String requirement : check.getRequirements())
               {
                  Object value = Contexts.lookupInStatefulContexts(requirement);
                  if (value != null)
                  {
                     handles.add (securityContext.insert(value));
                  }
               }               
            }
            
            synchronizeContext();

            handles.add( securityContext.insert(roleCheck));
            handles.add( securityContext.insert(check));
            
            securityContext.fireAllRules();
         }
         finally
         {
            for (FactHandle handle : handles)
            {
               securityContext.retract(handle);
            }
         }
      }
      
      return roleCheck.isGranted();
   }
   
   @SuppressWarnings("unchecked")  
   @Observer(Identity.EVENT_LOGGED_OUT)
   public void unAuthenticate()
   {
	// No longer need to do anything
   }
   
   /**
    *  Synchronises the state of the security context with that of the subject
    */
   private void synchronizeContext()
   {
      Identity identity = Identity.instance();
      
      if (getSecurityContext() != null)
      {
         getSecurityContext().insert(identity.getPrincipal());
         
         for ( Group sg : identity.getSubject().getPrincipals(Group.class) )      
         {
            if ( Identity.ROLES_GROUP.equals( sg.getName() ) )
            {
               Enumeration e = sg.members();
               while (e.hasMoreElements())
               {
                  Principal role = (Principal) e.nextElement();
   
                  boolean found = false;
                  Iterator<Role> iter = (Iterator<Role>) getSecurityContext().iterateObjects(new ClassObjectFilter(Role.class)); 
                  while (iter.hasNext()) 
                  {
                     Role r = iter.next();
                     if (r.getName().equals(role.getName()))
                     {
                        found = true;
                        break;
                     }
                  }
                  
                  if (!found)
                  {
                     getSecurityContext().insert(new Role(role.getName()));
                  }
                  
               }
            }
         }    
         
         Iterator<Role> iter = (Iterator<Role>) getSecurityContext().iterateObjects(new ClassObjectFilter(Role.class)); 
         while (iter.hasNext()) 
         {
            Role r = iter.next();
            if (!identity.hasRole(r.getName()))
            {
               FactHandle fh = getSecurityContext().getFactHandle(r);
               getSecurityContext().retract(fh);
            }
         }
      }
   }
   
   
   public StatefulSession getSecurityContext()
   {
      return (StatefulSession) Component.getInstance("securityContext");
   }
   
   public void setSecurityContext(StatefulSession securityContext)
   {
	// No longer need to do anything
   }
   

   public RuleBase getSecurityRules()
   {
      return (RuleBase) Component.getInstance("securityRuleBase");
   }

   public void setSecurityRules(RuleBase securityRules)
   {
	   RuleBaseFactory.instance().setSecurityRuleBase(securityRules);
   }       
   
   public static RuleBasedPermissionResolver instance()
   {
      if ( !Contexts.isSessionContextActive() )
      {
         throw new IllegalStateException("No active session context");
      }

      RuleBasedPermissionResolver instance = (RuleBasedPermissionResolver) Component.getInstance(
            RuleBasedPermissionResolver.class, ScopeType.SESSION);

      if (instance == null)
      {
         throw new IllegalStateException("No RuleBasedPermissionResolver could be created");
      }

      return instance;
   }
   
   /**
    * Post-authentication event observer
    */
   @Observer(Identity.EVENT_POST_AUTHENTICATE)
   public void setUserAccountInSecurityContext()
   {
		// No longer need to do anything
   }
}
