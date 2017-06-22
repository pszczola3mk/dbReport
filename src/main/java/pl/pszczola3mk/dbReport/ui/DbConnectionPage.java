package pl.pszczola3mk.dbReport.ui;

import com.vaadin.event.UIEvents;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import pl.pszczola3mk.dbReport.business.ReportCreatorBusiness;
import pl.pszczola3mk.dbReport.model.User;

@SpringUI(path = "/ui/dbConnection")
public class DbConnectionPage extends UI {

	public enum PageState {
		first, second
	}
	private static final Logger log = LoggerFactory.getLogger(DbConnectionPage.class);
	@Autowired
	private ReportCreatorBusiness business;
	private PageState pageState = PageState.first;

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		setPollInterval(5000);
		addPollListener(new UIEvents.PollListener() {

			@Override
			public void poll(UIEvents.PollEvent pollEvent) {
				switch (pageState) {
				case first:
					addForm();
					break;
				case second:
					addBrowser();
					break;
				}
			}
		});
		addForm();
	}

	private void addForm() {
		FormLayout form = new FormLayout();
		TextField tfUserName = new TextField("User name");
		tfUserName.setRequiredIndicatorVisible(true);
		form.addComponent(tfUserName);
		TextField tfPassword = new TextField("Password");
		tfPassword.setRequiredIndicatorVisible(true);
		form.addComponent(tfPassword);
		TextField tfUrl = new TextField("Url");
		tfUrl.setRequiredIndicatorVisible(true);
		form.addComponent(tfUrl);
		Button button = new Button("Check db connection");
		button.addClickListener(
				clickEvent -> Try.ofCallable(new User(tfUrl.getValue(), tfUserName.getValue(), tfPassword.getValue(), this.business)).andThen(this::showSuccess).getOrElse(this::showError));
		form.addComponent(button);
		setContent(form);
		pageState = PageState.second;
	}

	private void addBrowser() {
		HorizontalLayout layout = new HorizontalLayout();
		BrowserFrame browser = new BrowserFrame("Browser", new ExternalResource("http://pg.edu.pl"));
		browser.setWidth("1200px");
		browser.setHeight("800px");
		layout.addComponent(browser);
		setContent(layout);
		pageState = PageState.first;
	}

	private boolean showError() {
		Notification.show("test");
		return true;
	}

	private boolean showSuccess() {
		Notification.show("ok");
		return true;
	}
}
