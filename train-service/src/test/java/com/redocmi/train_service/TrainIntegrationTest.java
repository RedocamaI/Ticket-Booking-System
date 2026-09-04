package com.redocmi.train_service;

import com.jayway.jsonpath.JsonPath;
import com.redocmi.train_service.entity.Seat;
import com.redocmi.train_service.repository.ScheduleRepository;
import com.redocmi.train_service.repository.SeatRepository;
import com.redocmi.train_service.repository.TrainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class TrainIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("train_test_db")
                    .withUsername("redocmi")
                    .withPassword("password123");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private SeatRepository seatRepository;

    @BeforeEach
    void setup() {
        seatRepository.deleteAll();
        scheduleRepository.deleteAll();
        trainRepository.deleteAll();
    }

    @Test
    void createSchedule_shouldAutoGenerateSeats() throws Exception {
//        Step 1: create train
        MvcResult trainResult = mockMvc.perform(post("/api/admin/create-train")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "trainNumber": "712001",
                            "name": "Redocmi Express",
                            "source": "Delhi",
                            "destination": "Dhanbad",
                            "totalSeats": 60
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String trainId = JsonPath.read(
                trainResult.getResponse().getContentAsString(),
                "$.data.id");

//        Step 2: create schedule
        MvcResult scheduleResponse = mockMvc.perform(post("/api/admin/create-schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "trainId": "%s",
                            "travelDate": "2026-10-01",
                            "departureTime": "16:00:00",
                            "arrivalTime": "08:00:00",
                            "price": 1500.00
                        }
                        """.formatted(trainId)))
                .andExpect(status().isCreated())
                .andReturn();

        String scheduleId = JsonPath.read(
                scheduleResponse.getResponse().getContentAsString(),
                "$.data.id");

//        Step 3: verify 60 seats generated(hard coded value taken for now).
//        will be configurable by the admin in create train in the future updates.
        List<Seat> seats = seatRepository.findByScheduleId(UUID.fromString(scheduleId));
        assertThat(seats).hasSize(60);

//        Verify 30 sleeper and 30 ac:
        long sleeperCount = seats.stream()
                .filter(s -> s.getSeatClass() == Seat.SeatClass.SLEEPER)
                .count();
        long acCount = seats.stream()
                .filter(s -> s.getSeatClass() == Seat.SeatClass.AC)
                .count();
        assertThat(sleeperCount).isEqualTo(30);
        assertThat(acCount).isEqualTo(30);

//        Verify all seats are AVAILABLE:
        long availableCount = seats.stream()
                .filter(s -> s.getStatus() == Seat.SeatStatus.AVAILABLE)
                .count();
        assertThat(availableCount).isEqualTo(60);
    }

    @Test
    void searchTrains_shouldReturnResults_whenMatchFound() throws Exception {
//        Step 1: create train
        MvcResult trainResponse = mockMvc.perform(post("/api/admin/create-train")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "trainNumber": "712001",
                            "name": "Redocmi Express",
                            "source": "Delhi",
                            "destination": "Dhanbad",
                            "totalSeats": 60
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String trainId = JsonPath.read(
                trainResponse.getResponse().getContentAsString(),
                "$.data.id");

//        Step 2: create a schedule
        mockMvc.perform(post("/api/admin/create-schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "trainId": "%s",
                            "travelDate": "2026-10-01",
                            "departureTime": "16:00:00",
                            "arrivalTime": "08:00:00",
                            "price": 1500.00
                        }
                        """.formatted(trainId)))
                .andExpect(status().isCreated());

//        Step 3: search for the train
        mockMvc.perform(get("/api/trains/search")
                .param("source", "Delhi")
                .param("destination", "Dhanbad")
                .param("date", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].availableSeats").value(60));
    }
}
