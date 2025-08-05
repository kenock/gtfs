
package com.ocklund.gtfs;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class GtfsController {

    private final GtfsService gtfsService;

    public GtfsController(GtfsService gtfsService) {
        this.gtfsService = gtfsService;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(value = "darkMode", required = false, defaultValue = "false") boolean darkMode,
            Model model
    ) {
        LocalDateTime currentTime = LocalDateTime.now();
        //System.out.println("index(darkMode: " + darkMode + ", time: " + currentTime + ")");
        List<String> reports = gtfsService.getStopReports();
        model.addAttribute("reports", reports);
        model.addAttribute("darkMode", darkMode);
        model.addAttribute("currentTime", currentTime);
        return "index";
    }
}
