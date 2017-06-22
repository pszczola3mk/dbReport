package pl.pszczola3mk.dbReport.controller;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
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

	@RequestMapping("/redirect")
	public void redirect(HttpServletResponse response, String url) throws IOException {
		if(StringUtils.isEmpty(url)){
			response.sendRedirect("http://www.google.com");
		}else{
			response.sendRedirect(url);
		}

	}

}
