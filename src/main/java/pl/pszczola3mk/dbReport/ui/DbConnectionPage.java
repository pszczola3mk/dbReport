package pl.pszczola3mk.dbReport.ui;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import io.vavr.control.Try;
import pl.pszczola3mk.dbReport.business.ReportCreatorBusiness;

@SpringUI(path = "/ui/dbConnection")
public class DbConnectionPage extends UI {

	private static final Logger log = LoggerFactory.getLogger(DbConnectionPage.class);
	@Autowired
	private ReportCreatorBusiness business;
	private Path uploadedJarPath;
	private String uploadedFileName;
	private String lastGeneratedScript;
	private String lastSqlScript;
	//
	private TextField tfUserName;
	private TextField tfPassword;
	private TextField tfUrl;
	private TextField tfComponent;
	private Label lbDateOfUpload;
	//
	private TextArea taScript;
	private Label lbDateOfGeneration;
	//
	private TextArea tfHqlScript;
	private TextArea tfSqlScript;
	private Label lbDateOfTranslate;
	//
	private Grid<Map<String, Object>> gridSqlResult = new Grid<>("SQL execution result");

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		TabSheet tabsheet = new TabSheet();
		tabsheet.addTab(getConnectionTabForm(), "Connection check");
		tabsheet.addTab(getGenerationTabForm(), "Script generation");
		tabsheet.addTab(getHqlToSqlTabForm(), "HQL to SQL translate");
		tabsheet.addTab(getExecuteSqlForm(), "Execute SQL");
		setContent(tabsheet);
	}

	private FormLayout getExecuteSqlForm() {
		FormLayout form = new FormLayout();
		Button btExecSql = new Button("Execute SQL");
		btExecSql.addClickListener(clickEvent -> Try.of(this::executeSql).andThen(this::showSuccess).recover(ex -> showError(ex)));
		form.addComponent(btExecSql);
		form.addComponent(this.gridSqlResult);
		return form;
	}

	private FormLayout getHqlToSqlTabForm() {
		FormLayout form = new FormLayout();
		this.tfHqlScript = new TextArea("HQL script", "select a from AddressTypeEntity a");
		this.tfHqlScript.setWordWrap(false);
		this.tfHqlScript.setWidth("800px");
		this.tfHqlScript.setHeight("400px");
		form.addComponent(this.tfHqlScript);
		//
		this.tfSqlScript = new TextArea("SQL script");
		this.tfSqlScript.setWordWrap(false);
		this.tfSqlScript.setWidth("800px");
		this.tfSqlScript.setHeight("400px");
		form.addComponent(this.tfSqlScript);
		//
		this.lbDateOfTranslate = new Label("Date of translate:");
		form.addComponent(this.lbDateOfTranslate);
		//
		Button btTranslate = new Button("Translate HQL to SQL");
		btTranslate.addClickListener(clickEvent -> translateHqlToSql(clickEvent));
		form.addComponent(btTranslate);
		//
		return form;
	}

	private FormLayout getGenerationTabForm() {
		FormLayout form = new FormLayout();
		this.taScript = new TextArea("Generated script");
		this.taScript.setWordWrap(false);
		this.taScript.setWidth("800px");
		this.taScript.setHeight("400px");
		form.addComponent(this.taScript);
		//
		this.lbDateOfGeneration = new Label("Date of generation:");
		form.addComponent(this.lbDateOfGeneration);
		//
		Button buttonCompare = new Button("Compare jar with db");
		buttonCompare.addClickListener(clickEvent -> compareJarWithDb(clickEvent));
		form.addComponent(buttonCompare);
		return form;
	}

	private FormLayout getConnectionTabForm() {
		FormLayout form = new FormLayout();
		//
		this.tfUserName = new TextField("User name", "seodbmigrationmanager");
		this.tfUserName.setRequiredIndicatorVisible(true);
		form.addComponent(this.tfUserName);
		//
		this.tfPassword = new TextField("Password", "dbmigration");
		this.tfPassword.setRequiredIndicatorVisible(true);
		form.addComponent(this.tfPassword);
		//
		this.tfUrl = new TextField("Url", "pasat3.dev.pg.gda.pl:5432/seoprod");
		this.tfUrl.setRequiredIndicatorVisible(true);
		form.addComponent(this.tfUrl);
		//
		this.tfComponent = new TextField("Component name", "dictManager");
		this.tfComponent.setRequiredIndicatorVisible(true);
		form.addComponent(this.tfComponent);
		//
		Upload upload = new Upload("Component JAR", (fileName, miemType) -> receiveUpload(new String[] { fileName, miemType }));
		upload.setImmediateMode(false);
		upload.addSucceededListener(succeededEvent -> sucessUpload(new Object[] { succeededEvent }));
		form.addComponent(upload);
		//
		this.lbDateOfUpload = new Label("Date of upload:");
		form.addComponent(this.lbDateOfUpload);
		//
		Button btCheckConnection = new Button("Check db connection");
		btCheckConnection.addClickListener(clickEvent -> Try.of(this::checkConnection).andThen(this::showSuccess).recover(ex -> showError(ex)));
		form.addComponent(btCheckConnection);
		return form;
	}

	private boolean checkConnection() {
		business.checkConnection(this.tfUrl.getValue(), this.tfUserName.getValue(), this.tfPassword.getValue());
		return true;
	}

	private boolean executeSql() throws SQLException {
		List<Map<String, Object>> sqlResult = this.business.executeSql(this.tfSqlScript.getValue(), this.tfUrl.getValue(), this.tfUserName.getValue(), this.tfPassword.getValue());
		this.gridSqlResult.setItems(sqlResult);
		this.gridSqlResult.setHeight(800, Unit.PIXELS);
		this.gridSqlResult.setWidth(1600, Unit.PIXELS);
		this.gridSqlResult.removeAllColumns();
		for (String col : sqlResult.get(0).keySet()) {
			this.gridSqlResult.addColumn(m -> m.get(col)).setCaption(col);
		}
		this.gridSqlResult.getDataProvider().refreshAll();
		return true;
	}

	private void translateHqlToSql(Button.ClickEvent clickEvent) {
		boolean exists = Files.exists(uploadedJarPath);
		if (exists) {
			try {
				this.lastSqlScript = this.business.translateFromHqlToSql(this.tfHqlScript.getValue(), this.uploadedJarPath, this.tfComponent.getValue(), this.tfUrl.getValue(),
						this.tfUserName.getValue(), this.tfPassword.getValue());
				this.tfSqlScript.setValue(lastSqlScript);
				//
				this.lbDateOfTranslate.setValue("Date of translate:" + getCurrentDateTime());
				Notification.show("Translate finished", Notification.Type.WARNING_MESSAGE);
			} catch (Exception ex) {
				Notification.show("Error during translate generation: " + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
		}
	}

	private void compareJarWithDb(Button.ClickEvent clickEvent) {
		boolean exists = Files.exists(uploadedJarPath);
		if (exists) {
			try {
				this.lastGeneratedScript = this.business.generateScript(this.uploadedJarPath, this.tfComponent.getValue(), this.tfUrl.getValue(), this.tfUserName.getValue(),
						this.tfPassword.getValue());
				this.taScript.setValue(this.lastGeneratedScript);
				//
				this.lbDateOfGeneration.setValue("Date of generation:" + getCurrentDateTime());
				Notification.show("Compare finished", Notification.Type.WARNING_MESSAGE);
			} catch (Exception ex) {
				Notification.show("Error during script generation: " + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
		}
	}

	private String getCurrentDateTime() {
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		return formatter.format(new Date());
	}

	private void sucessUpload(Object[] args) {
		boolean exists = Files.exists(uploadedJarPath);
		if (exists) {
			Notification.show("File exists", Notification.Type.WARNING_MESSAGE);
			this.lbDateOfUpload.setValue("Date of upload:" + getCurrentDateTime() + ", file: " + this.uploadedFileName);
		}
	}

	private OutputStream receiveUpload(String[] fileData) {
		try {
			this.uploadedFileName = fileData[0];
			this.uploadedJarPath = Files.createTempFile("scriptGenerator", "tmpjar");
			return new FileOutputStream(this.uploadedJarPath.toFile());
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
