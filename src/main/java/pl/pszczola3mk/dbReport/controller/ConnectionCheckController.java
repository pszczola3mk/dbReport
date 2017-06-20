package pl.pszczola3mk.dbReport.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pszczola3mk.dbReport.business.ReportCreatorBusiness;

@RestController
@RequestMapping("/api")
public class ConnectionCheckController {

	@Autowired
	private ReportCreatorBusiness business;

	@RequestMapping("/checkConnection")
	public String checkConnection() {
		return "Success";
	}
	@RequestMapping("/checkConnectionDB")
	public String checkConnectionDB(String url, String userName, String password) {
		this.business.checkConnection(url,userName,password);
		return "DB Success";
	}
}
