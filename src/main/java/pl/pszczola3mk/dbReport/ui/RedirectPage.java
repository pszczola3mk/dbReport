package pl.pszczola3mk.dbReport.ui;

import com.vaadin.ui.HorizontalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.UI;

@SpringUI(path = "/ui/redirect")
public class RedirectPage extends UI {

	private static final Logger log = LoggerFactory.getLogger(RedirectPage.class);

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		HorizontalLayout layout = new HorizontalLayout();
		BrowserFrame browser = new BrowserFrame("Browser", new ExternalResource("http://pg.edu.pl"));
		browser.setWidth("1200px");
		browser.setHeight("800px");
		layout.addComponent(browser);
		setContent(layout);
	}
}
