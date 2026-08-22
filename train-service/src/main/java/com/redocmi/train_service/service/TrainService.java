package com.redocmi.train_service.service;

import com.redocmi.train_service.dto.request.CreateScheduleRequest;
import com.redocmi.train_service.dto.request.CreateTrainRequest;
import com.redocmi.train_service.dto.response.ScheduleResponse;
import com.redocmi.train_service.dto.response.SeatResponse;
import com.redocmi.train_service.dto.response.TrainResponse;
import com.redocmi.train_service.dto.response.TrainSearchResponse;
import com.redocmi.train_service.entity.Schedule;
import com.redocmi.train_service.entity.Seat;
import com.redocmi.train_service.entity.Train;
import com.redocmi.train_service.exception.DuplicateTrainException;
import com.redocmi.train_service.exception.ResourceNotFoundException;
import com.redocmi.train_service.exception.SeatNotAvailableException;
import com.redocmi.train_service.repository.ScheduleRepository;
import com.redocmi.train_service.repository.SeatRepository;
import com.redocmi.train_service.repository.TrainRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// TODO [Phase 3]: Implement multi-stop route support. Currently trains have a fixed
// source and destination with no intermediate stops. A real implementation would
// introduce a Route/Station entity with ordered stops, allowing users to search
// for trains passing through their source and destination — not just end-to-end journeys.
// e.g. Delhi → Kanpur → Lucknow → Varanasi → Ranchi as a single train.

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainService {
    private final TrainRepository trainRepository;
    private final ScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public TrainResponse createTrain(CreateTrainRequest request) {
        if(trainRepository.existsByTrainNumber(request.getTrainNumber())) {
            throw new DuplicateTrainException(
                    "Train with number " + request.getTrainNumber() + " already exists"
            );
        }

        Train train = Train.builder()
                .trainNumber(request.getTrainNumber())
                .name(request.getName())
                .source(request.getSource())
                .destination(request.getDestination())
                .totalSeats(request.getTotalSeats())
                .build();

        Train savedTrain = trainRepository.save(train);
        log.info("Created train: {} at {}", savedTrain.getTrainNumber(), savedTrain.getCreatedAt());

        return mapToTrainResponse(savedTrain);
    }

    public TrainResponse getTrainById(UUID id) {
        Train train = trainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Train with ID " + id + " does not exist."));

        return mapToTrainResponse(train);
    }

    public List<TrainResponse> getAllTrains() {
        return trainRepository.findAll()
                .stream()
                .map(this::mapToTrainResponse)
                .toList();
    }

    @Transactional
    public ScheduleResponse createSchedule(CreateScheduleRequest request) {
        Train train = trainRepository.findById(request.getTrainId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Train with id: " + request.getTrainId() + " does not exist."
                ));

        log.info("Train fetched {}", train.getTrainNumber());

        Schedule schedule = Schedule.builder()
                .train(train)
                .travelDate(request.getTravelDate())
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .price(request.getPrice())
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);

        // TODO [Phase 3]: Replace fixed 50/50 SLEEPER/AC split with a CoachConfiguration
        // entity defined per train. Currently totalSeats is divided equally between both
        // classes as a simplification. A real implementation would source seat class
        // distribution from train-specific coach layout data.
        // For now we are just generating half seats each for SLEEPER and AC.

        // auto-generate seats:
        List<Seat> seats = Stream.of(Seat.SeatClass.SLEEPER, Seat.SeatClass.AC)
                .flatMap(seatClass -> IntStream.rangeClosed(1, train.getTotalSeats()/2)
                        .mapToObj(seatNumber -> Seat.builder()
                                .train(train)
                                .schedule(savedSchedule)
                                .seatNumber((seatClass == Seat.SeatClass.SLEEPER ? "SL-" : "AC-")
                                    + String.format("%02d", seatNumber))
                                .seatClass(seatClass)
                                .status(Seat.SeatStatus.AVAILABLE)
                                .build()))
                .toList();

        seatRepository.saveAll(seats);
        log.info("Created schedule {} with {} seats for train {}",
                savedSchedule.getId(), seats.size(), train.getTrainNumber());

        return mapToScheduleResponse(savedSchedule);
    }

    public ScheduleResponse getScheduleById(UUID id) {
//        we will need to use findByIdWithTrain because train is lazy fetch
//        in schedule entity and so by the time we actually need the train
//        to map it to the response dto, the jwt session might already have
//        closed, giving us internal error.
        Schedule schedule = scheduleRepository.findByIdWithTrain(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule with id: " + id + " does not exist."));

        return mapToScheduleResponse(schedule);
    }

    private TrainResponse mapToTrainResponse(Train train) {
        return TrainResponse.builder()
                .id(train.getId())
                .trainNumber(train.getTrainNumber())
                .name(train.getName())
                .source(train.getSource())
                .destination(train.getDestination())
                .totalSeats(train.getTotalSeats())
                .build();
    }

    private ScheduleResponse mapToScheduleResponse(Schedule schedule) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .trainId(schedule.getTrain().getId())
                .trainName(schedule.getTrain().getName())
                .trainNumber(schedule.getTrain().getTrainNumber())
                .travelDate(schedule.getTravelDate())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .price(schedule.getPrice())
                .build();
    }

    public List<TrainSearchResponse> searchTrains(String source, String destination, LocalDate travelDate) {
        List<Schedule> schedules = scheduleRepository.searchSchedules(source, destination, travelDate);

        return schedules.stream()
                .map(schedule -> {
                    Long availableSeats = seatRepository.countAvailableSeatsByScheduleId(schedule.getId());

                    return TrainSearchResponse.builder()
                            .scheduleId(schedule.getId())
                            .trainId(schedule.getTrain().getId())
                            .trainNumber(schedule.getTrain().getTrainNumber())
                            .trainName(schedule.getTrain().getName())
                            .source(schedule.getTrain().getSource())
                            .destination(schedule.getTrain().getDestination())
                            .travelDate(schedule.getTravelDate())
                            .departureTime(schedule.getDepartureTime())
                            .arrivalTime(schedule.getArrivalTime())
                            .price(schedule.getPrice())
                            .availableSeats(availableSeats)
                            .build();
                })
                .toList();
    }

    public List<SeatResponse> getSeatsByScheduleId(UUID scheduleId) {
        // verify if the schedule exists:
        scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("shcedule with id " + scheduleId + " does not exist"));

        return seatRepository.findByScheduleIdWithDetails(scheduleId)
                .stream()
                .map(this::mapToSeatResponse)
                .toList();
    }

    @Transactional
    public SeatResponse lockSeat(UUID seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat with id: " + seatId + " does not exist"));

        if(seat.getStatus() != Seat.SeatStatus.AVAILABLE)
            throw new SeatNotAvailableException(
                    "Seat " + seat.getSeatNumber() + " is not available."
            );

        seat.setStatus(Seat.SeatStatus.LOCKED);
        seatRepository.save(seat);
        log.info("Seat {} locked successfully.", seat.getSeatNumber());

        return mapToSeatResponse(seat);
    }

    @Transactional
    public SeatResponse confirmSeat(UUID seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat with id: " + seatId + " does not exist."));

        seat.setStatus(Seat.SeatStatus.BOOKED);
        seatRepository.save(seat);
        log.info("Seat {} confirmed successfully.", seat.getSeatNumber());

        return mapToSeatResponse(seat);
    }

    @Transactional
    public SeatResponse releaseSeat(UUID seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat with id: " + seatId + " does not exist."));

        seat.setStatus(Seat.SeatStatus.AVAILABLE);
        seatRepository.save(seat);
        log.info("Seat {} released successfully", seat.getSeatNumber());

        return mapToSeatResponse(seat);
    }

    private SeatResponse mapToSeatResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .seatClass(seat.getSeatClass().name())
                .status(seat.getStatus().name())
                .build();
    }
}
