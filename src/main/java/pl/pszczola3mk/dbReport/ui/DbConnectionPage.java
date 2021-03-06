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
import com.vaadin.ui.Component;
import org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.components.grid.HeaderRow;
import com.vaadin.ui.themes.ValoTheme;
import io.vavr.control.Try;
import pl.pszczola3mk.dbReport.business.LogAnalyzeBusiness;
import pl.pszczola3mk.dbReport.business.ReportCreatorBusiness;
import pl.pszczola3mk.dbReport.model.Entity;
import pl.pszczola3mk.dbReport.model.ExceptionLog;
import pl.pszczola3mk.dbReport.model.FileForCompare;
import pl.pszczola3mk.dbReport.model.LogsMethod;
import pl.pszczola3mk.dbReport.model.MethodInvoke;

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
	private PasswordField tfPassword;
	private TextField tfUrl;
	private TextField tfComponent;
	private Label lbDateOfUpload;
	private Label lbFileList;
	//
	//
	private TextField tfSshUserName;
	private PasswordField tfSshPassword;
	private TextField tfSshHost;
	private TextField tfSshFile;
	private TextField tfSshMethodName;
	private TextField tfPersonNumber;
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
	private List<String> methodsToCompare = new ArrayList<>();
	private Label lbMethodsForCompare;
	private VerticalLayout logsForm;
	private Grid<FileForCompare> gridLogsCompare = new Grid<>("Logs Compare");
	private Grid<LogsMethod> gridTrackPersonSummary = new Grid<>("Track Summary");
	private Grid<MethodInvoke> gridTrackPerson = new Grid<>("Track");
	private Grid<ExceptionLog> gridExceptions = new Grid<>("Exceptions");
	//
	private TextField tfFilteringMessageField;
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

	private VerticalLayout getLogsForm() {
		this.logsForm = new VerticalLayout();
		HorizontalLayout hlUser = new HorizontalLayout();
		HorizontalLayout hlServer = new HorizontalLayout();
		//
		this.tfSshUserName = new TextField("Ssh user name", "");
		hlUser.addComponent(this.tfSshUserName);
		//
		this.tfSshPassword = new PasswordField("Ssh password", "");
		hlUser.addComponent(this.tfSshPassword);
		//
		this.tfSshHost = new TextField("Ssh server", "");
		hlServer.addComponent(this.tfSshHost);
		//
		this.tfSshFile = new TextField("Ssh log file path", "");
		hlServer.addComponent(this.tfSshFile);
		//
		HorizontalLayout hl = new HorizontalLayout();
		Button btDownloadSsh = new Button("Download Log file");
		btDownloadSsh.addClickListener(clickEvent -> Try.of(() -> downloadSshFile(true)).andThen(this::showSuccess).recover(ex -> showError(ex)));
		hl.addComponent(btDownloadSsh);
		Button btExecSsh = new Button("Analyze Log file");
		btExecSsh.addClickListener(clickEvent -> Try.of(() -> downloadSshFile(false)).andThen(this::showSuccess).recover(ex -> showError(ex)));
		hl.addComponent(btExecSsh);
		//
		HorizontalLayout hl2 = new HorizontalLayout();
		this.lbMethodsForCompare = new Label("Methods for compare " + this.methodsToCompare.toString());
		hl2.addComponent(this.lbMethodsForCompare);
		Button btCompare = new Button("Compare");
		btCompare.addClickListener(clickEvent -> Try.of(() -> compareFiles()).andThen(this::showSuccess).recover(ex -> showError(ex)));
		hl2.addComponent(btCompare);
		Button btCompareClear = new Button("Clear");
		btCompareClear.addClickListener(clickEvent -> Try.of(() -> clearCompareFiles()).andThen(this::showSuccess).recover(ex -> showError(ex)));
		hl2.addComponent(btCompareClear);
		//
		HorizontalLayout hl3 = new HorizontalLayout();
		this.tfPersonNumber = new TextField("Person number", "");
		hl3.addComponent(this.tfPersonNumber);
		Button btTrackSummary = new Button("Track Summary");
		btTrackSummary.addClickListener(clickEvent -> Try.of(() -> trackPersonSummary()).andThen(this::showSuccess).recover(ex -> showError(ex)));
		hl3.addComponent(btTrackSummary);
		//
		Button btTrack = new Button("Track");
		btTrack.addClickListener(clickEvent -> Try.of(() -> trackPerson()).andThen(this::showSuccess).recover(ex -> showError(ex)));
		hl3.addComponent(btTrack);
		//
		this.logsForm.addComponent(hlUser);
		this.logsForm.addComponent(hlServer);
		this.logsForm.addComponent(hl2);
		this.logsForm.addComponent(hl3);
		this.logsForm.addComponent(hl);
		TabSheet tabsheet = new TabSheet();
		FormLayout fl1 = new FormLayout();
		fl1.addComponent(this.gridLogsResult);
		//
		FormLayout fl2 = new FormLayout();
		fl2.addComponent(this.gridLogsCompare);
		//
		FormLayout fl3 = new FormLayout();
		fl3.addComponent(this.gridTrackPersonSummary);
		//
		FormLayout fl4 = new FormLayout();
		fl4.addComponent(this.gridTrackPerson);
		//
		FormLayout fl5 = new FormLayout();
		fl5.addComponent(createGridEx());
		//
		tabsheet.addTab(fl1, "Logs stats");
		tabsheet.addTab(fl2, "Logs compare");
		tabsheet.addTab(fl3, "Track summary");
		tabsheet.addTab(fl4, "Person track");
		tabsheet.addTab(fl5, "Exceptions");
		this.logsForm.addComponent(tabsheet);
		return this.logsForm;
	}

	private Component createGridEx() {
		List<Grid.Column<ExceptionLog, ?>> columns = this.gridExceptions.getColumns();
		for (Grid.Column<ExceptionLog, ?> c : columns) {
			this.gridExceptions.removeColumn(c);
		}
		HeaderRow filterRow = this.gridExceptions.addHeaderRowAt(1);
		this.gridExceptions.setHeight(500, Unit.PIXELS);
		this.gridExceptions.setWidth(1600, Unit.PIXELS);
		this.gridExceptions.addColumn(ExceptionLog::getShortMessage).setCaption("Message").setId("Message");
		this.gridExceptions.addColumn(ExceptionLog::getInvokeCount).setCaption("Invoke count");
		this.gridExceptions.addColumn(ExceptionLog::getLevel).setCaption("Level").setId("Level");
		this.gridExceptions.addColumn(ExceptionLog::getFirstInvokeDate).setCaption("First date");
		tfFilteringMessageField = getColumnFilterField();
		filterRow.getCell("Message").setComponent(tfFilteringMessageField);
		this.gridExceptions.addItemClickListener(clickEvent -> showDataEx(clickEvent));
		return this.gridExceptions;
	}

	private boolean showExceptions() {
		List<ExceptionLog> exLogs = this.logAnalyzeBusiness.searchAllExceptions();

		ListDataProvider<ExceptionLog> dataProvider = new ListDataProvider<>(exLogs);
		this.gridExceptions.setDataProvider(dataProvider);
		tfFilteringMessageField.addValueChangeListener(event -> {
			dataProvider.setFilter(ExceptionLog::getShortMessage, m -> {
				if (m == null) {
					return false;
				}
				if (m.equals("")) {
					return true;
				}
				String companyLower = m.toLowerCase();
				String filterLower = event.getValue().toLowerCase();
				return companyLower.contains(filterLower);
			});
		});
		if (tfFilteringMessageField.getValue() == null || tfFilteringMessageField.getValue().equals("")) {
			dataProvider.clearFilters();
		}
		this.gridExceptions.setCaption("Logs [" + exLogs.size() + "]");
		this.gridExceptions.getDataProvider().refreshAll();
		return true;
	}

	private TextField getColumnFilterField() {
		TextField filter = new TextField();
		filter.setWidth("100%");
		filter.addStyleName(ValoTheme.TEXTFIELD_TINY);
		filter.setPlaceholder("Filter");
		return filter;
	}

	private void showDataEx(Grid.ItemClick<ExceptionLog> clickEvent) {
		String message = clickEvent.getItem().getMessage();
		Window window = new Window("Exception message");
		window.setWidth(800, Unit.PIXELS);
		String wrap = WordUtils.wrap(message, 100, "<BR/>", false);
		Label label = new Label(wrap, ContentMode.HTML);
		window.setContent(label);
		this.getUI().addWindow(window);
	}

	private void addToCompare(Grid.ItemClick<LogsMethod> clickEvent) {
		String beanName = clickEvent.getItem().getBeanName();
		String methodName = clickEvent.getItem().getMethodName();
		this.methodsToCompare.clear();
		this.methodsToCompare.add(methodName);
		this.lbMethodsForCompare.setValue("Methods for compare " + methodName);
	}

	//
	private boolean trackPersonSummary() {
		List<LogsMethod> trackPersonInvokes = this.logAnalyzeBusiness.trackPersonInvokes(this.tfPersonNumber.getValue());
		List<Grid.Column<LogsMethod, ?>> columns = this.gridTrackPersonSummary.getColumns();
		for (Grid.Column<LogsMethod, ?> c : columns) {
			this.gridTrackPersonSummary.removeColumn(c);
		}
		this.gridTrackPersonSummary.setCaption("Track Summary [" + trackPersonInvokes.size() + "]");
		this.gridTrackPersonSummary.setItems(trackPersonInvokes);
		this.gridTrackPersonSummary.setHeight(500, Unit.PIXELS);
		this.gridTrackPersonSummary.setWidth(1600, Unit.PIXELS);
		this.gridTrackPersonSummary.addColumn(LogsMethod::getBeanName).setCaption("Bean");
		this.gridTrackPersonSummary.addColumn(LogsMethod::getMethodName).setCaption("Method");
		this.gridTrackPersonSummary.addColumn(LogsMethod::getAvgDuration).setCaption("Avg");
		this.gridTrackPersonSummary.addColumn(LogsMethod::getInvokeCount).setCaption("Count");
		this.gridTrackPersonSummary.addColumn(LogsMethod::getMaxDuration).setCaption("Max");
		this.gridTrackPersonSummary.addColumn(LogsMethod::getMinDuration).setCaption("Min");
		this.gridTrackPersonSummary.addColumn(LogsMethod::getSummaryTime).setCaption("Sum");
		this.gridTrackPersonSummary.getDataProvider().refreshAll();
		return true;
	}

	private boolean trackPerson() {
		List<LogsMethod> trackPersonInvokes = this.logAnalyzeBusiness.trackPersonInvokes(this.tfPersonNumber.getValue());
		List<MethodInvoke> miList = new ArrayList<>();
		for (LogsMethod lm : trackPersonInvokes) {
			miList.addAll(lm.getInvokeList());
		}
		List<Grid.Column<MethodInvoke, ?>> columns = this.gridTrackPerson.getColumns();
		for (Grid.Column<MethodInvoke, ?> c : columns) {
			this.gridTrackPerson.removeColumn(c);
		}
		this.gridTrackPerson.setCaption("Track [" + miList.size() + "]");
		this.gridTrackPerson.setItems(miList);
		this.gridTrackPerson.setHeight(500, Unit.PIXELS);
		this.gridTrackPerson.setWidth(1600, Unit.PIXELS);
		this.gridTrackPerson.addColumn(MethodInvoke::getMethodName).setCaption("Method");
		this.gridTrackPerson.addColumn(MethodInvoke::getDurationInMilis).setCaption("Duration");
		this.gridTrackPerson.addColumn(MethodInvoke::getInvokeDateFormated).setCaption("InvokeDate");
		this.gridTrackPerson.getDataProvider().refreshAll();
		return true;
	}

	private boolean clearCompareFiles() {
		this.tfSshMethodName.setValue(null);
		methodsToCompare.clear();
		this.lbMethodsForCompare.setValue("Methods for compare");
		return true;
	}

	private boolean compareFiles() {
		List<FileForCompare> compare = this.logAnalyzeBusiness.compare(this.methodsToCompare.get(0));
		List<Grid.Column<FileForCompare, ?>> columns = this.gridLogsCompare.getColumns();
		for (Grid.Column<FileForCompare, ?> c : columns) {
			this.gridLogsCompare.removeColumn(c);
		}
		this.gridLogsCompare.setCaption("Logs Compare [" + compare.size() + "]");
		this.gridLogsCompare.setItems(compare);
		this.gridLogsCompare.setHeight(500, Unit.PIXELS);
		this.gridLogsCompare.setWidth(1600, Unit.PIXELS);
		this.gridLogsCompare.addColumn(FileForCompare::getFileName).setCaption("FileName");
		this.gridLogsCompare.addColumn(FileForCompare::getMethodName).setCaption("Name");
		this.gridLogsCompare.addColumn(FileForCompare::getBeanName).setCaption("BeanName");
		this.gridLogsCompare.addColumn(FileForCompare::getAvgDuration).setCaption("Avg");
		this.gridLogsCompare.addColumn(FileForCompare::getInvokeCount).setCaption("Count");
		this.gridLogsCompare.addColumn(FileForCompare::getMaxDuration).setCaption("Max");
		this.gridLogsCompare.addColumn(FileForCompare::getMinDuration).setCaption("Min");
		this.gridLogsCompare.addColumn(FileForCompare::getSummaryTime).setCaption("Sum");
		this.gridLogsCompare.getDataProvider().refreshAll();
		return true;
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
		this.tfPassword = new PasswordField("Password", this.defaultPassword);
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
		Upload upload = new Upload("Component JAR", (fileName, miemType) -> receiveUpload(new String[]{fileName, miemType}));
		upload.setImmediateMode(false);
		upload.addSucceededListener(succeededEvent -> sucessUpload(new Object[]{succeededEvent}));
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

	private boolean downloadSshFile(boolean refresh) throws Exception {
		List<LogsMethod> logsMethod = this.logAnalyzeBusiness.logAnalyze(null, this.tfSshUserName.getValue(), this.tfSshPassword.getValue(), this.tfSshHost.getValue(), this.tfSshFile.getValue(),
				refresh, this.methodsToCompare);
		List<Grid.Column<LogsMethod, ?>> columns = this.gridLogsResult.getColumns();
		for (Grid.Column<LogsMethod, ?> c : columns) {
			this.gridLogsResult.removeColumn(c);
		}
		this.gridLogsResult.setCaption("Logs [" + logsMethod.size() + "]");
		this.gridLogsResult.setItems(logsMethod);
		this.gridLogsResult.setHeight(500, Unit.PIXELS);
		this.gridLogsResult.setWidth(1600, Unit.PIXELS);
		this.gridLogsResult.addColumn(LogsMethod::getBeanName).setCaption("Name");
		this.gridLogsResult.addColumn(LogsMethod::getAvgDuration).setCaption("Avg");
		this.gridLogsResult.addColumn(LogsMethod::getInvokeCount).setCaption("Count");
		this.gridLogsResult.addColumn(LogsMethod::getMaxDuration).setCaption("Max");
		this.gridLogsResult.addColumn(LogsMethod::getMinDuration).setCaption("Min");
		this.gridLogsResult.addColumn(LogsMethod::getSummaryTime).setCaption("Sum");
		this.gridLogsResult.getDataProvider().refreshAll();
		this.gridLogsResult.addItemClickListener(clickEvent -> showDataForBean(clickEvent));
		//
		this.showExceptions();
		return true;
	}

	private void showDataForBean(Grid.ItemClick<LogsMethod> clickEvent) {
		try {
			String beanName = clickEvent.getItem().getBeanName();
			List<LogsMethod> logsMethod = this.logAnalyzeBusiness.logAnalyze(beanName, this.tfSshUserName.getValue(), this.tfSshPassword.getValue(), this.tfSshHost.getValue(),
					this.tfSshFile.getValue(), false, this.methodsToCompare);
			List<Grid.Column<LogsMethod, ?>> columns = this.gridLogsResult.getColumns();
			for (Grid.Column<LogsMethod, ?> c : columns) {
				this.gridLogsResult.removeColumn(c);
			}
			this.gridLogsResult.setCaption("Logs [" + logsMethod.size() + "]");
			this.gridLogsResult.setItems(logsMethod);
			this.gridLogsResult.setHeight(500, Unit.PIXELS);
			this.gridLogsResult.setWidth(1600, Unit.PIXELS);
			this.gridLogsResult.addColumn(LogsMethod::getMethodName).setCaption("Name");
			this.gridLogsResult.addColumn(LogsMethod::getAvgDuration).setCaption("Avg");
			this.gridLogsResult.addColumn(LogsMethod::getInvokeCount).setCaption("Count");
			this.gridLogsResult.addColumn(LogsMethod::getMaxDuration).setCaption("Max");
			this.gridLogsResult.addColumn(LogsMethod::getMinDuration).setCaption("Min");
			this.gridLogsResult.addColumn(LogsMethod::getSummaryTime).setCaption("Sum");
			this.gridLogsResult.getDataProvider().refreshAll();
			this.gridLogsResult.addItemClickListener(clickEvent2 -> addToCompare(clickEvent2));
		} catch (Exception e) {
			showError(e);
		}
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
		List<Grid.Column<Map<String, Object>, ?>> columns = this.gridSqlResult.getColumns();
		for (Grid.Column<Map<String, Object>, ?> c : columns) {
			this.gridSqlResult.removeColumn(c);
		}
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
