package com.eegalepoint.citybus.passenger;

import com.eegalepoint.citybus.domain.UserEntity;
import com.eegalepoint.citybus.domain.messaging.MessageEntity;
import com.eegalepoint.citybus.domain.messaging.MessageQueueEntity;
import com.eegalepoint.citybus.domain.messaging.MessageQueueRepository;
import com.eegalepoint.citybus.domain.messaging.MessageRepository;
import com.eegalepoint.citybus.domain.passenger.CheckinEntity;
import com.eegalepoint.citybus.domain.passenger.CheckinRepository;
import com.eegalepoint.citybus.domain.passenger.ReminderPreferenceEntity;
import com.eegalepoint.citybus.domain.passenger.ReminderPreferenceRepository;
import com.eegalepoint.citybus.domain.passenger.ReservationEntity;
import com.eegalepoint.citybus.domain.passenger.ReservationRepository;
import com.eegalepoint.citybus.domain.transit.ScheduleEntity;
import com.eegalepoint.citybus.domain.transit.ScheduleRepository;
import com.eegalepoint.citybus.domain.transit.StopEntity;
import com.eegalepoint.citybus.domain.transit.StopRepository;
import com.eegalepoint.citybus.domain.transit.StopVersionEntity;
import com.eegalepoint.citybus.domain.transit.StopVersionRepository;
import com.eegalepoint.citybus.passenger.dto.CheckinResponse;
import com.eegalepoint.citybus.passenger.dto.CreateCheckinRequest;
import com.eegalepoint.citybus.passenger.dto.CreateReservationRequest;
import com.eegalepoint.citybus.passenger.dto.ReminderPreferenceResponse;
import com.eegalepoint.citybus.passenger.dto.ReservationResponse;
import com.eegalepoint.citybus.passenger.dto.UpdateReminderPreferenceRequest;
import com.eegalepoint.citybus.passenger.dto.UpdateReservationRequest;
import com.eegalepoint.citybus.repo.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PassengerService {

  private final ReservationRepository reservationRepository;
  private final CheckinRepository checkinRepository;
  private final ReminderPreferenceRepository reminderPreferenceRepository;
  private final ScheduleRepository scheduleRepository;
  private final StopRepository stopRepository;
  private final StopVersionRepository stopVersionRepository;
  private final UserRepository userRepository;
  private final MessageRepository messageRepository;
  private final MessageQueueRepository messageQueueRepository;

  public PassengerService(
      ReservationRepository reservationRepository,
      CheckinRepository checkinRepository,
      ReminderPreferenceRepository reminderPreferenceRepository,
      ScheduleRepository scheduleRepository,
      StopRepository stopRepository,
      StopVersionRepository stopVersionRepository,
      UserRepository userRepository,
      MessageRepository messageRepository,
      MessageQueueRepository messageQueueRepository) {
    this.reservationRepository = reservationRepository;
    this.checkinRepository = checkinRepository;
    this.reminderPreferenceRepository = reminderPreferenceRepository;
    this.scheduleRepository = scheduleRepository;
    this.stopRepository = stopRepository;
    this.stopVersionRepository = stopVersionRepository;
    this.userRepository = userRepository;
    this.messageRepository = messageRepository;
    this.messageQueueRepository = messageQueueRepository;
  }

  @Transactional
  public ReservationResponse createReservation(CreateReservationRequest req) {
    UserEntity user = currentUser();
    ScheduleEntity schedule = scheduleRepository.findById(req.scheduleId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
    StopEntity stop = stopRepository.findById(req.stopId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stop not found"));

    ReservationEntity reservation = reservationRepository.save(
        new ReservationEntity(user, schedule, stop));

    String stopName = latestStopName(stop.getId());
    MessageEntity msg = messageRepository.save(new MessageEntity(
        user,
        "Reservation created",
        "Your reservation for trip " + schedule.getTripCode()
            + " at stop " + stop.getCode() + " (" + stopName + ") has been created."));
    messageQueueRepository.save(new MessageQueueEntity(msg));

    return toReservationResponse(reservation, schedule, stop, stopName);
  }

  @Transactional(readOnly = true)
  public List<ReservationResponse> listReservations() {
    UserEntity user = currentUser();
    return reservationRepository.findByUser_IdOrderByReservedAtDesc(user.getId()).stream()
        .map(r -> {
          ScheduleEntity sch = r.getSchedule();
          StopEntity st = r.getStop();
          return toReservationResponse(r, sch, st, latestStopName(st.getId()));
        })
        .toList();
  }

  @Transactional
  public ReservationResponse updateReservation(long id, UpdateReservationRequest req) {
    UserEntity user = currentUser();
    ReservationEntity reservation = reservationRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
    if (!reservation.getUser().getId().equals(user.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your reservation");
    }
    reservation.setStatus(req.status());
    reservationRepository.save(reservation);

    ScheduleEntity sch = reservation.getSchedule();
    StopEntity st = reservation.getStop();
    String stopName = latestStopName(st.getId());

    MessageEntity msg = messageRepository.save(new MessageEntity(
        user,
        "Reservation " + req.status().toLowerCase(),
        "Your reservation #" + reservation.getId() + " for trip " + sch.getTripCode()
            + " at " + st.getCode() + " has been " + req.status().toLowerCase() + "."));
    messageQueueRepository.save(new MessageQueueEntity(msg));

    return toReservationResponse(reservation, sch, st, stopName);
  }

  @Transactional
  public CheckinResponse createCheckin(CreateCheckinRequest req) {
    UserEntity user = currentUser();
    StopEntity stop = stopRepository.findById(req.stopId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stop not found"));

    ReservationEntity reservation = null;
    if (req.reservationId() != null) {
      reservation = reservationRepository.findById(req.reservationId())
          .orElseThrow(
              () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
      if (!reservation.getUser().getId().equals(user.getId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your reservation");
      }
    }

    CheckinEntity checkin = checkinRepository.save(new CheckinEntity(user, reservation, stop));
    String stopName = latestStopName(stop.getId());
    return toCheckinResponse(checkin, stop, stopName);
  }

  @Transactional(readOnly = true)
  public List<CheckinResponse> listCheckins() {
    UserEntity user = currentUser();
    return checkinRepository.findByUser_IdOrderByCheckedInAtDesc(user.getId()).stream()
        .map(c -> {
          StopEntity st = c.getStop();
          return toCheckinResponse(c, st, latestStopName(st.getId()));
        })
        .toList();
  }

  @Transactional(readOnly = true)
  public ReminderPreferenceResponse getReminderPreferences() {
    UserEntity user = currentUser();
    return reminderPreferenceRepository.findByUser_Id(user.getId())
        .map(p -> new ReminderPreferenceResponse(p.isEnabled(), p.getMinutesBefore(), p.getChannel()))
        .orElse(new ReminderPreferenceResponse(true, 15, "IN_APP"));
  }

  @Transactional
  public ReminderPreferenceResponse updateReminderPreferences(UpdateReminderPreferenceRequest req) {
    UserEntity user = currentUser();
    ReminderPreferenceEntity pref = reminderPreferenceRepository.findByUser_Id(user.getId())
        .orElseGet(() -> new ReminderPreferenceEntity(user));
    pref.setEnabled(req.enabled());
    pref.setMinutesBefore(req.minutesBefore());
    pref.setChannel(req.channel());
    reminderPreferenceRepository.save(pref);
    return new ReminderPreferenceResponse(pref.isEnabled(), pref.getMinutesBefore(), pref.getChannel());
  }

  private UserEntity currentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
  }

  private String latestStopName(long stopId) {
    return stopVersionRepository.findFirstByStop_IdOrderByVersionNumberDesc(stopId)
        .map(StopVersionEntity::getName)
        .orElse("Unknown");
  }

  private ReservationResponse toReservationResponse(
      ReservationEntity r, ScheduleEntity sch, StopEntity st, String stopName) {
    return new ReservationResponse(
        r.getId(),
        sch.getId(),
        sch.getTripCode(),
        sch.getDepartureTime(),
        st.getId(),
        st.getCode(),
        stopName,
        r.getStatus(),
        r.getReservedAt(),
        r.getUpdatedAt());
  }

  private CheckinResponse toCheckinResponse(CheckinEntity c, StopEntity st, String stopName) {
    return new CheckinResponse(
        c.getId(),
        st.getId(),
        st.getCode(),
        stopName,
        c.getReservation() != null ? c.getReservation().getId() : null,
        c.getCheckedInAt());
  }
}
