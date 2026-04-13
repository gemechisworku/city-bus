package com.eegalepoint.citybus.ingestion;

import com.eegalepoint.citybus.transit.dto.CanonicalImportRequest;
import com.eegalepoint.citybus.transit.dto.RouteImportDto;
import com.eegalepoint.citybus.transit.dto.ScheduleImportDto;
import com.eegalepoint.citybus.transit.dto.StopImportDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Parses structured HTML content into canonical import requests.
 * Extracts route, stop, and schedule data from HTML table structures.
 */
@Service
public class HtmlImportService {

  private static final Logger log = LoggerFactory.getLogger(HtmlImportService.class);

  private static final Pattern TABLE_ROW = Pattern.compile(
      "<tr[^>]*>(.*?)</tr>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  private static final Pattern TABLE_CELL = Pattern.compile(
      "<t[dh][^>]*>(.*?)</t[dh]>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

  public CanonicalImportRequest parseHtml(String html, String templateName) {
    log.info("Parsing HTML content with template '{}'", templateName);

    List<List<String>> rows = extractTableRows(html);
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("No table data found in HTML content");
    }

    List<RouteImportDto> routes = new ArrayList<>();
    RouteImportDto currentRoute = null;
    List<StopImportDto> currentStops = new ArrayList<>();
    List<ScheduleImportDto> currentSchedules = new ArrayList<>();
    int stopSeq = 0;

    for (List<String> cells : rows) {
      if (cells.size() < 2) continue;
      String firstCell = cells.get(0).trim().toLowerCase();

      if (firstCell.contains("route") || firstCell.contains("线路")) {
        if (currentRoute != null) {
          routes.add(new RouteImportDto(
              currentRoute.routeCode(), currentRoute.name(),
              currentRoute.effectiveFrom(), currentStops, currentSchedules));
        }
        String code = cells.size() > 1 ? stripHtml(cells.get(1)) : "UNKNOWN";
        String name = cells.size() > 2 ? stripHtml(cells.get(2)) : code;
        currentRoute = new RouteImportDto(code, name, LocalDate.now(), List.of(), List.of());
        currentStops = new ArrayList<>();
        currentSchedules = new ArrayList<>();
        stopSeq = 0;
      } else if (firstCell.contains("stop") || firstCell.contains("站")) {
        stopSeq++;
        String stopCode = cells.size() > 1 ? stripHtml(cells.get(1)) : "S-" + stopSeq;
        String stopName = cells.size() > 2 ? stripHtml(cells.get(2)) : stopCode;
        BigDecimal lat = cells.size() > 3 ? parseBigDecimal(cells.get(3)) : null;
        BigDecimal lon = cells.size() > 4 ? parseBigDecimal(cells.get(4)) : null;
        currentStops.add(new StopImportDto(
            stopCode, stopName, lat, lon, LocalDate.now(), stopSeq));
      } else if (firstCell.contains("schedule") || firstCell.contains("时刻")) {
        String tripCode = cells.size() > 1 ? stripHtml(cells.get(1)) : "T-" + currentSchedules.size();
        LocalTime time = cells.size() > 2 ? parseTime(cells.get(2)) : LocalTime.of(8, 0);
        currentSchedules.add(new ScheduleImportDto(tripCode, time));
      }
    }

    if (currentRoute != null) {
      routes.add(new RouteImportDto(
          currentRoute.routeCode(), currentRoute.name(),
          currentRoute.effectiveFrom(), currentStops, currentSchedules));
    }

    log.info("Parsed {} routes from HTML", routes.size());
    return new CanonicalImportRequest(templateName, routes);
  }

  private List<List<String>> extractTableRows(String html) {
    List<List<String>> result = new ArrayList<>();
    Matcher rowMatcher = TABLE_ROW.matcher(html);
    while (rowMatcher.find()) {
      String rowContent = rowMatcher.group(1);
      List<String> cells = new ArrayList<>();
      Matcher cellMatcher = TABLE_CELL.matcher(rowContent);
      while (cellMatcher.find()) {
        cells.add(stripHtml(cellMatcher.group(1)));
      }
      if (!cells.isEmpty()) {
        result.add(cells);
      }
    }
    return result;
  }

  private String stripHtml(String text) {
    return HTML_TAG.matcher(text).replaceAll("").trim();
  }

  private BigDecimal parseBigDecimal(String text) {
    try {
      return new BigDecimal(stripHtml(text).trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private LocalTime parseTime(String text) {
    try {
      return LocalTime.parse(stripHtml(text).trim());
    } catch (Exception e) {
      return LocalTime.of(8, 0);
    }
  }
}
