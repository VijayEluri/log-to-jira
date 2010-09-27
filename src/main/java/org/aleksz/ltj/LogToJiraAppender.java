package org.aleksz.ltj;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class LogToJiraAppender extends AppenderSkeleton {

	private Config config = new Config();
	private BugReport bugReport = new BugReport(config);
	private JiraSoapServiceServiceLocator jiraSoapServiceServiceLocator;
	private JiraSoapService jiraSoapService;

	public LogToJiraAppender() {
		jiraSoapServiceServiceLocator = new JiraSoapServiceServiceLocator();
	}

	@Override
	protected void append(LoggingEvent loggingEvent) {
		try {
			String token = jiraSoapService.login(config.getUsername(), config.getPassword());
			jiraSoapService.createIssue(token, bugReport.getIssue(loggingEvent));
			jiraSoapService.logout(token);
		} catch (RemoteAuthenticationException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
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
		try {
			this.jiraSoapService = jiraSoapServiceServiceLocator.getJirasoapserviceV2(new URL(url));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (ServiceException e) {
			e.printStackTrace();
		}
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
