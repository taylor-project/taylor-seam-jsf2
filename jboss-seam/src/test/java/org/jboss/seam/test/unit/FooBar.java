package org.jboss.seam.test.unit;

import javax.inject.Inject;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("fooBar")
@Scope(ScopeType.APPLICATION)
public class FooBar
{
   @Inject Foo foo; 
   
   public Foo delayedGetFoo(InvocationControl invocationControl)
   {
      System.out.println("enter: " + invocationControl.getName() + " " + foo);
      invocationControl.init();
      invocationControl.markStarted();
      System.out.println("exit: " + invocationControl.getName() + " " + foo);
      return foo;
   }
}
