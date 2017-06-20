package pl.pszczola3mk.dbReport.model;

import java.util.concurrent.Callable;
import lombok.Data;
import pl.pszczola3mk.dbReport.business.ReportCreatorBusiness;

@Data
public class User implements Callable<Boolean> {

	private String url;
	private String userName;
	private String password;
	private ReportCreatorBusiness business;

	public User(String url, String userName, String password, ReportCreatorBusiness business) {
		this.url = url;
		this.userName = userName;
		this.password = password;
		this.business = business;
	}

	@Override
	public Boolean call() throws Exception {
		business.checkConnection(this.url, this.userName, this.password);
		return true;
	}
}
