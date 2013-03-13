//$Id: JavaBeanInterceptor.java 9715 2008-12-04 08:00:42Z dan.j.allen $
package org.jboss.seam.intercept;

import java.lang.reflect.Method;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.jboss.seam.Component;
import org.jboss.seam.ComponentType;
import org.jboss.seam.annotations.ReadOnly;
import org.jboss.seam.annotations.intercept.InterceptorType;
import org.jboss.seam.core.Mutable;

/**
 * Controller interceptor for JavaBean components
 *
 * @author Gavin King
 * @author Marel Novotny
 */
public class JavaBeanInterceptor extends RootInterceptor
     implements MethodHandler
{
  private static final long serialVersionUID = -771725005103740533L;

  private final Object bean;
  private final Class beanClass;
  private transient boolean dirty;

  public JavaBeanInterceptor(Object bean, Component component)
  {
     super(InterceptorType.ANY);
     this.bean = bean;
     this.beanClass = component.getBeanClass();
     init(component);
  }

  public Object invoke(final Object proxy, final Method method, final Method proceed, final Object[] params) throws Throwable
  {

     if ( params!=null )
     {
        if ( params.length==0 )
        {
           String methodName = method.getName();
           if ( "finalize".equals(methodName) )
           {
              return proceed.invoke (proxy, params);
           }
           else if ( "writeReplace".equals(methodName) )
           {
              return this;
           }
           else if ( "clearDirty".equals(methodName) && !(bean instanceof Mutable) )
           {
              //clear and return the dirty flag
              boolean result = dirty;
              dirty = false;
              return result;
           }
           else if ( "getComponent".equals(methodName) )
           {
              return getComponent();
           }
        }
        else if ( params.length==1 && (params[0] instanceof HttpSessionEvent) )
        {
           String methodName = method.getName();
           if ( "sessionDidActivate".equals(methodName) )
           {
              callPostActivate();
              return (bean instanceof HttpSessionActivationListener) ? method.invoke(bean, params) : null;
           }
           else if ( "sessionWillPassivate".equals(methodName) )
           {
              callPrePassivate();
              return (bean instanceof HttpSessionActivationListener) ? method.invoke(bean, params) : null;
           }
        }
        else if ( params.length==1 && method.getName().equals("equals") && (params[0] == proxy) )
        {
           //make default equals() method return true when called on itself
           //by unwrapping the proxy
           //We don't let calling this equals make us dirty, as we assume it is without side effects
           //this assumption is required, as Mojarra 2.0 calls equals during SessionMap.put, see JBSEAM-4966
           if ( method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object.class)
           {
              return proceed.invoke(proxy, params);
           }

        } // JBSEAM-5066 - we need to skip the interceptInvocation in case of Object.equals(otherObject)
        else if (  params.length==1 && method.getName().equals("equals") && (params[0] != proxy))
        {
             if (proceed.getDeclaringClass().isAssignableFrom(bean.getClass()))
                    return proceed.invoke(bean, params);
             else {
                    return false;
             }
        }

     }

     if ( markDirty(method) )
     {
        //mark it dirty each time it gets called
        //this flag will be ignored if the bean
        //implements Mutable
        dirty = true;
     }

     Object result = interceptInvocation(method, params);
     return result==bean ? proxy : result;

  }

  private boolean markDirty(Method method)
  {
     return !getComponent().getBeanClass().isAnnotationPresent(ReadOnly.class) &&
           !method.isAnnotationPresent(ReadOnly.class);
  }

  public void postConstruct()
  {
     super.postConstruct(bean);
     callPostConstruct();
  }

  private void callPostConstruct()
  {
     final Component component = getComponent();
     if (!component.hasPostConstructMethod())
     {
        return;
     }

     InvocationContext context = new RootInvocationContext( bean, component.getPostConstructMethod(), new Object[0] )
     {
        @Override
        public Object proceed() throws Exception
        {
           component.callPostConstructMethod(bean);
           return null;
        }

     };
     invokeAndHandle(context, EventType.POST_CONSTRUCT);
  }

  private void callPrePassivate()
  {
     final Component component = getComponent();
     if (!component.hasPrePassivateMethod())
     {
        return;
     }

     InvocationContext context = new RootInvocationContext( bean, component.getPrePassivateMethod(), new Object[0] )
     {
        @Override
        public Object proceed() throws Exception
        {
           component.callPrePassivateMethod(bean);
           return null;
        }

     };
     invokeAndHandle(context, EventType.PRE_PASSIVATE);
  }

  private void callPostActivate()
  {
     final Component component = getComponent();
     if (!component.hasPostActivateMethod())
     {
        return;
     }

     RootInvocationContext context = new RootInvocationContext(bean, component.getPostActivateMethod(), new Object[0])
     {
        @Override
        public Object proceed() throws Exception
        {
           component.callPostActivateMethod(bean);
           return null;
        }

     };
     invokeAndHandle(context, EventType.POST_ACTIVATE);
  }

  private Object interceptInvocation(final Method method, final Object[] params) throws Exception
  {
     return invoke( new RootInvocationContext(bean, method, params), EventType.AROUND_INVOKE );
  }

  // TODO: copy/paste from ClientSide interceptor
  Object readResolve()
  {
     Component comp = null;
     try
     {
        comp = getComponent();
     }
     catch (IllegalStateException ise) {
        //this can occur when tomcat deserializes persistent sessions
     }

     try
     {
        if (comp==null)
        {
           ProxyObject proxy = Component.createProxyFactory(
                 ComponentType.JAVA_BEAN,
                 beanClass,
                 Component.getBusinessInterfaces(beanClass)
              ).newInstance();
           proxy.setHandler(this);
           return proxy;
        }
        else
        {
           return comp.wrap(bean, this);
        }
     }
     catch (Exception e)
     {
        throw new RuntimeException(e);
     }
  }

}