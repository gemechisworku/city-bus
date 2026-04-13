package com.eegalepoint.citybus.passenger;

import com.eegalepoint.citybus.passenger.dto.CheckinResponse;
import com.eegalepoint.citybus.passenger.dto.CreateCheckinRequest;
import com.eegalepoint.citybus.passenger.dto.CreateDndWindowRequest;
import com.eegalepoint.citybus.passenger.dto.CreateReservationRequest;
import com.eegalepoint.citybus.passenger.dto.DndWindowResponse;
import com.eegalepoint.citybus.passenger.dto.ReminderPreferenceResponse;
import com.eegalepoint.citybus.passenger.dto.ReservationResponse;
import com.eegalepoint.citybus.passenger.dto.UpdateReminderPreferenceRequest;
import com.eegalepoint.citybus.passenger.dto.UpdateReservationRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/passenger", produces = MediaType.APPLICATION_JSON_VALUE)
public class PassengerController {

  private final PassengerService passengerService;

  public PassengerController(PassengerService passengerService) {
    this.passengerService = passengerService;
  }

  @PostMapping(path = "/reservations", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public ReservationResponse createReservation(@Valid @RequestBody CreateReservationRequest req) {
    return passengerService.createReservation(req);
  }

  @GetMapping("/reservations")
  @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
  public List<ReservationResponse> listReservations() {
    return passengerService.listReservations();
  }

  @PutMapping(path = "/reservations/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
  public ReservationResponse updateReservation(
      @PathVariable("id") long id, @Valid @RequestBody UpdateReservationRequest req) {
    return passengerService.updateReservation(id, req);
  }

  @PostMapping(path = "/checkins", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public CheckinResponse createCheckin(@Valid @RequestBody CreateCheckinRequest req) {
    return passengerService.createCheckin(req);
  }

  @GetMapping("/checkins")
  @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
  public List<CheckinResponse> listCheckins() {
    return passengerService.listCheckins();
  }

  @GetMapping("/reminder-preferences")
  @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
  public ReminderPreferenceResponse getReminderPreferences() {
    return passengerService.getReminderPreferences();
  }

  @PutMapping(path = "/reminder-preferences", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
  public ReminderPreferenceResponse updateReminderPreferences(
      @Valid @RequestBody UpdateReminderPreferenceRequest req) {
    return passengerService.updateReminderPreferences(req);
  }

  @GetMapping("/dnd-windows")
  @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
  public List<DndWindowResponse> listDndWindows() {
    return passengerService.listDndWindows();
  }

  @PostMapping(path = "/dnd-windows", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public DndWindowResponse createDndWindow(@Valid @RequestBody CreateDndWindowRequest req) {
    return passengerService.createDndWindow(req);
  }

  @DeleteMapping("/dnd-windows/{id}")
  @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDndWindow(@PathVariable("id") long id) {
    passengerService.deleteDndWindow(id);
  }
}
