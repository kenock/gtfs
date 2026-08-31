
package com.ocklund.gtfs;

import com.ocklund.gtfs.configuration.TimeProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Controller
public class GtfsController {

    private final GtfsService gtfsService;
    private final TimeProvider timeProvider;
    private static final ZoneId STOCKHOLM_ZONE = ZoneId.of("Europe/Stockholm");

    public GtfsController(GtfsService gtfsService, TimeProvider timeProvider) {
        this.gtfsService = gtfsService;
        this.timeProvider = timeProvider;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(value = "darkMode", required = false, defaultValue = "false") boolean darkMode,
            Model model
    ) {
        LocalDateTime currentTime = timeProvider.now(STOCKHOLM_ZONE);
        //System.out.println("index(darkMode: " + darkMode + ", time: " + currentTime + ")");
        List<String> reports = gtfsService.getStopReports();
        model.addAttribute("reports", reports);
        model.addAttribute("darkMode", darkMode);
        model.addAttribute("currentTime", currentTime);
        return "index";
    }

    /**
     * Returns just the departures grid so the page can poll for updates without
     * re-downloading the stylesheet and logo on every refresh.
     */
    @GetMapping("/departures")
    public String departures(Model model, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        model.addAttribute("reports", gtfsService.getStopReports());
        model.addAttribute("currentTime", timeProvider.now(STOCKHOLM_ZONE));
        return "index :: departures";
    }
}
