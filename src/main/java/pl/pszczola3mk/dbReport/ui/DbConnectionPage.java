package pl.pszczola3mk.dbReport.ui;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import pl.pszczola3mk.dbReport.business.ReportCreatorBusiness;
import pl.pszczola3mk.dbReport.business.ScriptGeneratorBusiness;
import pl.pszczola3mk.dbReport.model.User;

@SpringUI(path = "/ui/dbConnection")
public class DbConnectionPage extends UI {

	private static final Logger log = LoggerFactory.getLogger(DbConnectionPage.class);
	@Autowired
	private ReportCreatorBusiness business;
	@Autowired
	private ScriptGeneratorBusiness generator;
	private Path uploadedJarPath;
	private User user;

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		addForm();
	}

	private void addForm() {
		Upload upload = new Upload("Upload it here", (fileName, miemType) -> receiveUpload(new String[] { fileName, miemType }));
		upload.setImmediateMode(false);
		upload.addSucceededListener(succeededEvent -> sucessUpload(succeededEvent));
		FormLayout form = new FormLayout();
		//
		TextField tfUserName = new TextField("User name");
		tfUserName.setRequiredIndicatorVisible(true);
		form.addComponent(tfUserName);
		//
		TextField tfPassword = new TextField("Password");
		tfPassword.setRequiredIndicatorVisible(true);
		form.addComponent(tfPassword);
		//
		TextField tfUrl = new TextField("Url");
		tfUrl.setRequiredIndicatorVisible(true);
		form.addComponent(tfUrl);
		//
		TextField tfComponent = new TextField("Component name");
		tfComponent.setRequiredIndicatorVisible(true);
		form.addComponent(tfComponent);
		//
		this.user = new User(tfUrl, tfUserName, tfPassword, this.business);
		Button button = new Button("Check db connection");
		button.addClickListener(clickEvent -> Try.ofCallable(this.user).andThen(this::showSuccess).recover(ex -> showError(ex)));
		form.addComponent(button);
		form.addComponent(upload);
		Button buttonCompare = new Button("Compare jar with db");
		buttonCompare.addClickListener(clickEvent -> compareJarWithDb(new Object[] { clickEvent, tfComponent.getValue() }));
		form.addComponent(buttonCompare);
		setContent(form);
	}

	private void compareJarWithDb(Object[] args) {
		boolean exists = Files.exists(uploadedJarPath);
		if (exists) {
			try {
				this.generator.generateScript(uploadedJarPath, (String) args[1], this.user);
				Notification.show("Compare finished", Notification.Type.WARNING_MESSAGE);
			} catch (Exception ex) {
				Notification.show("Error during script generation: " + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
		}
	}

	private void sucessUpload(Upload.SucceededEvent succeededEvent) {
		boolean exists = Files.exists(uploadedJarPath);
		if (exists) {
			Notification.show("File exists", Notification.Type.WARNING_MESSAGE);
		}
	}

	private OutputStream receiveUpload(String[] fileData) {
		try {
			uploadedJarPath = Files.createTempFile("scriptGenerator", "tmpjar");
			return new FileOutputStream(uploadedJarPath.toFile());
		} catch (Exception ex) {
			Notification.show("Error during upload: " + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
			return null;
		}
	}

	private boolean showError(Throwable ex) {
		Notification.show("Error: " + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
		return true;
	}

	private boolean showSuccess() {
		Notification.show("Success", Notification.Type.WARNING_MESSAGE);
		return true;
	}
}
