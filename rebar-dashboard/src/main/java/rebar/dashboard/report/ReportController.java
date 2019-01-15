package rebar.dashboard.report;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Component
@Controller
public class ReportController {

	@Autowired
	ReportManager reportManager;
	
	public ReportController() {
		// TODO Auto-generated constructor stub
	}
	@RequestMapping(value = "/reports", method = { RequestMethod.GET })
	public ModelAndView home(HttpServletRequest request) {
		
	
		return new ModelAndView("reports");
	}
	
	
	ReportManager getReportManager() {
		return reportManager;
	}
}
