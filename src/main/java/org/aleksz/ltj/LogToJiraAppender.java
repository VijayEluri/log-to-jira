package org.aleksz.ltj;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.aleksz.ltj.soap.JiraSoapService;
import org.aleksz.ltj.soap.JiraSoapServiceServiceLocator;
import org.aleksz.ltj.soap.RemoteAuthenticationException;
import org.aleksz.ltj.soap.RemoteIssue;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;

public class LogToJiraAppender extends AppenderSkeleton {

	private Config config = new Config();
	private AppenderService service;
	private JiraSoapServiceServiceLocator jiraSoapServiceServiceLocator;
	private JiraSoapService jiraSoapService;

	@Override
	protected void append(LoggingEvent loggingEvent) {
		try {

			String token = getJiraService().login(config.getUsername(), config.getPassword());
			RemoteIssue issue = getService().createIssue(loggingEvent);
			if (!getService().duplicateExists(issue, token)) {
				getJiraService().createIssue(token, issue);
			}
			getJiraService().logout(token);

		} catch (RemoteAuthenticationException e) {
			errorHandler.error("JIRA auth failed", e, ErrorCode.GENERIC_FAILURE, loggingEvent);
		} catch (RemoteException e) {
			errorHandler.error("JIRA problem", e, ErrorCode.GENERIC_FAILURE, loggingEvent);
		}
	}

	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	public void setUrl(String url) {
		jiraSoapServiceServiceLocator = new JiraSoapServiceServiceLocator();
		try {
			this.jiraSoapService = jiraSoapServiceServiceLocator.getJirasoapserviceV2(new URL(url));
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		} catch (ServiceException e) {
			errorHandler.error("JIRA connection problem", e, ErrorCode.GENERIC_FAILURE);
		}
	}

	protected JiraSoapService getJiraService() {
		return jiraSoapService;
	}

	protected AppenderService getService() {

		if (service != null) {
			return service;
		}

		service = new AppenderServiceImpl(config, jiraSoapService);

		return service;
	}

	public void setUsername(String username) {
		config.setUsername(username);
	}

	public void setPassword(String password) {
		config.setPassword(password);
	}

	public void setProject(String project) {
		config.setProject(project);
	}

	public void setIssueTypeId(String issueTypeId) {
		config.setIssueTypeId(issueTypeId);
	}
}
