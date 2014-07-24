package org.jboss.seam.async;

import java.io.Serializable;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * Provides control over the Quartz Job.
 * 
 * @author Michael Yuan
 *
 */
public class QuartzTriggerHandle implements Serializable
{
   private static final long serialVersionUID = 1L;
   private final String triggerName;
   private final String schedulerName;
   
   // Hold a transient reference to the scheduler to allow control of the
   // scheduler outside of Seam contexts (useful in a testing context)
   private transient Scheduler scheduler;
     
   public QuartzTriggerHandle(String triggerName, String  schedulerName) 
   {
      this.triggerName = triggerName; 
      this.schedulerName = schedulerName;
   }

   public void cancel() throws SchedulerException
   {
      getScheduler().unscheduleJob(triggerName, null);
   }
   
   public void pause() throws SchedulerException
   {
      getScheduler().pauseTrigger(triggerName, null);  
   }
   
   public Trigger getTrigger() throws SchedulerException
   {
      return getScheduler().getTrigger(triggerName, null);
   }
   
   public void resume() throws SchedulerException
   {
      getScheduler().resumeTrigger(triggerName, null);
   }
   
   private Scheduler getScheduler()
   {
       if (scheduler == null)
       {
           scheduler = QuartzDispatcher.instance(schedulerName).getScheduler();
       }
       return scheduler;
   }

   @Override
   public String toString() {
      return "triggerName=" + triggerName
         + ", schedulerName=" + schedulerName;
   }
   
}
  