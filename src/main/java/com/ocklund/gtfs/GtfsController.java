
package com.ocklund.gtfs;

import com.ocklund.gtfs.configuration.TimeProvider;
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
}
