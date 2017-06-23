package pl.pszczola3mk.dbReport.model;

import java.util.concurrent.Callable;
import com.vaadin.ui.TextField;
import lombok.Data;
import pl.pszczola3mk.dbReport.business.ReportCreatorBusiness;

@Data
public class User implements Callable<Boolean> {

	private TextField url;
	private TextField userName;
	private TextField password;
	private ReportCreatorBusiness business;

	public User(TextField url, TextField userName, TextField password, ReportCreatorBusiness business) {
		this.url = url;
		this.userName = userName;
		this.password = password;
		this.business = business;
	}

	@Override
	public Boolean call() throws Exception {
		business.checkConnection(this.url.getValue(), this.userName.getValue(), this.password.getValue());
		return true;
	}
}
