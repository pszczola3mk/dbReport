package pl.pszczola3mk.dbReport.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ConnectionCheckController {

	@RequestMapping("/checkConnection")
	public String checkConnection() {
		return "Success";
	}
}
