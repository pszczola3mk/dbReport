package pl.pszczola3mk.dbReport.ui;

import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
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

	private static final Logger log = LoggerFactory.getLogger(DbConnectionPage.class);
	@Autowired
	private ReportCreatorBusiness business;

	@Override
	protected void init(VaadinRequest vaadinRequest) {
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
		button.addClickListener(clickEvent -> Try.ofCallable(new User(tfUrl.getValue(), tfUserName.getValue(), tfPassword.getValue(), this.business)).andThen(this::showSuccess).getOrElse(this::showError));
		form.addComponent(button);
		setContent(form);
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
