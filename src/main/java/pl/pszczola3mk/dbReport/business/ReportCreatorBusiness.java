package pl.pszczola3mk.dbReport.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.pszczola3mk.dbReport.service.ReportCreatorDBService;

@Component
public class ReportCreatorBusiness {



	@Autowired
	private ReportCreatorDBService service;

	public void checkConnection() {
		this.service.checkConnection();
	}
}
