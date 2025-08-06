package com.ocklund.gtfs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GtfsControllerTest {

    @Mock
    private GtfsService gtfsService;
    @InjectMocks
    private GtfsController controller;

    @Mock
    private Model model;

    @Test
    void index_shouldWorkWithDarkModeTrue() {
        List<String> mockReports = Arrays.asList("Report 1", "Report 2", "Report 3", "Report 4");
        when(gtfsService.getStopReports()).thenReturn(mockReports);

        String viewName = controller.index(true, model);

        assertEquals("index", viewName, "View name should be 'index'");
        verify(model).addAttribute(eq("reports"), eq(mockReports));
        verify(model).addAttribute(eq("darkMode"), eq(true));
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(model).addAttribute(eq("currentTime"), timeCaptor.capture());
        assertInstanceOf(LocalDateTime.class, timeCaptor.getValue(), "Current time should be a LocalDateTime");
    }

    @Test
    void index_shouldWorkWithDarkModeFalse() {
        List<String> mockReports = Arrays.asList("Report 1", "Report 2", "Report 3", "Report 4");
        when(gtfsService.getStopReports()).thenReturn(mockReports);

        String viewName = controller.index(false, model);

        assertEquals("index", viewName, "View name should be 'index'");
        verify(model).addAttribute(eq("reports"), eq(mockReports));
        verify(model).addAttribute(eq("darkMode"), eq(false));
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(model).addAttribute(eq("currentTime"), timeCaptor.capture());
        assertInstanceOf(LocalDateTime.class, timeCaptor.getValue(), "Current time should be a LocalDateTime");
    }

    @Test
    void index_shouldWorkWithEmptyReports() {
        List<String> emptyReports = List.of();
        when(gtfsService.getStopReports()).thenReturn(emptyReports);

        String viewName = controller.index(false, model);

        assertEquals("index", viewName, "View name should be 'index'");
        verify(model).addAttribute(eq("reports"), eq(emptyReports));
        verify(model).addAttribute(eq("darkMode"), eq(false));
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(model).addAttribute(eq("currentTime"), timeCaptor.capture());
        assertInstanceOf(LocalDateTime.class, timeCaptor.getValue(), "Current time should be a LocalDateTime");
    }
}
