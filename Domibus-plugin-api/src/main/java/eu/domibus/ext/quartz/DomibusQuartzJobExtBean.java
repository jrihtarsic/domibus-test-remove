package eu.domibus.ext.quartz;

import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.DomainExtService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
public abstract class DomibusQuartzJobExtBean extends QuartzJobBean {

    @Autowired
    protected DomainExtService domainExtService;

    @Autowired
    protected DomainContextExtService domainContextExtService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            final DomainDTO currentDomain = getDomain(context);
            domainContextExtService.setCurrentDomain(currentDomain);
            executeJob(context, currentDomain);
        } finally {
            domainContextExtService.clearCurrentDomain();
        }
    }

    protected DomainDTO getDomain(JobExecutionContext context) throws JobExecutionException {
        try {
            final String schedulerName = context.getScheduler().getSchedulerName();
            return domainExtService.getDomainForScheduler(schedulerName);
        } catch (SchedulerException e) {
            throw new JobExecutionException("Could not get Quartz Scheduler", e);
        }
    }

    protected abstract void executeJob(final JobExecutionContext context, final DomainDTO domain) throws JobExecutionException;

}
