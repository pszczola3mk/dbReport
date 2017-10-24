package pl.pszczola3mk.dbReport.ui;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.vaadin.data.HasValue;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import io.vavr.control.Try;
import pl.pszczola3mk.dbReport.business.LogAnalyzeBusiness;
import pl.pszczola3mk.dbReport.business.ReportCreatorBusiness;
import pl.pszczola3mk.dbReport.model.Entity;
import pl.pszczola3mk.dbReport.model.LogsMethod;

@SpringUI(path = "/ui/dbConnection")
public class DbConnectionPage extends UI {

	private static final Logger log = LoggerFactory.getLogger(DbConnectionPage.class);
	@Autowired
	private ReportCreatorBusiness business;
	@Autowired
	private LogAnalyzeBusiness logAnalyzeBusiness;
	private List<Path> uploadedJarPathList = new ArrayList<>();
	private List<String> uploadedFileNameList = new ArrayList<>();
	private Path uploadedLastFile;
	private String uploadedFileName;
	private String lastGeneratedScript;
	private String lastSqlScript;
	//
	private TextField tfUserName;
	private TextField tfPassword;
	private TextField tfUrl;
	private TextField tfComponent;
	private Label lbDateOfUpload;
	private Label lbFileList;
	//
	//
	private TextField tfSshUserName;
	private TextField tfSshPassword;
	private TextField tfSshHost;
	private TextField tfSshFile;
	//
	private TextArea taScript;
	private Label lbDateOfGeneration;
	//
	private TextArea tfHqlScript;
	private TextArea tfSqlScript;
	private Label lbDateOfTranslate;
	private List<Entity> entiesList = new ArrayList<>();
	private ComboBox<Entity> cbEntites = new ComboBox<>("Select entity", this.entiesList);
	private VerticalLayout vlColumnsList = new VerticalLayout();
	//
	private int currentPage = 0;
	private int pageSize = 10;
	private int pages = 0;
	private Label lbRowCount;
	private int rowCount = 0;
	private Button btPrev;
	private Button btNext;
	private List<List<Map<String, Object>>> parts;
	private Grid<Map<String, Object>> gridSqlResult = new Grid<>("SQL execution result");
	//
	private Grid<LogsMethod> gridLogsResult = new Grid<>("Logs");
	//
	@Value("${pszczola.datasource.username}")
	private String defaultUserName;
	@Value("${pszczola.datasource.password}")
	private String defaultPassword;
	@Value("${pszczola.datasource.url}")
	private String defaultUrl;

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		TabSheet tabsheet = new TabSheet();
		tabsheet.addTab(getConnectionTabForm(), "Connection check");
		tabsheet.addTab(getGenerationTabForm(), "Script generation");
		tabsheet.addTab(getHqlToSqlTabForm(), "HQL to SQL translate");
		tabsheet.addTab(getExecuteSqlForm(), "Execute SQL");
		tabsheet.addTab(getLogsForm(), "Logs");
		setContent(tabsheet);
	}

	private FormLayout getLogsForm() {
		FormLayout form = new FormLayout();
		//
		this.tfSshUserName = new TextField("Ssh user name", "");
		form.addComponent(this.tfSshUserName);
		//
		this.tfSshPassword = new TextField("Ssh password", "");
		form.addComponent(this.tfSshPassword);
		//
		this.tfSshHost = new TextField("Ssh server", "");
		form.addComponent(this.tfSshHost);
		//
		this.tfSshFile = new TextField("Ssh log file path", "");
		form.addComponent(this.tfSshFile);
		//
		Button btExecSsh = new Button("Analyze Log file");
		btExecSsh.addClickListener(clickEvent -> Try.of(this::downloadSshFile).andThen(this::showSuccess).recover(ex -> showError(ex)));
		form.addComponent(btExecSsh);
		form.addComponent(this.gridLogsResult);
		return form;
	}

	private FormLayout getExecuteSqlForm() {
		FormLayout form = new FormLayout();
		Button btExecSql = new Button("Execute SQL");
		btExecSql.addClickListener(clickEvent -> Try.of(this::executeSql).andThen(this::showSuccess).recover(ex -> showError(ex)));
		form.addComponent(btExecSql);
		form.addComponent(this.gridSqlResult);
		//
		this.lbRowCount = new Label("Liczba wierszy = 0");
		form.addComponent(this.lbRowCount);
		//
		HorizontalLayout vert = new HorizontalLayout();
		//
		btPrev = new Button("<");
		btPrev.addClickListener(clickEvent -> prevGridPage(clickEvent));
		vert.addComponent(btPrev);
		//
		btNext = new Button(">");
		btNext.addClickListener(clickEvent -> nextGridPage(clickEvent));
		vert.addComponent(btNext);
		//
		form.addComponent(vert);
		return form;
	}

	private GridLayout getHqlToSqlTabForm() {
		GridLayout grid = new GridLayout(2, 1);
		//
		VerticalLayout vlHqlAndSql = new VerticalLayout();
		VerticalLayout vlEntitesAndColumns = new VerticalLayout();
		grid.addComponent(vlHqlAndSql);
		grid.addComponent(vlEntitesAndColumns);
		//vlHqlAndSql
		this.tfHqlScript = new TextArea("HQL script", "select a from AddressTypeEntity a");
		this.tfHqlScript.setWordWrap(false);
		this.tfHqlScript.setWidth("800px");
		this.tfHqlScript.setHeight("400px");
		vlHqlAndSql.addComponent(tfHqlScript);
		this.tfSqlScript = new TextArea("SQL script");
		this.tfSqlScript.setWordWrap(false);
		this.tfSqlScript.setWidth("800px");
		this.tfSqlScript.setHeight("400px");
		vlHqlAndSql.addComponent(this.tfSqlScript);
		//
		this.lbDateOfTranslate = new Label("Date of translate:");
		vlHqlAndSql.addComponent(this.lbDateOfTranslate);
		//
		Button btTranslate = new Button("Translate HQL to SQL");
		btTranslate.addClickListener(clickEvent -> translateHqlToSql(clickEvent));
		vlHqlAndSql.addComponent(btTranslate);
		//
		// vlEntitesAndColumns
		//
		Button btRefresh = new Button("Refresh entites");
		btRefresh.addClickListener(clickEvent -> Try.of(this::refreshEntites).andThen(this::showSuccess).recover(ex -> showError(ex)));
		vlEntitesAndColumns.addComponent(btRefresh);
		//
		HorizontalLayout hlEntitesAndColumns = new HorizontalLayout();
		vlEntitesAndColumns.addComponent(hlEntitesAndColumns);
		this.cbEntites.setPlaceholder("No entity selected... ");
		this.cbEntites.setItemCaptionGenerator(Entity::getName);
		this.cbEntites.setEmptySelectionAllowed(false);
		this.cbEntites.setWidth(400, Unit.PIXELS);
		this.cbEntites.addValueChangeListener(event -> refreshColumns(event));
		hlEntitesAndColumns.addComponent(this.cbEntites);
		this.vlColumnsList.setCaption("Column list");
		hlEntitesAndColumns.addComponent(this.vlColumnsList);
		//
		return grid;
	}

	private void refreshColumns(HasValue.ValueChangeEvent<Entity> event) {
		String value = this.tfHqlScript.getValue();
		this.tfHqlScript.setValue(value + " " + event.getValue().getName());
		this.vlColumnsList.removeAllComponents();
		for (String s : event.getValue().getColumns()) {
			this.vlColumnsList.addComponent(new Label(s));
		}
	}

	private boolean refreshEntites() throws IOException, ClassNotFoundException {
		this.entiesList.clear();
		this.entiesList.addAll(this.business.searchEntites(this.uploadedJarPathList, this.tfComponent.getValue()));
		this.cbEntites.setItems(this.entiesList);
		return true;
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
		this.tfUserName = new TextField("User name", this.defaultUserName);
		this.tfUserName.setRequiredIndicatorVisible(true);
		form.addComponent(this.tfUserName);
		//
		this.tfPassword = new TextField("Password", this.defaultPassword);
		this.tfPassword.setRequiredIndicatorVisible(true);
		form.addComponent(this.tfPassword);
		//
		this.tfUrl = new TextField("Url", this.defaultUrl);
		this.tfUrl.setRequiredIndicatorVisible(true);
		form.addComponent(this.tfUrl);
		//
		this.tfComponent = new TextField("Component name", "scienceManager");
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
		this.lbFileList = new Label("File list:");
		form.addComponent(this.lbFileList);
		//
		Button btClearFileList = new Button("Clear file list");
		btClearFileList.addClickListener(clickEvent -> Try.of(this::clearFileList).andThen(this::showSuccess).recover(ex -> showError(ex)));
		form.addComponent(btClearFileList);
		//
		Button btCheckConnection = new Button("Check db connection");
		btCheckConnection.addClickListener(clickEvent -> Try.of(this::checkConnection).andThen(this::showSuccess).recover(ex -> showError(ex)));
		form.addComponent(btCheckConnection);
		return form;
	}

	private boolean clearFileList() {
		this.uploadedJarPathList.clear();
		this.uploadedFileNameList.clear();
		this.uploadedLastFile = null;
		this.uploadedFileName = null;
		this.lbDateOfUpload.setValue("Date of upload:");
		this.lbFileList.setValue("File list:");
		return true;
	}

	private boolean checkConnection() {
		business.checkConnection(this.tfUrl.getValue(), this.tfUserName.getValue(), this.tfPassword.getValue());
		return true;
	}

	private boolean downloadSshFile() throws Exception {
		List<LogsMethod> logsMethod = this.logAnalyzeBusiness.logAnalyze(this.tfSshUserName.getValue(), this.tfSshPassword.getValue(), this.tfSshHost.getValue(), this.tfSshFile.getValue());
		this.gridLogsResult.setItems(logsMethod);
		this.gridLogsResult.setHeight(500, Unit.PIXELS);
		this.gridLogsResult.setWidth(1600, Unit.PIXELS);
		this.gridLogsResult.removeAllColumns();
		this.gridLogsResult.addColumn(LogsMethod::getMethodName).setCaption("Name");
		this.gridLogsResult.addColumn(LogsMethod::getAvgDuration).setCaption("Avg");
		this.gridLogsResult.addColumn(LogsMethod::getInvokeCount).setCaption("Count");
		this.gridLogsResult.addColumn(LogsMethod::getMaxDuration).setCaption("Max");
		this.gridLogsResult.addColumn(LogsMethod::getMinDuration).setCaption("Min");
		this.gridLogsResult.addColumn(LogsMethod::getSummaryTime).setCaption("Sum");
		this.gridSqlResult.getDataProvider().refreshAll();
		return true;
	}

	private boolean executeSql() throws SQLException {
		List<Map<String, Object>> sqlResult = this.business.executeSql(this.tfSqlScript.getValue(), this.tfUrl.getValue(), this.tfUserName.getValue(), this.tfPassword.getValue());
		this.currentPage = 0;
		this.rowCount = sqlResult.size();
		this.parts = this.splitList(sqlResult, this.pageSize);
		this.lbRowCount.setValue("Liczba wierszy: " + rowCount + " strona 1 z " + this.parts.size());
		this.gridSqlResult.setItems(parts.get(0));
		this.gridSqlResult.setHeight(500, Unit.PIXELS);
		this.gridSqlResult.setWidth(1600, Unit.PIXELS);
		this.gridSqlResult.removeAllColumns();
		for (String col : sqlResult.get(0).keySet()) {
			this.gridSqlResult.addColumn(m -> m.get(col)).setCaption(col);
		}
		this.gridSqlResult.getDataProvider().refreshAll();
		return true;
	}

	private void nextGridPage(Button.ClickEvent clickEvent) {
		if ((currentPage + 1) == this.parts.size()) {
			return;
		}
		this.currentPage = this.currentPage + 1;
		this.lbRowCount.setValue("Liczba wierszy: " + this.rowCount + " strona " + (this.currentPage + 1) + " z " + this.parts.size());
		this.gridSqlResult.setItems(parts.get(this.currentPage));
		this.gridSqlResult.getDataProvider().refreshAll();
	}

	private void prevGridPage(Button.ClickEvent clickEvent) {
		if (currentPage == 0) {
			return;
		}
		this.currentPage = this.currentPage - 1;
		this.lbRowCount.setValue("Liczba wierszy: " + this.rowCount + " strona " + (this.currentPage + 1) + " z " + this.parts.size());
		this.gridSqlResult.setItems(parts.get(this.currentPage));
		this.gridSqlResult.getDataProvider().refreshAll();
	}

	private void translateHqlToSql(Button.ClickEvent clickEvent) {
		try {
			this.lastSqlScript = this.business.translateFromHqlToSql(this.tfHqlScript.getValue(), this.uploadedJarPathList, this.tfComponent.getValue(), this.tfUrl.getValue(),
					this.tfUserName.getValue(), this.tfPassword.getValue());
			this.tfSqlScript.setValue(lastSqlScript);
			//
			this.lbDateOfTranslate.setValue("Date of translate:" + getCurrentDateTime());
			Notification.show("Translate finished", Notification.Type.WARNING_MESSAGE);
		} catch (Exception ex) {
			Notification.show("Error during translate generation: " + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
		}
	}

	private void compareJarWithDb(Button.ClickEvent clickEvent) {
		try {
			this.lastGeneratedScript = this.business.generateScript(this.uploadedJarPathList, this.tfComponent.getValue(), this.tfUrl.getValue(), this.tfUserName.getValue(),
					this.tfPassword.getValue());
			this.taScript.setValue(this.lastGeneratedScript);
			//
			this.lbDateOfGeneration.setValue("Date of generation:" + getCurrentDateTime());
			Notification.show("Compare finished", Notification.Type.WARNING_MESSAGE);
		} catch (Exception ex) {
			Notification.show("Error during script generation: " + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
		}
	}

	private String getCurrentDateTime() {
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		return formatter.format(new Date());
	}

	private void sucessUpload(Object[] args) {
		boolean exists = Files.exists(uploadedLastFile);
		if (exists) {
			String label = "File list: ";
			for (String fn : uploadedFileNameList) {
				label = label + " " + fn + "; ";
			}
			this.lbFileList.setValue(label);
			Notification.show("File exists", Notification.Type.WARNING_MESSAGE);
			this.lbDateOfUpload.setValue("Date of last upload:" + getCurrentDateTime() + ", file: " + this.uploadedFileName);
		}
	}

	private OutputStream receiveUpload(String[] fileData) {
		try {
			this.uploadedFileName = fileData[0];
			this.uploadedFileNameList.add(uploadedFileName);
			uploadedLastFile = Files.createTempFile("scriptGenerator", "tmpjar");
			uploadedJarPathList.add(uploadedLastFile);
			return new FileOutputStream(uploadedLastFile.toFile());
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

	public <T> List<List<T>> splitList(List<T> list, final int partSize) {
		List<List<T>> parts = new ArrayList<List<T>>();
		final int N = list.size();
		for (int i = 0; i < N; i += partSize) {
			parts.add(new ArrayList<T>(list.subList(i, Math.min(N, i + partSize))));
		}
		return parts;
	}
}
